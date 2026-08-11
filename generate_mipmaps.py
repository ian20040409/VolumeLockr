import os
import shutil
from PIL import Image

raw_icon_path = "/Users/linenyou/Github/VolumeLockr/icon_2/icon2.png"
app_res_dir = "/Users/linenyou/Github/VolumeLockr/app/src/main/res"

mipmap_sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

if os.path.exists(raw_icon_path):
    icon_img = Image.open(raw_icon_path).convert("RGBA")
    
    for density, size in mipmap_sizes.items():
        out_dir = os.path.join(app_res_dir, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)
        
        resized_icon = icon_img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save as ic_volumelockr.png and ic_launcher.png
        for name in ["ic_volumelockr.png", "ic_launcher.png"]:
            out_path = os.path.join(out_dir, name)
            resized_icon.save(out_path, "PNG")
            print(f"Generated {out_path} ({size}x{size})")
            
    # Also update the play store icon and fastlane icon
    playstore_icon = icon_img.resize((512, 512), Image.Resampling.LANCZOS)
    playstore_icon.save("/Users/linenyou/Github/VolumeLockr/app/src/main/ic_volumelockr-playstore.png", "PNG")
    os.makedirs("/Users/linenyou/Github/VolumeLockr/fastlane/metadata/android/en-US/images", exist_ok=True)
    playstore_icon.save("/Users/linenyou/Github/VolumeLockr/fastlane/metadata/android/en-US/images/icon.png", "PNG")
    print("Updated Play Store icons.")
