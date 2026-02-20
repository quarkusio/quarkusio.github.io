#!/usr/bin/env python3
"""
Generate _data/domains.yaml from quarkus.yaml and guides-landing-page-domains.md.

This script:
1. Parses _data/versioned/latest/index/quarkus.yaml to build a title-to-guide lookup
2. Parses the 15 domain sections in guides-landing-page-domains.md
3. Outputs _data/domains.yaml with domain entries containing ordered guide URLs

Usage:
    cd quarkusio.github.io
    python3 _scripts/generate-domains-yaml.py
"""

import re
import sys
from pathlib import Path

import yaml


SITE_ROOT = Path(__file__).resolve().parent.parent
QUARKUS_YAML = SITE_ROOT / "_data" / "versioned" / "latest" / "index" / "quarkus.yaml"
DOMAINS_MD = SITE_ROOT / "_prototype" / "guides-landing-page-domains.md"
OUTPUT = SITE_ROOT / "_data" / "domains.yaml"

# Cross-referenced guides: maps (title, secondary_domain_number) to mark also-in.
# Primary domain listed first in the domains doc gets no flag;
# secondary domain gets also-in: true.
CROSS_REFS = {
    # title -> set of domain numbers where it's secondary
    "Building Quarkus apps with Quarkus Command Line Interface (CLI)": {11},  # secondary in "Use Build Tools"
    "Secrets in Configuration": {9},  # secondary in "Understand the Runtime"
    "Tips for writing native applications": {13},  # secondary in "Write Extensions"
}

# Title aliases: domains.md title -> quarkus.yaml title.
# Handles cases where the domains document uses a shortened or outdated title.
TITLE_ALIASES = {
    "Writing REST Services with Quarkus REST": "Writing REST Services with Quarkus REST (formerly RESTEasy Reactive)",
    "Migrating to Quarkus REST": "Migrating to Quarkus REST (formerly RESTEasy Reactive)",
    "Config Reference Guide": "Configuration Reference Guide",
}


def load_quarkus_guides(path: Path) -> dict[str, dict]:
    """Build a title -> guide metadata lookup from quarkus.yaml."""
    with open(path) as f:
        data = yaml.safe_load(f)

    lookup = {}
    for type_key, guides in data.get("types", {}).items():
        if guides is None:
            continue
        for guide in guides:
            title = guide.get("title", "").strip()
            if title:
                lookup[title] = guide
    return lookup


def parse_domains_md(path: Path) -> list[dict]:
    """Parse domain sections from guides-landing-page-domains.md.

    Returns a list of domain dicts with keys:
      id, title, job, order, guides (list of {url, also_in})
    """
    text = path.read_text()
    # Split on domain headers: ## 1. Get Started, ## 2. Build Backend APIs, etc.
    domain_pattern = re.compile(r'^## (\d+)\. (.+)$', re.MULTILINE)
    matches = list(domain_pattern.finditer(text))

    domains = []
    for i, match in enumerate(matches):
        order = int(match.group(1))
        title = match.group(2).strip()
        domain_id = title_to_id(title)

        # Extract the text between this header and the next
        start = match.end()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        section_text = text[start:end]

        # Extract the job statement (first blockquote that isn't a learning path or note)
        job = extract_job(section_text)

        # Extract guide titles from the markdown table
        guide_titles = extract_table_titles(section_text, order)

        domains.append({
            "order": order,
            "id": domain_id,
            "title": title,
            "job": job,
            "guide_titles": guide_titles,
        })

    return domains


def title_to_id(title: str) -> str:
    """Convert a domain title to kebab-case id."""
    # Lowercase, replace spaces and special chars with hyphens
    result = title.lower()
    result = re.sub(r'[^a-z0-9]+', '-', result)
    return result.strip('-')


def extract_job(section_text: str) -> str:
    """Extract the JTBD job statement from the first blockquote in a section."""
    lines = section_text.split('\n')
    job_lines = []
    in_job = False

    for line in lines:
        stripped = line.strip()
        if stripped.startswith('> "When') or stripped.startswith("> \"When"):
            in_job = True
            # Remove the leading > and quotes
            content = stripped.lstrip('>').strip().strip('"')
            job_lines.append(content)
        elif in_job and stripped.startswith('>') and not stripped.startswith('> **'):
            # Continuation of the job blockquote, but not a learning path or note
            content = stripped.lstrip('>').strip().strip('"')
            if content:
                job_lines.append(content)
        elif in_job:
            break

    return ' '.join(job_lines).rstrip('"')


def extract_table_titles(section_text: str, domain_order: int) -> list[dict]:
    """Extract guide titles from markdown table rows.

    Returns list of {title: str, also_in: bool}.
    """
    lines = section_text.split('\n')
    titles = []
    in_table = False
    header_col = None

    for line in lines:
        stripped = line.strip()
        if not stripped.startswith('|'):
            if in_table:
                break  # End of table
            continue

        cells = [c.strip() for c in stripped.split('|')]
        # Remove empty first/last from leading/trailing pipes
        cells = [c for c in cells if c or cells.index(c) not in (0, len(cells) - 1)]
        if not cells:
            continue

        # Detect header row
        if 'Current Title' in cells[0]:
            header_col = 0
            in_table = True
            continue
        elif 'Expected Title' in cells[0]:
            # Domain 15 (Build AI Applications) has no current guides
            header_col = 0
            in_table = True
            continue

        # Skip separator row
        if in_table and cells[0].startswith('---'):
            continue

        if in_table and header_col is not None:
            title = cells[header_col].strip()
            if title:
                also_in = title in CROSS_REFS and domain_order in CROSS_REFS[title]
                titles.append({"title": title, "also_in": also_in})

    return titles


def generate_domains_yaml(domains: list[dict], lookup: dict[str, dict]) -> dict:
    """Build the final domains.yaml structure."""
    output_domains = []
    unmatched = []

    for domain in domains:
        guides = []
        for entry in domain["guide_titles"]:
            title = entry["title"]
            # Try exact match first, then alias
            guide = lookup.get(title) or lookup.get(TITLE_ALIASES.get(title, ""))

            if guide is None:
                # Domain 15 has "Expected Title" entries that won't match
                if domain["order"] == 15:
                    continue
                unmatched.append((domain["title"], title))
                continue

            guide_entry = {
                "url": guide["url"],
                "title": guide["title"],
                "type": guide.get("type", "guide"),
            }
            if entry["also_in"]:
                guide_entry["also-in"] = True
            guides.append(guide_entry)

        output_domains.append({
            "id": domain["id"],
            "title": domain["title"],
            "job": domain["job"],
            "order": domain["order"],
            "guides": guides,
        })

    if unmatched:
        print("WARNING: Unmatched titles:", file=sys.stderr)
        for domain_title, guide_title in unmatched:
            print(f"  [{domain_title}] {guide_title!r}", file=sys.stderr)

    return {"domains": output_domains}


def main():
    if not QUARKUS_YAML.exists():
        print(f"Error: {QUARKUS_YAML} not found", file=sys.stderr)
        sys.exit(1)
    if not DOMAINS_MD.exists():
        print(f"Error: {DOMAINS_MD} not found", file=sys.stderr)
        sys.exit(1)

    print(f"Loading guides from {QUARKUS_YAML}...")
    lookup = load_quarkus_guides(QUARKUS_YAML)
    print(f"  Found {len(lookup)} guides in quarkus.yaml")

    print(f"Parsing domains from {DOMAINS_MD}...")
    domains = parse_domains_md(DOMAINS_MD)
    print(f"  Found {len(domains)} domains")

    result = generate_domains_yaml(domains, lookup)

    # Count total guides
    total = sum(len(d["guides"]) for d in result["domains"])
    print(f"  Total guide entries in domains.yaml: {total}")

    with open(OUTPUT, 'w') as f:
        f.write("# Initially generated by _scripts/generate-domains-yaml.py — manual edits are the preferred update method\n")
        f.write("---\n")
        yaml.dump(result, f, default_flow_style=False, sort_keys=False, allow_unicode=True, width=120)

    print(f"Written to {OUTPUT}")


if __name__ == "__main__":
    main()
