"""Testes sem acesso a configuração, dados ou repositórios de produção."""
import base64
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import uuid

SPEC = importlib.util.spec_from_file_location(
    "backup", Path(__file__).resolve().parents[1] / "financebot_backup.py")
b = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(b)


def keys():
    result = dict.fromkeys(b.KEY_NAMES, "")
    result[b.KEY_NAMES[0]] = base64.b64encode(b"a" * 32).decode()
    return result


class BackupTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.password = self.root / "password"
        self.password.write_text("fixture-only-password")
        self.password.chmod(0o600)
        self.cfg = dict(repository="s3:https://fixture.invalid/bucket", password_file=str(self.password),
                        source_container="api", postgres_container="db", backup_host="fixture",
                        state_dir=str(self.root / "state"), staging_root=str(self.root), max_dump_mib=1)
        self.op = b.Operation(self.cfg)

    def tearDown(self):
        self.tmp.cleanup()

    def config(self):
        path = self.root / "config.json"
        path.write_text(json.dumps(self.cfg))
        path.chmod(0o600)
        return path

    def test_config_rejects_local_repository_and_public_password(self):
        self.cfg["repository"] = str(self.root / "repo")
        with self.assertRaises(b.BackupError):
            b.load_config(self.config())
        self.cfg["repository"] = "sftp:fixture:/repo"
        self.password.chmod(0o644)
        with self.assertRaises(b.BackupError):
            b.load_config(self.config())

    def test_config_rejects_symlink_and_invalid_identifiers(self):
        path = self.config()
        link = self.root / "symlink"
        link.symlink_to(path)
        with self.assertRaises(b.BackupError):
            b.load_config(link)
        self.cfg["postgres_container"] = "--privileged"
        with self.assertRaises(b.BackupError):
            b.load_config(self.config())

    def test_previous_keyring_uses_api_equals_format(self):
        ring = keys()
        ring[b.KEY_NAMES[3]] = "old=" + ring[b.KEY_NAMES[0]]
        b.validate_keyring(ring)
        ring[b.KEY_NAMES[3]] = "old:invalid"
        with self.assertRaises(b.BackupError):
            b.validate_keyring(ring)

    def test_partial_dump_and_oversize_fail(self):
        import sys
        for program, limit in (("import sys; print('partial'); sys.exit(42)", 1024),
                               ("print('x'*1024)", 10)):
            path = self.root / uuid.uuid4().hex
            with self.assertRaises(b.BackupError):
                self.op.write_command([sys.executable, "-c", program], path, limit)

    def test_low_space_or_non_tmpfs_prevents_repository_access(self):
        with patch.object(b, "run", return_value=b"ext4\n"), patch.object(self.op, "restic") as restic:
            with self.assertRaises(b.BackupError):
                self.op.preflight()
            restic.assert_not_called()
        usage = shutil._ntuple_diskusage(100, 99, 1)
        with patch.object(b, "run", return_value=b"tmpfs\n"), \
                patch.object(b.shutil, "disk_usage", return_value=usage), \
                patch.object(self.op, "restic") as restic:
            with self.assertRaises(b.BackupError):
                self.op.preflight()
            restic.assert_not_called()

    def test_backup_failure_never_runs_retention(self):
        with patch.object(self.op, "preflight"), patch.object(self.op, "keyring", return_value=("id", keys())), \
                patch.object(self.op, "dump", side_effect=b.BackupError("failed")), \
                patch.object(self.op, "restic") as restic:
            with self.assertRaises(b.BackupError):
                self.op.backup()
            restic.assert_not_called()
        self.assertEqual([], list(self.root.glob("financebot-*")))

    def test_rotation_mid_dump_aborts_before_upload(self):
        with patch.object(self.op, "preflight"), \
                patch.object(self.op, "keyring", side_effect=[("before", keys()), ("after", keys())]), \
                patch.object(self.op, "dump"), patch.object(self.op, "restic") as restic:
            with self.assertRaises(b.BackupError):
                self.op.backup()
            restic.assert_not_called()

    def test_failed_restore_never_marks_verified_or_runs_retention(self):
        def dump(directory):
            (directory / "postgres.dump").write_bytes(b"fixture")
        snapshot = "a" * 64
        response = json.dumps(dict(message_type="summary", snapshot_id=snapshot)).encode()
        with patch.object(self.op, "preflight"), patch.object(self.op, "keyring", return_value=("id", keys())), \
                patch.object(self.op, "dump", side_effect=dump), \
                patch.object(self.op, "restore_check", side_effect=b.BackupError("restore failed")), \
                patch.object(self.op, "restic", return_value=response) as restic:
            with self.assertRaises(b.BackupError):
                self.op.backup()
            self.assertEqual(["backup"], [call.args[0] for call in restic.call_args_list])
        self.assertEqual([], list(self.root.glob("financebot-*")))

    def test_retention_is_filtered_and_dry_run_by_default(self):
        with patch.object(self.op, "restic") as restic:
            self.op.retention()
            args = restic.call_args.args
            self.assertIn("--dry-run", args)
            self.assertNotIn("--prune", args)
            self.assertEqual(b.VERIFIED, args[args.index("--tag") + 1])
            self.assertEqual("fixture", args[args.index("--host") + 1])
        with patch.object(self.op, "restic") as restic:
            self.op.retention(True)
            self.assertIn("--prune", restic.call_args_list[0].args)
            self.assertEqual(("check",), restic.call_args_list[1].args)

    def test_restore_refuses_ambiguous_snapshot_or_foreign_host(self):
        with self.assertRaises(b.BackupError):
            self.op.restore_check("latest", self.root)
        with patch.object(self.op, "restic", return_value=b'[{"hostname":"other","tags":[]}]'):
            with self.assertRaises(b.BackupError):
                self.op.restore_check("a" * 64, self.root)

    def test_corrupted_recovered_dump_never_reaches_postgres(self):
        snapshot = "b" * 64
        saved = [{"hostname": "fixture", "tags": [b.TAG], "paths": ["/fixture"]}]
        def recover(args, destination, limit, env=None):
            if destination.name == "keyring.json":
                destination.write_text(json.dumps(keys()))
            elif destination.name == "postgres.dump":
                destination.write_bytes(b"corrupted")
            else:
                destination.write_text(json.dumps({"format": 1, "sha256": {
                    "postgres.dump": "0" * 64, "keyring.json": "0" * 64}}))
        with patch.object(self.op, "restic", return_value=json.dumps(saved).encode()), \
                patch.object(self.op, "write_command", side_effect=recover), \
                patch.object(self.op, "isolated_postgres") as postgres:
            with self.assertRaises(b.BackupError):
                self.op.restore_check(snapshot, self.root)
            postgres.assert_not_called()

    def test_missing_alert_prevents_backup(self):
        usage = shutil._ntuple_diskusage(100 * 1024**3, 1, 99 * 1024**3)
        with patch.object(b, "run", return_value=b"tmpfs\n"), \
                patch.object(b.shutil, "disk_usage", return_value=usage), \
                patch.dict(os.environ, {}, clear=True), patch.object(self.op, "restic") as restic:
            with self.assertRaises(b.BackupError):
                self.op.preflight()
            restic.assert_not_called()

    def test_restore_container_is_always_removed_on_failure(self):
        calls = []
        def run(args, **kwargs):
            calls.append(args)
            if "pg_restore" in args:
                raise b.BackupError("failed")
            return b""
        path = self.root / "dump"
        path.write_bytes(b"fixture")
        with patch.object(b, "run", side_effect=run), \
                patch.object(b.subprocess, "run", return_value=subprocess.CompletedProcess([], 0)):
            with self.assertRaises(b.BackupError):
                self.op.isolated_postgres(path)
        self.assertIn("--network", calls[0])
        self.assertIn("none", calls[0])
        self.assertNotIn("--publish", calls[0])
        self.assertEqual(["docker", "rm", "--force"], calls[-1][:3])
        self.assertEqual(calls[0][calls[0].index("--name") + 1], calls[-1][-1])


@unittest.skipUnless(os.environ.get("FINANCEBOT_BACKUP_SMOKE") == "1",
                     "Smoke real requer Restic/Docker e opt-in explícito")
class IntegrationTests(unittest.TestCase):
    setUp = BackupTests.setUp
    tearDown = BackupTests.tearDown

    def test_real_encryption_backup_restore_and_retention(self):
        self.cfg["repository"] = str(self.root / "fixture-repository")
        self.cfg["apply_retention"] = True
        self.op = b.Operation(self.cfg)  # Repo local exclusivo do teste; loader recusa em operação.
        self.op.restic("init")
        name = "financebot-fixture-" + uuid.uuid4().hex
        self.cfg["postgres_container"] = name
        try:
            b.run(["docker", "run", "--pull=never", "-d", "--name", name,
                   "--network", "none", "--memory", "512m", "--cpus", "0.5",
                   "--tmpfs", "/var/lib/postgresql/data:rw,nosuid,size=512m",
                   "-e", "POSTGRES_HOST_AUTH_METHOD=trust", "-e", "POSTGRES_USER=postgres",
                   "-e", "POSTGRES_DB=postgres", "postgres:17"], stdout=subprocess.PIPE)
            for _ in range(60):
                result = subprocess.run(["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "postgres"],
                                        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                if result.returncode == 0:
                    break
                b.time.sleep(1)
            b.run(["docker", "exec", name, "psql", "-U", "postgres", "-v", "ON_ERROR_STOP=1", "-c",
                   "CREATE TABLE flyway_schema_history(success boolean); "
                   "INSERT INTO flyway_schema_history VALUES(true); "
                   "CREATE TABLE users(id int); CREATE TABLE transactions(id int); "
                   "INSERT INTO users VALUES(1); INSERT INTO transactions VALUES(2);"], stdout=subprocess.PIPE)
            with patch.object(self.op, "preflight"), \
                    patch.object(self.op, "keyring", return_value=("fixture", keys())):
                self.op.backup()
            snapshots = json.loads(self.op.restic("snapshots", "--json", "--tag", b.VERIFIED))
            self.assertEqual(1, len(snapshots))
            fixture = self.root / "retention-fixture"
            fixture.mkdir()
            (fixture / "data").write_text("fixture-only")
            for month in range(1, 13):
                self.op.restic("backup", "--host", "fixture", "--tag", b.VERIFIED,
                               "--time", f"2020-{month:02d}-01 00:00:00", str(fixture))
            self.op.restic("backup", "--host", "unrelated-host", "--tag", b.VERIFIED,
                           "--time", "2020-01-01 00:00:00", str(fixture))
            self.op.restic("backup", "--host", "fixture", "--tag", "unverified-fixture",
                           "--time", "2020-01-01 00:00:00", str(fixture))
            self.op.retention(True)
            all_saved = json.loads(self.op.restic("snapshots", "--json"))
            self.assertTrue(any(s["hostname"] == "unrelated-host" for s in all_saved))
            self.assertTrue(any("unverified-fixture" in s.get("tags", []) for s in all_saved))
            verified = [s for s in all_saved if s["hostname"] == "fixture" and b.VERIFIED in s.get("tags", [])]
            self.assertLess(len(verified), 13)
            # Diárias preservam períodos COM backup, sem inventar TTL de 90 dias.
            self.assertTrue(any(s["id"] == snapshots[0]["id"] for s in all_saved))
            self.op.restic("check", "--read-data")
            # Hash de dados fictícios só aparece dentro do repositório cifrado.
            encrypted_files = [p for p in (self.root / "fixture-repository").rglob("*") if p.is_file()]
            marker = keys()[b.KEY_NAMES[0]].encode()
            self.assertTrue(all(marker not in p.read_bytes() for p in encrypted_files))
        finally:
            subprocess.run(["docker", "rm", "--force", name], stdout=subprocess.DEVNULL,
                           stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    unittest.main()
