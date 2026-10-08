package com.kisanbandhu.service;

import com.kisanbandhu.domain.MarketPrice;
import com.kisanbandhu.repo.MarketPriceRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class MarketPriceService {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceService.class);

    private final MarketPriceRepo repo;
    private final boolean agmarknetEnabled;
    private final String baseUrl;

    public MarketPriceService(MarketPriceRepo repo,
                              @Value("${kisan.agmarknet.enabled}") boolean agmarknetEnabled,
                              @Value("${kisan.agmarknet.base-url}") String baseUrl) {
        this.repo = repo;
        this.agmarknetEnabled = agmarknetEnabled;
        this.baseUrl = baseUrl;
    }

    @Transactional(readOnly = true)
    public List<MarketPrice> prices(String commodity) {
        if (commodity == null || commodity.isBlank()) {
            return repo.findTop100ByOrderByPriceDateDescFetchedAtDesc();
        }
        return repo.findByCommodityIgnoreCaseOrderByPriceDateDesc(commodity);
    }

    @Transactional
    public int syncFromAgmarknet() {
        if (!agmarknetEnabled) {
            log.info("Agmarknet sync skipped (kisan.agmarknet.enabled=false) — serving cached/seeded prices");
            return 0;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = new RestTemplate().getForObject(baseUrl + "/api/prices", List.class);
            if (rows == null) return 0;
            int saved = 0;
            for (Map<String, Object> row : rows) {
                MarketPrice p = new MarketPrice();
                p.setCommodity(String.valueOf(row.get("commodity")));
                p.setMandiName(String.valueOf(row.getOrDefault("mandi", "")));
                p.setState(String.valueOf(row.getOrDefault("state", "")));
                p.setPrice(Double.parseDouble(String.valueOf(row.getOrDefault("price", 0))));
                p.setPriceDate(LocalDate.parse(String.valueOf(row.getOrDefault("date", LocalDate.now()))));
                p.setSource("AGMARKNET");
                repo.save(p);
                saved++;
            }
            return saved;
        } catch (Exception e) {
            log.warn("Agmarknet sync failed, keeping cached prices: {}", e.getMessage());
            return 0;
        }
    }
}
