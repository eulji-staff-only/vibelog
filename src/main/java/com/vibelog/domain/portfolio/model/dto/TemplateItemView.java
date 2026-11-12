package com.vibelog.domain.portfolio.model.dto;

import com.vibelog.domain.portfolio.model.entity.PortfolioTemplateItem;
import com.vibelog.domain.portfolio.model.entity.TemplateItemKey;

import java.util.Arrays;
import java.util.List;

/*
 * [TemplateItemView]
 * 역할 : 개인 작품 템플릿 항목 하나 (U-34 입력 칸 이름·안내 문구, A-12 목록). itemKey 는 TemplateItemKey 이름이다.
 */
public record TemplateItemView(String itemKey, String label, String guide) {

    public static TemplateItemView from(PortfolioTemplateItem item) {
        return new TemplateItemView(item.getItemKey().name(), item.getLabel(), item.getGuide());
    }

    /* 테이블에 행이 없을 때 쓰는 P2 문구 5개 (portfolio.md P3) */
    public static List<TemplateItemView> defaults() {
        return Arrays.stream(TemplateItemKey.values())
                .map(key -> new TemplateItemView(key.name(), key.getDefaultLabel(), key.getDefaultGuide()))
                .toList();
    }
}
