#!/bin/bash
set -e

# 1. Replace warmup.html with our Direct APK Download Center HTML so warmup screen never blocks mobile users
if [ -f /app/applet/public/apk-download.html ]; then
  cp -f /app/applet/public/apk-download.html /var/www/assets/warmup.html 2>/dev/null || true
fi

# 2. Inject direct /apks/*.apk and /download locations into /etc/nginx/nginx.conf if not already present
if [ -f /etc/nginx/nginx.conf ] && ! grep -q "location ~ \^/apks/" /etc/nginx/nginx.conf; then
  python3 - << 'PYEOF'
import pathlib

for conf_path in ["/etc/nginx/nginx.conf", "/etc/nginx/nginx.conf.template"]:
    p = pathlib.Path(conf_path)
    if not p.exists():
        continue
    text = p.read_text()
    if "location ~ ^/apks/" in text:
        continue
    apk_block = """
        location ~ ^/apks/(.*\\.apk)$ {
            alias /app/applet/.build-outputs/app-debug.apk;
            default_type application/vnd.android.package-archive;
            add_header Content-Disposition 'attachment; filename="$1"';
            add_header Cache-Control "no-store, no-cache, must-revalidate";
            add_header Access-Control-Allow-Origin "*";
        }

        location = /download {
            alias /app/applet/public/apk-download.html;
            default_type text/html;
            add_header Cache-Control "no-store, no-cache, must-revalidate";
        }
"""
    target = "location /warmup.html {"
    if target in text:
        text = text.replace(target, apk_block + "\n        " + target, 1)
        p.write_text(text)
PYEOF
  nginx -s reload 2>/dev/null || true
fi
