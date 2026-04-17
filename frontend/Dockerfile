# Stage 1: Build the React Application
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve the application via Nginx
FROM nginx:alpine
# Copy exactly what we built into the Nginx static folder
COPY --from=build /app/dist /usr/share/nginx/html
# Inject our precise routing configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
