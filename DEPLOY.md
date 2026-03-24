# Deployment

This application is deployed under:

```text
https://shawnidea.com/community
```

The backend already uses this context path:

```properties
server.servlet.context-path=/community
```

Production shape:

- Nginx serves `https://shawnidea.com`
- Spring Boot listens on `127.0.0.1:8080`
- Nginx forwards `/community` traffic to Spring Boot

## Production Environment

Create `.env.prod` from the example:

```bash
cp .env.prod.example .env.prod
```

Important values:

- `COMMUNITY_DOMAIN` must be `https://shawnidea.com`
- Do not set it to `https://shawnidea.com/community`
- R2 public base URLs should be bucket or domain roots without a trailing slash

## Start the Production App

```bash
chmod +x scripts/prod-run.sh
./scripts/prod-run.sh
```

Local production process URL:

```text
http://127.0.0.1:8080/community
```

## Nginx

Use `deploy/nginx/shawnidea.com.conf` as the reference and merge its `/community` rules into your existing `shawnidea.com` server block.

Core proxy rule:

```nginx
location /community/ {
    proxy_pass http://127.0.0.1:8080;
}
```

Redirect `/community` to `/community/`:

```nginx
location = /community {
    return 301 /community/;
}
```

## DNS and HTTPS

- Point the `A` record for `shawnidea.com` to the server IP
- Issue an HTTPS certificate for `shawnidea.com`
- Reload Nginx after config changes

## Verification

After deployment, verify:

- `https://shawnidea.com/community`
- `https://shawnidea.com/community/login`
- `https://shawnidea.com/community/register`

If registration email is enabled, activation links should resolve to:

```text
https://shawnidea.com/community/activation/{userId}/{code}
```

## GitHub Actions Auto Deploy

This repository includes `.github/workflows/deploy-ec2.yml`.

On every push to `main`, GitHub Actions will:

- build the project with Java 17
- SSH into the EC2 server
- run `git pull --ff-only origin main`
- run `mvn -B clean package -DskipTests`
- restart `tech-community`
- verify:
  - `http://127.0.0.1:8080/community/app/index.html`
  - `http://127.0.0.1:8080/community/api/v1/posts?page=0&limit=1`

Required GitHub repository secrets:

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY_B64`
- `EC2_PORT`

Important:

- The EC2 user must be allowed to run `sudo systemctl restart tech-community`
- The server checkout must already exist at `/var/www/tech-community`
- `.env.prod` must already exist on the server
