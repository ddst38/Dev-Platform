# Dev-Platform

Infrastructure Docker pour tests de migration et CI/CD.

## Architecture

| Service | Port | Image/Build |
|---------|------|-------------|
| CVS | 2401 | `./cvs` |
| Reports | 8085 | `./apache` |
| Filebrowser | 8086 | `filebrowser/filebrowser` |
| Artifactory | 8082 | `artifactory-oss:7.77.5` |
| Jenkins | 8081 | `./jenkins` |
| GitLab | 8083 / 2222 | `gitlab/gitlab-ce` |

## Commandes

```bash
docker compose up -d          # Démarrer
docker compose ps             # État
docker compose logs -f <svc>  # Logs
docker compose down           # Arrêter (⚠️ pas --volumes)
```

## Accès

| Service | URL | Credentials |
|---------|-----|-------------|
| Artifactory | http://localhost:8082 | admin/password |
| Jenkins | http://localhost:8081 | - |
| GitLab | http://localhost:8083 | root/(voir logs) |
| Filebrowser | http://localhost:8086 | admin/jsa2uMBrYlm8xJ98 |
| Reports | http://localhost:8085 | - |

## Pipeline Migration

`pipelines/migration-cvs-to-git.groovy`

1. Checkout CVS → 2. ant2maven → 3. Publish report → 4. Push GitLab

**Credentials Jenkins:**
- `cvs-jenkins` (Username/Password)
- `gitlab-token` (Secret text)

## Structure

```
├── docker-compose.yml
├── apache/           # Reports server
├── cvs/              # CVS server
├── jenkins/          # Jenkins + Docker + CVS
├── filebrowser/      # Config
├── artifactory/      # Config
├── pipelines/        # Jenkinsfiles
└── jar/              # ant2maven (non versionné)
```
