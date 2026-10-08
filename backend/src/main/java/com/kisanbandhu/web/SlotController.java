package com.kisanbandhu.web;

import com.kisanbandhu.domain.Mandi;
import com.kisanbandhu.dto.SlotDtos.*;
import com.kisanbandhu.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/public/mandis")
    public List<Map<String, Object>> mandis() {
        return slotService.allMandis().stream().map(this::mandiView).toList();
    }

    @GetMapping("/public/availability")
    public AvailabilityResponse availability(@RequestParam Long mandiId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.availability(mandiId, date);
    }

    @PostMapping("/slots/book")
    public SlotResponse book(@Valid @RequestBody BookRequest req) {
        return slotService.book(slotService.user(currentPhone()), req);
    }

    @PostMapping("/slots/{id}/cancel")
    public SlotResponse cancel(@PathVariable Long id) {
        return slotService.cancel(slotService.user(currentPhone()), id);
    }

    @PostMapping("/slots/checkin")
    public SlotResponse checkin(@RequestBody Map<String, String> body) {
        return slotService.checkIn(body.get("token"), currentPhone());
    }

    @GetMapping("/slots/mine")
    public List<SlotResponse> mine() {
        return slotService.mySlots(currentPhone());
    }

    @PostMapping("/slots/waitlist")
    public WaitlistResponse joinWaitlist(@Valid @RequestBody WaitlistRequest req) {
        return slotService.joinWaitlist(slotService.user(currentPhone()), req);
    }

    @GetMapping("/slots/waitlist/mine")
    public List<WaitlistResponse> myWaitlist() {
        return slotService.myWaitlist(currentPhone());
    }

    private String currentPhone() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Map<String, Object> mandiView(Mandi m) {
        return Map.of(
                "id", m.getId(), "name", m.getName(), "code", m.getCode(),
                "district", m.getDistrict(), "state", m.getState(),
                "tollFree", m.getTollFreeNumber() == null ? "" : m.getTollFreeNumber(),
                "open", m.getOpenTime(), "close", m.getCloseTime(),
                "slotMinutes", m.getSlotMinutes());
    }
}
