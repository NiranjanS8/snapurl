# AWS Deployment Plan

This project is best deployed on AWS credits with:

- **Frontend:** Vercel
- **Backend stack:** EC2 + Docker Compose
- **Domain:** Cloudflare or Route 53

Recommended domain structure:

- `snapurl.in` -> frontend
- `www.snapurl.in` -> frontend
- `api.snapurl.in` -> backend

## 1. EC2

Use a small Ubuntu instance and open:

- `22` for SSH
- `80` for HTTP
- `443` for HTTPS
- `9090` only temporarily if you want to test before Nginx

Copy the backend project to EC2 and use:

- [`Dockerfile`](/c:/Users/Niranjan/Desktop/SnapURL/snapurl/Dockerfile)
- [`docker-compose.aws.yml`](/c:/Users/Niranjan/Desktop/SnapURL/snapurl/docker-compose.aws.yml)
- [`.env.aws.example`](/c:/Users/Niranjan/Desktop/SnapURL/snapurl/.env.aws.example)

Create:

- `.env.aws`

from the example and fill in real secrets.

Then run:

```bash
docker compose -f docker-compose.aws.yml --env-file .env.aws up -d --build
```

## 2. Backend Environment

Set these correctly in `.env.aws`:

- database credentials
- JWT secret
- mail credentials
- CORS allowed origins
- Redis and RabbitMQ internal hostnames

Important values:

- `DB_URL=jdbc:mysql://mysql:3306/snap_url`
- `REDIS_HOST=redis`
- `RABBITMQ_HOST=rabbitmq`
- `CORS_ALLOWED_ORIGINS=https://snapurl.in,https://www.snapurl.in`

## 3. Frontend

Deploy `snapURL-frontend` to Vercel.

Use:

- [`snapURL-frontend/.env.production.example`](/c:/Users/Niranjan/Desktop/SnapURL/snapurl/snapURL-frontend/.env.production.example)

Set:

```text
VITE_BACKEND_URL=https://api.snapurl.in
```

## 4. Domain Mapping

Point:

- `snapurl.in` -> Vercel
- `www.snapurl.in` -> Vercel
- `api.snapurl.in` -> EC2 public IP

If you use Cloudflare:

- root and `www` go to Vercel using Vercel’s instructions
- `api` gets an `A` record to EC2

## 5. Final Production Layer

On EC2, place Nginx in front of the backend so:

- Nginx listens on `80` and `443`
- Spring Boot stays on internal `9090`
- TLS is handled by Nginx + Certbot

Target behavior:

- `https://api.snapurl.in` -> Nginx -> Spring Boot on `9090`

## 6. Notes

- CORS is now environment-driven through `CORS_ALLOWED_ORIGINS`
- backend stack runs cleanly inside Docker Compose
- RabbitMQ, Redis, and MySQL stay private inside the EC2 host
- frontend stays simple and cheap on Vercel

## 7. Best Order

1. buy / connect domain
2. deploy frontend to Vercel
3. launch EC2
4. copy backend project to EC2
5. create `.env.aws`
6. run Docker Compose
7. point `api` subdomain to EC2
8. add Nginx + HTTPS
