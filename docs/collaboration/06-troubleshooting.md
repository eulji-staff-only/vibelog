# 06. 문제 해결 — 실수해도 괜찮아요

> Git에서 일어나는 실수는 **거의 다 되돌릴 수 있습니다.** 당황해서 이것저것 명령어를 치는 것이 오히려 더 위험합니다.
> 상황을 찾아 순서대로 따라 하고, 안 되면 바로 질문하세요.

[← 협업 규칙 README로 돌아가기](../../README.md)

---

## 목차

| # | 상황 |
|---|---|
| 1 | [충돌(conflict)이 났다](#1-충돌conflict이-났다) |
| 2 | [커밋 메시지를 잘못 썼다](#2-커밋-메시지를-잘못-썼다) |
| 3 | [커밋에 파일 하나를 빠뜨렸다](#3-커밋에-파일-하나를-빠뜨렸다) |
| 4 | [develop에서 작업해 버렸다](#4-develop에서-작업해-버렸다) |
| 5 | [엉뚱한 파일을 git add 했다](#5-엉뚱한-파일을-git-add-했다) |
| 6 | [고친 내용을 버리고 처음으로 돌아가고 싶다](#6-고친-내용을-버리고-처음으로-돌아가고-싶다) |
| 7 | [급하게 다른 브랜치로 가야 하는데 작업이 덜 끝났다](#7-급하게-다른-브랜치로-가야-하는데-작업이-덜-끝났다) |
| 8 | [push가 거절됐다 (rejected)](#8-push가-거절됐다-rejected) |
| 9 | [PR에 내가 안 만든 커밋이 잔뜩 보인다](#9-pr에-내가-안-만든-커밋이-잔뜩-보인다) |
| 10 | [.env나 API 키를 커밋해 버렸다](#10-env나-api-키를-커밋해-버렸다) |
| 11 | [git commit을 쳤더니 이상한 화면(vim)에 갇혔다](#11-git-commit을-쳤더니-이상한-화면vim에-갇혔다) |
| 🆘 | [그래도 모르겠다면 — 질문하는 법](#-그래도-모르겠다면--질문하는-법) |

---

### 1. 충돌(conflict)이 났다

`git merge develop`이나 `git pull` 후 이런 메시지가 보이는 상황입니다.

```
CONFLICT (content): Merge conflict in src/quiz/grade.ts
Automatic merge failed; fix conflicts and then commit the result.
```

**충돌은 에러가 아닙니다.** 두 사람이 같은 곳을 고쳐서 Git이 "어느 쪽으로 할지 사람이 정해 주세요"라고 묻는 것뿐입니다.

**① 어떤 파일이 충돌했는지 확인**

```bash
git status      # "both modified:" 라고 적힌 파일이 충돌 파일
```

**② 파일을 열면 이런 표시가 있습니다**

```
<<<<<<< HEAD
const PASS_RATE = 50;            ← 내 브랜치의 코드
=======
const PASS_RATE = 0.5;           ← develop에서 들어온 코드
>>>>>>> develop
```

**③ 최종 모습으로 직접 고칩니다**

- 둘 중 하나를 남기거나, 둘을 합친 새 코드로 씁니다.
- `<<<<<<<`, `=======`, `>>>>>>>` **표시 줄은 전부 지워야 합니다.**
- VS Code를 쓰면 충돌 위에 **Accept Current / Accept Incoming / Accept Both** 버튼이 떠서 클릭으로 고를 수 있습니다.
- **어느 쪽이 맞는지 모르겠다면 상대 코드를 쓴 팀원에게 물어보세요.** 마음대로 지우면 남의 작업이 사라집니다.

**④ 저장하고, 실행해서 동작을 확인한 뒤 마무리**

```bash
git add src/quiz/grade.ts     # 해결한 파일을 담기
git commit                    # 에디터에 자동 메시지가 있으면 그대로 저장·닫기
git push
```

**도저히 못 하겠으면 합치기 전으로 되돌리기**

```bash
git merge --abort             # merge 하기 전 상태로 돌아감. 그다음 팀원에게 도움 요청
```

---

### 2. 커밋 메시지를 잘못 썼다

**아직 push 안 했다면** — 마지막 커밋 메시지를 고칩니다.

```bash
git commit --amend -m "feat(quiz): 정답률 계산 함수 추가"
```

**이미 push 했다면** — 그냥 두세요. Squash merge 때 PR 제목이 최종 메시지가 되므로, **PR 제목만 규칙에 맞으면 괜찮습니다.**

---

### 3. 커밋에 파일 하나를 빠뜨렸다

**아직 push 안 했다면**

```bash
git add 빠뜨린파일.ts
git commit --amend --no-edit      # 메시지는 그대로, 직전 커밋에 파일만 추가
```

**이미 push 했다면** — 새 커밋으로 추가하면 됩니다.

```bash
git add 빠뜨린파일.ts
git commit -m "feat(quiz): 누락된 타입 정의 파일 추가"
git push
```

---

### 4. develop에서 작업해 버렸다

**커밋하기 전에 알았다면** — 고친 내용은 그대로 들고 새 브랜치로 옮겨 갑니다.

```bash
git switch -c feat/12-quiz-grading    # 수정 사항이 새 브랜치로 따라옴
```

**develop에 커밋까지 했다면 (push는 안 함)**

```bash
git switch -c feat/12-quiz-grading    # ① 지금 상태 그대로 새 브랜치를 만들어 커밋을 보존
git switch develop                    # ② develop으로 돌아가서
git reset --hard origin/develop       # ③ develop을 GitHub 상태로 되돌림
git switch feat/12-quiz-grading       # ④ 내 브랜치에서 이어서 작업
```

> ⚠️ ③의 `reset --hard`는 되돌릴 수 없는 명령입니다. **반드시 ①을 먼저 하고**, `git log`로 새 브랜치에 내 커밋이 있는지 확인한 뒤 실행하세요. 자신 없으면 팀원과 함께 하세요.

> 💡 develop은 보호되어 있어서 push는 거절됩니다. 그러니 이 실수는 GitHub에 영향을 주지 않아요.

---

### 5. 엉뚱한 파일을 git add 했다

```bash
git restore --staged 잘못담은파일     # 담기만 취소. 파일 내용은 그대로
```

---

### 6. 고친 내용을 버리고 처음으로 돌아가고 싶다

```bash
git restore 파일이름                  # 그 파일을 마지막 커밋 상태로 되돌림
```

> ⚠️ 커밋하지 않은 수정 내용은 **영원히 사라집니다.** 정말 버려도 되는지 한 번 더 확인하세요.

---

### 7. 급하게 다른 브랜치로 가야 하는데 작업이 덜 끝났다

작업 중인 내용을 잠시 **서랍(stash)** 에 넣어 둡니다.

```bash
git stash push -m "퀴즈 채점 작업 중"   # 서랍에 넣기 → 작업 폴더가 깨끗해짐
git switch develop                    # 이제 자유롭게 이동 가능
# … 다른 일 …
git switch feat/12-quiz-grading       # 원래 브랜치로 돌아와서
git stash pop                         # 서랍에서 꺼내기
```

> 💡 서랍에 넣은 걸 잊어버리기 쉽습니다. `git stash list`로 남아 있는 게 없는지 가끔 확인하세요. 간단히 "WIP" 커밋을 하고 나중에 이어서 작업하는 것도 좋은 방법입니다.

---

### 8. push가 거절됐다 (rejected)

```
! [rejected]   feat/12-quiz-grading -> feat/12-quiz-grading (fetch first)
```

GitHub에 내 컴퓨터에 없는 커밋이 있다는 뜻입니다. (다른 컴퓨터에서 작업했거나, GitHub 화면에서 **Update branch** 를 눌렀을 때 등)

```bash
git pull          # GitHub 내용을 먼저 받아 합치고
git push          # 다시 push
```

`develop`이나 `main`에 push 하다가 거절됐다면? → 보호 규칙 때문입니다. 정상이에요. 내 브랜치를 만들어서 PR로 올리세요. ([4번](#4-develop에서-작업해-버렸다) 참고)

> 🚫 **`git push --force`로 해결하지 마세요.** 다른 사람의 커밋을 지울 수 있습니다.
> 내 브랜치에서 `--amend` 후 꼭 필요하다면 **혼자 쓰는 브랜치인 경우에만** 더 안전한 `git push --force-with-lease`를 씁니다.

---

### 9. PR에 내가 안 만든 커밋이 잔뜩 보인다

대부분 **base 브랜치를 잘못 고른 경우**입니다.

1. PR 제목 옆 **Edit** 를 누릅니다.
2. base를 `main` → **`develop`** 으로 바꿉니다.

그래도 이상하면 브랜치를 오래된 develop에서 만든 경우일 수 있습니다. [01. 작업 흐름 4단계](01-workflow.md#4단계-작업-중에-develop이-바뀌었다면-반영하기)대로 최신 develop을 합쳐 주세요.

---

### 10. .env나 API 키를 커밋해 버렸다

**push 전이라면**

```bash
git restore --staged .env             # 아직 커밋 전이면 이것으로 끝
# 이미 커밋했다면:
git reset --soft HEAD~1               # 직전 커밋을 취소 (수정 내용은 남음)
git restore --staged .env
```

그리고 `.gitignore`에 `.env`가 있는지 확인합니다.

**이미 push 했다면 — 아래 순서를 꼭 지키세요**

1. 🚨 **즉시 팀장(저장소 관리자)에게 알립니다.** 숨기지 마세요. 누구나 할 수 있는 실수입니다.
2. 🔑 **그 키를 발급한 서비스에 가서 키를 폐기(revoke)하고 새로 발급합니다.** ← 가장 중요!
   커밋을 지워도 이미 누군가 복사해 갔을 수 있으므로, **키 자체를 무효화하는 것만이 확실한 해결책**입니다.
3. 저장소에서 파일을 빼고 다시 올라가지 않게 막습니다.

   ```bash
   git rm --cached .env                    # GitHub에서만 지우고 내 컴퓨터 파일은 유지
   echo ".env" >> .gitignore
   git add .gitignore
   git commit -m "chore(config): .env 파일 추적 해제"
   git push
   ```

4. 과거 기록에서 완전히 지우는 작업은 관리자가 판단해서 진행합니다.

> 💡 팀원에게 필요한 환경 변수는 **`.env.example`** 파일에 **이름만** 적어서 공유합니다. 예: `DATABASE_URL=` (값은 비워 둠)

---

### 11. git commit을 쳤더니 이상한 화면(vim)에 갇혔다

1. `i` 를 눌러 입력 모드로 바꾸고 메시지를 씁니다.
2. `Esc` 를 누르고 `:wq` 를 입력한 뒤 `Enter` → 저장하고 나가기
3. 커밋을 취소하고 나가고 싶다면 `Esc` → `:q!` → `Enter`

다시는 갇히지 않도록 에디터를 VS Code로 바꿔 두세요.

```bash
git config --global core.editor "code --wait"
```

---

## 🆘 그래도 모르겠다면 — 질문하는 법

**혼자 30분 이상 붙잡지 말고 바로 질문하세요.** 아래 4가지를 함께 보내 주면 훨씬 빨리 해결됩니다.

```
1. 하려던 것:   develop 최신 내용을 내 브랜치에 합치려고 했어요
2. 입력한 명령: git merge develop
3. 나온 메시지: (에러 메시지 전체를 복사해서 붙여넣기 — 캡처보다 텍스트가 좋아요)
4. 지금 상태:   (git status 결과 붙여넣기)
```

> 💬 "Git 잘 모르는데 괜찮을까요?"라는 걱정은 하지 않아도 됩니다. 모두 처음이 있었고, 질문하는 사람이 팀을 더 안전하게 만듭니다.
