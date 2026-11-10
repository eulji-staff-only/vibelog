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
# 환경 변수(선택): DB_NAME(기본 vibelog), DB_USER(~/.my.cnf 대신 쓸 때), BACKUP_DIR(기본 /var/backups/vibelog), KEEP_DAYS(기본 7)
set -euo pipefail

DB_NAME="${DB_NAME:-vibelog}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/vibelog}"
KEEP_DAYS="${KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/$DB_NAME-$(date +%Y%m%d-%H%M).sql.gz"
trap 'rm -f "$file.tmp"' EXIT   # 중간에 실패하면 반쯤 쓴 파일을 남기지 않는다

# --single-transaction: 서비스를 멈추지 않고 한 시점의 일관된 내용으로 받는다 (InnoDB)
mysqldump ${DB_USER:+--user="$DB_USER"} --single-transaction --routines --triggers --no-tablespaces "$DB_NAME" | gzip > "$file.tmp"
mv "$file.tmp" "$file"

# KEEP_DAYS 일보다 오래된 백업을 지운다 (매일 1개씩이라 KEEP_DAYS 개가 남는다)
find "$BACKUP_DIR" -name "$DB_NAME-*.sql.gz" -mtime +$((KEEP_DAYS - 1)) -delete

echo "$(date '+%F %T') 백업 완료: $file ($(du -h "$file" | cut -f1))"
