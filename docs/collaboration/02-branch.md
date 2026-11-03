# 02. 브랜치 규칙

[← 협업 규칙 README로 돌아가기](../../README.md)

---

## 브랜치 전략 한눈에 보기

```
main     o------------------------------------o---->   배포된 코드
         |                                    ^
develop  +--o----------o----o-------------o---o---->   모두의 작업이 모이는 곳
            |          ^    |             ^
feat/12     +--o--o--o-+    |             |        기능 하나 = 브랜치 하나
fix/20                      +--o--o-------+
```

- `o` 는 커밋, `^` 는 아래 브랜치가 위 브랜치로 **합쳐지는(merge) 지점**입니다.
- `feat`·`fix` 등 작업 브랜치 → `develop` : **Squash merge** (PR 하나 = 커밋 하나)
- `develop` → `main` : **Merge commit** (배포할 때만, 릴리스 PR로)

---

## 브랜치 종류

### 항상 존재하는 브랜치 (보호됨 · 직접 push 금지)

| 브랜치 | 역할 | 누가 합치나요 |
|---|---|---|
| `main` | **배포된(발표·시연용) 코드만** 있는 곳. 언제나 동작해야 합니다 | 배포 담당자가 릴리스 PR로 |
| `develop` | 모든 팀원의 작업이 **모이는 곳**. 저장소의 기본(default) 브랜치입니다 | 각자 PR 승인 후 직접 |

### 작업할 때 만드는 브랜치 (작업 후 삭제)

항상 **`develop`에서 출발**하고, **`develop`으로 돌아옵니다.** (`hotfix`만 예외)

| 접두어 | 언제 쓰나요 | 예시 |
|---|---|---|
| `feat/` | 새 기능 | `feat/12-quiz-grading` |
| `fix/` | 버그 수정 | `fix/20-login-redirect` |
| `design/` | 화면(UI)·CSS 변경 | `design/31-portfolio-card` |
| `refactor/` | 동작은 같고 코드 구조 개선 | `refactor/27-exam-report-split` |
| `test/` | 테스트 코드 추가·수정 | `test/33-quiz-pass-rate` |
| `docs/` | 문서 작업 | `docs/1-collaboration-rules` |
| `chore/` | 설정·패키지·빌드 등 | `chore/5-project-setup` |
| `hotfix/` | **`main`에서 발견된 긴급 버그** (아래 별도 설명) | `hotfix/40-signup-crash` |

> 💡 접두어는 [커밋 타입](03-commit.md#타입-type--무슨-종류의-변경인가요)과 똑같습니다. 하나만 외우면 둘 다 쓸 수 있어요.

---

## 이름 짓는 규칙

```
<타입>/<이슈번호>-<짧은-영어-설명>
```

1. **전부 소문자 영어**, 단어 사이는 **하이픈(`-`)**
2. **이슈 번호는 필수** (`#`은 빼고 숫자만)
3. 설명은 **2~4 단어**로 짧게
4. 한글, 띄어쓰기, 대문자, 밑줄(`_`) 사용 금지

| | 예시 | 이유 |
|---|---|---|
| ⭕ | `feat/12-quiz-grading` | 규칙 그대로 |
| ⭕ | `fix/20-login-redirect` | 규칙 그대로 |
| ❌ | `quiz` | 타입·이슈 번호가 없음 |
| ❌ | `feat/퀴즈채점` | 한글 사용 |
| ❌ | `Feat/12_Quiz_Grading` | 대문자·밑줄 사용 |
| ❌ | `feat/12-add-quiz-grading-and-pass-check-and-retry` | 너무 김 |
| ❌ | `hong-branch` | 사람 이름 브랜치 금지. 브랜치는 "사람"이 아니라 "일" 단위 |

---

## 브랜치 사용 원칙

1. **이슈 1개 = 브랜치 1개 = PR 1개.** 한 브랜치에서 여러 이슈를 처리하지 않습니다.
2. **브랜치는 짧게 삽니다.** 만든 지 2~3일 안에 PR을 올리는 것이 목표입니다. 길어진다면 이슈를 더 작게 쪼갤 신호입니다.
3. **브랜치에서 브랜치를 따지 않습니다.** 항상 최신 `develop`에서 새로 만듭니다.
   (내 작업이 아직 머지 안 된 동료의 코드에 꼭 필요하면, 팀에 먼저 공유하고 동료의 PR이 머지된 뒤 시작하는 것을 우선으로 합니다.)
4. **다른 사람의 브랜치에는 허락 없이 push 하지 않습니다.**
5. **머지가 끝난 브랜치는 바로 삭제합니다.** (GitHub은 자동, 내 컴퓨터는 [01. 작업 흐름 9단계](01-workflow.md#9단계-뒷정리))

---

## 배포하기 (develop → main) — 배포 담당자용

발표·시연·마일스톤 마감처럼 "지금 버전을 확정하자"는 시점에만 합니다.

1. 팀원 모두에게 "배포 PR 올립니다, 머지 중인 작업 확인해 주세요"라고 알립니다.
2. **Pull requests → New pull request** 에서 `base: main` ← `compare: develop` 으로 PR을 만듭니다.
3. 제목: `[Release] v0.1.0` 처럼 버전을 적습니다.
4. 본문에 이번 배포에 포함된 PR 목록을 적습니다.
5. 규칙에 따라 승인을 받은 뒤 **Create a merge commit** 방식으로 머지합니다. (⚠️ 여기서만 Squash가 아닙니다)
6. **Releases → Draft a new release** 에서 태그 `v0.1.0`, 대상 `main`을 고르고 **Generate release notes**를 눌러 발행합니다.

**버전 번호 붙이는 법 (간단 버전)**

| 자리 | 언제 올리나요 | 예시 |
|---|---|---|
| `v0.X.0` | 마일스톤·발표 단위로 기능이 묶여 나갈 때 | `v0.1.0` → `v0.2.0` |
| `v0.X.Y` | 배포 후 버그만 고쳐서 다시 낼 때 (hotfix) | `v0.2.0` → `v0.2.1` |
| `v1.0.0` | 정식 공개할 때 | |

---

## 긴급 수정 (hotfix) — main에서 버그가 발견됐을 때

`develop`에는 아직 배포하면 안 되는 작업이 섞여 있을 수 있으므로, **`main`에서 바로** 고칩니다.

```bash
git switch main
git pull origin main
git switch -c hotfix/40-signup-crash
# 수정 → 커밋 → push
git push -u origin hotfix/40-signup-crash
```

1. PR을 `base: main` 으로 올리고, 승인 후 머지합니다.
2. **잊지 말기!** 같은 수정이 `develop`에도 들어가도록 `base: develop` ← `compare: main` PR을 하나 더 만들어 **Create a merge commit**으로 머지합니다.
3. 릴리스 태그를 `v0.2.1`처럼 올립니다.

> 💡 hotfix는 "지금 배포본이 망가졌다"는 긴급 상황에만 씁니다. 일반 버그는 `fix/` 브랜치로 `develop`에서 고칩니다.

---

## 브랜치 보호 규칙 (저장소에 설정됨)

| 항목 | `develop` | `main` |
|---|---|---|
| 직접 push | ❌ 금지 | ❌ 금지 |
| 강제 push (`--force`) | ❌ 금지 | ❌ 금지 |
| 브랜치 삭제 | ❌ 금지 | ❌ 금지 |
| 머지 전 필요한 승인 | 1명 이상 | 1명 이상 (팀원이 4명 이상이면 2명 권장) |
| 대화(코멘트) 모두 해결 | 필수 | 필수 |
| 머지 방식 | Squash merge (hotfix 반영만 Merge commit) | Merge commit |

설정 방법은 [07. 저장소 설정](07-repo-setup.md)을 보세요.
