import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'node:fs';
import path from 'node:path';

function mobileApkDownloadMiddleware() {
  const handler = (req: any, res: any, next: any) => {
    const rawUrl = req.url || '';
    const pathname = rawUrl.split('?')[0];
    if (pathname.startsWith('/apks/') && (pathname.endsWith('.apk') || pathname.endsWith('.zip'))) {
      const requestedFile = path.basename(pathname);
      const publicPath = path.resolve(process.cwd(), 'public', 'apks', requestedFile);
      const fallbackApkPath = path.resolve(process.cwd(), '.build-outputs', 'app-debug.apk');

      const targetPath = fs.existsSync(publicPath)
        ? publicPath
        : pathname.endsWith('.apk') && fs.existsSync(fallbackApkPath)
        ? fallbackApkPath
        : null;

      if (targetPath && fs.existsSync(targetPath)) {
        const stat = fs.statSync(targetPath);
        const isZip = requestedFile.endsWith('.zip');
        res.setHeader(
          'Content-Type',
          isZip ? 'application/zip' : 'application/vnd.android.package-archive'
        );
        res.setHeader('Content-Disposition', `attachment; filename="${requestedFile}"`);
        res.setHeader('Content-Length', String(stat.size));
        res.setHeader('Accept-Ranges', 'bytes');
        res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
        res.setHeader('Access-Control-Allow-Origin', '*');
        const stream = fs.createReadStream(targetPath);
        stream.pipe(res);
        return;
      }
    }
    next();
  };

  return {
    name: 'mobile-apk-download-middleware',
    configureServer(server: any) {
      server.middlewares.use(handler);
    },
    configurePreviewServer(server: any) {
      server.middlewares.use(handler);
    },
  };
}

export default defineConfig({
  base: './',
  plugins: [mobileApkDownloadMiddleware(), react()],
  build: {
    outDir: process.env.VITE_OUT_DIR || 'dist',
    emptyOutDir: true,
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    allowedHosts: true,
  },
});
