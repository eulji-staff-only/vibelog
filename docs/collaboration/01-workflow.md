# 01. 작업 흐름 — 이슈를 받고 머지할 때까지

> 이 문서는 **처음부터 끝까지 그대로 따라 하는 행동 지침**입니다.
> 예시는 "12번 이슈: 퀴즈 정답률 계산 기능"을 만드는 상황입니다. 내 이슈 번호와 내용으로 바꿔서 따라 하세요.

[← 협업 규칙 README로 돌아가기](../../README.md)

---

## 전체 순서

| 단계 | 할 일 | 어디서 |
|---|---|---|
| 0 | 이슈 고르기 · 만들기 · 담당자 지정 | GitHub |
| 1 | 최신 `develop` 받기 | 터미널 |
| 2 | 내 브랜치 만들기 | 터미널 |
| 3 | 작업하고 커밋하기 (여러 번 반복) | 터미널 · 에디터 |
| 4 | 작업 중 `develop`이 바뀌었으면 반영하기 | 터미널 |
| 5 | GitHub에 push 하기 | 터미널 |
| 6 | PR 만들기 | GitHub |
| 7 | 리뷰 받고 반영하기 | GitHub · 터미널 |
| 8 | 머지하기 | GitHub |
| 9 | 뒷정리 | 터미널 · GitHub |

---

## 0단계. 이슈 고르기 · 만들기

1. 저장소의 **Projects 보드**에서 `📝 Todo` 칸의 이슈 중 내가 할 일을 고릅니다.
   할 일이 이슈로 없다면 **Issues → New issue**에서 양식을 골라 새로 만듭니다. ([04. 이슈 규칙](04-issue.md))
2. 이슈 오른쪽 사이드바의 **Assignees**에 나를 지정합니다. → "이건 내가 한다"는 표시
3. 보드에서 그 이슈를 `🏃 In Progress` 칸으로 옮깁니다.
4. 이슈 번호를 기억해 두세요. (예: `#12`)

> ✋ **다른 사람이 Assignees에 있는 이슈는 가져가지 않습니다.** 하고 싶으면 이슈에 댓글로 먼저 물어보세요.

---

## 1단계. 최신 develop 받기

내 컴퓨터의 `develop`이 오래됐을 수 있으니, **브랜치를 만들기 직전에 항상** 최신으로 받아옵니다.

```bash
git switch develop          # develop 브랜치로 이동
git pull origin develop     # GitHub의 최신 develop을 받아 합치기
```

> 💡 `git switch develop`에서 "Your local changes would be overwritten…" 에러가 나면, 커밋하지 않은 작업이 남아 있다는 뜻입니다. [06. 문제 해결 → 급하게 다른 브랜치로 가야 할 때](06-troubleshooting.md#7-급하게-다른-브랜치로-가야-하는데-작업이-덜-끝났다)를 보세요.

---

## 2단계. 내 브랜치 만들기

이름 규칙은 `타입/이슈번호-짧은-영어-설명` 입니다. ([02. 브랜치 규칙](02-branch.md))

```bash
git switch -c feat/12-quiz-grading    # 새 브랜치를 만들고 바로 이동
git branch                            # * 표시가 내 브랜치 옆에 있는지 확인
```

---

## 3단계. 작업하고 커밋하기

코드를 작성하다가 **의미 있는 한 덩어리**가 끝날 때마다 커밋합니다. 하루에 한 번 몰아서 커밋하지 말고, 작게 자주 하세요.

```bash
git status                      # ① 무엇이 바뀌었는지 확인
git diff                        # ② 바뀐 내용을 자세히 확인 (q를 누르면 빠져나옴)
git add src/quiz/grade.ts       # ③ 이번 커밋에 넣을 파일만 골라서 담기
git status                      # ④ 초록색 = 담긴 파일. 엉뚱한 파일이 없는지 확인
git commit                      # ⑤ 템플릿이 열리면 첫 줄에 메시지 작성 → 저장 → 창 닫기
```

짧은 메시지는 에디터 없이 바로 쓸 수도 있습니다.

```bash
git commit -m "feat(quiz): 정답률 계산 함수 추가"
```

> ⚠️ `git add .` (전부 담기)는 편하지만, `.env`나 임시 파일까지 담길 수 있습니다. 쓰기 전에 반드시 `git status`로 목록을 확인하세요.

> 🤖 AI로 코드를 만들었다면 이 단계에서 `git diff`를 특히 꼼꼼히 보세요. 부탁하지 않은 파일이 바뀌어 있으면 `git restore <파일>`로 되돌립니다.

커밋 메시지 규칙은 [03. 커밋 규칙](03-commit.md)을 보세요.

---

## 4단계. 작업 중에 develop이 바뀌었다면 반영하기

다른 팀원의 PR이 먼저 머지되면 `develop`이 앞서 나갑니다. 오래 작업할수록 충돌 위험이 커지니, **하루에 한 번** 또는 **PR 올리기 직전에** 최신 `develop`을 내 브랜치에 합쳐 줍니다.

```bash
# 먼저 내 작업을 모두 커밋해 둔 상태여야 합니다.
git switch develop
git pull origin develop               # develop 최신화
git switch feat/12-quiz-grading       # 내 브랜치로 돌아오기
git merge develop                     # 최신 develop을 내 브랜치에 합치기
```

- 에디터가 열리면 메시지를 그대로 두고 저장·닫기 하면 됩니다.
- **충돌(CONFLICT)이 나면** 당황하지 말고 [06. 문제 해결 → 충돌 해결](06-troubleshooting.md#1-충돌conflict이-났다)을 따라 하세요.

> 🚫 우리 팀은 `git rebase`를 쓰지 않고 `git merge develop`으로 통일합니다. rebase는 강제 push가 필요해 초보자에게 사고 위험이 큽니다.

---

## 5단계. GitHub에 push 하기

```bash
git push -u origin feat/12-quiz-grading    # 이 브랜치를 처음 올릴 때 (한 번만)
git push                                   # 두 번째부터는 이것만
```

> 💡 **작업이 덜 끝나도 하루가 끝나면 push 하세요.** 컴퓨터가 고장 나도 작업이 GitHub에 남아 있고, 팀원도 진행 상황을 볼 수 있습니다.

---

## 6단계. PR 만들기

1. GitHub 저장소에 들어가면 노란 배너 **Compare & pull request** 버튼이 보입니다. 클릭하세요.
   (안 보이면 **Pull requests → New pull request**에서 직접 고릅니다.)
2. 위쪽 브랜치 선택이 **`base: develop` ← `compare: feat/12-quiz-grading`** 인지 확인합니다.
   ⚠️ base가 `main`이면 안 됩니다!
3. **제목**을 커밋 규칙대로 씁니다: `feat(quiz): 퀴즈 정답률 계산과 통과 판정 추가`
4. **본문**은 자동으로 채워진 템플릿을 따라 작성합니다. 특히 `Closes #12`의 번호를 내 이슈 번호로 바꾸세요. 머지되면 이슈가 자동으로 닫힙니다.
5. 오른쪽 사이드바를 채웁니다.
   - **Reviewers**: 리뷰해 줄 팀원 1명 이상
   - **Assignees**: 나
   - **Labels**: 이슈와 같은 라벨
   - **Projects**: 팀 보드
6. **Create pull request** 를 누릅니다.
   아직 작업 중이지만 미리 의견을 받고 싶다면 버튼 옆 ▼를 눌러 **Create draft pull request** 를 고르세요.
7. 보드에서 이슈를 `👀 In Review` 칸으로 옮깁니다.

자세한 PR 작성법은 [05. PR · 코드 리뷰 규칙](05-pull-request.md)을 보세요.

---

## 7단계. 리뷰 받고 반영하기

1. 리뷰어가 남긴 코멘트를 읽습니다. 머리말을 보면 무엇이 중요한지 알 수 있습니다.
   - `[필수]` 반드시 고쳐야 머지 가능
   - `[제안]` 고치면 더 좋지만, 이유가 있다면 안 고쳐도 됨
   - `[질문]` 답변 필요
2. **같은 브랜치에서** 코드를 고치고 커밋·push 합니다. PR은 자동으로 업데이트됩니다. 새 PR을 만들 필요 없습니다.

   ```bash
   git add src/quiz/grade.ts
   git commit -m "fix(quiz): 리뷰 반영 - 문제 0개일 때 0으로 나누는 문제 수정"
   git push
   ```

3. 각 코멘트에 **답글**을 남깁니다. 예: `반영했습니다! (a1b2c3d)` 또는 `이런 이유로 그대로 두었습니다 …`
4. 다 반영했으면 리뷰어에게 다시 봐 달라고 요청합니다. (Reviewers 옆 🔄 버튼)

---

## 8단계. 머지하기

아래 조건이 **모두** 충족되면 **PR 작성자인 내가** 머지합니다.

- [ ] Approve 1개 이상
- [ ] `[필수]` 코멘트가 모두 해결됨 (Resolve conversation)
- [ ] "This branch has no conflicts with the base branch" 표시 (충돌 없음)

순서:

1. PR 아래쪽 초록 버튼 옆 ▼를 눌러 **Squash and merge** 가 선택되어 있는지 확인합니다.
2. 버튼을 누르면 커밋 메시지 입력칸이 나옵니다. **첫 줄이 PR 제목과 같은지** 확인합니다. (뒤에 `(#15)`처럼 PR 번호가 붙는 것은 정상입니다.)
3. **Confirm squash and merge** 를 누릅니다.
4. 브랜치는 저장소 설정에 따라 자동 삭제됩니다. **Delete branch** 버튼이 보이면 눌러 주세요.

---

## 9단계. 뒷정리

```bash
git switch develop
git pull origin develop               # 내 작업이 합쳐진 최신 develop 받기
git branch -D feat/12-quiz-grading    # 내 컴퓨터의 브랜치 삭제
git fetch --prune                     # GitHub에서 지워진 브랜치 정보 정리
```

> 💡 왜 `-d`가 아니라 `-D`인가요? Squash merge는 커밋을 새로 하나로 묶기 때문에, Git이 "아직 안 합쳐진 브랜치"라고 착각해 `-d`를 거부합니다. **GitHub에서 머지된 것을 확인했다면** `-D`로 지워도 안전합니다.

마지막으로 GitHub에서 확인합니다.

- 이슈 `#12`가 자동으로 **Closed** 되었는지
- 보드에서 `✅ Done` 칸으로 옮겨졌는지

수고하셨습니다! 다음 이슈로 0단계부터 다시 시작하면 됩니다. 🙌

---

## 📅 하루 루틴 체크리스트

| 언제 | 할 일 |
|---|---|
| **작업 시작할 때** | `git switch develop` → `git pull origin develop` → 내 브랜치로 이동 → `git merge develop` |
| **작업 중** | 작은 단위로 자주 커밋. 커밋 전엔 항상 `git status`, `git diff` |
| **리뷰 요청이 왔을 때** | 24시간 안에 첫 응답 (바로 못 보면 "내일 오전에 볼게요" 댓글이라도) |
| **작업 끝날 때** | 덜 끝났어도 커밋하고 `git push`. 보드 상태가 맞는지 확인 |
| **막혔을 때** | 30분 넘게 혼자 붙잡지 말고 질문 ([질문하는 법](06-troubleshooting.md#-그래도-모르겠다면--질문하는-법)) |
