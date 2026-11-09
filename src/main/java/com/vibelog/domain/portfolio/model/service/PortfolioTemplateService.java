package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.PortfolioTemplateItemRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioTemplateForm;
import com.vibelog.domain.portfolio.model.dto.TemplateItemView;
import com.vibelog.domain.portfolio.model.entity.PortfolioTemplateItem;
import com.vibelog.domain.portfolio.model.entity.TemplateItemKey;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [PortfolioTemplateService]
 * 역할 : 개인 작품 템플릿 관리 (A-12, API-PRT-14·15, FR-PRT-015)와 U-34 가 그릴 항목 (API-PRT-12 P3).
 * 책임 : - 항목은 5개로 고정이다 (#168 결정 A). 운영자는 이름·안내 문구·순서만 바꾼다.
 *        - 저장 때 5개 항목 키가 한 번씩 모두 와야 한다. 빠지거나 겹치거나 모르는 키면 INVALID_INPUT.
 *        - 테이블에 행이 없으면 P2 문구(TemplateItemKey 기본값)로 보여 주고, 저장하면 행을 만든다.
 *        - 이름을 바꿔도 저장 칸은 item_key 로 정해져서 이미 등록된 개인 작품은 그대로다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioTemplateService {

    private static final Set<String> ALL_KEYS = Arrays.stream(TemplateItemKey.values()).map(Enum::name).collect(Collectors.toSet());

    private final PortfolioTemplateItemRepository portfolioTemplateItemRepository;

    /* U-34·A-12 항목 순서대로 */
    public List<TemplateItemView> getItems() {
        List<PortfolioTemplateItem> items = portfolioTemplateItemRepository.findAllByOrderBySortOrderAsc();
        return items.isEmpty() ? TemplateItemView.defaults() : items.stream().map(TemplateItemView::from).toList();
    }

    /* API-PRT-15. 보낸 순서가 곧 순서다. 안내 문구가 비면 NULL 로 둔다 */
    @Transactional
    public void save(PortfolioTemplateForm form) {
        List<String> keys = form.getItems().stream().map(PortfolioTemplateForm.Item::getItemKey).toList();
        if (keys.size() != ALL_KEYS.size() || !new HashSet<>(keys).equals(ALL_KEYS)) {
            throw new BusinessException(PortfolioErrorCode.INVALID_INPUT);
        }
        Map<TemplateItemKey, PortfolioTemplateItem> saved = portfolioTemplateItemRepository.findAll().stream()
                .collect(Collectors.toMap(PortfolioTemplateItem::getItemKey, Function.identity()));
        for (int i = 0; i < form.getItems().size(); i++) {
            PortfolioTemplateForm.Item item = form.getItems().get(i);
            TemplateItemKey key = TemplateItemKey.valueOf(item.getItemKey());
            String label = item.getLabel().strip();
            String guide = item.getGuide() == null || item.getGuide().isBlank() ? null : item.getGuide().strip();
            PortfolioTemplateItem row = saved.get(key);
            if (row == null) {
                portfolioTemplateItemRepository.save(PortfolioTemplateItem.create(key, label, guide, i + 1));
            } else {
                row.edit(label, guide, i + 1);
            }
        }
    }
}
