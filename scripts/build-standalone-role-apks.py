import hashlib
import os
import shutil
import struct
import subprocess
import zlib

BASE_APK = ".build-outputs/app-debug.apk"
KEYSTORE = "debug.keystore"


def extract_private_key_pem():
    pem = subprocess.check_output(
        ["openssl", "pkcs12", "-in", KEYSTORE, "-passin", "pass:android", "-nodes", "-nocerts"]
    )
    key_path = "/tmp/qd_apk_sign_key.pem"
    with open(key_path, "wb") as f:
        f.write(pem)
    return key_path


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

    # Parse string pool
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

    # 1. Replace "ltkeqf" with the 6-char role suffix in the UTF-16LE string pool
    for i, s in enumerate(strings):
        if "com.aistudio.quickdelivery.ltkeqf" in s:
            replaced = s.replace("ltkeqf", new_suffix)
            assert len(replaced) == len(s)
            p = pool_base + offsets[i]
            axml[p + 2 : p + 2 + len(replaced) * 2] = replaced.encode("utf-16le")

    # Find string indices for activities and labels
    idx_main_act = strings.index("com.example.MainActivity")
    idx_cust_act = strings.index("com.example.CustomerMainActivity")
    idx_rider_act = strings.index("com.example.RiderMainActivity")
    idx_admin_act = strings.index("com.example.AdminMainActivity")

    idx_cust_lbl = strings.index("QD Customer App")
    idx_rider_lbl = strings.index("QD Rider Partner")
    idx_admin_lbl = strings.index("QD Admin Console")

    role_target = {
        "CUSTOMER": (idx_cust_act, idx_cust_lbl),
        "RIDER": (idx_rider_act, idx_rider_lbl),
        "ADMIN": (idx_admin_act, idx_admin_lbl),
    }
    target_act_idx, target_lbl_idx = role_target[target_role]

    # 2. Walk XML elements and update <application> label and swap <activity> name/label
    pos = 8 + sp_size
    TYPE_STRING = 0x03000008
    while pos < len(axml):
        ctype, cheader, csize = struct.unpack("<HHI", axml[pos : pos + 8])
        if ctype == 0x0102:  # START_ELEMENT
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

                # The first activity (idx_main_act) has the MAIN/LAUNCHER intent-filter.
                # Point it to target_act_idx and target_lbl_idx, and point the secondary entry to idx_main_act.
                if act_name_val == idx_main_act and name_ap is not None:
                    struct.pack_into("<III", axml, name_ap + 8, target_act_idx, TYPE_STRING, target_act_idx)
                    if label_ap is not None:
                        struct.pack_into("<III", axml, label_ap + 8, target_lbl_idx, TYPE_STRING, target_lbl_idx)
                elif act_name_val == target_act_idx and name_ap is not None:
                    struct.pack_into("<III", axml, name_ap + 8, idx_main_act, TYPE_STRING, idx_main_act)
        pos += csize

    return bytes(axml)


def replace_manifest_and_resign_v2(base_apk_bytes: bytes, new_manifest_bytes: bytes, key_path: str) -> bytes:
    """
    Replaces AndroidManifest.xml compressed stream inside the APK, updates Local File Header
    and Central Directory offsets/CRC32/sizes, and re-signs the APK with APK Signature Scheme v2.
    """
    eocd_off = base_apk_bytes.rfind(b"PK\x05\x06")
    cd_size = struct.unpack("<I", base_apk_bytes[eocd_off + 12 : eocd_off + 16])[0]
    cd_off = struct.unpack("<I", base_apk_bytes[eocd_off + 16 : eocd_off + 20])[0]
    block_size_2 = struct.unpack("<Q", base_apk_bytes[cd_off - 24 : cd_off - 16])[0]
    sig_block_start = cd_off - 8 - block_size_2

    # Extract existing v2 signing block template
    orig_sig_block = bytearray(base_apk_bytes[sig_block_start:cd_off])

    # Compress new AndroidManifest.xml with raw deflate (wbits=-15)
    comp_obj = zlib.compressobj(level=6, method=zlib.DEFLATED, wbits=-15)
    new_comp_data = comp_obj.compress(new_manifest_bytes) + comp_obj.flush()
    new_crc = zlib.crc32(new_manifest_bytes) & 0xFFFFFFFF
    new_comp_size = len(new_comp_data)
    new_uncomp_size = len(new_manifest_bytes)

    # Parse Central Directory to find AndroidManifest.xml entry
    cd_bytes = bytearray(base_apk_bytes[cd_off:eocd_off])
    pos = 0
    manifest_lh_off = None
    old_comp_size = None
    manifest_cd_pos = None

    while pos < len(cd_bytes):
        assert cd_bytes[pos : pos + 4] == b"PK\x01\x02"
        fname_len, extra_len, comment_len = struct.unpack("<HHH", cd_bytes[pos + 28 : pos + 34])
        fname = cd_bytes[pos + 46 : pos + 46 + fname_len].decode("utf-8", "ignore")
        lh_off = struct.unpack("<I", cd_bytes[pos + 42 : pos + 46])[0]
        if fname == "AndroidManifest.xml":
            manifest_cd_pos = pos
            manifest_lh_off = lh_off
            old_comp_size = struct.unpack("<I", cd_bytes[pos + 20 : pos + 24])[0]
            break
        pos += 46 + fname_len + extra_len + comment_len

    assert manifest_lh_off is not None
    # Inspect Local File Header at manifest_lh_off
    assert base_apk_bytes[manifest_lh_off : manifest_lh_off + 4] == b"PK\x03\x04"
    lh_fname_len, lh_extra_len = struct.unpack(
        "<HH", base_apk_bytes[manifest_lh_off + 26 : manifest_lh_off + 30]
    )
    lh_data_start = manifest_lh_off + 30 + lh_fname_len + lh_extra_len
    lh_data_end = lh_data_start + old_comp_size

    # Check if data descriptor flag (bit 3) is set on Local File Header
    gp_flag = struct.unpack("<H", base_apk_bytes[manifest_lh_off + 6 : manifest_lh_off + 8])[0]
    has_data_desc = (gp_flag & 0x0008) != 0
    dd_len = 0
    if has_data_desc:
        if base_apk_bytes[lh_data_end : lh_data_end + 4] == b"PK\x07\x08":
            dd_len = 16
        else:
            dd_len = 12

    delta = new_comp_size - old_comp_size

    # Rebuild Local Header for AndroidManifest.xml
    new_lh = bytearray(base_apk_bytes[manifest_lh_off:lh_data_start])
    struct.pack_into("<III", new_lh, 14, new_crc, new_comp_size, new_uncomp_size)

    new_dd = bytearray()
    if has_data_desc:
        if dd_len == 16:
            new_dd = bytearray(struct.pack("<4sIII", b"PK\x07\x08", new_crc, new_comp_size, new_uncomp_size))
        else:
            new_dd = bytearray(struct.pack("<III", new_crc, new_comp_size, new_uncomp_size))

    # Build new Section 1 (all ZIP entries before APK Signing Block)
    sec1 = (
        base_apk_bytes[:manifest_lh_off]
        + bytes(new_lh)
        + new_comp_data
        + bytes(new_dd)
        + base_apk_bytes[lh_data_end + dd_len : sig_block_start]
    )

    # Ensure 4096-byte alignment for APK Signing Block by adjusting VerityPaddingBlock (0x42726577) if needed
    # Update Central Directory entries whose lh_off > manifest_lh_off
    pos = 0
    while pos < len(cd_bytes):
        fname_len, extra_len, comment_len = struct.unpack("<HHH", cd_bytes[pos + 28 : pos + 34])
        lh_off = struct.unpack("<I", cd_bytes[pos + 42 : pos + 46])[0]
        if pos == manifest_cd_pos:
            struct.pack_into("<III", cd_bytes, pos + 16, new_crc, new_comp_size, new_uncomp_size)
        elif lh_off > manifest_lh_off:
            struct.pack_into("<I", cd_bytes, pos + 42, lh_off + delta)
        pos += 46 + fname_len + extra_len + comment_len

    new_sig_block_start = len(sec1)
    # Adjust padding block inside orig_sig_block so total size before CD is a multiple of 4096
    pair1_len = struct.unpack("<Q", orig_sig_block[8:16])[0]
    v2_pair_bytes = bytearray(orig_sig_block[8 : 16 + pair1_len])

    # Calculate required padding pair length so (new_sig_block_start + total_sig_block_bytes) % 4096 == 0
    # total_sig_block_bytes = 8 (size1) + len(v2_pair_bytes) + (8 + 4 + pad_len) + 8 (size2) + 16 (magic)
    base_overhead = 8 + len(v2_pair_bytes) + 12 + 8 + 16
    rem = (new_sig_block_start + base_overhead) % 4096
    pad_len = (4096 - rem) % 4096
    pad_pair_bytes = struct.pack("<QI", 4 + pad_len, 0x42726577) + (b"\x00" * pad_len)
    block_size_val = len(v2_pair_bytes) + len(pad_pair_bytes) + 8 + 16

    new_cd_off = new_sig_block_start + 8 + block_size_val
    eocd_bytes = bytearray(base_apk_bytes[eocd_off:])
    struct.pack_into("<II", eocd_bytes, 12, len(cd_bytes), new_cd_off)

    # Compute APK Signature Scheme v2 digest over (sec1, cd_bytes, eocd_with_cd_off_at_sig_block_start)
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

    # Patch new_top_digest into v2_pair_bytes and sign signed_data
    # v2_pair_bytes layout: [8: pair_len][4: 0x7109871a][4: signers_len][4: signer_len][4: signed_data_len]...
    signed_data_len = struct.unpack("<I", v2_pair_bytes[20:24])[0]
    # Inside signed_data: [4: digests_len][4: digest_entry_len][4: alg_id][4: digest_len (32)][32: digest]
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

    # Signature bytes inside v2_pair_bytes: after signed_data -> [4: sigs_len][4: sig_entry_len][4: alg_id][4: sig_len (256)][256: sig]
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


def main():
    os.makedirs("public/apks", exist_ok=True)
    os.makedirs("/var/www/assets/apks", exist_ok=True)

    key_path = extract_private_key_pem()
    base_apk_bytes = open(BASE_APK, "rb").read()

    import zipfile
    with zipfile.ZipFile(BASE_APK, "r") as z:
        orig_manifest = z.read("AndroidManifest.xml")

    variants = [
        ("CUSTOMER", "QuickDelivery-Customer.apk"),
        ("RIDER", "QuickDelivery-Rider-Partner.apk"),
        ("ADMIN", "QuickDelivery-Admin-Enterprise.apk"),
    ]

    for role, filename in variants:
        patched_manifest = patch_axml_manifest(orig_manifest, role)
        signed_apk = replace_manifest_and_resign_v2(base_apk_bytes, patched_manifest, key_path)
        out_pub = os.path.join("public/apks", filename)
        out_www = os.path.join("/var/www/assets/apks", filename)
        with open(out_pub, "wb") as f:
            f.write(signed_apk)
        with open(out_www, "wb") as f:
            f.write(signed_apk)
        print(f"Built & v2-signed {filename} ({len(signed_apk)} bytes) for role={role}")


if __name__ == "__main__":
    main()
