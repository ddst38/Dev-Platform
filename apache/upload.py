#!/usr/bin/env python3
import cgi
import cgitb
import os
import tarfile
from datetime import datetime

cgitb.enable()

UPLOAD_ROOT = "/var/www/reports"
TIMESTAMP = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")

form = cgi.FieldStorage()

project = form.getvalue("project", "unknown-project")

target_dir = os.path.join(UPLOAD_ROOT, project, TIMESTAMP)
os.makedirs(target_dir, exist_ok=True)

fileitem = form["archive"]

if not fileitem.file:
    print("Status: 400 Bad Request")
    print("Content-Type: application/json\n")
    print('{"error":"missing archive"}')
    exit(1)

tmp_path = "/tmp/report.tar.gz"

with open(tmp_path, "wb") as f:
    f.write(fileitem.file.read())

with tarfile.open(tmp_path, "r:gz") as tar:
    tar.extractall(path=target_dir)

os.remove(tmp_path)

print("Content-Type: application/json\n")
print(f'{{"status":"ok","project":"{project}","path":"{TIMESTAMP}"}}')