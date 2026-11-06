# community API 명세 (커뮤니티)

> 영역: community / API ID: API-CMT-01~10
> 공통 규칙(응답 방식·URL·표 읽는 법)은 [_template.md](_template.md) 0장, 에러 코드는 7장입니다.
> 화면은 [화면 흐름 설계서](../01-screen-flow.md), 테이블·상태 값은 [DB 설계서](../02-database.md), [테이블 정의서](../03-table-definition.md)를 봅니다.

| ID | 기능 | Method | URL | 접근 | 요청 값 | 성공 응답 | 실패 | 관련 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-CMT-01 | 게시판 목록 | GET | `/community` | 전체 | query `category`(FREE, QNA, SHOWCASE, 없으면 전체), `q`(제목 검색), `page` | U-21 + 글 목록(카테고리, 제목, 작성자, 작성일, 댓글 수) 최신순, 페이지 정보 | 결과 없음은 빈 목록 | FR-CMT-001, PL-CMT-005 |
| API-CMT-02 | 게시글 상세 | GET | `/community/{postId}` | 전체 | path `postId` | U-22 + 카테고리, 제목, 작성자, 작성일, 본문, 사진, (작품 공유) 작품 링크·사용한 도구, 댓글·답글(삭제된 댓글은 "삭제된 댓글입니다", 수정됨 표시), 내 글 여부 | `POST_NOT_FOUND`(없음·삭제) | FR-CMT-002 |
| API-CMT-03 | 글쓰기 화면 | GET | `/community/write` | 회원 | - | U-23. 미인증이면 안내 상자와 등록 버튼 막음 | - | FR-CMT-003 |
| API-CMT-04 | 글 등록 | POST | `/community` | 인증 | form `category`, `title`, `body`, `workUrl`, `tools`, file `images`(최대 3장) | → `/community/{새 postId}` | `INVALID_INPUT`(제목 1~100자, 본문 1~5,000자, PL-CMT-010), `SHOWCASE_LINK_REQUIRED`(PL-CMT-008), `INVALID_FILE`(3장·5MB·JPG·PNG·WebP, PL-CMT-009, PL-DAT-004), `BANNED_WORD`, `EMAIL_NOT_VERIFIED` → 같은 폼 다시 | FR-CMT-003 |
| API-CMT-05 | 글 수정 화면 | GET | `/community/{postId}/edit` | 회원(작성자) | path `postId` | U-23 + 기존 내용 | `NOT_OWNER`, `POST_NOT_FOUND` | FR-CMT-003 |
| API-CMT-06 | 글 수정 | POST | `/community/{postId}/edit` | 인증(작성자) | path `postId`, form API-CMT-04와 같음 + `keepPostImageIds` | → `/community/{postId}` | API-CMT-04와 같음, `NOT_OWNER` | FR-CMT-003 |
| API-CMT-07 | 글 삭제 | POST | `/community/{postId}/delete` | 회원(작성자) | path `postId` (M-05 확인 후) | 삭제 표시 → `/community` + toast | `NOT_OWNER` | FR-CMT-003, PL-DAT-002 |
| API-CMT-08 | 댓글·답글 쓰기 | POST | `/community/{postId}/comments` | 인증 | path `postId`, form `content`, `parentCommentId`(답글이면) | → `/community/{postId}#comment-{commentId}` | `INVALID_INPUT`(500자, PL-CMT-006), `REPLY_DEPTH_EXCEEDED`(답글에 답글), `BANNED_WORD`, `EMAIL_NOT_VERIFIED` | FR-CMT-004 |
| API-CMT-09 | 댓글 수정 | POST | `/comments/{commentId}/edit` | 인증(작성자) | path `commentId`, form `content` | → `/community/{postId}#comment-{commentId}` | `NOT_OWNER`, `INVALID_INPUT`, `BANNED_WORD` | PL-CMT-006 |
| API-CMT-10 | 댓글 삭제 | POST | `/comments/{commentId}/delete` | 회원(작성자) | path `commentId` | 삭제 표시 → `/community/{postId}` | `NOT_OWNER` | PL-CMT-006 |

- 비회원도 목록과 상세를 본다(PL-USR-008). 작성·댓글은 회원, 그중 이메일 인증을 마친 회원만 한다(PL-USR-003).
- 댓글 알림(FR-CMT-004 "글 작성자에게 알린다")은 알림(FR-NTF-001)이 P2라 P2에서 붙인다.
