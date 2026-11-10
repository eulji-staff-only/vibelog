#!/usr/bin/env bash
# DB 매일 백업 (NFR-AVL-001, PL-DAT-003, #369): 매일 1회 받아 7일 보관한다. 복구 순서는 docs/collaboration/09-deploy.md
#
# 운영 서버 cron 에 한 줄로 등록한다 (매일 새벽 4시 10분):
#   10 4 * * * /opt/vibelog/scripts/backup-db.sh >> /var/log/vibelog-backup.log 2>&1
#
# 접속 정보는 이 파일·cron 에 적지 않고 ~/.my.cnf 에 둔다 (chmod 600):
#   [client]
#   user=vibelog
#   password=...
#
# 환경 변수(선택): DB_NAME(기본 vibelog), DB_USER(~/.my.cnf 대신 쓸 때), BACKUP_DIR(기본 /var/backups/vibelog), KEEP_DAYS(남길 개수, 기본 7),
#   BACKUP_REMOTE(다른 서버에도 둘 때 rsync 대상, 예: backup@10.0.0.5:/backups/vibelog/)
set -euo pipefail
umask 077   # 백업(회원 정보 포함)은 백업 계정만 읽는다: 폴더 700, 파일 600

DB_NAME="${DB_NAME:-vibelog}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/vibelog}"
KEEP_DAYS="${KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/$DB_NAME-$(date +%Y%m%d-%H%M).sql.gz"
trap 'rm -f "$file.tmp"' EXIT   # 중간에 실패하면 반쯤 쓴 파일을 남기지 않는다

# --single-transaction: 서비스를 멈추지 않고 한 시점의 일관된 내용으로 받는다 (InnoDB)
mysqldump ${DB_USER:+--user="$DB_USER"} --single-transaction --routines --triggers --no-tablespaces "$DB_NAME" | gzip > "$file.tmp"
mv "$file.tmp" "$file"

echo "$(date '+%F %T') 백업 완료: $file ($(du -h "$file" | cut -f1))"

# 새것부터 KEEP_DAYS 개만 남긴다 (매일 1개라 KEEP_DAYS 일치). 날짜로 지우면 백업이 며칠 실패했을 때 남은 백업까지 다 지워지므로 개수로 센다.
# 파일 이름이 날짜·시각 순이라 이름순 = 오래된 순이다
ls -1 "$BACKUP_DIR/$DB_NAME"-*.sql.gz | sort | head -n -"$KEEP_DAYS" | xargs -r rm --

# 서버가 통째로 망가져도 복구할 수 있게 다른 곳에도 둔다. 보내기만 하고(--delete 없음) 원격 보관 개수는 원격에서 따로 관리한다.
# BatchMode: 암호 있는 SSH 키 등으로 비대화형 접속이 안 되면 cron 에서 기다리지 않고 바로 실패한다
if [ -n "${BACKUP_REMOTE:-}" ]; then
    rsync -a -e "ssh -o BatchMode=yes" "$file" "$BACKUP_REMOTE"
    echo "$(date '+%F %T') 원격 복사 완료: $BACKUP_REMOTE"
fi
