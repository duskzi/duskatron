#!/usr/bin/env python3

from pathlib import Path
import sys


def collect_java_files(root: Path):
    return sorted(root.rglob("*.java"))


def merge_java_files(root: Path, output: Path):
    files = collect_java_files(root)

    if not files:
        print(f"No .java files found in {root}")
        return

    with output.open("w", encoding="utf-8") as out:
        for i, file in enumerate(files):
            out.write(f"/* ->  {file.relative_to(root)}.java  */\n\n")

            content = file.read_text(encoding="utf-8")

            # Remove package declarations because everything
            # will be placed into one source file.
            lines = content.splitlines()

            filtered = [
                line for line in lines
                if not line.strip().startswith("package ")
            ]

            out.write("\n".join(filtered))
            out.write("\n\n")

            if i != len(files) - 1:
                out.write("\n")

    print(f"Merged {len(files)} classes into {output}")


def main():
    if len(sys.argv) not in (2, 3):
        print(f"Usage: {sys.argv[0]} <directory> [output.java]")
        sys.exit(1)

    root = Path(sys.argv[1]).resolve()
    output = (
        Path(sys.argv[2]).resolve()
        if len(sys.argv) == 3
        else root / "AllClasses.java"
    )

    if not root.is_dir():
        print(f"Error: {root} is not a directory")
        sys.exit(1)

    merge_java_files(root, output)


if __name__ == "__main__":
    main()
