# 바이브로그 — 팀 공용 Claude 규칙

이 파일은 팀원 전원이 같이 쓰는 규칙입니다. 수정은 이슈 → PR → **전원 승인**으로만 합니다.
나만 쓰는 설정은 `CLAUDE.local.md`, `.claude/settings.local.json`에 둡니다 (커밋되지 않음).

## 프로젝트

- 바이브코딩 학습 플랫폼 **바이브로그**. 팀 프로젝트이고, 사용자(팀원)는 Spring을 배우는 중인 부트캠프 수강생이다.
- 기술: Java 21, Spring Boot 3.5, Spring Data JPA, Thymeleaf, Validation, Lombok, MySQL 8.0, Gradle
- 답변은 **한국어**로, 쉬운 말로 한다. 사용자가 리뷰어에게 설명할 수 있도록 **왜 그렇게 짰는지**도 짧게 알려 준다.

## 먼저 읽을 문서

| 알고 싶은 것 | 문서 |
|---|---|
| 기능 요구사항 (`FR-USR-001` 같은 ID) | `docs/planning/06-requirements.md` |
| 정책 값 (`PL-USR-001` 같은 ID) | `docs/planning/07-policy.md` |
| 코드 구조 · 예외 · DB · 리뷰어 규칙 | `README.md`의 "🧱 코드 구조 규칙" |
| 브랜치 · 커밋 · PR 규칙 | `docs/collaboration/02-branch.md`, `03-commit.md`, `05-pull-request.md` |

- 요구사항·정책 ID가 나오면 **해당 문서를 찾아 읽고** 그대로 구현한다. 문서에 없는 규칙(숫자, 문구, 동작)은 지어내지 말고 사용자에게 묻는다.
- **PL-CST-001: 서비스 기능에서 사용량 과금 AI API를 호출하지 않는다.** 채점·리포트·랭킹은 규칙 기반으로 만든다.

## 작업 범위

- **사용자가 말한 이슈 범위 안에서만** 수정한다. 관련 없는 파일 수정, 프로젝트 전체 포맷, 대규모 리팩터링은 하지 않는다.
- 필요해 보이는 추가 작업은 직접 하지 말고 "별도 이슈로 만들자"고 제안한다.
- 다음 파일은 그 일을 하는 이슈가 아니면 고치지 않는다. 고쳐야 하면 먼저 사용자에게 알린다.
  - `build.gradle`, `application.yaml`, `.github/`, `global/` → 형상관리 담당 리뷰 필요
  - `sql/` → DB 담당 리뷰 필요
  - `CLAUDE.md`, `.claude/settings.json` → 전원 승인 필요
- **의존성을 마음대로 추가하지 않는다.** 꼭 필요하면 이유와 함께 사용자에게 먼저 묻는다.

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
│       ├── policy/                  ← 여러 데이터를 봐야 판단하는 비즈니스 규칙
│       └── service/                 ← @Transactional, 흐름 조립
└── global/                          ← 공통 (config, entity, exception)

templates/<영역>/*.html               ← 화면도 영역별 폴더
```

- 다른 영역의 코드가 필요하면 그 영역의 **service를 호출**한다. 다른 영역의 dao·entity 내부를 직접 고치지 않는다.

## 예외 처리

- 규칙 위반은 `throw new BusinessException(QuizErrorCode.XXX)`로 **던지기만** 한다. Controller·Service에서 `try-catch`로 에러 화면을 만들지 않는다. 화면은 `GlobalExceptionHandler`가 만든다.
- 에러 목록은 **내 영역의 enum**에 추가한다. 다른 영역의 enum이나 `global/exception`에 추가하지 않는다.
- 오류 문구는 PL-ERR-001을 따른다: 원인과 다음 행동을 알리고, 기술 용어·서버 내부 정보는 보여 주지 않는다.

## DB · Entity

- `ddl-auto`는 `none`이다. **Entity로 테이블을 만들거나 바꾸지 않는다.** 테이블은 `sql/01-schema.sql`이 기준이다.
- Entity 필드는 `sql/01-schema.sql`의 컬럼 이름·타입·NULL 여부와 맞춘다. 맞지 않으면 Entity를 고치지 말고 사용자에게 알린다 (테이블 변경은 📐 요구사항·정책 변경 요청 이슈로 DB 팀과 합의).
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
- 커밋 전에 `git status`와 `git diff`로 **이슈와 상관없는 변경이 없는지** 사용자와 함께 확인한다.

## 완료 전 확인

- 코드를 고쳤으면 `./gradlew build`(테스트 포함)를 실행하고 결과를 그대로 알린다. 실패를 숨기거나 "아마 될 것"이라고 하지 않는다.
- 테스트는 로컬 MySQL이 켜져 있어야 통과한다. DB 연결 오류면 코드 문제가 아니라 README 최초 세팅 9단계(DB 만들기)를 안내한다.
- 비즈니스 규칙(정책 값, 분기)을 새로 만들었으면 그 규칙이 깨지면 실패하는 테스트를 하나 이상 남긴다.
