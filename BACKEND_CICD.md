# Backend GitHub Actions CI/CD for EC2

This repo now includes a backend-only GitHub Actions workflow at `.github/workflows/backend-ci-cd.yml`.

## What it does

On every push to `main` or `master` that changes backend-related files, GitHub Actions will:

1. run `./mvnw test`
2. connect to your EC2 server over SSH
3. update the backend repo on EC2
4. run:

```bash
docker compose -f docker-compose.aws.yml --env-file .env.aws up -d --build
```
5. wait for `http://127.0.0.1:9090/api/health` to return success

Pull requests only run the test job. They do not deploy.

## GitHub Secrets

Add these repository secrets in GitHub:

- `EC2_HOST` - your EC2 public IP or DNS
- `EC2_USER` - usually `ubuntu` on Ubuntu EC2
- `EC2_SSH_KEY` - the full private key content used to SSH into EC2
- `EC2_APP_DIR` - absolute path of the backend repo on EC2

Example `EC2_APP_DIR`:

```text
/home/ubuntu/snapurl
```

## EC2 requirements

Your EC2 instance should already have:

- Docker installed
- Docker Compose available as `docker compose`
- the backend repo cloned
- `.env.aws` created inside the repo directory

The workflow expects this structure on EC2:

```text
/home/ubuntu/snapurl
  .git
  .env.aws
  docker-compose.aws.yml
  Dockerfile
```

## Recommended first-time setup on EC2

Clone the repo once on the server:

```bash
git clone <your-repo-url> /home/ubuntu/snapurl
cd /home/ubuntu/snapurl
cp .env.aws.example .env.aws
```

Fill `.env.aws` with your real production values, then test once manually:

```bash
docker compose -f docker-compose.aws.yml --env-file .env.aws up -d --build
```

Recommended production values:

```text
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
RATE_LIMIT_FAIL_OPEN=false
```

## Important note

The deploy script checks out and pulls the same branch that triggered the workflow. In most cases, that will be `main`.
