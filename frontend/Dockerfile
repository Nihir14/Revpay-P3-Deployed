# ==========================================
# STAGE 1: Build the Angular App
# ==========================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependency definitions and install
COPY package*.json ./
RUN npm install

# Copy the rest of the code and build for production
COPY . .
RUN npm run build

# ==========================================
# STAGE 2: Serve with NGINX
# ==========================================
FROM nginx:alpine

# Copy the custom NGINX configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the compiled Angular files from the Builder stage
# NOTE: Replace 'frontend' below with your actual project name from angular.json!
# (If using Angular 17+, it might be /app/dist/frontend/browser)
COPY --from=builder /app/dist/revpay-frontend/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
