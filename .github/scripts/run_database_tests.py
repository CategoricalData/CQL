#!/usr/bin/env python3
"""Run the JDBCSQL example on one database and compare its page.

Requires ``./gradlew classes`` and, except for h2 and sqlite, Docker.
"""

import argparse
import dataclasses
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

import snapshots

REPO = Path(__file__).resolve().parent.parent.parent
EXPECTED = REPO / ".github" / "databases" / "expected"
RESOURCES = REPO / "build" / "resources" / "main"
EXAMPLE = Path("examples") / "cql" / "JDBCSQL.cql"
WORK = REPO / "build" / "database-tests"
DRIVERS = WORK / "drivers"

MAVEN = "https://repo1.maven.org/maven2/"
DOWNLOAD_ATTEMPTS = 3
EXAMPLE_TIMEOUT_SECONDS = 600
READY_TIMEOUT_SECONDS = 240

# The two lines of JDBCSQL.cql that its comments say to change for another
# database, and what each becomes; {url} is the database's JDBC URL.
EDITS = [
    (
        '\t#jdbc_default_string = "jdbc:postgresql://localhost:5432/postgres'
        '?user=postgres&password=password"',
        '\tjdbc_default_string = "{url}"',
    ),
    (
        'command load = exec_jdbc "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" {',
        'command load = exec_jdbc "{url}" {',
    ),
]


@dataclasses.dataclass(frozen=True)
class Driver:
    """A JDBC driver jar on Maven Central and the SHA-256 of the file."""

    path: str
    sha256: str

    @property
    def file(self) -> Path:
        """The local copy of the jar."""
        return DRIVERS / Path(self.path).name


@dataclasses.dataclass(frozen=True)
class Server:
    """A database server in Docker, the command that succeeds once it
    accepts TCP connections, and the commands that prepare it after that."""

    image: str
    port: int
    container_port: int
    environment: dict[str, str]
    ready: list[str]
    prepare: list[list[str]] = dataclasses.field(
        default_factory=list[list[str]]
    )


@dataclasses.dataclass(frozen=True)
class Database:
    """A database under test: its JDBC URL, drivers and server, if any."""

    name: str
    url: str
    drivers: list[Driver]
    server: Server | None = None


SQLCMD = [
    "/opt/mssql-tools18/bin/sqlcmd",
    "-C",
    "-S",
    "localhost",
    "-U",
    "sa",
    "-P",
    "Passw0rd!cql",
]


def h2() -> Database:
    """H2 in memory, the database JDBCSQL is written for."""
    return Database(
        name="h2", url="jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1", drivers=[]
    )


def sqlite(work: Path) -> Database:
    """SQLite, in a file."""
    return Database(
        name="sqlite",
        url=f"jdbc:sqlite:{work / 'rt.db'}",
        drivers=[
            Driver(
                "org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar",
                "4a4832720a65eaf7f4d6fd7ede52087b"
                "994dc5633c076f9e994dc0c8b4b0b4fa",
            ),
            Driver(
                "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
                "d3ef575e3e4979678dc01bf1dcce5102"
                "1493b4d11fb7f1be8ad982877c16a1c0",
            ),
        ],
    )


def postgresql() -> Database:
    """PostgreSQL 17."""
    return Database(
        name="postgresql",
        url=(
            "jdbc:postgresql://localhost:55432/rt"
            "?user=postgres&password=password"
        ),
        drivers=[
            Driver(
                "org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
                "188976721ead8e8627eb6d8389d500dc"
                "cc0c9bebd885268a3047180274a6031e",
            )
        ],
        server=Server(
            image="postgres:17",
            port=55432,
            container_port=5432,
            environment={"POSTGRES_PASSWORD": "password", "POSTGRES_DB": "rt"},
            ready=["pg_isready", "-h", "127.0.0.1", "-U", "postgres"]
            + ["-d", "rt"],
        ),
    )


def mysql() -> Database:
    """MySQL 8.4. JDBCSQL drops its tables in one string of six statements,
    which needs allowMultiQueries=true, as its comments say."""
    return Database(
        name="mysql",
        url=(
            "jdbc:mysql://localhost:53306/rt?user=root&password=password"
            "&allowPublicKeyRetrieval=true&useSSL=false"
            "&nullDatabaseMeansCurrent=true&allowMultiQueries=true"
        ),
        drivers=[
            Driver(
                "com/mysql/mysql-connector-j/8.4.0/"
                "mysql-connector-j-8.4.0.jar",
                "d77962877d010777cff997015da90ee6"
                "89f0f4bb76848340e1488f2b83332af5",
            )
        ],
        server=Server(
            image="mysql:8.4",
            port=53306,
            container_port=3306,
            environment={
                "MYSQL_ROOT_PASSWORD": "password",
                "MYSQL_DATABASE": "rt",
            },
            ready=["mysql", "-h127.0.0.1", "-uroot", "-ppassword"]
            + ["-e", "SELECT 1", "rt"],
        ),
    )


def mariadb() -> Database:
    """MariaDB 11.4, with allowMultiQueries=true for the same reason as
    MySQL."""
    return Database(
        name="mariadb",
        url=(
            "jdbc:mariadb://localhost:53307/rt?user=root&password=password"
            "&nullDatabaseMeansCurrent=true&allowMultiQueries=true"
        ),
        drivers=[
            Driver(
                "org/mariadb/jdbc/mariadb-java-client/3.4.1/"
                "mariadb-java-client-3.4.1.jar",
                "f60e4b282f1f4bdb74f0a26436ba7078"
                "a5e480b6f6702f6a7b45d9ba5e604a24",
            )
        ],
        server=Server(
            image="mariadb:11.4",
            port=53307,
            container_port=3306,
            environment={
                "MARIADB_ROOT_PASSWORD": "password",
                "MARIADB_DATABASE": "rt",
            },
            ready=["mariadb", "-h127.0.0.1", "-uroot", "-ppassword"]
            + ["-e", "SELECT 1", "rt"],
        ),
    )


def sqlserver() -> Database:
    """SQL Server 2022, with a database rt created once the server is up."""
    return Database(
        name="sqlserver",
        url=(
            "jdbc:sqlserver://localhost:51433;databaseName=rt;user=sa;"
            "password=Passw0rd!cql;encrypt=true;trustServerCertificate=true"
        ),
        drivers=[
            Driver(
                "com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/"
                "mssql-jdbc-12.8.1.jre11.jar",
                "e6933c0711e598a224060e52ed31392f"
                "720a4a7664e85d8ae37c52a85b67ebb0",
            )
        ],
        server=Server(
            image="mcr.microsoft.com/mssql/server:2022-latest",
            port=51433,
            container_port=1433,
            environment={
                "ACCEPT_EULA": "Y",
                "MSSQL_SA_PASSWORD": "Passw0rd!cql",
            },
            ready=SQLCMD + ["-Q", "SELECT 1"],
            prepare=[
                SQLCMD + ["-Q", "IF DB_ID('rt') IS NULL CREATE DATABASE rt"]
            ],
        ),
    )


def databases(work: Path) -> dict[str, Database]:
    """The six databases of the round-trip harness in #120, by name."""
    return {
        database.name: database
        for database in [
            h2(),
            sqlite(work),
            postgresql(),
            mysql(),
            mariadb(),
            sqlserver(),
        ]
    }


def container_name(database: Database) -> str:
    """The name of the database's Docker container."""
    return f"cql-database-tests-{database.name}"


def start_server(database: Database) -> None:
    """Start the database's container and wait until it accepts TCP
    connections."""
    server = database.server
    if server is None:
        return
    name = container_name(database)
    docker = ["docker", "rm", "-f", name]
    subprocess.run(docker, capture_output=True, check=False)
    command = ["docker", "run", "-d", "--name", name]
    command += ["-p", f"127.0.0.1:{server.port}:{server.container_port}"]
    for key, value in server.environment.items():
        command += ["-e", f"{key}={value}"]
    command.append(server.image)
    subprocess.run(command, check=True, stdout=subprocess.DEVNULL)
    deadline = time.monotonic() + READY_TIMEOUT_SECONDS
    while True:
        ready = subprocess.run(
            ["docker", "exec", name] + server.ready,
            capture_output=True,
            check=False,
        )
        if ready.returncode == 0:
            break
        if time.monotonic() > deadline:
            logs = subprocess.run(
                ["docker", "logs", name],
                capture_output=True,
                text=True,
                check=False,
            )
            raise RuntimeError(
                f"{server.image} did not start:\n{logs.stdout}{logs.stderr}"
            )
        time.sleep(2)
    for step in server.prepare:
        subprocess.run(
            ["docker", "exec", name] + step, check=True, capture_output=True
        )


def stop_server(database: Database) -> None:
    """Remove the database's container, if it has one."""
    if database.server is not None:
        subprocess.run(
            ["docker", "rm", "-f", container_name(database)],
            capture_output=True,
            check=False,
        )


def sha256(path: Path) -> str:
    """The SHA-256 of a file, in hexadecimal."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fetch(driver: Driver) -> None:
    """Download a driver, retrying a failed or corrupted download, and
    check its SHA-256."""
    problem = ""
    for _ in range(DOWNLOAD_ATTEMPTS):
        try:
            url = MAVEN + driver.path
            with urllib.request.urlopen(url, timeout=120) as response:
                driver.file.write_bytes(response.read())
        except OSError as error:
            problem = str(error)
            continue
        actual = sha256(driver.file)
        if actual == driver.sha256:
            return
        driver.file.unlink()
        problem = f"SHA-256 {actual}, expected {driver.sha256}"
    raise RuntimeError(f"{driver.path}: {problem}")


def download_drivers(database: Database) -> None:
    """Download the database's JDBC drivers, unless a copy with the right
    checksum is present."""
    DRIVERS.mkdir(parents=True, exist_ok=True)
    for driver in database.drivers:
        if not (driver.file.exists() and sha256(driver.file) == driver.sha256):
            fetch(driver)


def example_for(database: Database) -> str:
    """JDBCSQL.cql with the two edits its comments describe, pointing it at
    the database."""
    text = (RESOURCES / EXAMPLE).read_text(encoding="utf-8")
    for old, new in EDITS:
        if text.count(old) != 1:
            raise RuntimeError(f"{EXAMPLE} no longer has the line\n{old}")
        text = text.replace(old, new.replace("{url}", database.url))
    return text


def java() -> str:
    """The java launcher of JAVA_HOME, or the one on the PATH."""
    home = os.environ.get("JAVA_HOME")
    return str(Path(home) / "bin" / "java") if home else "java"


def classpath(examples_root: Path) -> str:
    """The edited example first, so that it is the only one CQL finds, then
    CQL's classes, resources, libraries and the drivers."""
    parts = [
        examples_root,
        REPO / "build" / "classes" / "java" / "main",
        RESOURCES,
        REPO / "lib" / "*",
        DRIVERS / "*",
    ]
    return os.pathsep.join(str(part) for part in parts)


def render(database: Database, work: Path) -> str:
    """Render the edited example with AqlExamples and return the page, or a
    description of the failure if AqlExamples wrote none."""
    examples_root = work / "classpath"
    edited = examples_root / EXAMPLE
    edited.parent.mkdir(parents=True)
    edited.write_text(example_for(database), encoding="utf-8")
    site = work / "site"
    site.mkdir()
    # The drivers convert times with the JVM's time zone, which is fixed to
    # UTC so that the page does not depend on the machine.
    command = [
        java(),
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-Duser.timezone=UTC",
        "-Djava.awt.headless=true",
        "-Dpolyglot.engine.WarnInterpreterOnly=false",
        "-cp",
        classpath(examples_root),
        "catdata.cql.exp.AqlExamples",
    ]
    try:
        done = subprocess.run(
            command,
            cwd=site,
            capture_output=True,
            text=True,
            timeout=EXAMPLE_TIMEOUT_SECONDS,
            check=False,
        )
        outcome = f"AqlExamples exited with {done.returncode}"
        log = done.stdout + done.stderr
    except subprocess.TimeoutExpired:
        outcome = f"AqlExamples timed out after {EXAMPLE_TIMEOUT_SECONDS} s"
        log = ""
    (work / "aqlexamples.log").write_text(log, encoding="utf-8")
    page = site / "examples" / "JDBCSQL.html"
    if page.exists():
        return page.read_text(encoding="utf-8")
    return f"<h3>error</h3><pre>no page: {outcome}</pre>"


OBJECT_HASH = re.compile(r"@[0-9a-f]{5,}\b")
H2_OBJECT_ID = re.compile(r"\b(rs|prep|conn|stat|stmt)[0-9]+:")
MARIADB_CONNECTION = re.compile(r"\(conn=[0-9]+\)")


def result_of(page: str, work: Path) -> snapshots.Result:
    """The canonical result of the page, after replacing what changes from
    run to run: paths, object hashes, the numbers H2 gives its objects and
    MariaDB's connection numbers."""
    page = page.replace(str(work), "<work>")
    page = OBJECT_HASH.sub("@<hash>", page)
    page = H2_OBJECT_ID.sub(r"\1<n>:", page)
    page = MARIADB_CONNECTION.sub("(conn=<n>)", page)
    return snapshots.parse(page)


def run(
    database: Database, work: Path, keep_database: bool
) -> snapshots.Result:
    """Start the database, render the example, stop the database, and
    return the result of the page."""
    download_drivers(database)
    start_server(database)
    try:
        page = render(database, work)
    finally:
        if not keep_database:
            stop_server(database)
    result = result_of(page, work)
    (work / "JDBCSQL.json").write_text(
        snapshots.to_json(result), encoding="utf-8"
    )
    return result


def main() -> int:
    """Compare or update the expected page, and return the exit status."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", choices=sorted(databases(WORK)))
    parser.add_argument(
        "--update", action="store_true", help="rewrite the expected page"
    )
    parser.add_argument(
        "--keep-database",
        action="store_true",
        help="leave the database container running",
    )
    args = parser.parse_args()

    if not (REPO / "build" / "classes" / "java" / "main").is_dir():
        print("::error::run ./gradlew classes first")
        return 1
    work = WORK / args.database
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    database = databases(work)[args.database]
    results = {"JDBCSQL": run(database, work, args.keep_database)}
    expected_dir = EXPECTED / database.name
    if args.update:
        snapshots.update(results, expected_dir)
        print(f"{database.name}: wrote the expected page of JDBCSQL")
        return 0
    reports = snapshots.compare(results, expected_dir)
    return snapshots.print_report(reports, database.name, len(results))


if __name__ == "__main__":
    sys.exit(main())
