package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * [PortfolioTemplateForm]
 * 역할 : 템플릿 저장 폼 (API-PRT-15, FR-PRT-015).
 * 책임 : - items 는 보낸 순서가 곧 순서(sort_order)다.
 *        - 이름 1~50자, 안내 문구 200자까지 (portfolio_template_item.label VARCHAR(50), guide VARCHAR(200)).
 *          5개 항목 키가 한 번씩 모두 있는지는 Service 가 본다 (INVALID_INPUT).
 *        - 폼 값을 목록으로 묶어 받으려고 setter 가 있는 클래스로 둔다 (Entity 가 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class PortfolioTemplateForm {

    @Valid
    private List<Item> items = new ArrayList<>();

    /* [Item] 역할 : 항목 한 줄의 입력 값 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String itemKey;

        @NotBlank(message = "항목 이름을 입력해 주세요.")
        @Size(max = 50, message = "항목 이름은 50자까지 입력할 수 있습니다.")
        private String label;

        @Size(max = 200, message = "안내 문구는 200자까지 입력할 수 있습니다.")
        private String guide;

        public Item(String itemKey, String label, String guide) {
            this.itemKey = itemKey;
            this.label = label;
            this.guide = guide;
        }
    }

    public static PortfolioTemplateForm of(List<TemplateItemView> views) {
        PortfolioTemplateForm form = new PortfolioTemplateForm();
        form.items = new ArrayList<>(views.stream().map(v -> new Item(v.itemKey(), v.label(), v.guide())).toList());
        return form;
    }
}
