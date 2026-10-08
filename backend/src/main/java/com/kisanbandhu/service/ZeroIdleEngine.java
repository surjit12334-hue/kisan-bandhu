package com.kisanbandhu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ZeroIdleEngine {

    private static final Logger log = LoggerFactory.getLogger(ZeroIdleEngine.class);

    private final SlotService slotService;

    public ZeroIdleEngine(SlotService slotService) {
        this.slotService = slotService;
    }

    @Scheduled(fixedRate = 30_000)
    public void tick() {
        try {
            int expired = slotService.expireNoShows();
            int promoted = slotService.fillGaps();
            int reminders = slotService.sendReminders();
            if (expired > 0 || promoted > 0 || reminders > 0) {
                log.info("Zero-Idle tick: noShows={}, gapPromoted={}, reminders={}", expired, promoted, reminders);
            }
        } catch (Exception e) {
            log.error("Zero-Idle tick failed: {}", e.getMessage());
        }
    }
}
