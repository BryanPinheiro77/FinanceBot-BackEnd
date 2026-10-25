#!/usr/bin/env python3
"""Backup operacional opt-in. Não instala serviços nem restaura produção."""
import argparse
import base64
from datetime import datetime, timezone
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
import time
import signal
import urllib.request
import uuid

TAG = "financebot-backup-v1"
VERIFIED = "financebot-verified-v1"
KEY_NAMES = (
    "FINANCEBOT_DATA_ENCRYPTION_KEY",
    "FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID",
    "FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID",
    "FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS",
)
MIB = 1024 * 1024


class BackupError(Exception):
    pass


def run(args, **kwargs):
    # Ferramentas podem incluir dados/configuração em erros; nunca repassar stderr.
    result = subprocess.run(args, stderr=subprocess.DEVNULL, **kwargs)
    if result.returncode:
        raise BackupError("Ferramenta operacional falhou; consultar diagnóstico privado.")
    return result.stdout


def private_file(path):
    path = Path(path)
    mode = path.lstat().st_mode
    if not stat.S_ISREG(mode) or mode & 0o077 or path.stat().st_uid != os.geteuid():
        raise BackupError("Arquivo privado deve pertencer ao operador e ter modo 0600/0400.")
    return path


def load_config(path):
    cfg = json.loads(private_file(path).read_text())
    # Repositório dedicado, remoto; caminho local não é uma cópia externa.
    repo = cfg["repository"]
    if not repo.startswith(("sftp:", "s3:https://", "rest:https://")):
        raise BackupError("Configure destino remoto SFTP, S3 HTTPS ou REST HTTPS.")
    private_file(cfg["password_file"])
    for key in ("source_container", "postgres_container", "backup_host"):
        if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}", cfg[key]):
            raise BackupError("Identificador operacional inválido.")
    for key in ("state_dir", "staging_root"):
        if not Path(cfg[key]).is_absolute():
            raise BackupError("Diretórios operacionais precisam ser absolutos.")
    limit = cfg.get("max_dump_mib", 256)
    if type(limit) is not int or not 1 <= limit <= 1024:
        raise BackupError("Limite do dump deve estar entre 1 e 1024 MiB.")
    if type(cfg.get("apply_retention", False)) is not bool:
        raise BackupError("apply_retention deve ser booleano.")
    return cfg


class Operation:
    def __init__(self, cfg):
        self.cfg = cfg
        self.limit = cfg.get("max_dump_mib", 256) * MIB
        self.env = os.environ.copy()
        for name in ("RESTIC_PASSWORD", "RESTIC_PASSWORD_COMMAND", "RESTIC_REPOSITORY_FILE"):
            self.env.pop(name, None)
        self.env.update(RESTIC_REPOSITORY=cfg["repository"],
                        RESTIC_PASSWORD_FILE=cfg["password_file"])

    def restic(self, *args):
        return run(["restic", "--no-cache", *args], env=self.env, stdout=subprocess.PIPE)

    def preflight(self):
        fs = run(["findmnt", "-n", "-o", "FSTYPE", "--target", self.cfg["staging_root"]],
                 stdout=subprocess.PIPE).strip()
        if fs != b"tmpfs":
            raise BackupError("Staging precisa estar em tmpfs; dump em disco foi recusado.")
        if shutil.disk_usage("/").free < 5 * 1024**3:
            raise BackupError("Partição do sistema com menos de 5 GiB livres.")
        # Espaço para dump de origem, leitura da cópia e margem de 64 MiB.
        if shutil.disk_usage(self.cfg["staging_root"]).free < 2 * self.limit + 64 * MIB:
            raise BackupError("tmpfs sem capacidade para o limite configurado e o restore.")
        if not os.environ.get("FINANCEBOT_BACKUP_FAILURE_WEBHOOK_URL", "").startswith("https://"):
            raise BackupError("Canal de alerta de falha não configurado.")
        self.restic("snapshots", "--json")  # Nunca inicializa repositório por engano.

    def keyring(self):
        raw = run(["docker", "inspect", self.cfg["source_container"]], stdout=subprocess.PIPE)
        info = json.loads(raw)[0]
        if not info["State"]["Running"]:
            raise BackupError("API precisa estar em execução para inventariar o chaveiro.")
        env = dict(item.split("=", 1) for item in info["Config"]["Env"] if "=" in item)
        keys = {name: env.get(name, "") for name in KEY_NAMES}
        validate_keyring(keys)
        return info["Id"], keys

    def write_command(self, args, destination, limit, env=None):
        """Copia stdout limitado sem materializar plaintext em memória ou no disco."""
        with destination.open("xb") as output:
            with subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env) as child:
                total = 0
                try:
                    while block := child.stdout.read(65536):
                        total += len(block)
                        if total > limit:
                            raise BackupError("Conteúdo excedeu limite; cópias anteriores preservadas.")
                        output.write(block)
                    if child.wait() or not total:
                        raise BackupError("Exportação/leitura falhou ou retornou conteúdo vazio.")
                finally:
                    if child.poll() is None:
                        child.kill()
                        child.wait()
        return total

    def dump(self, directory):
        # Credenciais ficam no ambiente interno do PostgreSQL, sem aparecer no argv.
        return self.write_command([
            "docker", "exec", self.cfg["postgres_container"], "sh", "-c",
            'exec pg_dump --format=custom --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"',
        ], directory / "postgres.dump", self.limit)

    def restore_check(self, snapshot, directory):
        if not re.fullmatch(r"[a-f0-9]{64}", snapshot):
            raise BackupError("Exija ID completo e imutável do snapshot.")
        snapshots = json.loads(self.restic("snapshots", "--json", snapshot))
        if len(snapshots) != 1:
            raise BackupError("Snapshot não encontrado ou ambíguo.")
        saved = snapshots[0]
        if saved["hostname"] != self.cfg["backup_host"] or TAG not in saved.get("tags", []):
            raise BackupError("Snapshot não pertence a esta rotina.")
        paths = saved["paths"]
        if len(paths) != 1 or not Path(paths[0]).is_absolute():
            raise BackupError("Layout do snapshot inesperado.")
        for name, limit in (("manifest.json", MIB), ("keyring.json", MIB),
                            ("postgres.dump", self.limit)):
            args = ["restic", "--no-cache", "dump", snapshot, paths[0].rstrip("/") + "/" + name]
            # dump também exige o ambiente privado do repositório.
            self.write_command(args, directory / name, limit, env=self.env)
        manifest = json.loads((directory / "manifest.json").read_text())
        if manifest.get("format") != 1:
            raise BackupError("Formato de manifesto não suportado.")
        for name in ("postgres.dump", "keyring.json"):
            if digest(directory / name) != manifest["sha256"][name]:
                raise BackupError("Checksum divergente na cópia recuperada.")
        validate_keyring(json.loads((directory / "keyring.json").read_text()))
        self.isolated_postgres(directory / "postgres.dump")

    def isolated_postgres(self, dump):
        name = "financebot-restore-" + uuid.uuid4().hex
        created = False
        try:
            run(["docker", "run", "--pull=never", "--detach", "--name", name,
                 "--network", "none", "--memory", "512m", "--cpus", "0.5",
                 "--pids-limit", "128", "--read-only",
                 "--tmpfs", "/var/lib/postgresql/data:rw,nosuid,size=512m",
                 "--tmpfs", "/var/run/postgresql:rw,nosuid,size=16m",
                 "--tmpfs", "/tmp:rw,noexec,nosuid,size=16m",
                 "-e", "POSTGRES_HOST_AUTH_METHOD=trust", "postgres:17"], stdout=subprocess.PIPE)
            created = True
            for _ in range(60):
                ready = subprocess.run(["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "postgres"],
                                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                if ready.returncode == 0:
                    break
                time.sleep(1)
            else:
                raise BackupError("PostgreSQL isolado não iniciou no prazo.")
            with dump.open("rb") as source:
                run(["docker", "exec", "-i", name, "pg_restore", "--exit-on-error",
                     "--single-transaction", "--no-owner", "--no-privileges", "-U", "postgres",
                     "-d", "postgres"], stdin=source, stdout=subprocess.DEVNULL)
            result = run(["docker", "exec", name, "psql", "-XAt", "-v", "ON_ERROR_STOP=1",
                          "-U", "postgres", "-d", "postgres", "-c",
                          "SELECT count(*) FROM flyway_schema_history WHERE NOT success; "
                          "SELECT count(*) FROM users; SELECT count(*) FROM transactions;"],
                         stdout=subprocess.PIPE)
            counts = result.decode().strip().splitlines()
            if len(counts) != 3 or counts[0] != "0" or not all(x.isdigit() for x in counts):
                raise BackupError("Schema restaurado não passou nas verificações.")
        finally:
            # Nome gerado nesta execução; nunca remove containers de produção.
            if created:
                run(["docker", "rm", "--force", name], stdout=subprocess.DEVNULL)

    def retention(self, apply=False):
        args = ["forget", "--host", self.cfg["backup_host"], "--tag", VERIFIED,
                "--group-by", "host", "--keep-last", "1", "--keep-daily", "7",
                "--keep-weekly", "4", "--keep-monthly", "3"]
        self.restic(*args, *(["--prune"] if apply else ["--dry-run"]))
        if apply:
            self.restic("check")

    def backup(self):
        self.preflight()
        with tempfile.TemporaryDirectory(prefix="financebot-", dir=self.cfg["staging_root"]) as tmp:
            directory = Path(tmp)
            before = self.keyring()
            self.dump(directory)
            if self.keyring() != before:
                raise BackupError("API/chaveiro mudou durante o dump; repetir fora da manutenção.")
            (directory / "keyring.json").write_text(json.dumps(before[1]))
            manifest = {"format": 1, "created_at": datetime.now(timezone.utc).isoformat(),
                        "sha256": {name: digest(directory / name)
                                   for name in ("postgres.dump", "keyring.json")}}
            (directory / "manifest.json").write_text(json.dumps(manifest))
            for file in directory.iterdir():
                file.chmod(0o600)
            output = self.restic("backup", "--json", "--host", self.cfg["backup_host"],
                                 "--tag", TAG, "--", str(directory))
            summaries = [json.loads(line) for line in output.splitlines()]
            ids = [event["snapshot_id"] for event in summaries
                   if event.get("message_type") == "summary" and "snapshot_id" in event]
            if len(ids) != 1:
                raise BackupError("Backup não confirmou um snapshot único.")
            with tempfile.TemporaryDirectory(prefix="financebot-check-",
                                             dir=self.cfg["staging_root"]) as restored:
                self.restore_check(ids[0], Path(restored))
            self.restic("tag", "--add", VERIFIED, ids[0])
            self.retention(self.cfg.get("apply_retention", False))


def validate_keyring(keys):
    try:
        if len(base64.b64decode(keys[KEY_NAMES[0]], validate=True)) != 32:
            raise ValueError()
        for name in KEY_NAMES[1:3]:
            if keys[name] and not re.fullmatch(r"[a-zA-Z0-9_.-]{1,64}", keys[name]):
                raise ValueError()
        # PREVIOUS_KEYS usa o mesmo contrato keyId=base64 separado por vírgulas da API.
        for pair in filter(None, keys[KEY_NAMES[3]].split(",")):
            key_id, value = pair.strip().split("=", 1)
            if not re.fullmatch(r"[a-zA-Z0-9_.-]{1,64}", key_id):
                raise ValueError()
            if len(base64.b64decode(value, validate=True)) != 32:
                raise ValueError()
    except (KeyError, ValueError, TypeError):
        raise BackupError("Chaveiro ausente ou inválido.") from None


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def notify_failure():
    url = os.environ.get("FINANCEBOT_BACKUP_FAILURE_WEBHOOK_URL")
    if not url or not url.startswith("https://"):
        return
    request = urllib.request.Request(url, data=json.dumps({
        "content": "FinanceBot: backup/restore falhou. Verifique a rotina privada; não remover cópias anteriores."
    }).encode(), headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=10):
            pass
    except Exception:
        print("Alerta remoto falhou; verificar também o estado do serviço.", flush=True)


def stop_requested():
    raise BackupError("Operação interrompida; cópias anteriores preservadas.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True)
    parser.add_argument("action", choices=("backup", "retention-plan", "restore-check"))
    parser.add_argument("--snapshot")
    args = parser.parse_args()
    os.umask(0o077)
    signal.signal(signal.SIGTERM, lambda *_: stop_requested())
    try:
        cfg = load_config(args.config)
        state = Path(cfg["state_dir"])
        state.mkdir(mode=0o700, parents=True, exist_ok=True)
        if state.is_symlink() or state.stat().st_mode & 0o077 or state.stat().st_uid != os.geteuid():
            raise BackupError("Diretório de estado deve ser privado e pertencer ao operador.")
        with (state / "operation.lock").open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            operation = Operation(cfg)
            if args.action == "backup":
                operation.backup()
                temporary = state / "last-success.json.partial"
                temporary.write_text(json.dumps({"completed_at": datetime.now(timezone.utc).isoformat()}))
                temporary.replace(state / "last-success.json")
            elif args.action == "retention-plan":
                operation.retention()
            else:
                operation.preflight()
                with tempfile.TemporaryDirectory(prefix="financebot-check-", dir=cfg["staging_root"]) as tmp:
                    operation.restore_check(args.snapshot or "", Path(tmp))
        print("Operação concluída.", flush=True)
        return 0
    except Exception as error:
        if isinstance(error, BackupError):
            print(str(error), flush=True)
        # Nem traceback nem mensagens de terceiros: podem conter secrets/endereços privados.
        print("Operação falhou. Verificar configuração, capacidade e acesso no ambiente privado.", flush=True)
        notify_failure()
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
