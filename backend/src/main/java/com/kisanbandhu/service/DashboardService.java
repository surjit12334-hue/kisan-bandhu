package com.kisanbandhu.service;

import com.kisanbandhu.domain.Slot;
import com.kisanbandhu.domain.SlotStatus;
import com.kisanbandhu.domain.User;
import com.kisanbandhu.repo.MandiRepo;
import com.kisanbandhu.repo.SlotRepo;
import com.kisanbandhu.repo.UserRepo;
import com.kisanbandhu.repo.WaitlistRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final SlotRepo slotRepo;
    private final UserRepo userRepo;
    private final MandiRepo mandiRepo;
    private final WaitlistRepo waitlistRepo;
    private final AuditService auditService;

    public DashboardService(SlotRepo slotRepo, UserRepo userRepo, MandiRepo mandiRepo,
                            WaitlistRepo waitlistRepo, AuditService auditService) {
        this.slotRepo = slotRepo;
        this.userRepo = userRepo;
        this.mandiRepo = mandiRepo;
        this.waitlistRepo = waitlistRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long mandiId, LocalDate date) {
        final List<Slot> day = new ArrayList<>();
        for (SlotStatus st : SlotStatus.values()) {
            for (Slot s : slotRepo.findByDateAndStatus(date, st)) {
                if (mandiId == null || s.getMandi().getId().equals(mandiId)) day.add(s);
            }
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (SlotStatus st : SlotStatus.values()) {
            long count = slotRepo.findByDateAndStatus(date, st).stream()
                    .filter(s -> mandiId == null || s.getMandi().getId().equals(mandiId))
                    .count();
            byStatus.put(st.name(), count);
        }

        long completed = byStatus.getOrDefault("COMPLETED", 0L);
        long noShow = byStatus.getOrDefault("NO_SHOW", 0L);
        long processed = completed + noShow;
        double noShowRate = processed == 0 ? 0.0 : Math.round((noShow * 100.0) / processed) * 1.0;

        Map<String, Long> hourly = new LinkedHashMap<>();
        for (int h = 8; h < 17; h++) {
            final int hour = h;
            String label = String.format("%02d:00", hour);
            long count = day.stream()
                    .filter(s -> s.getStatus() == SlotStatus.COMPLETED || s.getStatus() == SlotStatus.IN_PROGRESS
                            || s.getStatus() == SlotStatus.CHECKED_IN)
                    .filter(s -> s.getStartTime().getHour() == hour)
                    .count();
            hourly.put(label, count);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("bookings", byStatus.getOrDefault("BOOKED", 0L));
        out.put("checkedIn", byStatus.getOrDefault("CHECKED_IN", 0L));
        out.put("completed", completed);
        out.put("noShow", noShow);
        out.put("cancelled", byStatus.getOrDefault("CANCELLED", 0L));
        out.put("rescheduled", byStatus.getOrDefault("BOOKED", 0L));
        out.put("totalSlotsToday", slotRepo.countByDate(date));
        out.put("noShowRatePct", noShowRate);
        out.put("farmers", userRepo.count());
        out.put("mandis", mandiRepo.count());
        out.put("hourlyThroughput", hourly);
        out.put("recentEvents", auditService.recent().stream().limit(20).map(e -> Map.of(
                "type", e.getType(),
                "detail", e.getDetail() == null ? "" : e.getDetail(),
                "at", e.getCreatedAt().toString())).toList());
        return out;
    }
}
