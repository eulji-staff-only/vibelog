package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioBrief;
import com.vibelog.domain.portfolio.model.dto.PublicPortfolioCard;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * [PortfolioReader]
 * 역할 : 다른 영역이 포트폴리오 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용. 값은 Entity 가 아니라 기본형·DTO 로 돌려준다.
 *        - 필요한 메서드는 부르는 쪽이 추가만 한다. 목록용은 ID 묶음을 받는다 (규칙 1-5).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioReader {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssignmentRepository portfolioAssignmentRepository;

    /* 회원의 포트폴리오 수 (삭제 표시 제외). 운영자 회원 상세(API-ADM-38, FR-USR-009) */
    public long countOf(Long memberId) {
        return portfolioRepository.countByMemberIdAndDeletedAtIsNull(memberId);
    }

    /* 회원의 공개 포트폴리오 (삭제 표시 제외) 최신순. 공개 프로필 (API-CMT-14, FR-CMT-009) */
    public List<PublicPortfolioCard> findPublicOf(Long memberId) {
        return portfolioRepository.findAllByMemberIdAndIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(memberId)
                .stream()
                .map(PublicPortfolioCard::from)
                .toList();
    }

    /* 포트폴리오 요약 1건 (community 동료 리뷰, #182). 삭제 표시된 것도 돌려주고 deleted 로 알린다. 없으면 빈 값 */
    public Optional<PortfolioBrief> findBrief(Long portfolioId) {
        return Optional.ofNullable(findBriefs(List.of(portfolioId)).get(portfolioId));
    }

    /* 포트폴리오 ID → 요약 (U-31 리뷰 요청 카드). 없는 ID 는 빠진다 */
    public Map<Long, PortfolioBrief> findBriefs(Collection<Long> portfolioIds) {
        List<Portfolio> portfolios = portfolioRepository.findAllById(portfolioIds);
        List<Long> courseIds = portfolios.stream().map(Portfolio::getCourseId).filter(Objects::nonNull).toList();
        Map<Long, Long> assignmentIds = courseIds.isEmpty() ? Map.of() : portfolioAssignmentRepository.findByCourseIdIn(courseIds).stream()
                .collect(Collectors.toMap(PortfolioAssignment::getCourseId, PortfolioAssignment::getPortfolioAssignmentId, (a, b) -> a));
        return portfolios.stream().collect(Collectors.toMap(Portfolio::getPortfolioId, p -> new PortfolioBrief(p.getPortfolioId(),
                p.getMemberId(), p.getTitle(), p.getSummary(), p.getCourseId(),
                p.getCourseId() == null ? null : assignmentIds.get(p.getCourseId()), p.isPublic(), p.isDeleted()), (a, b) -> a));
    }

    /* 회원의 공개 포트폴리오 요약, 최근 것부터 (contest 출품할 작품 고르기, #201) */
    public List<PortfolioBrief> findPublicBriefsOf(Long memberId) {
        List<Long> ids = portfolioRepository.findAllByMemberIdAndDeletedAtIsNull(memberId).stream()
                .filter(Portfolio::isPublic).map(Portfolio::getPortfolioId).toList();
        return findBriefs(ids).values().stream().sorted(Comparator.comparing(PortfolioBrief::portfolioId).reversed()).toList();
    }

    /*
     * 회원의 삭제 표시 없는 포트폴리오 ID (리뷰 요청 동시 3개 제한, PL-REV-002).
     * 그 행들에 쓰기 잠금을 걸어, 같은 회원의 동시 리뷰 요청이 차례로 검사되게 한다. 부르는 쪽 트랜잭션이 끝나면 풀린다.
     */
    @Transactional
    public List<Long> lockLivePortfolioIdsOf(Long memberId) {
        return portfolioRepository.findLiveByMemberIdForUpdate(memberId).stream().map(Portfolio::getPortfolioId).toList();
    }
}
