# EchoIM Production Deployment

This repository now uses a two-image production layout:

- `echoim-frontend`: one Nginx container serving `echoim-web` at `/` and `echoim-admin` at `/admin/`
- `echoim-backend`: one Spring Boot container serving HTTP on `8080` and IM/WebSocket on `8091`
- BT / BaoTa Nginx only handles domain, TLS, and reverse proxy
- MySQL and Redis remain on the host

## Server directories

- `/srv/echoim/app/current`
- `/srv/echoim/env/.env.prod`
- `/srv/echoim/env/release.env`
- `/srv/echoim/scripts/deploy.sh`

## Required GitHub Actions secrets

- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `SSH_HOST`
- `SSH_PORT`
- `SSH_USER`
- `SSH_PRIVATE_KEY`

## First deployment

1. Upload the repository to `/srv/echoim/app/current`.
2. Copy `.env.prod.example` to `/srv/echoim/env/.env.prod` and fill real secrets.
3. Set `MYSQL_HOST` and `REDIS_HOST` to values reachable from the backend container.
4. Install `deploy/nginx/echoim.conf` into the BT vhost config and proxy `/` to `127.0.0.1:18089`, `/api/` to `127.0.0.1:18081`, `/ws` to `127.0.0.1:18091`.
5. Run `/srv/echoim/app/current/deploy/scripts/deploy.sh` with `BUILD_ON_SERVER=1`.

## Routine deployment

1. CI builds and pushes both images to GHCR.
2. CI updates `/srv/echoim/env/release.env` on the server.
3. CI runs `/srv/echoim/scripts/deploy.sh`.

## Rollback

1. Edit `/srv/echoim/env/release.env` to the previous image tags.
2. Run `/srv/echoim/scripts/deploy.sh`.
