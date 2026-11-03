#!/usr/bin/env python3
"""Compare the generated example pages with their expected output.

Run after ``./gradlew generateExamples``.
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
    parser = argparse.ArgumentParser(description=__doc__)
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
