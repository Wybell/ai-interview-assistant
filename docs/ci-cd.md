# GitHub CI and Manual Production Deployment

This project uses GitHub Actions for automatic continuous integration (CI) and deliberately keeps production deployment (CD) manual. A push to `main` runs backend and frontend checks. After the checks are green, a maintainer starts the production workflow from GitHub Actions.

```text
git push origin main
        |
        v
GitHub CI: backend tests + frontend checks
        |
        v
Maintainer runs "Deploy Production" manually
        |
        v
SSH to Tencent Cloud -> database backup -> fast-forward Git update
        -> Docker rebuild -> Flyway migration -> loopback health checks
```

No password, API key, JWT secret, database value, or SSH private key is committed to this repository.

## One-Time Server Setup

The server already has a deploy key that lets the `ubuntu` user pull this private repository from GitHub. That key is for **server to GitHub** traffic. GitHub Actions needs a different key for **GitHub Actions to server** traffic.

1. On the local computer, generate a dedicated Actions deployment key. Do not reuse the server-to-GitHub deploy key and do not commit either file.

   ```bash
   ssh-keygen -t ed25519 -C "github-actions-ai-interview" \
     -f ~/.ssh/ai-interview-actions-deploy -N ""
   ```

2. Log in to Tencent Cloud as an administrator. Add the generated public key to the existing deployment user's authorized keys.

   ```bash
   sudo -u ubuntu -H bash -c '
   install -d -m 700 "$HOME/.ssh"
   touch "$HOME/.ssh/authorized_keys"
   chmod 600 "$HOME/.ssh/authorized_keys"
   '

   # Paste the single line from ai-interview-actions-deploy.pub after this command.
   sudoedit /home/ubuntu/.ssh/authorized_keys
   ```

   Verify the key from the local computer before continuing:

   ```bash
   ssh -i ~/.ssh/ai-interview-actions-deploy ubuntu@YOUR_SERVER_IP "whoami"
   ```

3. Ensure `ubuntu` owns the Git checkout, can use Docker without an interactive password, and has only the access required to read external configuration and write database backups.

   ```bash
   sudo usermod -aG docker ubuntu
   sudo chown -R ubuntu:ubuntu /opt/ai-interview/app
   sudo chown root:ubuntu /opt/ai-interview/config
   sudo chmod 750 /opt/ai-interview/config
   sudo chown root:ubuntu /opt/ai-interview/config/.env \
     /opt/ai-interview/config/application-prod.properties
   sudo chmod 640 /opt/ai-interview/config/.env \
     /opt/ai-interview/config/application-prod.properties
   sudo install -d -o ubuntu -g ubuntu -m 700 \
     /opt/ai-interview/config/backups/database
   ```

   Sign out and back in as `ubuntu`, then verify:

   ```bash
   cd /opt/ai-interview/app
   git status -sb
   docker compose -p backend --env-file /opt/ai-interview/config/.env \
     -f backend/docker-compose.prod.yml ps
   ```

   Do not change the ownership of `/opt/ai-interview/uploads/resumes`; it is written by the backend container. Do not grant `ubuntu` passwordless `sudo` for this workflow. The commands above keep the actual configuration files owned by `root` and readable only through the `ubuntu` group; do not apply a broad recursive ownership change to `/opt/ai-interview/config`.

4. Capture the current server host key from a trusted local network and keep the result for GitHub Secrets.

   ```bash
   ssh-keyscan -H YOUR_SERVER_IP
   ```

   Compare the fingerprint with Tencent Cloud's console before trusting it. This protects the Action from connecting to the wrong host.

## GitHub Configuration

In the repository, open **Settings -> Secrets and variables -> Actions** and create these repository secrets:

| Secret                    | Value                                                                      |
| ------------------------- | -------------------------------------------------------------------------- |
| `TENCENT_SSH_HOST`        | Tencent Cloud public IP or host name                                       |
| `TENCENT_SSH_PORT`        | SSH port, normally `22`                                                    |
| `TENCENT_SSH_USER`        | `ubuntu`                                                                   |
| `TENCENT_SSH_PRIVATE_KEY` | Complete content of local `~/.ssh/ai-interview-actions-deploy` private key |
| `TENCENT_SSH_KNOWN_HOSTS` | Complete `ssh-keyscan -H` output verified in the previous step             |

Also create a GitHub Environment named `production` under **Settings -> Environments**. The deployment workflow uses it. A sole maintainer can leave it without reviewers; a team can add required reviewers later to require a second approval.

## Daily Release Process

1. Test the change locally and commit only project files.
2. Push `main`:

   ```bash
   git push origin main
   ```

3. Open the **Actions** tab and wait for **Continuous Integration** to finish successfully.
4. Open **Deploy Production**, select **Run workflow**, check the confirmation box, then start it.
5. Read the Action log. It creates a MySQL dump, fast-forwards only to the exact `main` commit checked by the workflow, rebuilds the two application containers, and checks frontend `200` plus protected backend `401` locally on the server.
6. Open the public application and complete a short browser acceptance check.

The deployment script does not prune Docker data, delete volumes, overwrite `/opt/ai-interview/config`, or alter Nginx. It also refuses to deploy when the server checkout has local modifications or when GitHub's checked commit differs from `origin/main`.

## Recovery

Each deployment stores a database dump at:

```text
/opt/ai-interview/config/backups/database/YYYYMMDD-HHMMSS/interview_db.sql
```

For a code issue, stop further releases and create a new corrective commit, let CI pass, and deploy it manually. Do not use `git reset --hard` on production. Flyway migrations are forward-only: restoring a database backup is a deliberate maintenance operation that requires application-version compatibility review first.
