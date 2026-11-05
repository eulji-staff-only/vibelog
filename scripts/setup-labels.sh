#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# 팀 라벨을 한 번에 만드는 스크립트 (관리자용)
#
# 준비:   GitHub CLI 설치 (https://cli.github.com) → gh auth login
# 실행:   저장소 폴더 안에서
#         bash scripts/setup-labels.sh            # 라벨 만들기 (이미 있으면 색·설명 갱신)
#         bash scripts/setup-labels.sh --clean    # GitHub 기본 라벨 삭제 후 만들기
#
# 다른 저장소에 적용하려면:  REPO=조직이름/저장소이름 bash scripts/setup-labels.sh
#
# ⚠️ 이슈 템플릿(.github/ISSUE_TEMPLATE/*.yml)의 labels 값과 이름이 같아야 합니다.
#    라벨 이름을 바꾸면 템플릿도 함께 바꾸세요.
# ─────────────────────────────────────────────────────────────
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "❌ GitHub CLI(gh)가 없습니다. https://cli.github.com 에서 설치 후 'gh auth login'을 실행하세요."
  exit 1
fi

REPO_ARGS=()
if [[ -n "${REPO:-}" ]]; then
  REPO_ARGS=(--repo "$REPO")
fi

# ── 1. (선택) GitHub 기본 라벨 삭제 ─────────────────────────
if [[ "${1:-}" == "--clean" ]]; then
  echo "🧹 GitHub 기본 라벨을 삭제합니다..."
  for name in "bug" "documentation" "duplicate" "enhancement" "good first issue" \
              "help wanted" "invalid" "question" "wontfix"; do
    gh label delete "$name" --yes ${REPO_ARGS[@]+"${REPO_ARGS[@]}"} >/dev/null 2>&1 \
      && echo "   삭제: $name" || true
  done
fi

# ── 2. 라벨 생성 ────────────────────────────────────────────
create() {
  # $1 이름, $2 색상(# 없이), $3 설명
  gh label create "$1" --color "$2" --description "$3" --force ${REPO_ARGS[@]+"${REPO_ARGS[@]}"} >/dev/null
  echo "   ✔ $1"
}

echo "🏷️  타입"
create "✨ feature"   "A2EEEF" "새 기능"
create "🐛 bug"       "D73A4A" "버그"
create "🎨 design"    "F9D0C4" "화면(UI)·CSS 변경"
create "♻️ refactor"  "C5DEF5" "동작은 같고 코드 구조 개선"
create "✅ test"      "BFDADC" "테스트 추가·수정"
create "📝 docs"      "0075CA" "문서"
create "🔧 chore"     "EDEDED" "설정·패키지·빌드"

echo "🏷️  우선순위"
create "P0: 긴급"     "B60205" "하던 일을 멈추고 즉시 처리"
create "P1: 높음"     "D93F0B" "이번 마일스톤 안에 반드시"
create "P2: 보통"     "FBCA04" "이번 마일스톤 안에 가능하면 (기본값)"
create "P3: 낮음"     "0E8A16" "여유 있을 때"

echo "🏷️  파트"
create "part: FE"     "1D76DB" "프론트엔드"
create "part: BE"     "5319E7" "백엔드"
create "part: 공통"   "C2E0C6" "설정·배포·문서 등 파트 구분 없는 작업"

echo "🏷️  영역"
AREA_COLOR="D4C5F9"
# 요구사항 명세서 업무 코드(FR-{업무}-…)와 커밋 영역을 묶은 것
create "area: 회원"               "$AREA_COLOR" "USR · 커밋 영역: auth"
create "area: 진단·트랙·코스"     "$AREA_COLOR" "DGN, CRS · 커밋 영역: course"
create "area: 수강·수업·퀴즈"     "$AREA_COLOR" "ENR, LSN · 커밋 영역: lesson, quiz"
create "area: 모의고사·리포트"    "$AREA_COLOR" "EXM · 커밋 영역: exam"
create "area: 포트폴리오"         "$AREA_COLOR" "PRT · 커밋 영역: portfolio"
create "area: 커뮤니티·리뷰"      "$AREA_COLOR" "CMT, REV · 커밋 영역: community"
create "area: 콘테스트·배지"      "$AREA_COLOR" "CTS, RWD · 커밋 영역: contest"
create "area: 쪽지·신고·알림"     "$AREA_COLOR" "MSG, RPT, NTF · 커밋 영역: message"
create "area: 백오피스"           "$AREA_COLOR" "ADM · 커밋 영역: admin"
create "area: 공통·설정"          "$AREA_COLOR" "NFR, 인프라 · 커밋 영역: common, config"

echo "🏷️  상태"
create "🌱 good first issue" "7057FF" "처음 합류한 사람이 하기 좋은 쉬운 이슈"
create "🙋 help wanted"      "008672" "도움이 필요함"
create "❓ question"          "D876E3" "결정이 필요한 질문·논의"
create "⛔ blocked"           "5C5C5C" "다른 이슈·결정이 먼저 끝나야 진행 가능"
create "🔁 duplicate"         "CFD3D7" "이미 있는 이슈와 중복"

echo ""
echo "🎉 라벨 설정 완료! 저장소의 Issues → Labels 에서 확인하세요."
