# Dev-Platform

Infrastructure Docker pour tests de migration et CI/CD.

## Architecture

| Service | Port | Image/Build | Usage |
|---------|------|-------------|-------|
| CVS | 2401 | `./cvs` | Serveur CVS pserver |
| Reports | 8085 | `./apache` | Serveur Apache + upload CGI |
| Artifactory | 8082 | `artifactory-oss:7.77.5` | Repository d'artefacts |
| Jenkins | 8081 | `jenkins:lts-jdk21` | CI/CD |

## Commandes

```bash
# Démarrer tous les services
docker compose up -d

# Rebuild un service spécifique
docker compose build reports && docker compose up -d reports

# Logs
docker compose logs -f <service>
```

## Structure

```
.
├── apache/          # Serveur reports (Dockerfile, httpd.conf, upload.py)
├── cvs/             # Serveur CVS
├── cvsroot/         # Repository CVS (TEST module)
├── jenkins_home/    # Config Jenkins persistante
├── artifactory/     # Config Artifactory (system.yaml, keys)
├── data/reports/    # Volume monté pour les rapports uploadés
└── report/          # Archive report.tar.gz source
```

## Points d'attention

### Artifactory
- Port interne: 8082 (Router avec JF_ROUTER_ENABLED=true)
- Login par défaut: admin/password
- Startup ~60s

### Apache Reports
- Upload: `POST /upload` avec `multipart/form-data`
- Params: `archive` (fichier .tar.gz), `project` (nom)
- Extraction vers `/var/www/reports/<project>/<timestamp>/`

### Jenkins
- Setup wizard désactivé
- Docker socket monté pour builds Docker-in-Docker
