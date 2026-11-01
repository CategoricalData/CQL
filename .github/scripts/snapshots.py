"""Read the pages AqlExamples writes, and compare them with expected output.

An example page holds the example's source, then one section per definition
the example computed: ``<h3>instance I</h3>`` followed by one table per
entity, ``<h3>command c</h3>`` followed by the command's output, or a single
``<h3>error</h3>`` when the program failed. Only those sections are kept, as
a ``Result``: the source is in the repository already.

CQL prints rows, columns and labelled nulls in an order that changes from
run to run. A ``Result`` is therefore canonical: the columns after the row
ID are sorted by name, the rows are sorted, and every labelled null ``?<n>``
is written ``?N``. CQL also numbers the rows of some tables, such as the RDF
triples of the JSON and XML examples, in an order that changes; when no cell
of an instance refers to a table's row IDs, the ID column is dropped.

The expected output is one JSON file per page, written by ``to_json`` with
one row per line, so that it can be read and reviewed in a diff.
"""

import json
import re
import shutil
from html.parser import HTMLParser
from pathlib import Path
from typing import TypedDict

LABELLED_NULL = re.compile(r"\?[0-9]+")
MAX_ROWS_REPORTED = 10
MAX_LINES_REPORTED = 20


class Table(TypedDict):
    """The rows of one entity, below the names of their columns."""

    columns: list[str]
    rows: list[list[str]]


class Result(TypedDict):
    """What an example computed: its error, or its instances and commands."""

    error: list[str] | None
    instances: dict[str, dict[str, Table]]
    commands: dict[str, list[str]]


class _PageParser(HTMLParser):
    """Collects the instance tables, command outputs and error of a page."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.result: Result = {"error": None, "instances": {}, "commands": {}}
        self._section: tuple[str, str] | None = None
        self._text = ""
        self._caption = ""
        self._table: Table | None = None
        self._row: list[str] | None = None

    def handle_starttag(
        self, tag: str, attrs: list[tuple[str, str | None]]
    ) -> None:
        """Start collecting the text of a heading, cell or block."""
        if tag in ("h3", "pre", "caption", "th", "td"):
            self._text = ""
        elif tag == "table":
            self._table = {"columns": [], "rows": []}
        elif tag == "tr":
            self._row = []

    def handle_endtag(self, tag: str) -> None:
        """Store what the element that ends held."""
        if tag == "h3":
            kind, _, name = self._text.strip().partition(" ")
            self._section = (kind, name)
        elif tag == "pre" and self._section is not None:
            self._end_block(self._text)
        elif tag == "caption":
            self._caption = self._text.strip()
        elif tag in ("th", "td") and self._row is not None:
            self._row.append(self._text.strip())
        elif tag == "tr" and self._table is not None:
            self._end_row()
        elif tag == "table":
            self._end_table()

    def handle_data(self, data: str) -> None:
        """Collect text."""
        self._text += data

    def _end_block(self, text: str) -> None:
        assert self._section is not None
        kind, name = self._section
        lines = [line.rstrip() for line in text.strip("\n").split("\n")]
        if kind == "command":
            self.result["commands"][name] = lines
        elif kind == "error":
            self.result["error"] = lines

    def _end_row(self) -> None:
        assert self._table is not None
        if self._row is None:
            return
        if self._table["columns"]:
            self._table["rows"].append(self._row)
        else:
            self._table["columns"] = self._row
        self._row = None

    def _end_table(self) -> None:
        if self._table is not None and self._section is not None:
            kind, name = self._section
            if kind == "instance":
                tables = self.result["instances"].setdefault(name, {})
                tables[self._caption] = self._table
        self._table = None


def _without_labelled_nulls(text: str) -> str:
    return LABELLED_NULL.sub("?N", text)


def _canonical_table(table: Table, referenced: set[str]) -> Table:
    """Sort the columns after the row ID by name and the rows; drop the
    row ID when no cell of the instance refers to it."""
    columns = table["columns"]
    order = [0] + sorted(range(1, len(columns)), key=lambda i: columns[i])
    ids = {row[0] for row in table["rows"]}
    if table["rows"] and not ids & referenced:
        order = order[1:]
    return {
        "columns": [columns[i] for i in order],
        "rows": sorted(
            [_without_labelled_nulls(row[i]) for i in order]
            for row in table["rows"]
        ),
    }


def parse(page: str) -> Result:
    """The canonical result of an example page."""
    parser = _PageParser()
    parser.feed(page)
    raw = parser.result
    instances: dict[str, dict[str, Table]] = {}
    for name, tables in raw["instances"].items():
        referenced = {
            cell
            for table in tables.values()
            for row in table["rows"]
            for cell in row[1:]
        }
        instances[name] = {
            entity: _canonical_table(table, referenced)
            for entity, table in sorted(tables.items())
        }
    commands = {
        name: [_without_labelled_nulls(line) for line in lines]
        for name, lines in sorted(raw["commands"].items())
    }
    error = raw["error"]
    if error is not None:
        error = [_without_labelled_nulls(line) for line in error]
    return {"error": error, "instances": instances, "commands": commands}


def _json(value: object) -> str:
    return json.dumps(value, ensure_ascii=True)


def _json_lines(values: list[str], indent: str) -> list[str]:
    """A list of strings, one per line."""
    if not values:
        return ["[]"]
    body = [f"{indent}  {_json(value)}," for value in values]
    body[-1] = body[-1][:-1]
    return ["["] + body + [f"{indent}]"]


def _table_lines(table: Table, indent: str) -> list[str]:
    """A table with its columns on one line and one line per row."""
    lines = ["{", f'{indent}  "columns": {_json(table["columns"])},']
    if table["rows"]:
        lines.append(f'{indent}  "rows": [')
        rows = [f"{indent}    {_json(row)}," for row in table["rows"]]
        rows[-1] = rows[-1][:-1]
        lines += rows + [f"{indent}  ]"]
    else:
        lines.append(f'{indent}  "rows": []')
    return lines + [f"{indent}}}"]


def _members(members: list[tuple[str, list[str]]], indent: str) -> list[str]:
    """An object whose members are already laid out on lines."""
    if not members:
        return ["{}"]
    lines = ["{"]
    for index, (key, value) in enumerate(members):
        comma = "," if index < len(members) - 1 else ""
        lines.append(f"{indent}  {_json(key)}: {value[0]}")
        lines += value[1:]
        lines[-1] += comma
    return lines + [f"{indent}}}"]


def to_json(result: Result) -> str:
    """The result as JSON, with one row, command line or error line per
    line."""
    error = (
        ["null"]
        if result["error"] is None
        else _json_lines(result["error"], "  ")
    )
    instances = _members(
        [
            (
                name,
                _members(
                    [
                        (entity, _table_lines(table, "      "))
                        for entity, table in tables.items()
                    ],
                    "    ",
                ),
            )
            for name, tables in result["instances"].items()
        ],
        "  ",
    )
    commands = _members(
        [
            (name, _json_lines(lines, "    "))
            for name, lines in result["commands"].items()
        ],
        "  ",
    )
    top = _members(
        [("error", error), ("instances", instances), ("commands", commands)],
        "",
    )
    return "\n".join(top) + "\n"


def _row_report(where: str, expected: Table, actual: Table) -> list[str]:
    """The rows of a table that are missing or unexpected."""
    if expected["columns"] != actual["columns"]:
        return [
            f"{where}: columns {_json(actual['columns'])}, "
            f"expected {_json(expected['columns'])}"
        ]
    report = []
    missing = [row for row in expected["rows"] if row not in actual["rows"]]
    unexpected = [row for row in actual["rows"] if row not in expected["rows"]]
    for label, rows in (("missing", missing), ("unexpected", unexpected)):
        for row in rows[:MAX_ROWS_REPORTED]:
            report.append(f"{where}: {label} row {_json(row)}")
        if len(rows) > MAX_ROWS_REPORTED:
            more = len(rows) - MAX_ROWS_REPORTED
            report.append(f"{where}: {more} more {label} rows")
    return report


def _keys_report(
    where: str, expected: set[str], actual: set[str], kind: str
) -> list[str]:
    """The names present on one side only."""
    report = [
        f"{where}: missing {kind} {name}" for name in sorted(expected - actual)
    ]
    report += [
        f"{where}: unexpected {kind} {name}"
        for name in sorted(actual - expected)
    ]
    return report


def _lines_report(
    where: str, expected: list[str], actual: list[str]
) -> list[str]:
    """The lines of a command output or an error that differ."""
    report = [f"{where}: output differs"]
    report += [f"  expected: {line}" for line in expected[:MAX_LINES_REPORTED]]
    report += [f"  actual:   {line}" for line in actual[:MAX_LINES_REPORTED]]
    return report


def differences(name: str, expected: Result, actual: Result) -> list[str]:
    """Describe how a result differs from the expected one, by instance,
    entity and row, and by command."""
    if expected["error"] != actual["error"]:
        return _lines_report(
            f"{name} error", expected["error"] or [], actual["error"] or []
        )
    report = _keys_report(
        name, set(expected["instances"]), set(actual["instances"]), "instance"
    )
    for instance, tables in expected["instances"].items():
        other = actual["instances"].get(instance)
        if other is None:
            continue
        where = f"{name} instance {instance}"
        report += _keys_report(where, set(tables), set(other), "entity")
        for entity, table in tables.items():
            if entity in other:
                report += _row_report(
                    f"{where} entity {entity}", table, other[entity]
                )
    report += _keys_report(
        name, set(expected["commands"]), set(actual["commands"]), "command"
    )
    for command, lines in expected["commands"].items():
        other_lines = actual["commands"].get(command)
        if other_lines is not None and other_lines != lines:
            report += _lines_report(
                f"{name} command {command}", lines, other_lines
            )
    return report


def load(path: Path) -> Result:
    """Read an expected result."""
    data: Result = json.loads(path.read_text(encoding="utf-8"))
    return data


def compare(actual: dict[str, Result], expected_dir: Path) -> list[str]:
    """Return one report per page whose result differs from the expected
    one, or that is missing on either side."""
    reports: list[str] = []
    expected_names = {path.stem for path in expected_dir.glob("*.json")}
    for name in sorted(expected_names - actual.keys()):
        reports.append(f"{name}: expected output exists, but no page was made")
    for name, result in sorted(actual.items()):
        expected_file = expected_dir / f"{name}.json"
        if not expected_file.exists():
            reports.append(f"{name}: no expected output in {expected_file}")
            continue
        found = differences(name, load(expected_file), result)
        if found:
            reports.append("\n".join(found))
    return reports


def update(actual: dict[str, Result], expected_dir: Path) -> None:
    """Replace the expected output with the actual one."""
    if expected_dir.exists():
        shutil.rmtree(expected_dir)
    expected_dir.mkdir(parents=True)
    for name, result in actual.items():
        path = expected_dir / f"{name}.json"
        path.write_text(to_json(result), encoding="utf-8")


def print_report(reports: list[str], subject: str, count: int) -> int:
    """Print the reports, as GitHub annotations followed by the details,
    and return the exit status: 1 if any page differs."""
    for entry in reports:
        print(f"::error::{subject}: {entry.splitlines()[0]}")
        print(entry)
    print(f"{subject}: {count} checked, {len(reports)} differ")
    return 1 if reports else 0
