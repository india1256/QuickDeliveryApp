import hashlib
import os
import struct
import subprocess
import zipfile
import zlib

def resolve_base_apk() -> str:
    candidates = [
        "app/build/outputs/apk/debug/app-debug.apk",
        ".build-outputs/app-debug.apk",
    ]
    existing = [p for p in candidates if os.path.exists(p) and os.path.getsize(p) > 1000000]
    if not existing:
        return ".build-outputs/app-debug.apk"
    existing.sort(key=lambda p: os.path.getmtime(p), reverse=True)
    return existing[0]


KEYSTORE = "debug.keystore"


def extract_private_key_pem():
    pem = subprocess.check_output(
        ["openssl", "pkcs12", "-in", KEYSTORE, "-passin", "pass:android", "-nodes", "-nocerts"]
    )
    key_path = "/tmp/qd_apk_sign_key.pem"
    with open(key_path, "wb") as f:
        f.write(pem)
    return key_path


def patch_classes11_dex_for_customer_live_gps(raw_dex: bytes) -> bytes:
    """
    Patches classes11.dex so that in CustomerAppScreen:
    1. Format string '%.5f, %.5f' -> '%.6f, %.6f' (exact 6-decimal Latitude & Longitude).
    2. Label 'Pickup Area / Locality / Street' -> 'Pickup Area / Live Lat, Lng GPS'.
    3. Tapping the 'Use GPS' button in the Customer Pickup section always invokes
       locationPermissionLauncher and writes the exact live GPS Latitude & Longitude
       directly into pickupAddressText$delegate (v31) as well as pickupLat & pickupLng.
    4. Recomputes DEX SHA-1 signature and Adler32 checksum.
    """
    dex = bytearray(raw_dex)

    # 1. '%.5f, %.5f' -> '%.6f, %.6f' at offset 643548
    if dex[643548:643559] == b"\x0a%.5f, %.5f":
        dex[643548:643559] = b"\x0a%.6f, %.6f"

    # 2. 'Pickup Area / Locality / Street' -> 'Pickup Area / Live Lat, Lng GPS' at offset 744946
    if dex[744946:744978] == b"\x1fPickup Area / Locality / Street":
        dex[744946:744978] = b"\x1fPickup Area / Live Lat, Lng GPS"

    # 3. In CustomerAppScreen$lambda$490$...$lambda$326 (code_off = 160380):
    # Replace if-eqz v0, +0x14 (38 00 14 00) at 160380 + 16 + 12 = 160408 with goto +0x14; nop (28 14 00 00)
    if dex[160408:160412] == bytes.fromhex("38 00 14 00"):
        dex[160408:160412] = bytes.fromhex("28 14 00 00")

    # 4. In CustomerAppScreen$lambda$490 (code_off = 232444):
    # At 232444 + 16 + 10054 = 242514, replace move-object/from16 v19, v12 (08 13 0c 00)
    # with move-object/from16 v19, v31 (08 13 1f 00) so ExternalSyntheticLambda10 writes
    # the formatted live GPS coordinates directly into pickupAddressText$delegate (v31).
    if dex[242514:242518] == bytes.fromhex("08 13 0c 00"):
        dex[242514:242518] = bytes.fromhex("08 13 1f 00")

    # 5. Recompute DEX SHA-1 signature (bytes 12..32) and Adler32 checksum (bytes 8..12)
    sha1_digest = hashlib.sha1(dex[32:]).digest()
    dex[12:32] = sha1_digest
    adler_val = zlib.adler32(dex[12:]) & 0xFFFFFFFF
    struct.pack_into("<I", dex, 8, adler_val)

    return bytes(dex)


def patch_axml_manifest(raw_axml: bytes, target_role: str) -> bytes:
    """
    Patches the compiled binary AndroidManifest.xml so that:
    1. The package name & provider authorities use a distinct 6-char suffix:
       - CUSTOMER -> com.aistudio.quickdelivery.cust01
       - RIDER    -> com.aistudio.quickdelivery.ride02
       - ADMIN    -> com.aistudio.quickdelivery.admn03
    2. <application android:label="..."> and the single MAIN/LAUNCHER <activity>
       point directly to the role's standalone Activity and Label:
       - CUSTOMER -> com.example.CustomerMainActivity ("QD Customer App")
       - RIDER    -> com.example.RiderMainActivity ("QD Rider Partner")
       - ADMIN    -> com.example.AdminMainActivity ("QD Admin Console")
    """
    axml = bytearray(raw_axml)

    sp_type, sp_header_size, sp_size, str_count, style_count, flags, strings_start, styles_start = (
        struct.unpack("<HHIIIIII", axml[8:36])
    )
    offsets = [struct.unpack("<I", axml[36 + i * 4 : 40 + i * 4])[0] for i in range(str_count)]
    pool_base = 8 + strings_start
    strings = []
    for off in offsets:
        p = pool_base + off
        char_len = struct.unpack("<H", axml[p : p + 2])[0]
        strings.append(axml[p + 2 : p + 2 + char_len * 2].decode("utf-16le", "ignore"))

    suffix_map = {
        "CUSTOMER": "cust01",
        "RIDER": "ride02",
        "ADMIN": "admn03",
    }
    new_suffix = suffix_map[target_role]

    for i, s in enumerate(strings):
        if "com.aistudio.quickdelivery.ltkeqf" in s:
            replaced = s.replace("ltkeqf", new_suffix)
            assert len(replaced) == len(s)
            p = pool_base + offsets[i]
            axml[p + 2 : p + 2 + len(replaced) * 2] = replaced.encode("utf-16le")

    idx_main_act = strings.index("com.example.MainActivity")
    idx_cust_act = strings.index("com.example.CustomerMainActivity")
    idx_rider_act = strings.index("com.example.RiderMainActivity")
    idx_admin_act = strings.index("com.example.AdminMainActivity")

    idx_cust_lbl = strings.index("Quick Delivery")
    idx_rider_lbl = strings.index("Quick Delivery Partner")
    idx_admin_lbl = strings.index("Quick Delivery Admin")

    role_target = {
        "CUSTOMER": (idx_cust_act, idx_cust_lbl),
        "RIDER": (idx_rider_act, idx_rider_lbl),
        "ADMIN": (idx_admin_act, idx_admin_lbl),
    }
    target_act_idx, target_lbl_idx = role_target[target_role]

    pos = 8 + sp_size
    TYPE_STRING = 0x03000008
    while pos < len(axml):
        ctype, cheader, csize = struct.unpack("<HHI", axml[pos : pos + 8])
        if ctype == 0x0102:
            ns, name_idx, attr_start, attr_size, attr_count = struct.unpack(
                "<IIHHH", axml[pos + 16 : pos + 30]
            )
            tag = strings[name_idx] if name_idx < len(strings) else ""
            if tag == "application":
                for a in range(attr_count):
                    ap = pos + 16 + attr_start + a * attr_size
                    ans, aname, aval_str, atype, adata = struct.unpack("<IIIII", axml[ap : ap + 20])
                    k = strings[aname] if aname < len(strings) else ""
                    if k == "label":
                        struct.pack_into("<III", axml, ap + 8, target_lbl_idx, TYPE_STRING, target_lbl_idx)
            elif tag == "activity":
                act_name_val = None
                name_ap = None
                label_ap = None
                for a in range(attr_count):
                    ap = pos + 16 + attr_start + a * attr_size
                    ans, aname, aval_str, atype, adata = struct.unpack("<IIIII", axml[ap : ap + 20])
                    k = strings[aname] if aname < len(strings) else ""
                    if k == "name":
                        act_name_val = adata
                        name_ap = ap
                    elif k == "label":
                        label_ap = ap

                if act_name_val == idx_main_act and name_ap is not None:
                    struct.pack_into("<III", axml, name_ap + 8, target_act_idx, TYPE_STRING, target_act_idx)
                    if label_ap is not None:
                        struct.pack_into("<III", axml, label_ap + 8, target_lbl_idx, TYPE_STRING, target_lbl_idx)
                elif act_name_val == target_act_idx and name_ap is not None:
                    struct.pack_into("<III", axml, name_ap + 8, idx_main_act, TYPE_STRING, idx_main_act)
        pos += csize

    return bytes(axml)


def replace_zip_entry_raw(apk_bytes: bytes, target_entry_name: str, new_uncompressed_bytes: bytes) -> bytes:
    """
    Replaces a single DEFLATE-compressed entry inside the unsigned/v2-structured APK
    and updates Local File Header, Central Directory, and EOCD offsets.
    """
    eocd_off = apk_bytes.rfind(b"PK\x05\x06")
    cd_size = struct.unpack("<I", apk_bytes[eocd_off + 12 : eocd_off + 16])[0]
    cd_off = struct.unpack("<I", apk_bytes[eocd_off + 16 : eocd_off + 20])[0]
    block_size_2 = struct.unpack("<Q", apk_bytes[cd_off - 24 : cd_off - 16])[0]
    sig_block_start = cd_off - 8 - block_size_2
    orig_sig_block = apk_bytes[sig_block_start:cd_off]

    comp_obj = zlib.compressobj(level=6, method=zlib.DEFLATED, wbits=-15)
    new_comp_data = comp_obj.compress(new_uncompressed_bytes) + comp_obj.flush()
    new_crc = zlib.crc32(new_uncompressed_bytes) & 0xFFFFFFFF
    new_comp_size = len(new_comp_data)
    new_uncomp_size = len(new_uncompressed_bytes)

    cd_bytes = bytearray(apk_bytes[cd_off:eocd_off])
    pos = 0
    entry_lh_off = None
    old_comp_size = None
    entry_cd_pos = None

    while pos < len(cd_bytes):
        assert cd_bytes[pos : pos + 4] == b"PK\x01\x02"
        fname_len, extra_len, comment_len = struct.unpack("<HHH", cd_bytes[pos + 28 : pos + 34])
        fname = cd_bytes[pos + 46 : pos + 46 + fname_len].decode("utf-8", "ignore")
        lh_off = struct.unpack("<I", cd_bytes[pos + 42 : pos + 46])[0]
        if fname == target_entry_name:
            entry_cd_pos = pos
            entry_lh_off = lh_off
            old_comp_size = struct.unpack("<I", cd_bytes[pos + 20 : pos + 24])[0]
            break
        pos += 46 + fname_len + extra_len + comment_len

    assert entry_lh_off is not None, f"Entry {target_entry_name} not found"
    assert apk_bytes[entry_lh_off : entry_lh_off + 4] == b"PK\x03\x04"
    lh_fname_len, lh_extra_len = struct.unpack(
        "<HH", apk_bytes[entry_lh_off + 26 : entry_lh_off + 30]
    )
    lh_data_start = entry_lh_off + 30 + lh_fname_len + lh_extra_len
    lh_data_end = lh_data_start + old_comp_size

    gp_flag = struct.unpack("<H", apk_bytes[entry_lh_off + 6 : entry_lh_off + 8])[0]
    has_data_desc = (gp_flag & 0x0008) != 0
    dd_len = 0
    if has_data_desc:
        if apk_bytes[lh_data_end : lh_data_end + 4] == b"PK\x07\x08":
            dd_len = 16
        else:
            dd_len = 12

    delta = new_comp_size - old_comp_size

    new_lh = bytearray(apk_bytes[entry_lh_off:lh_data_start])
    struct.pack_into("<III", new_lh, 14, new_crc, new_comp_size, new_uncomp_size)

    new_dd = bytearray()
    if has_data_desc:
        if dd_len == 16:
            new_dd = bytearray(struct.pack("<4sIII", b"PK\x07\x08", new_crc, new_comp_size, new_uncomp_size))
        else:
            new_dd = bytearray(struct.pack("<III", new_crc, new_comp_size, new_uncomp_size))

    sec1 = (
        apk_bytes[:entry_lh_off]
        + bytes(new_lh)
        + new_comp_data
        + bytes(new_dd)
        + apk_bytes[lh_data_end + dd_len : sig_block_start]
    )

    pos = 0
    while pos < len(cd_bytes):
        fname_len, extra_len, comment_len = struct.unpack("<HHH", cd_bytes[pos + 28 : pos + 34])
        lh_off = struct.unpack("<I", cd_bytes[pos + 42 : pos + 46])[0]
        if pos == entry_cd_pos:
            struct.pack_into("<III", cd_bytes, pos + 16, new_crc, new_comp_size, new_uncomp_size)
        elif lh_off > entry_lh_off:
            struct.pack_into("<I", cd_bytes, pos + 42, lh_off + delta)
        pos += 46 + fname_len + extra_len + comment_len

    new_cd_off = len(sec1) + len(orig_sig_block)
    eocd_bytes = bytearray(apk_bytes[eocd_off:])
    struct.pack_into("<II", eocd_bytes, 12, len(cd_bytes), new_cd_off)

    return sec1 + orig_sig_block + bytes(cd_bytes) + bytes(eocd_bytes)


def resign_apk_v2(apk_bytes: bytes, key_path: str) -> bytes:
    """
    Re-computes 4096-byte aligned APK Signature Scheme v2 block and signs with debug.keystore.
    """
    eocd_off = apk_bytes.rfind(b"PK\x05\x06")
    cd_size = struct.unpack("<I", apk_bytes[eocd_off + 12 : eocd_off + 16])[0]
    cd_off = struct.unpack("<I", apk_bytes[eocd_off + 16 : eocd_off + 20])[0]
    block_size_2 = struct.unpack("<Q", apk_bytes[cd_off - 24 : cd_off - 16])[0]
    sig_block_start = cd_off - 8 - block_size_2

    orig_sig_block = bytearray(apk_bytes[sig_block_start:cd_off])
    sec1 = apk_bytes[:sig_block_start]
    cd_bytes = bytearray(apk_bytes[cd_off:eocd_off])

    new_sig_block_start = len(sec1)
    pair1_len = struct.unpack("<Q", orig_sig_block[8:16])[0]
    v2_pair_bytes = bytearray(orig_sig_block[8 : 16 + pair1_len])

    base_overhead = 8 + len(v2_pair_bytes) + 12 + 8 + 16
    rem = (new_sig_block_start + base_overhead) % 4096
    pad_len = (4096 - rem) % 4096
    pad_pair_bytes = struct.pack("<QI", 4 + pad_len, 0x42726577) + (b"\x00" * pad_len)
    block_size_val = len(v2_pair_bytes) + len(pad_pair_bytes) + 8 + 16

    new_cd_off = new_sig_block_start + 8 + block_size_val
    eocd_bytes = bytearray(apk_bytes[eocd_off:])
    struct.pack_into("<II", eocd_bytes, 12, len(cd_bytes), new_cd_off)

    eocd_for_digest = bytearray(eocd_bytes)
    struct.pack_into("<I", eocd_for_digest, 16, new_sig_block_start)

    chunk_digests = []
    for section in (sec1, bytes(cd_bytes), bytes(eocd_for_digest)):
        cpos = 0
        while cpos < len(section):
            chunk = section[cpos : cpos + 1048576]
            h = hashlib.sha256()
            h.update(b"\xa5" + struct.pack("<I", len(chunk)) + chunk)
            chunk_digests.append(h.digest())
            cpos += 1048576

    top_hash = hashlib.sha256()
    top_hash.update(b"\x5a" + struct.pack("<I", len(chunk_digests)) + b"".join(chunk_digests))
    new_top_digest = top_hash.digest()

    signed_data_len = struct.unpack("<I", v2_pair_bytes[20:24])[0]
    digest_offset_in_pair = 24 + 16
    assert len(new_top_digest) == 32
    v2_pair_bytes[digest_offset_in_pair : digest_offset_in_pair + 32] = new_top_digest

    signed_data = bytes(v2_pair_bytes[24 : 24 + signed_data_len])
    proc = subprocess.Popen(
        ["openssl", "dgst", "-sha256", "-sign", key_path],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
    )
    new_sig, _ = proc.communicate(signed_data)
    assert len(new_sig) == 256

    sig_offset_in_pair = 24 + signed_data_len + 16
    v2_pair_bytes[sig_offset_in_pair : sig_offset_in_pair + 256] = new_sig

    final_sig_block = (
        struct.pack("<Q", block_size_val)
        + bytes(v2_pair_bytes)
        + pad_pair_bytes
        + struct.pack("<Q", block_size_val)
        + b"APK Sig Block 42"
    )

    return sec1 + final_sig_block + bytes(cd_bytes) + bytes(eocd_bytes)


BUILD_TOOLS_DIR = "/opt/android/sdk/build-tools/36.0.0"
ZIPALIGN_BIN = os.path.join(BUILD_TOOLS_DIR, "zipalign")
APKSIGNER_BIN = os.path.join(BUILD_TOOLS_DIR, "apksigner")


def main():
    os.makedirs("public/apks", exist_ok=True)
    os.makedirs("/var/www/assets/apks", exist_ok=True)

    base_apk_path = resolve_base_apk()
    print(f"Using freshly compiled base APK: {base_apk_path}")
    raw_base_apk = open(base_apk_path, "rb").read()

    with zipfile.ZipFile(base_apk_path, "r") as z:
        orig_manifest = z.read("AndroidManifest.xml")
        large_dex_entries = [
            (info.filename, z.read(info.filename))
            for info in z.infolist()
            if info.filename.startswith("classes")
            and info.filename.endswith(".dex")
            and info.compress_size > 1000000
        ]

    # Re-compress large classes*.dex entries at DEFLATE level=6 so the APK stays ~30 MiB
    # (well under Google Cloud Run's 32.00 MiB = 33,554,432-byte HTTP response limit!)
    base_apk_shrunk = raw_base_apk
    for dex_name, dex_bytes in large_dex_entries:
        base_apk_shrunk = replace_zip_entry_raw(base_apk_shrunk, dex_name, dex_bytes)

    variants = [
        ("CUSTOMER", "QuickDelivery-Customer.apk"),
        ("RIDER", "QuickDelivery-Rider-Partner.apk"),
        ("ADMIN", "QuickDelivery-Admin-Enterprise.apk"),
    ]

    for role, filename in variants:
        patched_manifest = patch_axml_manifest(orig_manifest, role)
        apk_with_manifest = replace_zip_entry_raw(
            base_apk_shrunk, "AndroidManifest.xml", patched_manifest
        )

        unsigned_tmp = f"/tmp/unsigned_{role}.apk"
        aligned_tmp = f"/tmp/aligned_{role}.apk"
        signed_tmp = f"/tmp/signed_{role}.apk"

        with open(unsigned_tmp, "wb") as f:
            f.write(apk_with_manifest)

        # CRITICAL: Re-align all uncompressed entries (resources.arsc & .so) to 4-byte boundary!
        # Android 11+ (targetSdk 30+) strictly rejects unaligned resources.arsc with "App not installed."
        subprocess.run(
            [ZIPALIGN_BIN, "-f", "-p", "4", unsigned_tmp, aligned_tmp],
            check=True,
        )

        # Sign using official Android SDK apksigner with v1 + v2 + v3 schemes
        subprocess.run(
            [
                APKSIGNER_BIN,
                "sign",
                "--ks",
                KEYSTORE,
                "--ks-key-alias",
                "androiddebugkey",
                "--ks-pass",
                "pass:android",
                "--key-pass",
                "pass:android",
                "--v1-signing-enabled",
                "true",
                "--v2-signing-enabled",
                "true",
                "--v3-signing-enabled",
                "true",
                "--out",
                signed_tmp,
                aligned_tmp,
            ],
            check=True,
        )

        # Verify both 4-byte zipalign and apksigner before publishing
        subprocess.run([ZIPALIGN_BIN, "-c", "4", signed_tmp], check=True)
        subprocess.run([APKSIGNER_BIN, "verify", signed_tmp], check=True)

        signed_apk = open(signed_tmp, "rb").read()
        out_pub = os.path.join("public/apks", filename)
        out_www = os.path.join("/var/www/assets/apks", filename)
        with open(out_pub, "wb") as f:
            f.write(signed_apk)
        with open(out_www, "wb") as f:
            f.write(signed_apk)
        print(f"Built, zipaligned & v1/v2/v3-signed {filename} ({len(signed_apk)} bytes) for role={role}")


if __name__ == "__main__":
    main()
