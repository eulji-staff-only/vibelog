package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.entity.ReportTargetType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/* MessageReader 신고 대상 상수가 ReportTargetType 이름과 하나도 빠짐없이 같다 (#484). 다르면 숨긴 콘텐츠가 조용히 다시 보인다 */
class ReportTargetConstantsTest {

    @Test
    void 신고_대상_상수는_ReportTargetType_이름과_같다() {
        assertThat(new String[]{MessageReader.TARGET_POST, MessageReader.TARGET_COMMENT, MessageReader.TARGET_REVIEW,
                MessageReader.TARGET_PORTFOLIO, MessageReader.TARGET_MESSAGE})
                .containsExactlyInAnyOrder(Arrays.stream(ReportTargetType.values()).map(Enum::name).toArray(String[]::new));
    }
}
