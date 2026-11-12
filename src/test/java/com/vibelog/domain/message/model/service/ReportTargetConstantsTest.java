package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.entity.ReportTargetType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/* MessageReader 신고 대상 상수가 ReportTargetType 이름과 하나씩 같고 빠짐도 없다 (#484). 다르면 숨긴 콘텐츠가 조용히 다시 보인다 */
class ReportTargetConstantsTest {

    @Test
    void 신고_대상_상수는_짝이_되는_ReportTargetType_이름과_같다() {
        assertThat(MessageReader.TARGET_POST).isEqualTo(ReportTargetType.POST.name());
        assertThat(MessageReader.TARGET_COMMENT).isEqualTo(ReportTargetType.COMMENT.name());
        assertThat(MessageReader.TARGET_REVIEW).isEqualTo(ReportTargetType.REVIEW.name());
        assertThat(MessageReader.TARGET_PORTFOLIO).isEqualTo(ReportTargetType.PORTFOLIO.name());
        assertThat(MessageReader.TARGET_MESSAGE).isEqualTo(ReportTargetType.MESSAGE.name());
    }

    @Test
    void enum_에_종류가_늘면_상수도_추가해야_한다() {
        assertThat(Arrays.stream(ReportTargetType.values()).map(Enum::name))
                .containsExactlyInAnyOrder(MessageReader.TARGET_POST, MessageReader.TARGET_COMMENT, MessageReader.TARGET_REVIEW,
                        MessageReader.TARGET_PORTFOLIO, MessageReader.TARGET_MESSAGE);
    }
}
