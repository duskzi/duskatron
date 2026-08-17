#!/usr/bin/env python3

from pathlib import Path
from datetime import datetime
import re
import sys
import time

PACKAGE_NAME = "duskatron"
MAIN_CLASS = "Duskatron"

PACKAGE_RE = re.compile(r"^\s*package\s+[^;]+;\s*$")
IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([^;]+);\s*$")
PUBLIC_TYPE_RE = re.compile(
    r"^(\s*)public\s+"
    r"((?:(?:abstract|final|sealed|non-sealed)\s+)*)"
    r"(class|interface|enum|record)\s+([A-Za-z_$][\w$]*)"
)


def replace_static_calls(text: str, static_methods: dict[str, str]) -> str:
    """
    Convert:

        getBestPower(...)

    into:

        GunUtils.getBestPower(...)

    only when that method came from an internal static import.

    Already-qualified calls are left alone.
    """

    for method, owner in static_methods.items():
        pattern = rf"(?<![\w$.]){re.escape(method)}\s*\("

        text = re.sub(
            pattern,
            f"{owner}.{method}(",
            text,
        )

    return text


def process_file(path: Path):
    """
    Returns:

        external imports
        internal static imports
        source body
    """

    external_imports = set()
    static_imports = {}
    body = []

    for line in path.read_text(encoding="utf-8").splitlines():

        # Remove package declarations.
        # The merged file gets a single package declaration.
        if PACKAGE_RE.match(line):
            continue

        match = IMPORT_RE.match(line)

        if match:
            is_static = match.group(1) is not None
            imported = match.group(2).strip()

            # Internal Duskatron imports no longer exist after flattening.
            if imported.startswith("duskatron."):
                if is_static:
                    parts = imported.split(".")

                    method = parts[-1]
                    owner = parts[-2]

                    static_imports[method] = owner

                continue

            # Keep Java / Robocode / other external imports.
            external_imports.add(
                ("import static " if is_static else "import ")
                + imported
                + ";"
            )

            continue

        body.append(line)

    return external_imports, static_imports, body


def remove_public_from_non_main_types(
    lines: list[str],
    main_class: str = MAIN_CLASS,
) -> list[str]:

    result = []
    brace_depth = 0

    for line in lines:

        if brace_depth == 0:
            match = PUBLIC_TYPE_RE.match(line)

            if match:
                name = match.group(4)

                # Only the main Robocode robot remains public.
                if name != main_class:
                    indentation = match.group(1)
                    modifiers = match.group(2)
                    kind = match.group(3)

                    line = (
                        f"{indentation}"
                        f"{modifiers}"
                        f"{kind} "
                        f"{name}"
                        + line[match.end():]
                    )

        result.append(line)

        brace_depth += line.count("{")
        brace_depth -= line.count("}")

        if brace_depth < 0:
            brace_depth = 0

    return result


def merge_java_files(root: Path, output: Path):
    merge_started = time.perf_counter()
    start_datetime = datetime.now()

    files = sorted(
        path
        for path in root.rglob("*.java")
        if path.resolve() != output.resolve()
    )

    if not files:
        print(f"No Java files found in {root}")
        return

    all_external_imports = set()
    all_static_imports = {}
    sections = []

    for file in files:
        external_imports, static_imports, body = process_file(file)

        all_external_imports.update(external_imports)
        all_static_imports.update(static_imports)

        text = "\n".join(body)

        # Convert internal static imports into class-qualified calls.
        text = replace_static_calls(
            text,
            static_imports,
        )

        body = text.splitlines()

        # Only Duskatron stays public.
        body = remove_public_from_non_main_types(body)

        text = "\n".join(body).strip()

        if text:
            relative = file.relative_to(root)

            sections.append(
                f"/* ---- {relative} ---- */\n\n"
                f"{text}"
            )

    # Measure the actual merge/write operation.
    with output.open("w", encoding="utf-8") as out:

        # Keep the package declaration at the very top.
        out.write(f"package {PACKAGE_NAME};\n\n")

        # Metadata.
        out.write("/*\n")
        out.write("    .-------------------------.\n")
        out.write("    | Duskatron Merge Utility |\n")
        out.write("    '-------------------------'\n\n")
        out.write(f"    Script: {Path(__file__).name}\n")
        out.write(f"    Package: {PACKAGE_NAME}\n")
        out.write(f"    Main class: {MAIN_CLASS}\n")
        out.write(f"    Files merged: {len(files)}\n")
        out.write(f"    Source directory: {root}\n")
        out.write("*/\n\n")

        # Imports must appear before every type.
        for imp in sorted(all_external_imports):
            out.write(imp + "\n")

        out.write("\n\n")

        # All classes/interfaces/enums/etc.
        out.write(
            "\n\n\n".join(sections)
        )

        out.write("\n")

    merge_duration = time.perf_counter() - merge_started
    end_datetime = datetime.now()

    # Replace the temporary metadata with the real duration.
    content = output.read_text(encoding="utf-8")

    content = content.replace(
        "// Merge duration: calculated after merge",
        f"// Merge duration: {merge_duration:.6f} seconds",
    )

    content = content.replace(
        "// Merge started: "
        f"{start_datetime.strftime('%Y-%m-%d %H:%M:%S')}",
        "// Merge started: "
        f"{start_datetime.strftime('%Y-%m-%d %H:%M:%S')}",
    )

    # Add the exact completion timestamp.
    content = content.replace(
        f"// Merge duration: {merge_duration:.6f} seconds",
        f"// Merge duration: {merge_duration:.6f} seconds\n"
        f"// Merge finished: "
        f"{end_datetime.strftime('%Y-%m-%d %H:%M:%S')}",
    )

    output.write_text(content, encoding="utf-8")

    print(f"Merged {len(files)} files into {output}")
    print(f"Merge time: {merge_duration:.6f} seconds")


def main():
    if len(sys.argv) not in (2, 3):
        print(
            f"Usage: {sys.argv[0]} "
            f"<source-directory> [output.java]"
        )
        sys.exit(1)

    root = Path(sys.argv[1]).resolve()

    if not root.is_dir():
        print(f"Not a directory: {root}")
        sys.exit(1)

    output = (
        Path(sys.argv[2]).resolve()
        if len(sys.argv) == 3
        else root.parent / "Duskatron.java"
    )

    merge_java_files(root, output)


if __name__ == "__main__":
    main()