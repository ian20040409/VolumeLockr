import os
import shutil

src_base = "/Users/linenyou/Github/VolumeLockr/icon_2/android_icon2/res"
dst_base = "/Users/linenyou/Github/VolumeLockr/app/src/main/res"

if os.path.exists(src_base):
    # 1. Copy all contents of android_icon2/res directly to app/src/main/res
    for root, dirs, files in os.walk(src_base):
        rel_path = os.path.relpath(root, src_base)
        target_dir = os.path.join(dst_base, rel_path) if rel_path != "." else dst_base
        os.makedirs(target_dir, exist_ok=True)
        for f in files:
            if not f.startswith("."):
                src_f = os.path.join(root, f)
                dst_f = os.path.join(target_dir, f)
                shutil.copy2(src_f, dst_f)
                print(f"Copied {rel_path}/{f}")

    # 2. Duplicate icons for ic_volumelockr / round icon references
    densities = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
    for d in densities:
        d_dir = os.path.join(dst_base, f"mipmap-{d}")
        src_png = os.path.join(d_dir, "ic_launcher.png")
        if os.path.exists(src_png):
            for name in ["ic_volumelockr.png", "ic_launcher_round.png", "ic_volumelockr_round.png"]:
                shutil.copy2(src_png, os.path.join(d_dir, name))

    # 3. For anydpi-v26 XML files
    anydpi_dir = os.path.join(dst_base, "mipmap-anydpi-v26")
    src_xml = os.path.join(anydpi_dir, "ic_launcher.xml")
    if os.path.exists(src_xml):
        for name in ["ic_volumelockr.xml", "ic_launcher_round.xml", "ic_volumelockr_round.xml"]:
            shutil.copy2(src_xml, os.path.join(anydpi_dir, name))

    # 4. Copy playstore icon
    src_playstore = "/Users/linenyou/Github/VolumeLockr/icon_2/android_icon2/play_store_512.png"
    if os.path.exists(src_playstore):
        shutil.copy2(src_playstore, "/Users/linenyou/Github/VolumeLockr/app/src/main/ic_volumelockr-playstore.png")
        fastlane_dir = "/Users/linenyou/Github/VolumeLockr/fastlane/metadata/android/en-US/images"
        os.makedirs(fastlane_dir, exist_ok=True)
        shutil.copy2(src_playstore, os.path.join(fastlane_dir, "icon.png"))
        print("Copied play_store_512.png")
