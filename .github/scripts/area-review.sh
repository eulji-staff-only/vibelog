#!/usr/bin/env bash
# [area-review] PR 리뷰 검사 (.github/workflows/area-review.yml 에서 실행)
# 역할 : 사람 리뷰는 P1 PR 에만 요구한다 (팀 결정 #287).
#        - P1 이 아닌 PR(P2·P3·문서·설정 등): 리뷰어를 지정하지 않고 바로 통과한다.
#          머지 직전 확인은 CodeRabbit 이 맡는다 (리뷰 대화 해결 + build 는 Ruleset 이 그대로 강제).
#        - P1 PR: PM·형상관리 중 1명(작성자 본인 제외) 승인 + 다른 묶음 담당 영역·sql/ 을 바꿨으면 그 담당 승인.
#          아직 승인 안 한 사람을 리뷰어로 추가하고, 다 승인되면 통과한다.
#        - main 으로 가는 릴리스 PR: P1 과 상관없이 PM·형상관리 중 1명 승인 (모든 영역이 바뀌므로 영역 담당은 보지 않는다).
#          CODEOWNERS 를 비워 protect-main 의 코드 소유자 승인이 승인자를 가리지 못하므로 여기서 강제한다.
# 책임 : - P1 판단: PR 자체의 마일스톤과 본문의 Closes·Fixes·Resolves·Part of·Refs #번호 이슈의 마일스톤 중
#          하나라도 제목에 "P1" 이 있으면 P1 이다 (마일스톤 "3. P1 구현"). 이슈를 읽지 못하면 통과시키지 않는다.
#        - 영역 ↔ 담당 표는 README "영역 담당" 표와 같게 유지한다. (담당이 바뀌면 이 파일도 고친다)
#        - 임시 (#421): OPTIONAL_REVIEWERS(저장소 변수 AREA_REVIEW_OPTIONAL_REVIEWERS)에 적힌 사람은 리뷰어로 지정만 하고
#          승인은 요구하지 않는다 (예: PM 부재 중). 기본 승인에 남는 사람이 없으면(작성자 본인뿐) 기본 승인은 건너뛴다.
# 사용 : PR=<번호> REPO=<owner/name> GH_TOKEN=<토큰> bash .github/scripts/area-review.sh
#        DRY_RUN=1 이면 리뷰어 추가 없이 결과만 출력한다.
set -euo pipefail

BASE_REVIEWERS="dyryoo316 SeungBinYang"   # PM · 형상관리
OPTIONAL_REVIEWERS="${OPTIONAL_REVIEWERS:-}"
is_optional() { case " $OPTIONAL_REVIEWERS " in *" $1 "*) return 0 ;; *) return 1 ;; esac; }

owner_of() {
  case "$1" in
    auth|community) echo "SeungBinYang" ;;   # 묶음 A
    lesson|admin)   echo "dyryoo316" ;;      # 묶음 C
    exam|portfolio) echo "Choyeonik" ;;      # 묶음 D
    sql)            echo "Choyeonik" ;;      # DB 팀
    *)              echo "" ;;               # course·quiz(묶음 B, #204), common, error, contest, message 등 승인 담당 없는 곳
  esac
}

pr_view() { gh api "repos/$REPO/pulls/$PR" --jq "$1"; }
author=$(pr_view '.user.login')
base_ref=$(pr_view '.base.ref')
report="### 리뷰 검사 (작성자: $author, base: $base_ref)"$'\n'

if [ "$base_ref" != "main" ]; then
  # P1 인지: PR 마일스톤 + 본문에 연결한 이슈의 마일스톤
  milestones=$(pr_view '.milestone.title // ""')
  body=$(pr_view '.body // ""')   # 조회가 실패하면 set -e 로 여기서 검사가 실패한다
  issues=$(printf '%s\n' "$body" \
    | grep -oiE '(close[sd]?|fix(e[sd])?|resolve[sd]?|part of|refs?) #[0-9]+' | grep -oE '[0-9]+' | sort -u || true)
  for n in $issues; do
    if ! m=$(gh api "repos/$REPO/issues/$n" --jq '.milestone.title // ""'); then
      echo "연결한 이슈 #$n 을 읽지 못해 P1 인지 알 수 없습니다. 본문의 이슈 번호를 확인하고 다시 돌려 주세요."
      exit 1
    fi
    milestones+=$'\n'"$m"
  done

  if ! printf '%s\n' "$milestones" | grep -q 'P1'; then
    report+="- P1 이 아님 (PR 마일스톤·연결한 이슈: ${issues//$'\n'/ }) → 사람 리뷰 없이 머지 가능 (CodeRabbit 대화 해결 + build)"$'\n'
    printf '%s' "$report"
    [ -n "${GITHUB_STEP_SUMMARY:-}" ] && printf '%s' "$report" >> "$GITHUB_STEP_SUMMARY"
    exit 0
  fi
fi

# 사람별 마지막 리뷰 상태가 APPROVED 인 사람
approved=$(gh api --paginate "repos/$REPO/pulls/$PR/reviews" \
  --jq '.[] | select(.state != "COMMENTED") | "\(.user.login) \(.state)"' \
  | awk '{s[$1]=$2} END {for (u in s) if (s[u]=="APPROVED") print u}')
is_approved() { printf '%s\n' "$approved" | grep -qx "$1"; }

need=""   # 승인해야 통과하는 사람
ask=""    # 리뷰어로 지정할 사람 (아직 승인 안 함). 승인을 요구하지 않는 OPTIONAL_REVIEWERS 도 지정은 한다 (#421)

if [ -n "$OPTIONAL_REVIEWERS" ]; then
  report+="- 임시 (#421): $OPTIONAL_REVIEWERS 은(는) 리뷰어로만 지정하고 승인은 요구하지 않음"$'\n'
fi

# 기본 승인: PM·형상관리 중 작성자가 아닌 사람 1명 (OPTIONAL_REVIEWERS 는 승인 대상에서 빠진다)
base=""
required_base=""
for r in $BASE_REVIEWERS; do
  [ "$r" = "$author" ] && continue
  base="$base $r"
  is_optional "$r" || required_base="$required_base $r"
done
base_ok=""
for r in $base; do
  if is_approved "$r"; then base_ok="$r"; fi
done
if [ -n "$base_ok" ]; then
  report+="- 기본 승인 (PM·형상관리 중 1명): ✅ 승인됨 ($base_ok)"$'\n'
elif [ -z "$required_base" ]; then
  report+="- 기본 승인 (PM·형상관리 중 1명): ✅ 임시로 생략 (#421)"$'\n'
  ask="$ask $base"
else
  report+="- 기본 승인 (PM·형상관리 중 1명): ❌ 승인 필요 (${required_base# })"$'\n'
  need="$need $required_base"
  ask="$ask $base"
fi

# 바뀐 파일 → 영역 이름. 파일을 옮긴(rename) 경우 원래 위치(previous_filename)도 본다.
# main 릴리스 PR 은 모든 영역이 바뀌므로 영역 담당은 보지 않는다
areas=""
if [ "$base_ref" != "main" ]; then
  files=$(gh api --paginate "repos/$REPO/pulls/$PR/files" --jq '.[] | .filename, (.previous_filename // empty)')
  areas=$(printf '%s\n' "$files" | sed -nE \
    -e 's#^src/(main|test)/java/com/vibelog/domain/([a-z]+)/.*#\2#p' \
    -e 's#^src/main/resources/templates/([a-z]+)/.*#\1#p' \
    -e 's#^src/main/resources/static/(css|js)/([a-z]+)[./].*#\2#p' \
    -e 's#^sql/.*#sql#p' | sort -u)
fi

for area in $areas; do
  owners=$(owner_of "$area")
  [ -z "$owners" ] && continue
  case " $owners " in *" $author "*) continue ;; esac   # 내 영역 (DB 팀원이 sql/ 을 바꾼 경우 포함)
  ok="❌ 승인 필요"
  for o in $owners; do
    if is_approved "$o"; then ok="✅ 승인됨 ($o)"; fi
  done
  if [ "$ok" = "❌ 승인 필요" ]; then
    ask="$ask $owners"
    required=""
    for o in $owners; do is_optional "$o" || required="$required $o"; done
    if [ -z "$required" ]; then ok="✅ 임시로 생략 (#421)"; else need="$need $required"; fi
  fi
  report+="- 영역 \`$area\` → ${owners// /, }: $ok"$'\n'
done

printf '%s' "$report"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && printf '%s' "$report" >> "$GITHUB_STEP_SUMMARY"

# 아직 승인 안 한 사람을 리뷰어로 추가 (이미 요청돼 있으면 그대로). 승인을 요구하지 않는 사람도 지정은 한다 (#421)
if [ -z "${DRY_RUN:-}" ] && [ -n "$ask" ]; then
  for o in $(printf '%s\n' $ask | sort -u); do
    gh api -X POST "repos/$REPO/pulls/$PR/requested_reviewers" -f "reviewers[]=$o" >/dev/null 2>&1 \
      && echo "리뷰어 추가: $o" || echo "리뷰어 추가 실패(권한 없음 등): $o"
  done
fi

[ -z "$need" ] && exit 0
echo "P1 PR(또는 main 릴리스 PR)이라 승인이 필요합니다. 승인되면 이 검사가 다시 돌아 통과합니다."
exit 1
