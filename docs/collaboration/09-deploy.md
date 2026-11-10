# 09. 운영 서버 (HTTPS · DB 백업) — 배포 담당자용

> 서버 1대에 앱과 MySQL 을 두고, 앞에 Caddy 를 세워 HTTPS 를 처리한다 (#369 결정).
> 근거: NFR-SEC-002 "모든 통신은 HTTPS", NFR-AVL-001·PL-DAT-003 "DB 매일 1회 백업, 7일 보관, 4시간 안에 복구"

```
브라우저 ──HTTPS(443)──▶ Caddy ──HTTP──▶ 앱 127.0.0.1:8080 ──▶ MySQL (같은 서버)
          (80 은 443 으로)  (인증서 자동 발급·갱신)
```

## 1. HTTPS

### Caddy

도메인이 이 서버를 가리키게 한 뒤 Caddy 를 설치하고 `/etc/caddy/Caddyfile` 을 아래처럼 둔다. 인증서 발급·갱신, HTTP→HTTPS 이동, `X-Forwarded-*` 헤더는 Caddy 가 알아서 한다.

```
vibelog.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

### 앱 환경 변수 (운영 서버만)

설정 파일은 바꾸지 않는다. 로컬은 지금처럼 `http://localhost:8080` 으로 돌고, 운영 서버에서만 아래 값을 넣는다.

| 환경 변수 | 값 | 이유 |
|---|---|---|
| `SERVER_ADDRESS` | `127.0.0.1` | 앱을 서버 안에서만 열어, 8080 으로 HTTP 를 바로 쓰지 못하게 한다 |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `native` | Caddy 가 보낸 `X-Forwarded-Proto: https` 를 믿어 앱이 HTTPS 요청으로 본다 (로그인 이동 주소, HSTS, 로그인 유지 쿠키) |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` | 세션 쿠키를 HTTPS 에서만 보낸다 |
| `APP_BASE_URL` | `https://vibelog.example.com` | 메일 링크 주소 (README 최초 세팅 참고) |

로컬에서 확인한 것 (2026-11-10): 위 값으로 띄우고 `X-Forwarded-Proto: https` 로 요청하면 세션 쿠키에 `Secure`, `Strict-Transport-Security` 헤더가 붙고, 로그인 이동 주소가 `https://…` 이며, 앱은 `127.0.0.1:8080` 에서만 받는다.

## 2. DB 백업 (매일 1회, 7일 보관)

[`scripts/backup-db.sh`](../../scripts/backup-db.sh) 가 `mysqldump` 로 받아 `gzip` 으로 줄이고, 새것부터 7개(매일 1개라 7일치)만 남긴다.
날짜가 아니라 개수로 지우므로, 백업이 며칠 실패해도 남아 있던 백업이 사라지지 않는다.

0. 백업을 돌릴 계정(아래 예: 앱을 돌리는 `vibelog` 리눅스 계정)이 백업 폴더와 로그 파일에 쓸 수 있게 만든다.
   cron 은 로그 파일을 열지 못하면 스크립트를 실행하지 않는다.

   ```
   sudo install -d -o vibelog -g vibelog -m 700 /var/backups/vibelog
   sudo install -o vibelog -g vibelog -m 600 /dev/null /var/log/vibelog-backup.log
   ```

1. 접속 정보는 스크립트·cron 에 적지 않고, 백업을 돌리는 계정의 `~/.my.cnf` 에 둔다.

   ```
   [client]
   user=vibelog
   password=(운영 DB 비밀번호)
   ```

   `chmod 600 ~/.my.cnf`

2. 그 계정으로 cron 에 한 줄 등록한다 (`sudo -u vibelog crontab -e`, 매일 새벽 4시 10분).

   ```
   10 4 * * * /opt/vibelog/scripts/backup-db.sh >> /var/log/vibelog-backup.log 2>&1
   ```

3. 다음 날 `/var/backups/vibelog/` 에 `vibelog-YYYYMMDD-HHMM.sql.gz` 가 생겼는지, 로그에 "백업 완료" 가 있는지 본다.

4. **서버 밖에도 둔다.** 백업이 같은 서버 디스크에만 있으면 디스크가 망가질 때 DB 와 함께 잃는다.
   다른 서버(또는 그 서버에 붙인 저장소)에 **암호 없는 SSH 키**로 들어갈 수 있게 한 뒤, cron 줄 앞에 `BACKUP_REMOTE` 를 붙인다.
   스크립트가 백업할 때마다 새 백업 파일을 `rsync` 로 보낸다 (보내기만 하고 원격 파일은 지우지 않는다).
   백업 파일은 스크립트가 `umask 077` 로 만들어 백업 계정만 읽는다.

   ```
   10 4 * * * BACKUP_REMOTE=backup@10.0.0.5:/backups/vibelog/ /opt/vibelog/scripts/backup-db.sh >> /var/log/vibelog-backup.log 2>&1
   ```

   - 등록 전에 cron 과 같은 조건(백업 계정, 비대화형)으로 접속되는지 보고, 원격 백업 폴더를 만든다.
     "ok" 가 안 나오면 cron 에서도 원격 복사가 실패한다. 폴더가 없으면 `rsync` 가 만들지 못해 원격 복사가 매일 실패한다.

     ```
     sudo -u vibelog ssh -o BatchMode=yes backup@10.0.0.5 'mkdir -p -m 700 /backups/vibelog' && echo ok
     ```

   - 원격 서버에서는 새것부터 7개만 남기는 cron 을 따로 둔다 (원격 보관 개수는 원격이 관리).

     ```
     20 5 * * * cd /backups/vibelog && ls -1 vibelog-*.sql.gz | sort | head -n -7 | xargs -r rm --
     ```

   - 다음 날 로그에 그날 "원격 복사 완료" 가 있는지, 원격에 그날 파일이 있는지 본다.

     ```
     grep "원격 복사 완료" /var/log/vibelog-backup.log | tail -1
     sudo -u vibelog ssh -o BatchMode=yes backup@10.0.0.5 'ls -1 /backups/vibelog | tail -3'
     ```

## 3. 복구 (4시간 안에)

1. 앱을 멈춘다 (점검 중 화면을 띄우려면 `MAINTENANCE=true` 로 다시 시작한다, #365).
2. 되돌릴 백업을 고르고, **DB 를 지우기 전에** 그 파일이 온전한지 본다. 셋 중 하나라도 실패하면 다른 백업을 고른다
   (서버 디스크가 망가졌으면 `BACKUP_REMOTE` 쪽에서 가져온다).

   ```
   ls -lt /var/backups/vibelog/
   BACKUP=/var/backups/vibelog/vibelog-YYYYMMDD-HHMM.sql.gz     # 위에서 고른 파일
   test -s "$BACKUP" && gzip -t "$BACKUP" && gunzip -c "$BACKUP" | tail -1 | grep -q "Dump completed" && echo "쓸 수 있는 백업"
   ```

3. DB 를 새로 만들고 백업을 넣는다. 새 서버라 DB 가 없어도 되게 `IF EXISTS` 를 쓰고, DB 를 다시 만든 경우에만 백업을 넣도록 `&&` 로 잇는다.
   `pipefail` 이라 압축 풀기가 실패해도 성공으로 보이지 않는다.

   ```
   set -o pipefail
   mysql -e "DROP DATABASE IF EXISTS vibelog; CREATE DATABASE vibelog CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" && gunzip -c "$BACKUP" | mysql vibelog && echo "복구 완료"
   ```

4. "복구 완료" 를 본 뒤에만 앱을 다시 시작하고(`MAINTENANCE` 끄기) 메인·로그인·게시판이 열리는지 확인한다.

> 백업 시각 이후에 생긴 데이터(글·쪽지 등)는 되돌아오지 않는다. 업로드 파일(`UPLOAD_DIR`)은 DB 백업에 들어 있지 않다.
