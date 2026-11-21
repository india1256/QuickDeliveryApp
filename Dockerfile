# Production Dockerfile for Quick Delivery Server on Google Cloud Run
FROM node:20-alpine AS runner

WORKDIR /app

# Native Node.js modules only (zero external dependency bloat)
COPY package.json ./

# Copy production assets and scripts
COPY scripts/ ./scripts/
COPY public/ ./public/
COPY index.html ./

# Google Cloud Run default port
ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

# Run the 24/7 Live Sync, OTP Email & APK Delivery Server
CMD ["node", "scripts/apk-download-server.mjs"]
