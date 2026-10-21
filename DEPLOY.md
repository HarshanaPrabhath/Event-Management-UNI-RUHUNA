# Docker CI/CD → VM (industry standard)

Flow:

```
git push → GitHub Actions → build Docker image → push to GHCR
                         → SSH into VM → docker compose pull/up
```

Both repositories publish images, then run the same deploy script on the VM so MySQL + backend + frontend stay in sync.

| Service  | Image |
|----------|--------|
| Backend  | `ghcr.io/induwara90/event-management-uni-ruhuna:latest` |
| Frontend | `ghcr.io/induwara90/event-management-frontend:latest` |

The frontend nginx proxies `/api` and `/uploads` to the backend container, so users only open **port 8080**.

---

## 1. One-time VM setup

SSH into the VM (Ubuntu recommended):

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 curl
sudo usermod -aG docker $USER
# log out and back in so docker works without sudo
```

Create the deploy directory:

```bash
sudo mkdir -p /opt/event-management
sudo chown $USER:$USER /opt/event-management
```

Copy these files from this repo’s `deploy/` folder onto the VM (`/opt/event-management/`):

- `docker-compose.yml`
- `deploy.sh`
- `.env.example` → rename to `.env` and edit secrets

```bash
cd /opt/event-management
cp .env.example .env
nano .env
chmod +x deploy.sh
```

Generate a strong JWT secret:

```bash
openssl rand -base64 64
```

Paste it into `JWT_SECRET`. Set a strong `DB_PASSWORD`.

If your GHCR packages are **private**, add to `.env`:

```env
GHCR_USER=YOUR_GITHUB_USERNAME
GHCR_TOKEN=ghp_xxx   # classic PAT with read:packages
```

Make packages public (Package settings → Change visibility) if you prefer not to use a token.

First start:

```bash
cd /opt/event-management
./deploy.sh
```

Open: `http://YOUR_VM_IP:8080`

Security group / firewall: allow **22** (SSH) and **8080** (UI). You can leave 8081 closed if everything goes through the frontend proxy.

---

## 2. GitHub Actions secrets (both repos)

Add the same secrets in:

- Backend: `Event-Management-UNI-RUHUNA` (or your fork)
- Frontend: `EM` / `event-management-frontend`

| Secret | Value |
|--------|--------|
| `VM_HOST` | VM public IP or hostname |
| `VM_USER` | SSH username (e.g. `ubuntu`, `azureuser`) |
| `VM_SSH_PRIVATE_KEY` | Full private key contents (passphrase-free) |

### Create a deploy SSH key (recommended)

On your laptop:

```bash
ssh-keygen -t ed25519 -f ./vm_deploy -N ""
```

On the VM:

```bash
mkdir -p ~/.ssh
cat >> ~/.ssh/authorized_keys   # paste contents of vm_deploy.pub
chmod 600 ~/.ssh/authorized_keys
```

In GitHub → Settings → Secrets and variables → Actions → New repository secret:

- Name: `VM_SSH_PRIVATE_KEY`
- Value: full contents of `vm_deploy` (including `-----BEGIN ... PRIVATE KEY-----`)

Also set `VM_HOST` and `VM_USER`.

---

## 3. Everyday workflow

1. Push to `main` on backend **or** frontend.
2. Actions builds/pushes the image to GHCR.
3. Deploy job SSHs to the VM and runs `./deploy.sh` (pull + recreate).
4. App is live on `http://VM_IP:8080`.

Default login after first start: `ADMIN/001` / `1234`.

---

## 4. Optional: CORS for direct API access

If browsers call `:8081` directly, set in VM `.env`:

```env
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://YOUR_VM_IP:8080,http://YOUR_VM_IP:8081,http://localhost:*
```

Then re-run `./deploy.sh`.

---

## 5. HTTPS later

Put Nginx/Caddy or a cloud load balancer in front of port 8080, then set:

```env
APP_COOKIE_SECURE=true
```
