package com.kisanbandhu.repo;

import com.kisanbandhu.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketPriceRepo extends JpaRepository<MarketPrice, Long> {
    List<MarketPrice> findTop100ByOrderByPriceDateDescFetchedAtDesc();
    List<MarketPrice> findByCommodityIgnoreCaseOrderByPriceDateDesc(String commodity);
    long countByPriceDate(LocalDate date);
}
