package com.vibelog.domain.course.model.policy;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import com.vibelog.domain.course.model.entity.Track;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 추천 트랙 규칙 (PL-DGN-002): 고른 보기의 트랙 점수 합계가 가장 높은 트랙, 동점이면 트랙 순서가 앞인 트랙.
 * DB 없이 규칙만 본다. Entity 는 만들기 메서드가 없어(관리 기능은 P2) 테스트에서만 값을 넣는다.
 */
class TrackRecommendPolicyTest {

    private final TrackRecommendPolicy policy = new TrackRecommendPolicy();

    // 트랙 순서: Starter(1) → Controller(2) → Builder(3) → Explorer(4). 일부러 섞어서 넘긴다
    private final List<Track> tracks = List.of(track(30L, 3), track(10L, 1), track(40L, 4), track(20L, 2));

    @Test
    void 점수_합계가_가장_높은_트랙을_추천한다() {
        List<DiagnosisOption> chosen = List.of(option(10L, 1), option(20L, 1), option(20L, 1), option(30L, 1));

        assertThat(policy.recommend(chosen, tracks)).isEqualTo(20L);
    }

    @Test
    void 보기마다_점수가_다르면_점수를_더한다() {
        // 고른 개수는 Starter 가 많지만 점수 합계는 Builder 가 높다
        List<DiagnosisOption> chosen = List.of(option(10L, 1), option(10L, 1), option(30L, 3));

        assertThat(policy.recommend(chosen, tracks)).isEqualTo(30L);
    }

    @Test
    void 동점이면_트랙_순서가_앞인_트랙을_추천한다() {
        // Builder 와 Controller 가 2점씩 동점 → 순서가 앞인 Controller
        List<DiagnosisOption> chosen = List.of(option(30L, 1), option(20L, 1), option(30L, 1), option(20L, 1));

        assertThat(policy.recommend(chosen, tracks)).isEqualTo(20L);
    }

    @Test
    void 큰_점수를_여러_개_골라도_합계가_넘치지_않는다() {
        // int 로 더하면 Controller 합계가 음수로 넘쳐 1점인 Starter 가 추천된다 (admin.md 진단 설정 결정 1, #187)
        List<DiagnosisOption> chosen = List.of(option(20L, Integer.MAX_VALUE), option(20L, Integer.MAX_VALUE), option(10L, 1));

        assertThat(policy.recommend(chosen, tracks)).isEqualTo(20L);
        assertThat(policy.sumByTrack(chosen).get(20L)).isEqualTo(2L * Integer.MAX_VALUE);
    }

    @Test
    void 고른_보기가_없으면_추천하지_않는다() {
        assertThat(policy.recommend(List.of(), tracks)).isNull();
    }

    private static Track track(Long trackId, int sortOrder) {
        Track track = instantiate(Track.class);
        ReflectionTestUtils.setField(track, "trackId", trackId);
        ReflectionTestUtils.setField(track, "sortOrder", sortOrder);
        return track;
    }

    private static DiagnosisOption option(Long trackId, int score) {
        DiagnosisOption option = instantiate(DiagnosisOption.class);
        ReflectionTestUtils.setField(option, "trackId", trackId);
        ReflectionTestUtils.setField(option, "score", score);
        return option;
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
