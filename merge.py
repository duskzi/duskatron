#!/usr/bin/env python3

from pathlib import Path
import re
import sys


PACKAGE_RE = re.compile(r"^\s*package\s+[^;]+;\s*$")

IMPORT_RE = re.compile(
    r"^\s*import\s+(static\s+)?([^;]+);\s*$"
)

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
        # Don't replace:
        #
        #     GunUtils.getBestPower(...)
        #
        # with:
        #
        #     GunUtils.GunUtils.getBestPower(...)
        #
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
        if PACKAGE_RE.match(line):
            continue

        match = IMPORT_RE.match(line)

        if match:
            is_static = match.group(1) is not None
            imported = match.group(2).strip()

            # Internal Duskatron imports no longer exist after flattening.
            if imported.startswith("duskatron."):
                if is_static:
                    # Example:
                    #
                    # import static duskatron.gun.GunUtils.getBestPower;
                    #
                    # imported =
                    # duskatron.gun.GunUtils.getBestPower
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
    main_class: str = "Duskatron",
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
                f"// ===== {relative} =====\n\n"
                f"{text}"
            )

    with output.open("w", encoding="utf-8") as out:

        # Imports must appear before every type.
        for imp in sorted(all_external_imports):
            out.write(imp + "\n")

        out.write("\n\n")

        # All classes/interfaces/enums/etc.
        out.write(
            "\n\n\n".join(sections)
        )

        out.write("\n")

    print(
        f"Merged {len(files)} files into {output}"
    )


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
