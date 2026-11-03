# 07. 저장소 설정 — 관리자용

> 저장소를 처음 만들 때 **관리자(Admin 권한) 한 명이** 이 순서대로 설정합니다. 다른 팀원은 읽지 않아도 됩니다.
> GitHub 화면의 메뉴 이름은 업데이트로 조금씩 바뀔 수 있습니다. 비슷한 이름의 메뉴를 찾아 주세요.

[← 협업 규칙 README로 돌아가기](../../README.md)

---

## 전체 순서

| 단계 | 할 일 |
|---|---|
| 1 | 팀원 초대 |
| 2 | `develop` 브랜치 만들고 **기본 브랜치로 지정** |
| 3 | PR · 머지 방식 설정 |
| 4 | 라벨 만들기 (스크립트) |
| 5 | 프로젝트 보드 만들기 |
| 6 | 브랜치 보호 규칙(Ruleset) 설정 |
| 7 | 협업 규칙 파일을 PR로 추가 → **전원 Approve = 규칙 동의** |

---

## 1. 팀원 초대

**Settings → Collaborators and teams → Add people** (조직 저장소면 팀 단위로 추가)
권한은 모두 **Write** 로 줍니다. Admin은 관리자 1~2명만.

---

## 2. develop 브랜치 만들고 기본 브랜치로 지정

`main`에 최소 한 개의 커밋(예: 빈 README)이 있어야 합니다.

```bash
git switch main
git pull origin main
git switch -c develop
git push -u origin develop
```

**Settings → General → Default branch** 에서 ⇄ 버튼을 눌러 **`develop`** 으로 바꿉니다.

> ❓ **왜 develop을 기본 브랜치로 하나요?**
> - PR을 만들 때 base가 자동으로 `develop`이 되어, 실수로 `main`에 PR을 보내는 일이 줄어듭니다.
> - PR 본문의 `Closes #12`는 **기본 브랜치에 머지될 때만** 이슈를 자동으로 닫습니다. 기본 브랜치가 `main`이면 이슈가 안 닫힙니다.
> - 이슈 템플릿도 기본 브랜치에 있는 파일을 읽습니다.

---

## 3. PR · 머지 방식 설정

**Settings → General → Pull Requests**

| 항목 | 설정 | 이유 |
|---|---|---|
| Allow merge commits | ✅ 켜기 | `develop → main` 배포용 |
| Allow squash merging | ✅ 켜기 → 기본 메시지 **Pull request title** 선택 | 작업 PR용. PR 제목이 커밋 메시지가 됨 |
| Allow rebase merging | ❌ 끄기 | 방식이 섞이지 않게 |
| Always suggest updating pull request branches | ✅ 켜기 | develop이 앞서가면 **Update branch** 버튼 표시 |
| Automatically delete head branches | ✅ 켜기 | 머지된 브랜치 자동 삭제 |

**Settings → General → Features** 에서 **Issues** 가 켜져 있는지 확인합니다.

---

## 4. 라벨 만들기

[GitHub CLI(`gh`)](https://cli.github.com/) 로 한 번에 만듭니다.

```bash
# 최초 1회: 설치 후 로그인
gh auth login

# 저장소 폴더(협업 규칙 파일이 있는 곳)에서 실행
bash scripts/setup-labels.sh            # 우리 라벨 만들기 (이미 있으면 색·설명만 갱신)
bash scripts/setup-labels.sh --clean    # GitHub 기본 라벨(bug, enhancement 등)을 지우고 만들기 ← 추천
```

> 💡 이슈 템플릿이 자동으로 붙이는 `✨ feature`, `🐛 bug` 라벨은 **이름이 정확히 같아야** 붙습니다. 라벨 이름을 바꾸면 `.github/ISSUE_TEMPLATE/*.yml`의 `labels:`도 함께 바꾸세요.

`gh`를 쓸 수 없다면 **Issues → Labels → New label** 에서 [04. 이슈 규칙의 라벨 목록](04-issue.md#라벨-목록)대로 직접 만들어도 됩니다.

---

## 5. 프로젝트 보드 만들기

1. 저장소의 **Projects** 탭 → **Link a project → New project** (또는 조직/내 프로필의 Projects에서 생성)
2. 템플릿은 **Board** 를 고릅니다.
3. **Status** 필드의 칸을 아래처럼 맞춥니다.
   `📋 Backlog` / `📝 Todo` / `🏃 In Progress` / `👀 In Review` / `✅ Done`
4. 보드 오른쪽 위 **⋯ → Workflows** 에서 자동화를 켭니다.

| 워크플로 | 설정 |
|---|---|
| Auto-add to project | 이 저장소, 필터 `is:issue,pr is:open` → 새 이슈·PR이 보드에 자동 추가 |
| Item added to project | Status → `📋 Backlog` |
| Item closed | Status → `✅ Done` |
| Pull request merged | Status → `✅ Done` |
| Item reopened | Status → `📝 Todo` |

5. (선택) **Issues → Milestones → New milestone** 으로 `1차 발표`처럼 마감 단위를 만듭니다.

---

## 6. 브랜치 보호 규칙 (Ruleset)

**Settings → Rules → Rulesets → New ruleset → New branch ruleset**

### develop 규칙

| 항목 | 설정 |
|---|---|
| Ruleset Name | `protect-develop` |
| Enforcement status | **Active** |
| Bypass list | 비워 둠 (관리자도 예외 없이) |
| Target branches | **Add target → Include by pattern →** `develop` |
| Restrict deletions | ✅ |
| Block force pushes | ✅ |
| Require a pull request before merging | ✅ |
| └ Required approvals | **1** |
| └ Dismiss stale pull request approvals when new commits are pushed | 선택 (켜면 더 안전하지만, 리뷰 반영할 때마다 재승인이 필요) |
| └ Require conversation resolution before merging | ✅ |
| └ Allowed merge methods | **Squash**, **Merge** (Merge는 hotfix 반영용) |
| Require status checks to pass | 지금은 끔. 자동 검사(CI)를 추가하면 켭니다 |

### main 규칙

`develop` 규칙과 같게 만들되 아래만 다릅니다.

| 항목 | 설정 |
|---|---|
| Ruleset Name | `protect-main` |
| Target branches | `main` |
| Required approvals | **1** (팀원이 4명 이상이면 **2** 권장) |
| Allowed merge methods | **Merge**, **Squash** (Squash는 hotfix용) |

> ⚠️ **비공개(Private) 저장소 + 무료 플랜**에서는 규칙이 실제로 강제되지 않을 수 있습니다.
> 공개 저장소로 운영하거나, 학생이라면 [GitHub Education](https://education.github.com/) 혜택을 확인해 보세요.
> 강제되지 않더라도 **규칙은 똑같이 지킵니다.**

---

## 7. 협업 규칙 파일 추가 (= 팀 합의)

이 단계는 규칙대로 진행하는 **첫 번째 연습**이기도 합니다.

1. **이슈 만들기**: `[작업] 협업 규칙 적용` (이때는 아직 템플릿이 없으니 빈 이슈로 만들어도 됩니다) → 번호 확인 (예: `#1`)
2. **브랜치 만들기**

   ```bash
   git switch develop
   git pull origin develop
   git switch -c docs/1-collaboration-rules
   ```

3. **파일 복사**: 전달받은 협업 규칙 파일을 압축 해제해서 **폴더 구조 그대로** 저장소 최상위에 넣습니다.

   ```
   README.md
   .gitmessage.txt
   .github/
   docs/collaboration/
   scripts/
   ```

   > ⚠️ `.github`, `.gitmessage.txt`처럼 점(`.`)으로 시작하는 파일은 숨김 파일이라 탐색기·Finder에서 안 보일 수 있습니다. 맥은 `Cmd + Shift + .`, 윈도우는 탐색기 **보기 → 숨긴 항목** 으로 표시하세요.

4. **커밋 · push**

   ```bash
   git add .
   git status                     # 위 파일들만 있는지 확인
   git commit -m "docs: 협업 규칙 문서와 이슈·PR 템플릿 추가"
   git push -u origin docs/1-collaboration-rules
   ```

5. **PR 만들기**: 제목 `docs: 협업 규칙 문서와 이슈·PR 템플릿 추가`, 본문에 `Closes #1`, Reviewers에 **팀원 전원** 지정
6. **전원 Approve 후 머지** (Squash and merge). 승인 1명이면 머지 버튼이 활성화되지만, 이 PR만큼은 **모두가 Approve 할 때까지 기다립니다.**
7. **확인**
   - **Issues → New issue** 에 양식 3개가 보이는지
   - 새 PR을 만들 때 템플릿이 자동으로 채워지는지
   - 팀원 모두에게 README의 [최초 1회 세팅](../../README.md#-처음-합류했다면--최초-1회-세팅)을 하라고 공지

---

## 나중에 추가하면 좋은 것

| 항목 | 설명 |
|---|---|
| `.gitignore`, `.env.example` | 기술 스택이 정해지면 바로 추가 (`.env`는 반드시 ignore) |
| 코드 스타일 도구 | 포매터·린터 설정을 `chore(config)` PR로 추가하고 규칙 문서에 반영 |
| 자동 검사 (GitHub Actions) | 빌드·테스트·린트를 PR마다 실행 → Ruleset에서 **Require status checks** 켜기 |
| `CODEOWNERS` | 폴더별 담당자를 자동으로 리뷰어 지정 (파트가 명확히 나뉘면) |
| README 분리 | 나중에 README에 프로젝트 소개를 넣을 때, 이 협업 규칙은 `CONTRIBUTING.md`로 옮기면 GitHub이 PR·이슈 화면에서 자동으로 링크해 줍니다 |
