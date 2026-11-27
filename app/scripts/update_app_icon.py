import os
from PIL import Image

source_image_path = "/app/public/assets/aistudio/file_0000000097a08211983273d5ad080012_f227860d.png"
drawable_target = "/app/app/src/main/res/drawable/quick_delivery_logo.png"

if os.path.exists(source_image_path):
    img = Image.open(source_image_path).convert("RGBA")
    
    # Save to drawable
    os.makedirs(os.path.dirname(drawable_target), exist_ok=True)
    img.save(drawable_target, "PNG")
    print(f"Successfully copied logo to {drawable_target}")
    
    # Mipmap sizes
    sizes = {
        "mipmap-mdpi-v4": 48,
        "mipmap-hdpi-v4": 72,
        "mipmap-xhdpi-v4": 96,
        "mipmap-xxhdpi-v4": 144,
        "mipmap-xxxhdpi-v4": 192
    }
    
    for folder, size in sizes.items():
        folder_path = f"/app/app/src/main/res/{folder}"
        os.makedirs(folder_path, exist_ok=True)
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        launcher_path = os.path.join(folder_path, "ic_launcher.png")
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        
        resized.save(launcher_path, "PNG")
        resized.save(round_path, "PNG")
        print(f"Updated {folder} icons to {size}x{size}")
else:
    print(f"Source image not found at {source_image_path}")
