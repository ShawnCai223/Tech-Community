# Deploy to `shawnidea.com/community`

This project already runs under the `/community` context path:

```properties
server.servlet.context-path=/community
```

That means the clean production shape is:

- Nginx serves `https://shawnidea.com`
- Spring Boot keeps listening on `127.0.0.1:8080`
- Nginx forwards `/community` traffic to the Spring Boot app

## 1. Prepare production env

Copy `.env.prod.example` to `.env.prod` and fill in the real values:

```bash
cp .env.prod.example .env.prod
```

Important:

- `COMMUNITY_DOMAIN` must be `https://shawnidea.com`
- Do not set it to `https://shawnidea.com/community`
- The app already appends `/community` from `server.servlet.context-path`
- R2 public base URLs should be bucket/domain roots without a trailing slash

## 2. Start the application

```bash
chmod +x scripts/prod-run.sh
./scripts/prod-run.sh
```

The app will be available locally at:

```text
http://127.0.0.1:8080/community
```

## 3. Configure Nginx

Use the sample config in `deploy/nginx/shawnidea.com.conf` and merge its
`location /community/` block into your existing `shawnidea.com` server block.

Core rule:

```nginx
location /community/ {
    proxy_pass http://127.0.0.1:8080;
}
```

Also keep this redirect so `/community` becomes `/community/`:

```nginx
location = /community {
    return 301 /community/;
}
```

## 4. DNS and HTTPS

- Point the `A` record of `shawnidea.com` to your server IP
- Issue an HTTPS certificate for `shawnidea.com`
- Reload Nginx after the config change

## 5. Verify

After deployment, these URLs should work:

- `https://shawnidea.com/community`
- `https://shawnidea.com/community/login`
- `https://shawnidea.com/community/register`

If registration emails are enabled, the activation link should also point to:

```text
https://shawnidea.com/community/activation/{userId}/{code}
```
