package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.PostImageRepository;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.dto.PostImageView;
import com.vibelog.domain.community.model.dto.PostWriteRequest;
import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostImage;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.file.FileStorage;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 * [PostWriteService]
 * 역할 : 게시글 작성·수정·삭제 (API-CMT-04~07, FR-CMT-003).
 * 책임 : - 작성·수정은 이메일 인증한 회원만 (PL-USR-003, EMAIL_NOT_VERIFIED), 수정·삭제는 작성자만 (NOT_OWNER, NFR-SEC-003).
 *        - 작품 공유 글은 작품 링크가 있어야 하고(SHOWCASE_LINK_REQUIRED, PL-CMT-008), 제목·본문·도구에 금칙어가 없어야 한다 (PL-SEC-003).
 *        - 사진은 남긴 것과 새로 올린 것을 합쳐 3장까지, 형식·크기는 FileStorage 가 검사한다 (PL-CMT-009, INVALID_FILE).
 *        - 삭제는 deleted_at 만 채운다. 글과 댓글은 DB 에 남는다 (PL-DAT-002).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostWriteService {

    private static final int MAX_IMAGES = 3;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final MemberReader memberReader;
    private final BannedWordPolicy bannedWordPolicy;
    private final FileStorage fileStorage;

    public boolean canWrite(Long memberId) {
        return memberReader.isEmailVerified(memberId);
    }

    // ponytail: 파일을 먼저 저장하므로 DB 저장이 실패하면 파일만 남는다. 지우는 기능은 쓰는 곳이 늘면 FileStorage 에 추가한다
    @Transactional
    public Long write(Long memberId, PostWriteRequest request) {
        checkWritable(memberId, request);
        List<MultipartFile> images = request.chosenImages();
        if (images.size() > MAX_IMAGES) {
            throw new BusinessException(CommonErrorCode.INVALID_FILE);
        }
        List<String> imageUrls = fileStorage.saveImages(images);

        Post post = postRepository.save(Post.create(memberId, request.getCategory(), request.getTitle().strip(),
                request.getBody(), strip(request.getWorkUrl()), strip(request.getTools())));
        saveImages(post.getPostId(), imageUrls, 1);
        return post.getPostId();
    }

    /* 수정 화면 (API-CMT-05). 기존 내용과 사진을 채운다 */
    public PostWriteRequest getEditForm(Long postId, Long memberId) {
        Post post = getOwnPost(postId, memberId);
        List<Long> imageIds = imagesOf(postId).stream().map(PostImageView::postImageId).toList();
        return PostWriteRequest.from(post, imageIds);
    }

    /* 수정 폼의 기존 사진. 입력 오류로 폼을 다시 그릴 때도 작성자인지 먼저 본다 (남의 글·삭제된 글 사진을 내주지 않는다) */
    public List<PostImageView> getImages(Long postId, Long memberId) {
        getOwnPost(postId, memberId);
        return imagesOf(postId);
    }

    private List<PostImageView> imagesOf(Long postId) {
        return postImageRepository.findByPostIdOrderBySortOrder(postId).stream().map(PostImageView::from).toList();
    }

    /* API-CMT-06. keepPostImageIds 에 없는 기존 사진은 지운다 */
    @Transactional
    public void update(Long postId, Long memberId, PostWriteRequest request) {
        Post post = getOwnPost(postId, memberId);
        checkWritable(memberId, request);

        List<PostImage> existing = postImageRepository.findByPostIdOrderBySortOrder(postId);
        List<PostImage> kept = existing.stream()
                .filter(image -> request.getKeepPostImageIds().contains(image.getPostImageId()))
                .toList();
        List<MultipartFile> newImages = request.chosenImages();
        if (kept.size() + newImages.size() > MAX_IMAGES) {
            throw new BusinessException(CommonErrorCode.INVALID_FILE);
        }
        List<String> imageUrls = fileStorage.saveImages(newImages);

        postImageRepository.deleteAllInBatch(existing.stream().filter(image -> !kept.contains(image)).toList());
        // 남긴 사진 뒤에 붙인다. 번호 사이가 비어도 순서는 그대로다 (uk_post_image_post_sort 와 겹치지 않게 번호를 다시 매기지 않는다)
        int nextOrder = kept.stream().mapToInt(PostImage::getSortOrder).max().orElse(0) + 1;
        saveImages(postId, imageUrls, nextOrder);

        post.update(request.getCategory(), request.getTitle().strip(), request.getBody(),
                strip(request.getWorkUrl()), strip(request.getTools()));
    }

    /* API-CMT-07 */
    @Transactional
    public void delete(Long postId, Long memberId) {
        getOwnPost(postId, memberId).delete();
    }

    private void checkWritable(Long memberId, PostWriteRequest request) {
        if (!canWrite(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (request.isShowcase() && (request.getWorkUrl() == null || request.getWorkUrl().isBlank())) {
            throw new BusinessException(CommunityErrorCode.SHOWCASE_LINK_REQUIRED);
        }
        bannedWordPolicy.check(request.getTitle(), request.getBody(), request.isShowcase() ? request.getTools() : null);
    }

    private Post getOwnPost(Long postId, Long memberId) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        return post;
    }

    private void saveImages(Long postId, List<String> imageUrls, int firstOrder) {
        for (int i = 0; i < imageUrls.size(); i++) {
            postImageRepository.save(PostImage.create(postId, imageUrls.get(i), firstOrder + i));
        }
    }

    private static String strip(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
