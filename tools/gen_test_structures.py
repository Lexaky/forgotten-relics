"""Empty GameTest structures (gzipped NBT), modelled on Thaumaturge's data/thaumaturge/structure/empty_5x5x7.nbt.

Writes data/forgottenrelics/structure/empty_<x>x<y>x<z>.nbt. The infusion-altar tests need 17x8x17 (a 13x13 altar
with jars around it). Run: python -I tools/gen_test_structures.py
"""
import gzip
import io
import pathlib
import struct
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/data/forgottenrelics/structure"
TT_JAR = pathlib.Path.home() / ".gradle/caches/modules-2/files-2.1/curse.maven/thaumaturge-1628024"
SIZES = [(17, 8, 17)]


def read_data_version():
    """DataVersion of Thaumaturge's own empty structure (it targets the same Minecraft)."""
    jar = next(TT_JAR.rglob("thaumaturge-1628024-*.jar"))
    raw = gzip.decompress(zipfile.ZipFile(jar).read("data/thaumaturge/structure/empty_5x5x7.nbt"))
    i = raw.find(b"DataVersion")
    return struct.unpack(">i", raw[i + len(b"DataVersion"):i + len(b"DataVersion") + 4])[0]


def name(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b


def tag_int(n, v):
    return b"\x03" + name(n) + struct.pack(">i", v)


def tag_string(n, v):
    return b"\x08" + name(n) + name(v)


def tag_list(n, elem_type, payloads):
    return b"\x09" + name(n) + bytes([elem_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def structure(size, data_version):
    air = tag_string("Name", "minecraft:air") + b"\x00"  # one compound: {Name:"minecraft:air"}
    body = (tag_int("DataVersion", data_version)
            + tag_list("size", 3, [struct.pack(">i", v) for v in size])
            + tag_list("palette", 10, [air])
            + tag_list("blocks", 10, [])
            + tag_list("entities", 10, []))
    return b"\x0a" + name("") + body + b"\x00"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    version = read_data_version()
    for size in SIZES:
        path = OUT / f"empty_{size[0]}x{size[1]}x{size[2]}.nbt"
        buf = io.BytesIO()
        with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as gz:
            gz.write(structure(size, version))
        path.write_bytes(buf.getvalue())
        print(f"{path.relative_to(ROOT)} (DataVersion {version})")


if __name__ == "__main__":
    main()
