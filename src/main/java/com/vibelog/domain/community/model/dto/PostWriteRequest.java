package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/*
 * [PostWriteRequest]
 * 역할 : 글쓰기·수정 폼 값 (API-CMT-04·06 form category, title, body, workUrl, tools, file images, keepPostImageIds).
 * 책임 : - 길이 규칙(PL-CMT-010)은 여기서 검사한다. 작품 링크 필수(SHOWCASE_LINK_REQUIRED)·사진 수·금칙어·이메일 인증은 PostService 가 검사한다.
 *        - 작품 링크·사용한 도구는 작품 공유 글일 때만 검사한다. 다른 카테고리면 숨겨진 칸이라 저장하지 않는다 (PL-CMT-008).
 */
@Getter
@Setter
public class PostWriteRequest {

    private static final int WORK_URL_MAX = 500;
    private static final int TOOLS_MAX = 200;

    @NotNull(message = "카테고리를 골라 주세요")
    private PostCategory category = PostCategory.FREE;

    @NotBlank(message = "제목을 입력해 주세요")
    @Size(max = 100, message = "제목은 100자까지 쓸 수 있습니다")
    private String title;

    @NotBlank(message = "본문을 입력해 주세요")
    @Size(max = 5000, message = "본문은 5,000자까지 쓸 수 있습니다")
    private String body;

    private String workUrl;

    private String tools;

    private List<MultipartFile> images = new ArrayList<>();

    /* 수정할 때 남길 기존 사진 ID (API-CMT-06) */
    private List<Long> keepPostImageIds = new ArrayList<>();

    @AssertTrue(message = "작품 링크는 http:// 또는 https:// 로 시작하는 500자 이하 주소로 써 주세요")
    public boolean isWorkUrlValid() {
        return !isShowcase() || workUrl == null || workUrl.isBlank()
                || (workUrl.strip().matches("https?://\\S+") && workUrl.strip().length() <= WORK_URL_MAX);
    }

    @AssertTrue(message = "사용한 도구는 200자까지 쓸 수 있습니다")
    public boolean isToolsValid() {
        return !isShowcase() || tools == null || tools.strip().length() <= TOOLS_MAX;
    }

    public boolean isShowcase() {
        return category == PostCategory.SHOWCASE;
    }

    /* 비어 있는 파일 칸(사진을 고르지 않음)은 뺀다 */
    public List<MultipartFile> chosenImages() {
        return images.stream().filter(file -> !file.isEmpty()).toList();
    }

    /* 수정 화면에 기존 내용을 채운다. 기존 사진은 모두 남기는 것으로 시작한다 */
    public static PostWriteRequest from(Post post, List<Long> postImageIds) {
        PostWriteRequest request = new PostWriteRequest();
        request.category = post.getCategory();
        request.title = post.getTitle();
        request.body = post.getBody();
        request.workUrl = post.getWorkUrl();
        request.tools = post.getTools();
        request.keepPostImageIds = new ArrayList<>(postImageIds);
        return request;
    }
}
