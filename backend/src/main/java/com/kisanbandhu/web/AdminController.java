package com.kisanbandhu.web;

import com.kisanbandhu.dto.SlotDtos.SlotResponse;
import com.kisanbandhu.service.DashboardService;
import com.kisanbandhu.service.MarketPriceService;
import com.kisanbandhu.service.SmsService;
import com.kisanbandhu.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final SlotService slotService;
    private final SmsService smsService;
    private final MarketPriceService marketPriceService;

    public AdminController(DashboardService dashboardService, SlotService slotService,
                           SmsService smsService, MarketPriceService marketPriceService) {
        this.dashboardService = dashboardService;
        this.slotService = slotService;
        this.smsService = smsService;
        this.marketPriceService = marketPriceService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam(required = false) Long mandiId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardService.stats(mandiId, date == null ? LocalDate.now() : date);
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots(@RequestParam Long mandiId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.adminSlots(mandiId, date == null ? LocalDate.now() : date);
    }

    @PostMapping("/slots/{id}/checkout")
    public SlotResponse checkout(@PathVariable Long id) {
        return slotService.checkout(id);
    }

    @GetMapping("/sms")
    public List<?> smsOutbox() {
        return smsService.recent();
    }

    @PostMapping("/market/sync")
    public Map<String, Object> syncPrices() {
        return Map.of("synced", marketPriceService.syncFromAgmarknet());
    }
}
