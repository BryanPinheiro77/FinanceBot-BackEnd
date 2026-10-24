#!/usr/bin/env python3
"""Valida contexto e runtime Docker usando somente configuração fictícia."""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import uuid
import zipfile


ROOT = Path(__file__).resolve().parents[1]
SENTINEL = b"FINANCEBOT_FICTITIOUS_BUILD_SENTINEL_DO_NOT_SHIP"
CANARIES = (
    ".env", ".env.production", "credentials.env", ".git/config", ".ssh/id_ed25519",
    "backups/test.sql", "backup.dump", "financebot-telegram-bot/.env",
    "src/main/resources/.env", "src/main/resources/.env.local",
    "src/main/resources/credentials.env", "src/main/resources/private-key.pem",
    "src/main/resources/private.key", "src/main/resources/keystore.p12",
    "src/main/resources/keystore.pfx", "src/main/resources/.git/config",
    "src/main/resources/.ssh/id_ed25519", "src/main/resources/.m2/settings.xml",
    "src/main/resources/keystore.jks", "src/main/resources/test.dump",
    "src/main/resources/test.dump.enc", "src/main/resources/test.sql.gz",
    "src/main/resources/test.sql.bz2", "src/main/resources/backups/test.sql",
)


def run(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def prepare_context(source, context):
    # Nunca copiar .env ou o checkout inteiro. O teste injeta apenas sentinelas fictícias.
    context.mkdir()
    for name in ("Dockerfile", ".dockerignore", "mvnw", "pom.xml"):
        shutil.copy2(source / name, context / name)
    for name in (".mvn", "src"):
        shutil.copytree(source / name, context / name, ignore=shutil.ignore_patterns(
            ".env", ".env.*", "*.env", "*.pem", "*.key", "*.p12", "*.pfx", "*.jks",
            ".git", ".ssh", ".m2",
            "*.dump", "*.dump.*", "*.sql.gz", "*.sql.bz2", "backups"))
    for name in CANARIES:
        path = context / name
        require(not path.exists(), "Uma sentinela colide com uma entrada do projeto")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(SENTINEL)


def verify_context(context, work):
    probe = work / "context-probe.Dockerfile"
    probe.write_text("FROM scratch\nCOPY . /\n")
    exported = work / "exported-context"
    run("docker", "build", "--progress=plain", "--file", str(probe),
        "--output", f"type=local,dest={exported}", str(context))
    for name in CANARIES:
        require(not (exported / name).exists(), "Arquivo fictício indevido no contexto Docker: " + name)
    required = ("pom.xml", "mvnw", ".mvn/wrapper/maven-wrapper.properties",
                "src/main/resources/application.properties")
    for name in required:
        require((exported / name).is_file(), "Entrada necessária ausente do contexto: " + name)
    migrations = context / "src/main/resources/db/migration"
    if migrations.exists():
        for path in migrations.glob("*.sql"):
            relative = path.relative_to(context)
            require((exported / relative).read_bytes() == path.read_bytes(), "Migration alterada/excluída do contexto")
    print("Contexto validado: configuração fictícia excluída e fontes/migrations preservadas.", flush=True)


def verify_runtime(image, work):
    metadata = json.loads(run("docker", "image", "inspect", image,
                              capture_output=True, text=True).stdout)[0]["Config"]
    require(metadata["User"] == "10001:10001", "Runtime deve declarar usuário sem root")
    require(metadata["Cmd"] == ["java", "-jar", "/app/app.jar"], "Comando do runtime inesperado")
    run("docker", "run", "--rm", "--network", "none", image, "sh", "-ec",
        'test "$(id -u)" = 10001; test "$(id -g)" = 10001; '
        'test -r /app/app.jar; test ! -w /app/app.jar; test ! -w /app; '
        'test -w /tmp; ! command -v javac; ! command -v mvn; '
        'java -version')
    container = run("docker", "create", "--network", "none", image,
                    capture_output=True, text=True).stdout.strip()
    try:
        archive = work / "runtime.tar"
        run("docker", "export", "--output", str(archive), container)
        with tarfile.open(archive) as filesystem:
            app_files = {member.name.rstrip("/") for member in filesystem
                         if member.name.startswith("app/")}
            require(app_files == {"app/app.jar"}, "Runtime deve conter somente o JAR em /app")
            require(not any(member.name.startswith(("root/.m2", "app/.git")) for member in filesystem),
                    "Cache de compilação ou Git no runtime")
            jar = work / "app.jar"
            with filesystem.extractfile("app/app.jar") as source, jar.open("wb") as target:
                shutil.copyfileobj(source, target)
        with zipfile.ZipFile(jar) as application:
            names = application.namelist()
            require("BOOT-INF/classes/application.properties" in names, "Configuração da aplicação ausente do JAR")
            for name in names:
                if name.startswith("BOOT-INF/classes/") and not name.endswith("/"):
                    require(SENTINEL not in application.read(name), "Sentinela fictícia incorporada ao JAR")
    finally:
        run("docker", "rm", container, stdout=subprocess.DEVNULL)
    print("Runtime validado: Java 21 sem root, JAR legível e sem fontes/cache/configuração fictícia.", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module", required=True, choices=("api", "telegram-bot"))
    args = parser.parse_args()
    source = ROOT if args.module == "api" else ROOT / "financebot-telegram-bot"
    image = f"financebot-container-check-{args.module}:{uuid.uuid4().hex}"
    with tempfile.TemporaryDirectory(prefix="financebot-container-check-") as directory:
        work = Path(directory)
        context = work / "context"
        prepare_context(source, context)
        verify_context(context, work)
        try:
            run("docker", "build", "--progress=plain", "--target", "runtime", "--tag", image, str(context))
            verify_runtime(image, work)
        finally:
            # Somente a tag exclusiva deste teste; sem prune, volumes ou containers externos.
            subprocess.run(["docker", "image", "rm", image], stdout=subprocess.DEVNULL, check=False)
    print(f"Verificação Docker aprovada: {args.module}.", flush=True)


if __name__ == "__main__":
    main()
