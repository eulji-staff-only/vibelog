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

[`scripts/backup-db.sh`](../../scripts/backup-db.sh) 가 `mysqldump` 로 받아 `gzip` 으로 줄이고, 7일보다 오래된 백업을 지운다.

1. 접속 정보는 스크립트·cron 에 적지 않고, 백업을 돌리는 계정의 `~/.my.cnf` 에 둔다.

   ```
   [client]
   user=vibelog
   password=(운영 DB 비밀번호)
   ```

   `chmod 600 ~/.my.cnf`

2. cron 에 한 줄 등록한다 (`crontab -e`, 매일 새벽 4시 10분).

   ```
   10 4 * * * /opt/vibelog/scripts/backup-db.sh >> /var/log/vibelog-backup.log 2>&1
   ```

3. 다음 날 `/var/backups/vibelog/` 에 `vibelog-YYYYMMDD-HHMM.sql.gz` 가 생겼는지, 로그에 "백업 완료" 가 있는지 본다.

> 백업이 같은 서버 디스크에만 있으면 서버가 통째로 망가졌을 때 함께 잃는다. 여유가 생기면 백업 폴더를 다른 곳(다른 서버·저장소)으로 복사한다.

## 3. 복구 (4시간 안에)

1. 앱을 멈춘다 (점검 중 화면을 띄우려면 `MAINTENANCE=true` 로 다시 시작한다, #365).
2. 되돌릴 백업을 고른다: `ls -lt /var/backups/vibelog/`
3. DB 를 비우고 백업을 넣는다.

   ```
   mysql -e "DROP DATABASE vibelog; CREATE DATABASE vibelog CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
   gunzip -c /var/backups/vibelog/vibelog-20261110-0410.sql.gz | mysql vibelog
   ```

4. 앱을 다시 시작하고(`MAINTENANCE` 끄기) 메인·로그인·게시판이 열리는지 확인한다.

> 백업 시각 이후에 생긴 데이터(글·쪽지 등)는 되돌아오지 않는다. 업로드 파일(`UPLOAD_DIR`)은 DB 백업에 들어 있지 않다.
