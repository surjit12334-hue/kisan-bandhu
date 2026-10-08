package com.kisanbandhu.web;

import com.kisanbandhu.domain.MarketPrice;
import com.kisanbandhu.service.MarketPriceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketPriceService marketPriceService;

    public MarketController(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GetMapping("/prices")
    public List<Map<String, Object>> prices(@RequestParam(required = false) String commodity) {
        return marketPriceService.prices(commodity).stream().map(p -> Map.<String, Object>of(
                "commodity", p.getCommodity(),
                "mandi", p.getMandiName() == null ? "" : p.getMandiName(),
                "state", p.getState() == null ? "" : p.getState(),
                "price", p.getPrice(),
                "unit", p.getUnit(),
                "date", p.getPriceDate().toString(),
                "source", p.getSource())).toList();
    }
}
