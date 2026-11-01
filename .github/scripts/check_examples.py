#!/usr/bin/env python3
"""Compare the pages AqlExamples generates with the output expected of them.

AqlExamples (``./gradlew generateExamples``) writes one HTML page per
runnable built-in example. The instances, commands and error of each page,
made canonical by ``snapshots.parse``, are compared with
``.github/examples/expected/<example>.json``, which records what CQL
computes today.

Usage:
    python3 -B .github/scripts/check_examples.py GENERATED EXPECTED [--update]

With ``--update`` the expected output is rewritten from the generated pages.
"""

import argparse
import sys
from pathlib import Path

import snapshots


def generated_results(directory: Path) -> dict[str, snapshots.Result]:
    """The result of each example page in a directory, by example name."""
    return {
        page.stem: snapshots.parse(page.read_text(encoding="utf-8"))
        for page in sorted(directory.glob("*.html"))
        if page.name != "index.html"
    }


def main() -> int:
    """Compare or update the expected output, and return the exit status."""
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("generated_dir", type=Path)
    parser.add_argument("expected_dir", type=Path)
    parser.add_argument(
        "--update", action="store_true", help="rewrite the expected output"
    )
    args = parser.parse_args()

    results = generated_results(args.generated_dir)
    if not results:
        print(f"::error::no example pages in {args.generated_dir}")
        return 1
    if args.update:
        snapshots.update(results, args.expected_dir)
        print(f"examples: wrote the expected output of {len(results)}")
        return 0
    reports = snapshots.compare(results, args.expected_dir)
    return snapshots.print_report(reports, "examples", len(results))


if __name__ == "__main__":
    sys.exit(main())
