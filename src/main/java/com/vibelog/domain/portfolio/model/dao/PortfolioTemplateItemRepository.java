package com.vibelog.domain.portfolio.model.dao;

import com.vibelog.domain.portfolio.model.entity.PortfolioTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioTemplateItemRepository extends JpaRepository<PortfolioTemplateItem, Long> {

    /* U-34·A-12 에 보이는 순서 */
    List<PortfolioTemplateItem> findAllByOrderBySortOrderAsc();
}
