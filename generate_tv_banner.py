import os
import shutil
from PIL import Image

raw_icon_path = "/Users/linenyou/Github/VolumeLockr/icon_2/icon2.png"
app_res_dir = "/Users/linenyou/Github/VolumeLockr/app/src/main/res"

banner_sizes = {
    "drawable-mdpi": (320, 180),
    "drawable-hdpi": (480, 270),
    "drawable-xhdpi": (640, 360),
    "drawable-xxhdpi": (960, 540),
    "drawable-xxxhdpi": (1280, 720),
    "drawable": (640, 360)
}

if not os.path.exists(raw_icon_path):
    print(f"Error: {raw_icon_path} does not exist!")
else:
    icon_img = Image.open(raw_icon_path).convert("RGBA")
    
    for folder, (width, height) in banner_sizes.items():
        out_dir = os.path.join(app_res_dir, folder)
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "tv_banner.png")
        
        banner = Image.new("RGBA", (width, height), (255, 255, 255, 255))
        
        # Center the icon, make it 70% of the height
        icon_size = int(height * 0.7)
        resized_icon = icon_img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        
        x = (width - icon_size) // 2
        y = (height - icon_size) // 2
        
        banner.alpha_composite(resized_icon, (x, y))
        banner.save(out_path, "PNG")
        print(f"Generated {out_path} ({width}x{height})")

# Copy circle shape icons
circle_res_dir = "/Users/linenyou/Github/VolumeLockr/icon_2/icon2_circle/res"
densities = ["ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]

for density in densities:
    src_dir = os.path.join(circle_res_dir, f"mipmap-{density}")
    dst_dir = os.path.join(app_res_dir, f"mipmap-{density}")
    os.makedirs(dst_dir, exist_ok=True)
    
    src_png = os.path.join(src_dir, "icon_2_circle.png")
    if os.path.exists(src_png):
        shutil.copy(src_png, os.path.join(dst_dir, "ic_volumelockr_round.png"))
        shutil.copy(src_png, os.path.join(dst_dir, "ic_launcher_round.png"))
        print(f"Copied circle icon for {density}")
    else:
        print(f"Warning: {src_png} not found.")

