# DevOps Interview Project

Three independent parts: a Jenkins CI pipeline, an Ansible + Docker deployment, and a Bash utility.

```
.
├── 1-jenkins-pipeline/
│   ├── Jenkinsfile
│   └── shared-library/vars/prepareEnv.groovy
├── 2-ansible-docker/
│   ├── deploy-app.yml
│   ├── inventory.ini
│   ├── files/            # app.py, Dockerfile, requirements.txt
│   └── templates/        # docker-compose.yml.j2
└── 3-bash/extract
```

---

## 1. Jenkins pipeline

**Flow:** GitHub push → webhook → Jenkins → build → pack (`tar.gz`) → upload to JFrog Artifactory.

| Stage | What it does |
|---|---|
| Prepare Environment | Calls the shared library function `prepareEnv()` |
| Build Artifact | Copies the application files and writes `build-info.json` (commit, committer, build number) |
| Pack Artifact | Creates `interview-application-<build>-<commit>.tar.gz` |
| Upload to Repo | Uploads to the Artifactory generic repo with `curl` |

**Shared library: `prepareEnv`**
Passes the SCM trigger data into the pipeline as environment variables, read from `currentBuild.changeSets` (latest commit by timestamp):

| Variable | Content |
|---|---|
| `SCM_COMMITTER` | Author of the latest commit |
| `SCM_COMMIT_ID` | Short commit hash |
| `SCM_COMMIT_MSG` | Commit message |
| `BUILD_TRIGGER_CAUSE` | What started the build |

On a manual run or rebuild no changes are detected, so default values are used.

**Security**
- Credentials are stored in Jenkins (`artifactory-creds`), never in code.
- Upload uses single-quoted `sh` (no Groovy interpolation of secrets) and `curl --fail`, so a failed upload fails the build.
- A dedicated Artifactory user (`jenkins`) with Read + Deploy on a single repo.

**Jenkins setup**
- Job: *Pipeline script from SCM*, Script Path `1-jenkins-pipeline/Jenkinsfile`.
- Global Pipeline Library `my-shared-library`, Library Path `1-jenkins-pipeline/shared-library`.
- Trigger: `githubPush()` in the Jenkinsfile; GitHub webhook to `https://<ngrok-url>/github-webhook/` (content type `application/json`).

**Environment** (all on one local VM, Docker): Jenkins `lts-jdk17`, Artifactory OSS `7.55.10` + Postgres 15, ngrok for the public webhook endpoint.

---

## 2. Ansible + Docker

Deploys a compose app with two services:

| Service | Tech | Role |
|---|---|---|
| `web` | Python 3.11 + Flask | "Hello World" page, increments a counter per request |
| `db` | Redis 7 | Stores the hit counter in a named volume (survives restarts) |

```bash
cd 2-ansible-docker
ansible-playbook -i inventory.ini deploy-app.yml     # optional: -e host_port=9090
curl http://<host>:8080                              # counter grows on every call
```

Requirements: Docker with the compose plugin on the target, and `ansible-galaxy collection install community.docker`.
The playbook is idempotent (uses `file`, `copy`, `template`, `docker_compose_v2`).

---

## 3. Bash: `extract`

```
extract [-h] [-r] [-v] file [file...]
```

Unpacks files by **content** (parsed from `file -b`), never by name or extension. Supported: gzip, bzip2, zip, compress (`.Z`), tar.

- `-v` verbose (prints each unpacked file, warns on each file not unpacked)
- `-r` recurse into directories (without it, only the directory's own files)
- Prints the number of archives unpacked; the **exit code is the number of files NOT unpacked** (max 255)
- Uncompressed files are left untouched; existing files are overwritten

**Adding a format** is one line in the `case` inside `unpack()`, for example tar:
```bash
*"tar archive"*)  tar -xf "$1" -C "$(dirname "$1")" ;;
```

Requires: `file gzip bzip2 unzip tar ncompress`.

---
