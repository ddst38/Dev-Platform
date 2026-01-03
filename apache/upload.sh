#!/bin/sh
set -e

UPLOAD_ROOT="/var/www/data/reports"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")

# Récupération du projet
PROJECT=$(echo "$REQUEST_URI" | sed -n 's/.*project=\([^&]*\).*/\1/p')
[ -z "$PROJECT" ] && PROJECT="unknown-project"

TARGET_DIR="$UPLOAD_ROOT/$PROJECT/$TIMESTAMP"
mkdir -p "$TARGET_DIR"

TMP_ARCHIVE="/tmp/report.tar.gz"
TMP_BODY="/tmp/body.bin"

# Lire tout le body
cat > "$TMP_BODY"

# Extraction du fichier archive via Python (multipart)
python3 - <<'PY'
import re, sys

body = open("/tmp/body.bin","rb").read()

# Recherche du champ archive
m = re.search(
    rb'Content-Disposition: form-data; name="archive"; filename="[^"]+"\r\n'
    rb'Content-Type: .*?\r\n\r\n',
    body,
    re.S
)

if not m:
    print("archive field not found", file=sys.stderr)
    sys.exit(1)

start = m.end()
end = body.find(b"\r\n--", start)

with open("/tmp/report.tar.gz","wb") as f:
    f.write(body[start:end])
PY

tar xzf "$TMP_ARCHIVE" -C "$TARGET_DIR"

rm -f "$TMP_ARCHIVE" "$TMP_BODY"

echo "Content-Type: application/json"
echo ""
echo "{\"status\":\"ok\",\"project\":\"$PROJECT\",\"path\":\"$TIMESTAMP\"}"