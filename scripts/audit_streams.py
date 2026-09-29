#!/usr/bin/env python3
"""
Audit every HLS stream in the bundled channel catalogue.

A catalogue entry that is broken is worse than a missing one: it takes up a slot
in the grid, appears in a category, and fails only when the viewer commits to it.
The app cannot detect this at runtime cheaply, so it has to be caught here.

Why this is more than "does the URL return 200"
-----------------------------------------------
That check is close to useless on its own, and it has already fooled this
project once. A channel's fMP4 path answered 200 on the master, 200 on the
variant, and then 403 on every single media segment — so it passed every check
that stops at the playlist and could never play.

So each stream is walked all the way down to media bytes:

  1. fetch the URL and require a playlist body (not an HTML error page, which
     most CDNs serve with a 200)
  2. if it is a master playlist, pick the highest-bandwidth variant and fetch it
  3. fetch a real segment from that variant and require actual media bytes

Only a stream that survives all three is reported as OK. Anything short of that
is reported with the reason and the exact step it failed at, so the fix is
obvious.

Usage:
    python3 scripts/audit_streams.py                 # audit the catalogue
    python3 scripts/audit_streams.py --json out.json # machine-readable
    python3 scripts/audit_streams.py --url URL       # probe one URL
    python3 scripts/audit_streams.py --concurrency 6
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field, asdict

CATALOG = "app/src/main/java/com/example/kurdishtv/model/KurdishChannelCatalog.kt"

# The same User-Agent the app sends, so a CDN that treats the two differently
# is audited under the conditions the app actually meets.
UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
)

TIMEOUT = 12
# A segment smaller than this is an error page or an empty body wearing a
# correct content type. Real TS segments are tens of KB; fMP4 init+media more.
MIN_SEGMENT_BYTES = 1024

# Seconds to wait between retries, multiplied by the attempt number. Live
# playlists roll quickly, so a failure often resolves within a second or two.
RETRY_BACKOFF_S = 1.5

# A channel that takes this long to return its first playlist feels slow to tune
# to on a television. The audit flags these so a CDN-hosted alternative can be
# looked for - swapping one of these is usually the single biggest perceived
# speed-up available, and it is invisible in the source.
SLOW_MS = 400

# Magic bytes of container formats a real HLS segment can start with.
#  0x47 ....... MPEG-TS 188-byte packets
#  ....ftyp    ISO-BMFF (fMP4 / fragmented MP4)
#  ID3 / \x00\x00\x01\xb0  ID3-tagged MP3/AAC in TS
MEDIA_SIGNATURES = (b"\x47", b"ID3", b"\xff\xfb", b"\xff\xf3", b"\x00\x00\x01\xb0", b"OggS")


@dataclass
class Channel:
    id: str
    name: str
    stream_url: str
    category: str = ""
    logo_url: str = ""


@dataclass
class Result:
    id: str
    name: str
    url: str
    category: str = ""
    ok: bool = False
    failed_at: str = ""
    detail: str = ""
    flaky: bool = False
    attempts: int = 1
    kind: str = ""
    variant: str = ""
    segment_bytes: int = 0
    width: int = 0
    height: int = 0
    latency_ms: int = 0
    tags: list = field(default_factory=list)


# ── catalogue extraction ────────────────────────────────────────────────────

ENTRY_RE = re.compile(
    r"Channel\(\s*"
    r'id\s*=\s*"([^"]*)"\s*,\s*'
    r'name\s*=\s*"([^"]*)"\s*,\s*'
    r'streamUrl\s*=\s*"([^"]*)"',
    re.S,
)
CATEGORY_RE = re.compile(r'category\s*=\s*"([^"]*)"')
ID_FIELD_RE = re.compile(r'^\s*id\s*=\s*"', re.M)
# Line comments are stripped before parsing. A comment placed between `name` and
# `streamUrl` is enough to make a strict pattern miss the entry, and a parser
# that misses entries is the worst kind of bug in a tool whose entire job is to
# check things - it reports a clean bill of health for channels it never looked
# at. That is not hypothetical: it is how the first version of this script lost
# sight of three channels.
LINE_COMMENT_RE = re.compile(r"^\s*//.*$", re.M)


def load_catalog(path: str) -> list[Channel]:
    """Pull channel entries straight out of the Kotlin source.

    Parsing the source rather than a hand-maintained list is the point: the list
    cannot drift from what the app actually ships.
    """
    with open(path, encoding="utf-8") as fh:
        raw = fh.read()

    # Strip line comments, then blank the lines they occupied so a multi-line
    # comment cannot leave dangling tokens behind.
    src = LINE_COMMENT_RE.sub("", raw)

    channels: list[Channel] = []
    for match in ENTRY_RE.finditer(src):
        tail = src[match.end() : match.end() + 400]
        cat = CATEGORY_RE.search(tail)
        logo = re.search(r'logoUrl\s*=\s*"([^"]*)"', tail)
        channels.append(
            Channel(
                id=match.group(1),
                name=match.group(2),
                stream_url=match.group(3),
                category=cat.group(1) if cat else "",
                logo_url=logo.group(1) if logo else "",
            )
        )

    # Cross-check. If the source grew a channel in a shape this parser does not
    # recognise, the count below will not match and the audit would otherwise
    # quietly under-report. Failing loudly here is the whole difference between
    # a tool you can trust and a tool that reassures you.
    declared = len(ID_FIELD_RE.findall(src))
    if declared != len(channels):
        raise SystemExit(
            f"FATAL: {path} declares {declared} channels but the parser found "
            f"{len(channels)}.\n"
            f"The catalogue parser is out of date with the catalogue. Refusing to "
            f"report on a subset."
        )
    return channels


# ── playlist handling ───────────────────────────────────────────────────────


def fetch(url: str, limit: int | None = None, start: int = 0) -> tuple[int, bytes, str]:
    """GET with a byte cap, returning (status, body, content_type)."""
    headers = {
        "User-Agent": UA,
        "Accept": "*/*",
    }
    if limit is not None:
        headers["Range"] = f"bytes={start}-{start + limit - 1}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        body = resp.read(limit) if limit else resp.read()
        return resp.status, body, resp.headers.get("Content-Type", "")


def resolve(base: str, ref: str) -> str:
    from urllib.parse import urljoin

    return urljoin(base, ref.strip())


def looks_like_playlist(body: bytes) -> bool:
    if not body:
        return False
    head = body[:2048].lstrip()
    if head[:1] == b"<":
        return False  # an HTML error or captive-portal page
    return head[:7] == b"#EXTM3U" or b"#EXTINF" in head or b"#EXT-X-STREAM-INF" in head


def variant_attrs(line: str) -> dict:
    out = {}
    for key, val in re.findall(r'([A-Z0-9-]+)=("[^"]*"|[^,]*)', line):
        out[key] = val.strip('"')
    return out


def parse_playlist(text: str):
    """Return (variants, segments, is_master)."""
    variants, segments = [], []
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    for i, line in enumerate(lines):
        if line.startswith("#EXT-X-STREAM-INF"):
            nxt = lines[i + 1] if i + 1 < len(lines) else ""
            if not nxt.startswith("#"):
                variants.append((variant_attrs(line), nxt))
        elif line.startswith("#") or line == "":
            continue
        elif not any(
            line.startswith(p) for p in ("#EXT-X-KEY", "#EXT-X-MAP", "#EXT-X-MEDIA")
        ):
            segments.append(line)
    return variants, segments, bool(variants)


def pick_variant(variants):
    """Highest advertised bandwidth wins - that is what a viewer would get."""

    def bandwidth(v):
        attrs = v[0]
        try:
            return int(attrs.get("BANDWIDTH", 0))
        except ValueError:
            return 0

    return max(variants, key=bandwidth) if variants else None


def looks_like_media(body: bytes) -> bool:
    if len(body) < MIN_SEGMENT_BYTES:
        return False
    if body[:1] == b"<":
        return False
    if any(body.startswith(sig) for sig in MEDIA_SIGNATURES):
        return True
    # fMP4 and some packagers put a box header before 'ftyp'; look a little in.
    return b"ftyp" in body[:64] or b"moof" in body[:512] or b"styp" in body[:512]


# ── the audit ───────────────────────────────────────────────────────────────


def audit_one(ch: Channel) -> Result:
    r = Result(id=ch.id, name=ch.name, url=ch.stream_url, category=ch.category)
    url = ch.stream_url
    seen = set()

    # ── step 1+2: playlists, walking down through masters ──
    for depth in range(3):
        if url in seen:
            r.failed_at, r.detail = "playlist", f"redirect loop at {url}"
            return r
        seen.add(url)
        try:
            started = time.perf_counter()
            status, body, _ = fetch(url)
            r.latency_ms = int((time.perf_counter() - started) * 1000)
        except urllib.error.HTTPError as e:
            r.failed_at, r.detail = "playlist", f"HTTP {e.code}"
            return r
        except Exception as e:  # noqa: BLE001 - report the reason, do not abort
            r.failed_at, r.detail = "playlist", f"{type(e).__name__}: {e}"
            return r

        if status not in (200, 206):
            r.failed_at, r.detail = "playlist", f"HTTP {status}"
            return r
        if not looks_like_playlist(body):
            r.failed_at = "playlist"
            r.detail = "not a playlist (likely an HTML error page)"
            return r

        text = body.decode("utf-8", "replace")
        variants, segments, is_master = parse_playlist(text)
        if is_master:
            r.kind = "master" if not r.kind else "master>media"
        else:
            r.kind = r.kind or "media"

        if is_master:
            chosen = pick_variant(variants)
            if not chosen:
                r.failed_at, r.detail = "playlist", "master with no variant URIs"
                return r
            attrs, ref = chosen
            r.variant = f"{ref} ({attrs.get('BANDWIDTH', '?')} bps)"
            r.width, r.height = int(attrs.get("RESOLUTION", "0x0").split("x")[0] or 0), int(
                attrs.get("RESOLUTION", "0x0").split("x")[-1] or 0
            )
            url = resolve(url, ref)
            continue  # fetch the variant, then look for segments

        # ── step 3: a real media segment from a media playlist ──
        if not segments:
            r.failed_at, r.detail = "playlist", "media playlist with no segments"
            return r

        seg_url = resolve(url, segments[0])
        try:
            _, seg, _ = fetch(seg_url, limit=64 * 1024)
        except urllib.error.HTTPError as e:
            r.failed_at, r.detail = "segment", f"HTTP {e.code} on {segments[0]}"
            return r
        except Exception as e:  # noqa: BLE001
            r.failed_at, r.detail = "segment", f"{type(e).__name__}: {e}"
            return r

        if not looks_like_media(seg):
            r.failed_at = "segment"
            r.detail = f"no media bytes ({len(seg)}B, starts {seg[:8]!r})"
            return r

        r.segment_bytes = len(seg)
        r.ok = True
        return r

    r.failed_at, r.detail = "playlist", "more than 3 levels of master playlist"
    return r


def audit_url(url: str) -> Result:
    return audit_with_retries(Channel(id="-", name="(ad-hoc)", stream_url=url))


def audit_with_retries(ch: Channel, attempts: int = 3) -> Result:
    """Run [audit_one] until it succeeds or every attempt has failed.

    This is the difference between a tool you can act on and one that is not.

    A live stream is a moving target: the playlist is rewritten continuously, so
    a segment that was listed a moment ago can 404 by the time it is fetched, and
    an origin can drop a connection mid-body. Probing a live endpoint exactly once
    produces false negatives, and a false negative here is expensive - it reads as
    "this channel is dead" and invites deleting a channel that works fine.

    A failure is only reported as a failure once it has survived every attempt. A
    stream that succeeds on a retry is still reported, but flagged flaky so a
    genuinely unreliable source can be spotted rather than silently kept.
    """
    result = None
    for attempt in range(1, max(1, attempts) + 1):
        result = audit_one(ch)
        result.attempts = attempt
        if result.ok:
            if attempt > 1:
                result.flaky = True
                result.detail = f"verified on attempt {attempt} of {attempts}"
            return result
        if attempt < attempts:
            time.sleep(RETRY_BACKOFF_S * attempt)
    return result


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--catalog", default=CATALOG)
    ap.add_argument("--url", help="probe a single URL instead of the catalogue")
    ap.add_argument("--json", help="write results as JSON to this path")
    ap.add_argument("--concurrency", type=int, default=6)
    ap.add_argument(
        "--attempts",
        type=int,
        default=3,
        help="attempts per stream before it is called dead (default 3)",
    )
    args = ap.parse_args()

    if args.url:
        results = [audit_with_retries(
            Channel(id="-", name="(ad-hoc)", stream_url=args.url), args.attempts
        )]
    else:
        channels = load_catalog(args.catalog)
        if not channels:
            print(f"No channels parsed from {args.catalog}", file=sys.stderr)
            return 2
        print(
            f"Auditing {len(channels)} catalogue streams "
            f"({args.attempts} attempts each)...\n",
            file=sys.stderr,
        )
        with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
            results = list(
                pool.map(lambda c: audit_with_retries(c, args.attempts), channels)
            )

    ok = [r for r in results if r.ok]
    flaky = [r for r in ok if r.flaky]
    bad = [r for r in results if not r.ok]

    for r in sorted(bad, key=lambda x: (x.failed_at, x.name)):
        print(f"  DEAD  [{r.failed_at:9}] {r.name:<28} {r.url}")
        print(f"        {r.detail}  (after {r.attempts} attempts)")

    if flaky:
        print(f"\n  {len(flaky)} stream(s) needed a retry - unreliable, but working:")
        for r in sorted(flaky, key=lambda x: x.name):
            print(f"  flaky {r.name:<30} {r.detail}")

    print(f"\n{len(ok)}/{len(results)} streams verified end to end")

    if ok:
        # Latency to the first playlist is the most visible latency a viewer
        # experiences in a TV app: it is the wait between pressing OK and seeing
        # video. Sorted slowest-first, because that is the order worth acting on.
        #
        # These numbers are measured from wherever this script runs, so they
        # describe *that* vantage point, not a viewer's sofa. A ratio between two
        # origins (one CDN against one bare origin) survives that reasonably
        # well; an absolute millisecond figure does not. Treat the list as
        # "look here next", not as "these are slow for viewers".
        slow = [r for r in ok if r.latency_ms >= SLOW_MS]
        print("\nWorking streams (slowest first - first-playlist latency):")
        for r in sorted(ok, key=lambda x: -x.latency_ms):
            dims = f"{r.width}x{r.height}" if r.height else "?"
            mark = "flaky" if r.flaky else ("slow" if r.latency_ms >= SLOW_MS else "ok")
            print(
                f"  {mark:<5} {r.name:<28} {r.latency_ms:>6}ms  "
                f"{r.kind:<13} {dims:<10} {r.segment_bytes}B"
            )
        if slow:
            print(
                f"\n  {len(slow)} stream(s) at or above {SLOW_MS}ms to first playlist. "
                f"Look for a CDN-hosted alternative for these."
            )

    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump([asdict(r) for r in results], fh, indent=2)
        print(f"\nJSON written to {args.json}", file=sys.stderr)

    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
