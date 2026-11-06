#!/usr/bin/env bash
# [check-develop] 팀 공용 Claude Code 훅 (.claude/settings.json 에서 실행)
# 역할 : origin/develop 에 새 커밋이 생겼으면 사용자에게 알리고, Claude 가 git pull 을 권하게 한다.
#        새 커밋이 공유 파일(sql/, build.gradle, CLAUDE.md, global/ 등)을 바꿨으면 pull 후 할 일도 알려 준다.
# 책임 : - session : Claude Code 를 켤 때 (SessionStart) 확인한다.
#        - prompt  : 메시지를 보낼 때 (UserPromptSubmit) 확인한다. 10분에 한 번만 fetch 하고, 같은 develop 커밋은 한 번만 알린다.
#        - 오프라인·로그인 안 됨·git 없음 등 어떤 실패도 작업을 막지 않는다. (항상 exit 0, 알릴 게 없으면 출력 없음)

mode="${1:-session}"
cd "${CLAUDE_PROJECT_DIR:-.}" 2>/dev/null || exit 0
gitdir=$(git rev-parse --git-dir 2>/dev/null) || exit 0
stamp="$gitdir/vibelog-last-fetch"
seen="$gitdir/vibelog-last-notified"

now=$(date +%s)
if [ "$mode" = "prompt" ]; then
  last=$(cat "$stamp" 2>/dev/null || echo 0)
  [ $((now - last)) -lt 600 ] && exit 0
fi
echo "$now" > "$stamp" 2>/dev/null

# 비밀번호 입력 창이 뜨거나 느린 네트워크에서 멈추지 않게 한다
GIT_TERMINAL_PROMPT=0 GCM_INTERACTIVE=never \
  git -c http.lowSpeedLimit=1000 -c http.lowSpeedTime=5 fetch --quiet origin develop >/dev/null 2>&1 || exit 0

behind=$(git rev-list --count HEAD..origin/develop 2>/dev/null) || exit 0
[ "${behind:-0}" -gt 0 ] 2>/dev/null || exit 0

dev=$(git rev-parse origin/develop)
if [ "$mode" = "prompt" ] && [ "$(cat "$seen" 2>/dev/null)" = "$dev" ]; then
  exit 0
fi
echo "$dev" > "$seen" 2>/dev/null

# develop 에 새로 들어온 변경 파일 (내 브랜치가 갈라진 뒤 develop 쪽에서 바뀐 것만)
files=$(git diff --name-only HEAD...origin/develop 2>/dev/null)
has() { printf '%s\n' "$files" | grep -Eq "$1"; }
tags=""
todo=""
add() { tags="$tags, $1"; todo="$todo
- $2"; }

has '^sql/' && add "sql/" "sql/ 변경 → pull 후 sql/01-schema.sql, sql/02-seed.sql 다시 실행 (로컬 데이터가 초기화됨)"
has '^(build\.gradle|settings\.gradle|gradle/)' && add "build.gradle" "의존성·빌드 변경 → pull 후 IDE 에서 Gradle 새로고침"
has 'application[^/]*\.ya?ml$' && add "application.yaml" "설정 변경 → pull 후 실행 중인 앱 재시작"
has '^(CLAUDE\.md|\.claude/)' && add "CLAUDE.md" "팀 Claude 규칙 변경 → pull 후 Claude Code 다시 시작 (또는 /compact)"
has '^src/main/java/com/vibelog/global/|^src/main/resources/templates/(common|error)/|^src/main/resources/static/|/model/policy/|/Fake[A-Za-z]*\.java$' \
  && add "공통 코드" "공통 코드·레이아웃·CSS·계약 인터페이스 변경 → 내 브랜치에 바로 반영해야 충돌·빌드 오류가 작음"
has '^docs/(planning|design)/' && add "기획·설계 문서" "요구사항·정책·명세·화면 설계 변경 → 내 이슈와 관련 있는지 확인"
tags="${tags#, }"
[ -z "$todo" ] && todo="
- 없음 (공유 파일 변경 없음)"

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
if [ "$branch" = "develop" ]; then cmd="git pull"; else cmd="git pull origin develop"; fi
dirty=""
[ -n "$(git status --porcelain --untracked-files=no 2>/dev/null)" ] \
  && dirty=" 커밋하지 않은 변경이 있으니 먼저 커밋(또는 git stash)한 뒤 pull 하도록 안내한다."
commits=$(git log --no-merges --format='- %s' HEAD..origin/develop 2>/dev/null | head -8)

user_msg="📥 develop 에 새 커밋 ${behind}개 — \`${cmd}\` 를 권장합니다.${tags:+ (공유 파일 변경: ${tags})}"
context="[develop 변경 알림] 현재 브랜치 ${branch} 가 origin/develop 보다 커밋 ${behind}개 뒤처져 있다.
답변을 시작할 때 사용자에게 한두 줄로 먼저 알리고 \`${cmd}\` 를 권한다. pull 은 사용자가 원할 때만 실행한다.${dirty}
충돌이 나면 멈추고, 다른 사람 코드에서 난 충돌이면 CLAUDE.md 의 B. 팀 상의 안건으로 넘긴다.
새 커밋:
${commits}
pull 후 할 일:${todo}"

# JSON 문자열로 바꾼다: 탭은 공백으로, 줄바꿈을 뺀 나머지 제어 문자는 지워서 JSON 이 깨지지 않게 한다
json() { printf '%s' "$1" | tr '\t' ' ' | tr -d '\000-\010\013-\037' | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk 'NR>1{printf "\\n"} {printf "%s", $0}'; }
if [ "$mode" = "prompt" ]; then event="UserPromptSubmit"; else event="SessionStart"; fi
printf '{"systemMessage":"%s","hookSpecificOutput":{"hookEventName":"%s","additionalContext":"%s"}}\n' \
  "$(json "$user_msg")" "$event" "$(json "$context")"
exit 0
