import fs from 'fs';
import path from 'path';

const src = 'public/assets/aistudio/file_0000000097a08211983273d5ad080012_f227860d.png';
const targetDrawable = 'app/src/main/res/drawable/quick_delivery_logo.png';

if (fs.existsSync(src)) {
    // Copy to drawable
    fs.mkdirSync(path.dirname(targetDrawable), { recursive: true });
    fs.copyFileSync(src, targetDrawable);
    console.log('Copied logo to drawable:', targetDrawable);

    const mipmapFolders = [
        'mipmap-mdpi-v4',
        'mipmap-hdpi-v4',
        'mipmap-xhdpi-v4',
        'mipmap-xxhdpi-v4',
        'mipmap-xxxhdpi-v4'
    ];

    for (const folder of mipmapFolders) {
        const folderPath = path.join('app/src/main/res', folder);
        fs.mkdirSync(folderPath, { recursive: true });
        fs.copyFileSync(src, path.join(folderPath, 'ic_launcher.png'));
        fs.copyFileSync(src, path.join(folderPath, 'ic_launcher_round.png'));
        console.log('Updated icons in:', folder);
    }
    console.log('Successfully updated all app icons and logo!');
} else {
    console.error('Source image not found:', src);
}
