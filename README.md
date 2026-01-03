# Dev-Platform

Infrastructure Docker pour environnement de test CI/CD et migration.

## Services

| Service | Port | Description |
|---------|------|-------------|
| **CVS** | 2401 | Serveur CVS pserver |
| **Reports** | 8085 | Serveur Apache avec upload CGI |
| **Artifactory** | 8082 | Repository d'artefacts (OSS 7.x) |
| **Jenkins** | 8081 | Serveur CI/CD (LTS JDK21) |

## Prérequis

- Docker Desktop
- macOS / Linux (testé sur ARM64 M1)

## Démarrage

```bash
# Démarrer tous les services
docker compose up -d

# Vérifier l'état
docker compose ps
```

## Accès

- **Artifactory**: http://localhost:8082 (admin / password)
- **Jenkins**: http://localhost:8081
- **Reports**: http://localhost:8085

## Upload de rapports

```bash
# Envoyer un rapport HTML archivé
curl -X POST \
  -F "archive=@report.tar.gz" \
  -F "project=mon-projet" \
  http://localhost:8085/upload

# Accéder au rapport
# http://localhost:8085/mon-projet/<timestamp>/
```

## Structure

```
.
├── docker-compose.yml    # Orchestration des services
├── apache/               # Config serveur Reports
│   ├── Dockerfile
│   ├── httpd.conf
│   └── upload.py         # Script CGI upload
├── cvs/                  # Config serveur CVS
│   └── Dockerfile
└── artifactory/          # Config Artifactory
    └── etc/system.yaml
```

## Volumes (non versionnés)

Ces répertoires sont créés localement au premier démarrage :

- `jenkins_home/` - Données Jenkins
- `cvsroot/` - Repository CVS
- `data/reports/` - Rapports uploadés

## Notes techniques

- **Artifactory 7.x** : Le Router écoute sur 8082 (`JF_ROUTER_ENABLED=true`)
- **Apache** : Python 3.13 avec `legacy-cgi` pour compatibilité CGI
- **Jenkins** : Setup wizard désactivé, Docker socket monté
