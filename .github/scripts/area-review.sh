#!/usr/bin/env bash
# [area-review] 영역 간 연동 PR 검사 (.github/workflows/area-review.yml 에서 실행)
# 역할 : PR 이 "다른 묶음 담당 영역"의 파일을 바꾸면, 그 영역 담당을 리뷰어로 추가하고
#        그 담당이 승인(Approve)해야 통과시킨다. 내 영역만 바꾼 PR 은 바로 통과한다.
# 책임 : - 영역 ↔ 담당 표는 README "영역 담당" 표와 같게 유지한다. (담당이 바뀌면 이 파일도 고친다)
#        - 기본 리뷰어(PM·형상관리) 승인은 CODEOWNERS + Ruleset 이 따로 강제한다. 여기서는 영역 담당만 본다.
# 사용 : PR=<번호> REPO=<owner/name> GH_TOKEN=<토큰> bash .github/scripts/area-review.sh
#        DRY_RUN=1 이면 리뷰어 추가 없이 결과만 출력한다.
set -euo pipefail

owner_of() {
  case "$1" in
    auth|community) echo "SeungBinYang" ;;   # 묶음 A
    course|quiz)    echo "skinopero" ;;      # 묶음 B
    lesson|admin)   echo "dyryoo316" ;;      # 묶음 C
    exam|portfolio) echo "Choyeonik" ;;      # 묶음 D
    sql)            echo "Choyeonik skinopero" ;;  # DB 팀 (둘 중 1명)
    *)              echo "" ;;               # common, error, contest, message 등 담당 없는 곳
  esac
}

author=$(gh api "repos/$REPO/pulls/$PR" --jq '.user.login')
# 파일을 옮긴(rename) 경우 원래 위치(previous_filename)도 본다 — 다른 영역에서 파일을 빼 가는 것도 그 영역 변경이다
files=$(gh api --paginate "repos/$REPO/pulls/$PR/files" --jq '.[] | .filename, (.previous_filename // empty)')

# 바뀐 파일 → 영역 이름
areas=$(printf '%s\n' "$files" | sed -nE \
  -e 's#^src/(main|test)/java/com/vibelog/domain/([a-z]+)/.*#\2#p' \
  -e 's#^src/main/resources/templates/([a-z]+)/.*#\1#p' \
  -e 's#^src/main/resources/static/(css|js)/([a-z]+)[./].*#\2#p' \
  -e 's#^sql/.*#sql#p' | sort -u)

# 사람별 마지막 리뷰 상태가 APPROVED 인 사람
approved=$(gh api --paginate "repos/$REPO/pulls/$PR/reviews" \
  --jq '.[] | select(.state != "COMMENTED") | "\(.user.login) \(.state)"' \
  | awk '{s[$1]=$2} END {for (u in s) if (s[u]=="APPROVED") print u}')

report="### 영역 간 리뷰 (작성자: $author)"$'\n'
need=""
for area in $areas; do
  owners=$(owner_of "$area")
  [ -z "$owners" ] && continue
  case " $owners " in *" $author "*) continue ;; esac   # 내 영역 (DB 팀원이 sql/ 을 바꾼 경우 포함)
  ok="❌ 승인 필요"
  for o in $owners; do
    if printf '%s\n' "$approved" | grep -qx "$o"; then ok="✅ 승인됨 ($o)"; fi
  done
  report+="- \`$area\` → ${owners// /, }: $ok"$'\n'
  [ "$ok" = "❌ 승인 필요" ] && need="$need $owners"
done

if [ "$report" = "### 영역 간 리뷰 (작성자: $author)"$'\n' ]; then
  report+="- 다른 영역 파일 변경 없음 → 통과"$'\n'
fi
printf '%s' "$report"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && printf '%s' "$report" >> "$GITHUB_STEP_SUMMARY"

[ -z "$need" ] && exit 0

# 아직 승인 안 한 영역 담당을 리뷰어로 추가 (이미 요청돼 있으면 그대로)
if [ -z "${DRY_RUN:-}" ]; then
  for o in $(printf '%s\n' $need | sort -u); do
    gh api -X POST "repos/$REPO/pulls/$PR/requested_reviewers" -f "reviewers[]=$o" >/dev/null 2>&1 \
      && echo "리뷰어 추가: $o" || echo "리뷰어 추가 실패(권한 없음 등): $o"
  done
fi
echo "다른 영역 담당의 승인이 필요합니다. 승인되면 이 검사가 다시 돌아 통과합니다."
exit 1
