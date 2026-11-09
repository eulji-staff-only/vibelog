package com.vibelog.domain.portfolio.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [PortfolioTemplateItem]
 * 역할 : 개인 작품 템플릿 항목 테이블(portfolio_template_item)과 매핑된다 (FR-PRT-015, #253·#258).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 portfolio_template_item 그대로 둔다.
 *        - 운영자는 이름(label)·안내 문구(guide)·순서(sort_order)만 바꾼다. 항목 종류(item_key)는 바뀌지 않는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioTemplateItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioTemplateItemId;

    @Enumerated(EnumType.STRING)
    private TemplateItemKey itemKey;

    private String label;

    private String guide;   // 없으면 NULL

    private int sortOrder;

    /* 시드가 없는 DB 에서 처음 저장할 때 행을 만든다 */
    public static PortfolioTemplateItem create(TemplateItemKey itemKey, String label, String guide, int sortOrder) {
        PortfolioTemplateItem item = new PortfolioTemplateItem();
        item.itemKey = itemKey;
        item.label = label;
        item.guide = guide;
        item.sortOrder = sortOrder;
        return item;
    }

    public void edit(String label, String guide, int sortOrder) {
        this.label = label;
        this.guide = guide;
        this.sortOrder = sortOrder;
    }
}
