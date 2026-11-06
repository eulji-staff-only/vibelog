# 바이브로그 — 팀 공용 Claude 규칙

이 파일은 팀원 전원이 같이 쓰는 규칙입니다. 수정은 이슈 → PR로만 하고, 승인 조건은 다른 PR과 같습니다 (아래 Git "리뷰·승인").
나만 쓰는 설정은 `CLAUDE.local.md`, `.claude/settings.local.json`에 둡니다 (커밋되지 않음).

## 프로젝트

- 바이브코딩 학습 플랫폼 **바이브로그**. 팀 프로젝트이고, 사용자(팀원)는 Spring을 배우는 중인 부트캠프 수강생이다.
- 기술: Java 21, Spring Boot 3.5, Spring Data JPA, Thymeleaf, Validation, Lombok, MySQL 8.0, Gradle
- 답변은 **한국어**로, 쉬운 말로 한다. 사용자가 리뷰어에게 설명할 수 있도록 **왜 그렇게 짰는지**도 짧게 알려 준다.

## 작업 시작 전 확인 (항상)

어떤 작업이든 **코드를 읽거나 고치기 전에 저장소 상황부터 확인**하고, 결과를 3~5줄로 먼저 알려 준다. (읽기 전용 명령만 쓴다)

1. **로컬 상태** — `git fetch origin` 후 `git status`, `git log --oneline HEAD..origin/develop`
   - 현재 브랜치, 커밋 안 한 변경, 최신 `develop`보다 뒤처졌는지
2. **GitHub 상황** (`gh`가 있으면)
   - `gh issue view <이슈번호>` — 맡은 이슈가 열려 있는지, 담당자·마일스톤·`⛔ blocked` 라벨
   - `gh pr list --state open` — 같은 이슈·영역·파일을 다루는 열린 PR이 있는지
   - `gh api repos/{owner}/{repo}/milestones` — 지금 어느 단계(① DB 설계 / ② API 명세 / ③ P1 구현)인지
3. **문제가 보이면 작업 전에 알린다**
   - develop보다 뒤처짐 → `git pull origin develop` 먼저 제안
   - 이슈가 닫혔거나, 다른 사람이 담당이거나, 같은 작업의 PR이 이미 있음 → 진행 여부를 묻는다
   - 지금 단계가 아닌 작업 (예: ② API 명세 확정 전에 기능 구현) → 08 문서의 단계를 알려 주고, 구현을 시작하지 않는다
   - **예외: 화면 선행 작업** (`🎨 design` 라벨 이슈) → ①·② 단계에도 진행한다. 아래 "화면 선행 작업" 범위 안에서만 만든다
- `gh`가 없거나 로그인 안 되어 있으면 1번만 하고, `gh auth login`을 안내한다. 이슈 번호를 모르면 사용자에게 묻는다.

## 먼저 읽을 문서

| 알고 싶은 것 | 문서 |
|---|---|
| 기능 요구사항 (`FR-USR-001` 같은 ID) | `docs/planning/06-requirements.md` |
| 정책 값 (`PL-USR-001` 같은 ID) | `docs/planning/07-policy.md` |
| 코드 구조 · 예외 · DB · 리뷰어 규칙 | `README.md`의 "🧱 코드 구조 규칙" |
| 브랜치 · 커밋 · PR 규칙 | `docs/collaboration/02-branch.md`, `03-commit.md`, `05-pull-request.md` |
| 작업 순서 · 영역 간 계약 · 안 멈추는 규칙 | `docs/collaboration/08-task-flow.md` |
| API 명세 (URL · 요청 · 응답 · 영역 간 계약 메서드) | `docs/design/api/` (양식은 #21에서 확정) |
| 화면 URL · 화면 구성 · 디자인 토큰 (`U-12` 같은 화면 ID) | `docs/design/01-screen-flow.md` (화면 흐름 설계서, 프로토타입 링크 포함) |

- 요구사항·정책 ID가 나오면 **해당 문서를 찾아 읽고** 그대로 구현한다. 문서에 없는 규칙(숫자, 문구, 동작)은 지어내지 말고 사용자에게 묻는다.
- **화면 URL**(브라우저 주소창에 보이는 주소)은 화면 흐름 설계서에 있는 그대로 쓴다.
- **데이터를 주고받는 URL(폼 전송 등), 요청 값, 응답, 계약 메서드는 확정된 API 명세에 있는 그대로만** 만든다. 명세에 없거나 명세가 아직 없으면 지어내지 않는다. (예외: 다른 영역의 `<영역>Reader`에 읽기 전용 메서드를 추가하는 것은 명세 없이 한다)
- **PL-CST-001: 서비스 기능에서 사용량 과금 AI API를 호출하지 않는다.** 채점·리포트·랭킹은 규칙 기반으로 만든다.

## 작업 범위

- **사용자가 말한 이슈 범위 안에서만** 수정한다. 관련 없는 파일 수정, 프로젝트 전체 포맷, 대규모 리팩터링은 하지 않는다.
- 필요해 보이는 추가 작업은 직접 하지 말고 "별도 이슈로 만들자"고 제안한다.
- 다음 파일은 그 일을 하는 이슈가 아니면 고치지 않는다. 고쳐야 하면 멈추고 **A. 다른 팀 요청서**를 만든다.
  - `build.gradle`, `application.yaml`, `.github/`, `global/` → 형상관리 담당
  - `sql/` → DB 팀 담당
  - `CLAUDE.md`, `.claude/` → 팀 공용 규칙
- **의존성을 마음대로 추가하지 않는다.** 꼭 필요하면 이유와 함께 사용자에게 먼저 묻는다.

## 다른 영역을 기다리지 않는 방법

다른 영역의 기능·데이터가 아직 없는 것은 **멈출 이유가 아니다.** `docs/collaboration/08-task-flow.md`의 "🚦 안 멈추는 규칙"대로 계속 진행한다.

1. **다른 영역 기능이 필요함** → 08 문서 계약 표(①~⑫)에 있으면 받는 쪽 `model/policy/`의 계약 인터페이스를 주입해 쓴다. 아직 없으면 받는 쪽 API 명세의 "영역 간 계약" 그대로 인터페이스와 `Fake<이름>`(`@Component`)을 받는 쪽 `model/policy/`에 추가해 진행한다. 표에 없는 읽기 전용 값(닉네임·코스명·제목·목록)은 받는 쪽 `<영역>Reader`를 주입하고, 메서드가 없으면 Reader에 추가한다 (목록용은 ID 묶음을 받는다). (계약 인터페이스·Fake·Reader 메서드 추가는 다른 영역 수정 금지의 예외) 표에 없는데 다른 영역 테이블을 바꿔야 하면 **A. 다른 팀 요청서**로 계약 추가를 요청한다.
2. **코스·수업·퀴즈 같은 데이터가 필요함** → 운영자 기능을 기다리지 않고 `sql/02-seed.sql` 샘플 데이터를 쓴다. 부족하면 DB 팀에 A. 요청서
3. **로그인 회원이 필요함** → Service는 `memberId`를 파라미터로 받고, 로그인 회원 꺼내기는 Controller에서만 한다
4. **공용 자원(`sql/`, 계약 인터페이스, Reader, `global/`, 샘플 데이터)** 은 추가만 한다. 기존 것의 이름·타입·동작 변경, 삭제는 **B. 팀 상의 안건**
5. 작업 시작 전에 사용자가 맡은 작업이 08 문서의 몇 단계·어느 계약과 관련 있는지 먼저 알려 준다

## 멈추고 사람에게 넘기는 지점

아래 상황에 닿으면 **작업을 더 진행하지 않는다.** 임시 코드·추측으로 우회하지 않고, 지금까지 한 일을 정리한 뒤 아래 두 형식 중 맞는 것을 만들어 사용자에게 준다.

| 상황 | 만들 것 |
|---|---|
| 다른 사람의 담당 범위를 바꿔야 함 — 다른 영역 코드(계약 인터페이스·Fake·Reader 메서드 추가 제외), `sql/`(DB 팀), `build.gradle`·`global/`(형상관리) | **A. 다른 팀 요청서** |
| 위 "기다리지 않는 방법"으로도 진행할 수 없음 (예: #10 전에 DB 테스트) | 할 수 있는 다른 작업 제안 + 이슈에 `⛔ blocked` 라벨 안내 |
| 요구사항·정책 문서에 답이 없거나, 문서끼리 내용이 다름 | **B. 팀 상의 안건** |
| API 명세에 없는 데이터 URL(폼 전송 등)·값이 필요하거나, 명세와 다르게 만들어야 함 (화면 URL은 화면 흐름 설계서 기준) | **B. 팀 상의 안건** (명세 수정이 먼저) |
| 테이블 구조 변경, 의존성 추가, 보안·권한 방식 결정 | **B. 팀 상의 안건** |
| 머지 충돌이 다른 사람의 코드에서 남 | **B. 팀 상의 안건** |
| 승인·머지·배포처럼 규칙상 사람이 해야 하는 일 | 할 일만 한 줄로 안내 |

- 담당자는 README의 영역 담당 표와 "리뷰어 지정 약속" 표에서 찾는다. 표에 없으면 "담당자 확인 필요"라고 쓴다.
- 두 형식 모두 **GitHub 이슈나 팀 채팅에 그대로 붙여넣을 수 있게** 마크다운 블록 하나로 준다. 이슈를 직접 만들거나 메시지를 보내는 것은 사용자가 요청할 때만 한다.

### A. 다른 팀 요청서

```markdown
## [요청] <한 줄 요약>

- **받는 사람:** <영역/팀> 담당 (<이름 또는 "담당자 확인 필요">)
- **보내는 사람:** <내 영역>, 관련 이슈 #<번호>
- **급한 정도:** <이게 없으면 막히는 작업 / 언제까지 필요한지>

### 왜 필요한가요
<내가 하려는 기능과 요구사항·정책 ID, 막힌 지점>

### 요청 내용
- <구체적으로 무엇을: 메서드 이름·입력·출력, 컬럼 이름·타입, 화면 등>

### 제안 (참고용)
<내가 생각한 방법. 결정은 받는 사람이 한다>

### 받는 분의 Claude에 붙여넣을 프롬프트
> <이슈 번호>번 이슈 작업이다. <받는 쪽 파일/패키지>에 <요청 내용>을 구현해 줘.
> 근거: <요구사항·정책 ID>. 요청한 쪽에서 <이런 형태로> 사용할 예정이다.
> 이슈 범위 밖은 고치지 말고, 끝나면 `./gradlew build` 결과를 알려 줘.
```

- 이슈로 올릴 때 양식: 테이블·정책 변경이면 **📐 요구사항 · 정책 변경 요청**, 그 외에는 **🔧 작업**

### B. 팀 상의 안건

```markdown
## [상의] <결정할 것 한 줄>

- **관련:** 이슈 #<번호>, <요구사항·정책 ID>
- **결정할 사람:** <전원 / 영역 담당 / DB 팀 / 형상관리>
- **멈춘 작업:** <결정 전까지 못 하는 것, 그동안 할 수 있는 것>

### 상황
<무엇을 하다가, 어떤 문서·코드에서 무엇을 발견했는지. 문서 문장을 그대로 인용>

### 선택지
| | 방법 | 장점 | 단점 · 영향 범위 |
|---|---|---|---|
| A | | | |
| B | | | |

### Claude 추천
<추천과 이유 한두 줄. 사람이 정할 일이라는 것을 분명히 한다>

### 결정 후 할 일
- [ ] <문서 수정 / sql 수정 / 코드 수정 등, 누가>
```

- 이슈로 올릴 때는 **🔧 작업** 양식에 `❓ question` 라벨

## 코드 구조

```
com.vibelog
├── VibelogApplication.java          ← 옮기지 않는다
├── domain/<영역>/                    ← 영역 이름 = 커밋 영역 (auth, course, lesson, quiz, portfolio, exam, community, contest, message, admin)
│   ├── controller/                  ← 요청을 받고 화면 이름을 돌려준다. 정상 흐름만 쓴다
│   ├── exception/<영역>ErrorCode     ← ErrorCode 를 구현한 enum
│   └── model/
│       ├── dao/                     ← Repository (JpaRepository 상속)
│       ├── dto/                     ← 입력·출력 값 + @Valid 검증
│       ├── entity/                  ← BaseTimeEntity 상속 + 자기 상태로 판단하는 도메인 규칙
│       ├── policy/                  ← 여러 데이터를 봐야 판단하는 비즈니스 규칙 · 다른 영역에 주는 계약 인터페이스
│       └── service/                 ← @Transactional, 흐름 조립 · 다른 영역에 주는 `<영역>Reader`(읽기 전용)
└── global/                          ← 공통 (config, controller, entity, exception)
    └── controller/MainController    ← 메인 화면 `/` (어느 영역에도 속하지 않는 화면, #13)

templates/<영역>/*.html               ← 화면도 영역별 폴더
templates/common/                    ← 공통 레이아웃·UI 조각 (th:fragment, #13·#46)
templates/error/error.html           ← 오류 화면 한 장. GlobalExceptionHandler가 모든 오류를 여기로 보내므로 `status`(404·403·500)로 나눠 그린다
static/css/<영역>.css · static/js/    ← CSS·JS는 파일로 분리 (공통은 #13·#46이 만든 것을 쓴다)
```

- 다른 영역의 **Entity·Repository를 import 하지 않는다.** 다른 영역 데이터는 ID(`Long`)로 참조하고, 필요한 값은 계약 인터페이스나 받는 쪽 `<영역>Reader`로 받는다 (기본형·DTO로 주고받음).

## 화면 선행 작업 (`🎨 design` 이슈, ①·② 단계에도 진행)

DB 설계·API 명세가 확정되기 전에도, 그 문서가 바뀌어도 다시 짤 필요가 없는 화면 부분은 만든다. 기준은 화면 흐름 설계서다.

- ⭕ 만든다: `templates/<영역>/*.html` 마크업, `static/css/<영역>.css`, 설계서의 화면 URL로 `@GetMapping` 하고 **화면 이름만 돌려주는** Controller, 샘플 문구를 직접 적은 화면, 화면 사이 링크, 화면 안에서만 도는 JS(탭 전환·모달·토스트)
- ❌ 만들지 않는다 (③ 단계에서 명세대로): `th:text`·`th:each` 데이터 연결, `th:action`·입력 `name=`, POST Controller, Service·Entity·Repository·DTO, 서버를 부르는 JS, 로그인 상태별 분기, 백오피스(`/admin`) 화면
- 색·글꼴·간격은 설계서 6장 디자인 토큰을 쓰고, 템플릿 안에 `<style>`을 쓰지 않는다. 공통 조각(버튼·배지·안내 상자·모달·토스트)이 있으면 새로 만들지 않고 가져다 쓴다.
- 화면에 로그인·권한·학습 조건(잠김 등) 검사를 넣지 않는다. 잠김·미인증 같은 상태는 샘플 화면으로 보여 준다. 로그인 여부(회원·운영자)는 `SecurityConfig`(#12)가 막으므로, 회원 화면은 `sql/02-seed.sql` 테스트 계정으로 로그인해서 확인한다. 학습 조건 검사는 ③ 단계 기능 이슈에서 붙인다.

## 클래스 작성 방식

강의 실습(Spring Security 세션 모듈)의 방식 중 우리 규칙과 맞는 것을 따른다.

- **의존성 주입:** `src/main` 클래스(Controller·Service·Component)는 `@RequiredArgsConstructor` + `private final` 필드로 받는다 (생성자 주입). 테스트 클래스는 `@Autowired`를 써도 된다.
- **Entity 만들기:** `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. 새 Entity는 정적 팩토리 `Xxx.create(...)`로 만든다 (안쪽은 private 생성자나 `@Builder(access = AccessLevel.PRIVATE)` 중 편한 것). id(DB가 채움)와 `created_at`·`updated_at`(BaseTimeEntity가 채움)은 받지 않는다.
- **상태 바꾸기:** Entity의 도메인 메서드(예: 수업 공개 `publish()`)로 바꾸고, `@Transactional` Service에서 호출한다. 변경 감지(dirty checking)로 저장되므로 `save()`를 다시 부르지 않는다.
- **Entity ↔ DTO:** Entity → DTO는 DTO의 `static Xxx from(Entity)`, DTO → Entity는 `Entity.create(...)`. ModelMapper 같은 변환 라이브러리는 쓰지 않는다.
- **Repository:** 메서드 이름 쿼리(`findByEmail`, `existsByEmail`)를 먼저 쓴다. 없을 수도 있는 1건 조회는 `Optional`로 받고, 없으면 Service에서 `BusinessException`을 던진다.
- **Controller:** 화면만 보여주는 GET은 화면 이름(`String`)을 돌려준다. 로그인 회원은 Controller에서만 `@AuthenticationPrincipal LoginMember loginMember`로 꺼내고, Service에는 `loginMember.getMemberId()`만 넘긴다 (README "로그인 약속"). 로그인·운영자 여부는 `SecurityConfig`가 막으므로 직접 검사하지 않는다.
- **Controller 이름:** 자기 영역 이름으로 시작한다 (예: `LessonPageController`, `ExamController`). 패키지가 달라도 클래스 이름이 같으면 Spring Bean 이름이 겹쳐 앱이 뜨지 않는다 (`ConflictingBeanDefinitionException`).
- **주석:** 클래스 맨 위에 `global/` 클래스와 같은 형식으로 쓴다 — `/* [클래스명] 역할 : … 책임 : … */`.
- **강의 코드에서 따라 하지 않는 것:**
  - 실패를 `null`·`0` 반환이나 `try-catch`로 처리 → `BusinessException`을 던진다
  - `System.out.println`, `e.printStackTrace()` → 필요하면 `@Slf4j` 로그
  - 실행 클래스를 `global/config`로 옮기고 `@ComponentScan`·`@EnableJpaRepositories`로 스캔 범위 지정 → 실행 클래스는 루트에 두므로 필요 없다
  - 템플릿 안 `<style>` → `static/css`
  - 테이블·컬럼 이름 직접 짓기(`TBL_USER`), `Integer` id → `sql/01-schema.sql` 이름 그대로, id는 `Long`
  - 로그인 실패 시 어느 값이 틀렸는지 알려 주기 → PL-USR-007 문구 하나만
  - CSRF 끄기 → 켜 둔다. POST 폼은 `th:action`으로 만들면 토큰이 자동으로 들어간다
  - Remember Me 키를 코드에 적기 → 키는 환경 변수

## 예외 처리

- 규칙 위반은 `throw new BusinessException(QuizErrorCode.XXX)`로 **던지기만** 한다. Controller·Service에서 `try-catch`로 에러 화면을 만들지 않는다. 화면은 `GlobalExceptionHandler`가 만든다.
- 에러 목록은 **내 영역의 enum**에 추가한다. 다른 영역의 enum이나 `global/exception`에 추가하지 않는다.
- 오류 문구는 PL-ERR-001을 따른다: 원인과 다음 행동을 알리고, 기술 용어·서버 내부 정보는 보여 주지 않는다.

## DB · Entity

- `ddl-auto`는 `none`이다. **Entity로 테이블을 만들거나 바꾸지 않는다.** 테이블은 `sql/01-schema.sql`이 기준이다.
- Entity 필드는 `sql/01-schema.sql`의 컬럼 이름·타입·NULL 여부와 맞춘다. 맞지 않으면 Entity를 고치지 말고 멈춘 뒤 **B. 팀 상의 안건**을 만든다 (테이블 변경은 DB 팀과 합의, 이슈는 📐 요구사항 · 정책 변경 요청 양식).
- 모든 Entity는 `BaseTimeEntity`를 상속하고, `created_at`·`updated_at` 필드를 따로 만들지 않는다.
- Entity에 `@Setter`를 붙이지 않는다. 상태 변경은 의미 있는 이름의 메서드로 한다 (예: `stopSale()`).

## 보안

- 비밀번호, API 키, 토큰을 코드·설정 파일·테스트에 적지 않는다. 환경 변수로 받는다.
- `.env`를 읽거나 내용을 출력하지 않는다.
- 사용자 입력은 서버에서 검증한다 (NFR-SEC-004). 권한·소유권은 서버에서 검사한다 (NFR-SEC-003).

## Git

- **사용자가 요청할 때만** 커밋·push 한다.
- `develop`·`main`에 직접 커밋·push 하지 않는다. force push 하지 않는다.
- 브랜치: `<타입>/<이슈번호>-<짧은-영어-설명>` (예: `feat/12-quiz-grading`). 항상 최신 `develop`에서 만든다.
- 커밋 메시지: `<타입>(<영역>): <한국어 제목>` (예: `feat(quiz): 정답률 계산 함수 추가`). 제목 끝에 마침표 없음. 자세한 규칙은 `docs/collaboration/03-commit.md`
- PR은 `develop`으로, `.github/pull_request_template.md` 양식을 채운다. **"🤖 AI 활용" 칸에 Claude로 만든 부분을 적는다.**
- **리뷰·승인** (GitHub이 강제, 파일 종류와 상관없이 모든 PR이 같다. `CLAUDE.md`·`.claude/` 수정도 같다):
  - 리뷰어는 자동 지정된다. Claude가 리뷰어를 고르거나 "전원 승인"을 요구하지 않는다.
  - 머지 조건: **PM(`dyryoo316`)·형상관리(`SeungBinYang`) 중 1명 승인** + `build` ✅ + 리뷰 대화 해결
  - **영역 간 연동:** 다른 묶음 담당 영역의 파일(`domain/<영역>/`, `templates/<영역>/`, `static/*/<영역>.*`, 예: 상대 영역 `Fake`)이나 `sql/`(DB 팀이 아닐 때)을 바꾸면, 그 담당이 리뷰어로 추가되고 **그 담당 승인까지** 받아야 한다 (`area-review` ✅). PR 본문 "🔍 리뷰 포인트"에 왜 그 영역을 바꿨는지 적는다.
  - 자세한 표: README "리뷰어 지정 약속"
- 커밋 전에 `git status`와 `git diff`로 **이슈와 상관없는 변경이 없는지** 사용자와 함께 확인한다.

## develop 변경 알리기 (pull 시점)

- **받는 쪽:** `.claude/hooks/check-develop.sh` 훅이 Claude Code를 켤 때와 작업 중(10분마다)에 `origin/develop`을 확인한다. `[develop 변경 알림]` 컨텍스트가 오면 답변 처음에 한두 줄로 알리고 pull을 권한다. pull은 사용자가 원할 때만 하고, 커밋 안 한 변경이 있으면 먼저 커밋하게 한다. 알림에 적힌 "pull 후 할 일"(SQL 다시 실행, Gradle 새로고침, Claude 재시작 등)도 함께 안내한다.
- **PR 올리기 직전:** 훅 알림과 상관없이 `git fetch origin` 후 develop보다 뒤처졌으면 `git pull origin develop`으로 반영하고 빌드한 뒤 올린다.
- **보내는 쪽:** 내 PR이 아래 공유 파일을 바꾸면 PR 본문 "💬 기타"에 **📢 머지 후 팀 공지** 블록(팀 채팅에 그대로 붙여넣을 문구)을 넣고, 사용자에게 "머지되면 팀 채팅에 붙여넣기"를 안내한다.

| 바뀐 파일 | 공지에 적을 할 일 |
|---|---|
| `sql/` | develop pull 후 `sql/01-schema.sql`·`02-seed.sql` 다시 실행 (로컬 데이터 초기화) |
| `build.gradle`, `settings.gradle` | pull 후 IDE에서 Gradle 새로고침 |
| `application.yaml` | pull 후 앱 재시작 |
| `CLAUDE.md`, `.claude/` | pull 후 Claude Code 다시 시작 |
| `global/`, `templates/common/`, `static/`, `model/policy/` 계약, `Fake*`, `*Reader` | 공통 코드 변경 — 작업 중인 브랜치에 바로 `git pull origin develop` |
| `docs/planning/`, `docs/design/` | 요구사항·정책·명세·화면 설계 변경 — 관련 이슈 확인 |

```markdown
📢 develop 변경 공지 — #<PR 번호> <제목>
- 바뀐 것: <공유 파일과 한 줄 요약>
- 할 일: `git pull origin develop` 후 <위 표의 할 일>
```

## 완료 전 확인

- 코드를 고쳤으면 `./gradlew build`(테스트 포함)를 실행하고 결과를 그대로 알린다. 실패를 숨기거나 "아마 될 것"이라고 하지 않는다.
- 테스트는 로컬 MySQL이 켜져 있어야 통과한다. DB 연결 오류면 코드 문제가 아니라 README 최초 세팅 9단계(DB 만들기)를 안내한다.
- 비즈니스 규칙(정책 값, 분기)을 새로 만들었으면 그 규칙이 깨지면 실패하는 테스트를 하나 이상 남긴다.
