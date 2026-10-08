package com.kisanbandhu.service;

import com.kisanbandhu.domain.Mandi;
import com.kisanbandhu.domain.MarketPrice;
import com.kisanbandhu.domain.Role;
import com.kisanbandhu.domain.Slot;
import com.kisanbandhu.dto.SlotDtos;
import com.kisanbandhu.repo.MandiRepo;
import com.kisanbandhu.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IvrService {

    public record Option(String key, String text) {}
    public record Response(String node, String prompt, String action, List<Option> options, Map<String, Object> data) {}

    private final SlotService slotService;
    private final MandiRepo mandiRepo;
    private final UserRepo userRepo;
    private final MarketPriceService marketPriceService;

    public IvrService(SlotService slotService, MandiRepo mandiRepo, UserRepo userRepo,
                      MarketPriceService marketPriceService) {
        this.slotService = slotService;
        this.mandiRepo = mandiRepo;
        this.userRepo = userRepo;
        this.marketPriceService = marketPriceService;
    }

    public Response handle(String phone, String node, String digits) {
        String n = node == null || node.isBlank() ? "welcome" : node;
        String d = digits == null ? "" : digits.trim();
        return switch (n) {
            case "welcome" -> d.isEmpty() ? welcome() : (d.equals("1") ? menuEn() : menuHi());
            case "menu", "menu_en" -> dispatch(d, phone, "en");
            case "menu_hi" -> dispatch(d, phone, "hi");
            case "book" -> book(phone);
            case "status" -> status(phone);
            case "prices" -> prices();
            case "waitlist" -> waitlist(phone);
            default -> welcome();
        };
    }

    private Response dispatch(String digits, String phone, String lang) {
        return switch (digits) {
            case "1" -> book(phone);
            case "2" -> status(phone);
            case "3" -> prices();
            case "4" -> waitlist(phone);
            case "0", "" -> "hi".equals(lang)
                    ? new Response("done", "धन्यवाद! कॉल बंद की जा रही है। Thank you, goodbye.", "hangup", List.of(), Map.of())
                    : new Response("done", "Thank you, goodbye.", "hangup", List.of(), Map.of());
            default -> "hi".equals(lang) ? menuHi() : menuEn();
        };
    }

    private Response welcome() {
        return new Response("welcome",
                "नमस्ते! Kisan Bandhu पर आपका स्वागत है। Welcome to Kisan Bandhu, the toll-free mandi slot assistant. "
                        + "Press 1 for English, 2 for Hindi.",
                "gather",
                List.of(new Option("1", "English"), new Option("2", "हिंदी")),
                Map.of());
    }

    private Response menuEn() {
        return new Response("menu_en",
                "Press 1 to book a weighbridge slot, 2 for your token status, 3 for today's market prices, 4 to join the waitlist, 0 to exit.",
                "gather",
                List.of(new Option("1", "Book slot"), new Option("2", "Token status"),
                        new Option("3", "Market prices"), new Option("4", "Join waitlist"),
                        new Option("0", "Exit")),
                Map.of("lang", "en"));
    }

    private Response menuHi() {
        return new Response("menu_hi",
                "बुकिंग के लिए 1 दबाएँ, टोकन स्थिति के लिए 2, आज के भाव के लिए 3, प्रतीक्षा सूची के लिए 4, बाहर निकलने के लिए 0।",
                "gather",
                List.of(new Option("1", "स्लॉट बुक करें"), new Option("2", "टोकन स्थिति"),
                        new Option("3", "मंडी भाव"), new Option("4", "प्रतीक्षा सूची"),
                        new Option("0", "बंद करें")),
                Map.of("lang", "hi"));
    }

    private Response book(String phone) {
        try {
            var farmer = ensureFarmer(phone);
            Mandi mandi = mandiRepo.findAll().get(0);
            LocalDate today = LocalDate.now();
            LocalDate date = today;
            java.time.LocalTime now = java.time.LocalTime.now().withSecond(0).withNano(0);
            String slotTime = null;
            String bridge = null;
            for (LocalDate d : List.of(today, today.plusDays(1))) {
                var availability = slotService.availability(mandi.getId(), d);
                outer:
                for (SlotDtos.BridgeAvailability b : availability.bridges()) {
                    for (SlotDtos.SlotCell c : b.cells()) {
                        if (c.available() && (d.isAfter(today) || !java.time.LocalTime.parse(c.time()).isBefore(now))) {
                            date = d;
                            slotTime = c.time();
                            bridge = b.code();
                            break outer;
                        }
                    }
                }
                if (slotTime != null) break;
            }
            if (slotTime == null) {
                slotService.joinWaitlist(farmer, new SlotDtos.WaitlistRequest(
                        mandi.getId(), today.plusDays(1).toString(), "IVR-" + phone.substring(6), "गेहूँ / Wheat", 1.0));
                return new Response("done",
                        "अभी सभी स्लॉट भरे हैं। आपको प्रतीक्षा सूची में डाल दिया गया है। All slots are full; you have been added to the waitlist. धन्यवाद।",
                        "hangup", List.of(), Map.of("waitlisted", true));
            }
            var slot = slotService.book(farmer, new SlotDtos.BookRequest(
                    mandi.getId(), date.toString(), slotTime,
                    "IVR" + phone.substring(6), "गेहूँ / Wheat", 1.0));
            return new Response("done",
                    "स्लॉट बुक हो गया! Slot booked. आपका टोकन " + slot.token() + ", दिनांक " + slot.date()
                            + ", समय " + slot.startTime() + ", वीघब्रिज " + bridge + ", " + mandi.getName()
                            + "। SMS भेज दिया गया है। धन्यवाद।",
                    "hangup", List.of(),
                    Map.of("token", slot.token(), "time", slot.startTime(), "date", slot.date(), "bridge", bridge));
        } catch (Exception e) {
            return new Response("error", "माफ़ कीजिए, बुकिंग नहीं हो सकी। Sorry, booking failed: " + e.getMessage(),
                    "hangup", List.of(), Map.of("error", e.getMessage()));
        }
    }

    private Response status(String phone) {
        var slots = slotService.mySlots(phone);
        var today = LocalDate.now();
        var found = slots.stream().filter(s -> s.date().equals(today.toString())).findFirst()
                .or(() -> slots.stream().filter(s -> s.date().equals(today.plusDays(1).toString())).findFirst());
        if (found.isEmpty()) {
            return new Response("done", "आज के लिए कोई बुकिंग नहीं मिली। No upcoming booking found.",
                    "hangup", List.of(), Map.of());
        }
        SlotDtos.SlotResponse s = found.get();
        return new Response("done",
                "आपका टोकन " + s.token() + " है, दिनांक " + s.date() + ", समय " + s.startTime()
                        + ", वीघब्रिज " + s.bridgeCode() + ", स्थिति " + s.status()
                        + "। Your token is " + s.token() + " on " + s.date() + " at " + s.startTime() + ".",
                "hangup", List.of(),
                Map.of("token", s.token(), "status", s.status(), "time", s.startTime(), "date", s.date()));
    }

    private Response prices() {
        List<MarketPrice> list = marketPriceService.prices(null).stream().limit(3).toList();
        if (list.isEmpty()) {
            return new Response("done", "आज के भाव उपलब्ध नहीं हैं। Today's prices are unavailable.",
                    "hangup", List.of(), Map.of());
        }
        StringBuilder sb = new StringBuilder("आज के मुख्य भाव। Today's top mandi prices. ");
        Map<String, Object> data = new LinkedHashMap<>();
        int i = 1;
        for (MarketPrice p : list) {
            sb.append(i).append(". ").append(p.getCommodity()).append(" ₹").append((long) p.getPrice())
                    .append(" प्रति ").append(p.getUnit()).append(". ");
            data.put("commodity" + i, p.getCommodity());
            data.put("price" + i, p.getPrice());
            i++;
        }
        return new Response("done", sb.toString(), "hangup", List.of(), data);
    }

    private Response waitlist(String phone) {
        try {
            var farmer = ensureFarmer(phone);
            Mandi mandi = mandiRepo.findAll().get(0);
            var resp = slotService.joinWaitlist(farmer, new SlotDtos.WaitlistRequest(
                    mandi.getId(), LocalDate.now().toString(), "IVR-" + phone.substring(6), "गेहूँ / Wheat", 1.0));
            return new Response("done",
                    "आप प्रतीक्षा सूची में हैं, क्रमांक " + resp.position() + "। You are on the waitlist at position "
                            + resp.position() + ". स्लॉट खाली होते ही SMS आएगा।",
                    "hangup", List.of(), Map.of("position", resp.position()));
        } catch (IllegalArgumentException e) {
            return new Response("done", e.getMessage(), "hangup", List.of(), Map.of("error", e.getMessage()));
        }
    }

    private com.kisanbandhu.domain.User ensureFarmer(String phone) {
        return userRepo.findByPhone(phone).orElseGet(() -> {
            var u = new com.kisanbandhu.domain.User();
            u.setPhone(phone);
            u.setName("IVR Farmer " + phone.substring(6));
            byte[] rnd = new byte[40];
            new java.security.SecureRandom().nextBytes(rnd);
            String hash = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
            u.setPasswordHash("$2a$10$" + hash.substring(0, 53));
            u.setRole(Role.FARMER);
            u.setFeaturePhone(true);
            return userRepo.save(u);
        });
    }
}
