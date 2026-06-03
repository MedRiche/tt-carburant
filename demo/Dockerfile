# ─────────────────────────────────────────────
#  STAGE 1 — Build  (Node 18)
# ─────────────────────────────────────────────
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files first → cache npm install layer
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copy source and build
COPY . .
RUN npm run build:ci

# ─────────────────────────────────────────────
#  STAGE 2 — Serve  (Nginx Alpine)
# ─────────────────────────────────────────────
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy Angular build output
COPY --from=build /app/dist/demo/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]