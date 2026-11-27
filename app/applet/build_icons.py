import os
from PIL import Image

source_image_path = "public/assets/aistudio/file_0000000097a08211983273d5ad080012_f227860d.png"
drawable_target = "app/src/main/res/drawable/quick_delivery_logo.png"

if os.path.exists(source_image_path):
    img = Image.open(source_image_path).convert("RGBA")
    os.makedirs(os.path.dirname(drawable_target), exist_ok=True)
    img.save(drawable_target, "PNG")
    print(f"Copied logo to {drawable_target}")
    
    sizes = {
        "mipmap-mdpi-v4": 48,
        "mipmap-hdpi-v4": 72,
        "mipmap-xhdpi-v4": 96,
        "mipmap-xxhdpi-v4": 144,
        "mipmap-xxxhdpi-v4": 192
    }
    
    for folder, size in sizes.items():
        folder_path = f"app/src/main/res/{folder}"
        os.makedirs(folder_path, exist_ok=True)
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
        resized.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
        print(f"Updated {folder} to {size}x{size}")
else:
    print("Source image not found")
