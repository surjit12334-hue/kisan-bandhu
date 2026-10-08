package com.kisanbandhu;

import com.kisanbandhu.domain.*;
import com.kisanbandhu.dto.AuthDtos.*;
import com.kisanbandhu.dto.SlotDtos.*;
import com.kisanbandhu.repo.*;
import com.kisanbandhu.service.AuthService;
import com.kisanbandhu.service.IvrService;
import com.kisanbandhu.service.OtpService;
import com.kisanbandhu.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kbtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class KisanBandhuFlowTests {

    @Autowired AuthService authService;
    @Autowired OtpService otpService;
    @Autowired SlotService slotService;
    @Autowired IvrService ivrService;
    @Autowired UserRepo userRepo;
    @Autowired MandiRepo mandiRepo;
    @Autowired WeighbridgeRepo weighbridgeRepo;
    @Autowired SlotRepo slotRepo;
    @Autowired WaitlistRepo waitlistRepo;

    @Test
    void registerLoginAndOtp() {
        var reg = authService.register(new RegisterRequest("Test Farmer", "9000000001", "pass123", "hi", false));
        assertNotNull(reg.token());
        assertEquals("FARMER", reg.user().role());

        var login = authService.login(new LoginRequest("9000000001", "pass123"));
        assertNotNull(login.token());

        String code = otpService.request("9000000001", "LOGIN");
        assertEquals(6, code.length());
        assertTrue(otpService.verify("9000000001", code, "LOGIN"));
        assertFalse(otpService.verify("9000000001", "000000", "LOGIN"));
    }

    @Test
    void bookingConflictCancellationAndCheckout() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Mandi mandi = mandiRepo.findAll().get(0);
        List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(
                mandi.getId(), Weighbridge.BridgeStatus.ACTIVE);
        assertEquals(3, bridges.size());

        User f1 = farmer("9000000011", "Farmer One");
        User f2 = farmer("9000000012", "Farmer Two");
        User f3 = farmer("9000000013", "Farmer Three");
        User f4 = farmer("9000000014", "Farmer Four");

        Slot s1 = slotRepo.save(slotFor(f1, mandi, bridges.get(0), tomorrow, LocalTime.of(8, 0)));
        Slot s2 = slotRepo.save(slotFor(f2, mandi, bridges.get(1), tomorrow, LocalTime.of(8, 0)));
        Slot s3 = slotRepo.save(slotFor(f3, mandi, bridges.get(2), tomorrow, LocalTime.of(8, 0)));

        var avail = slotService.availability(mandi.getId(), tomorrow);
        long taken = avail.bridges().stream().flatMap(b -> b.cells().stream())
                .filter(c -> c.time().equals("08:00") && !c.available()).count();
        assertEquals(3, taken, "all three bridges should show 08:00 as taken");

        assertThrows(IllegalArgumentException.class, () ->
                slotService.book(f4, new BookRequest(mandi.getId(), tomorrow.toString(), "08:00",
                        "MP09ZZ0001", "Wheat", 2.0)), "fourth farmer must be rejected");

        assertThrows(IllegalArgumentException.class, () ->
                slotService.book(f1, new BookRequest(mandi.getId(), tomorrow.toString(), "09:00",
                        "MP09ZZ0011", "Wheat", 1.0)), "one active slot per farmer per day");

        slotService.cancel(f1, s1.getId());
        var booked = slotService.book(f4, new BookRequest(mandi.getId(), tomorrow.toString(), "08:00",
                "MP09ZZ0004", "Wheat", 3.0));
        assertEquals("BOOKED", booked.status());

        s2.setStatus(SlotStatus.CHECKED_IN);
        s2.setCheckedInAt(LocalDateTime.now());
        slotRepo.save(s2);
        var done = slotService.checkout(s2.getId());
        assertEquals("COMPLETED", done.status());
        assertNotNull(done.completedAt());
    }

    @Test
    void zeroIdleMarksExpiredBookingAsNoShow() {
        Mandi mandi = mandiRepo.findAll().get(0);
        User f = farmer("9000000021", "Lazy Farmer");
        List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(
                mandi.getId(), Weighbridge.BridgeStatus.ACTIVE);

        Slot expired = slotRepo.save(slotFor(f, mandi, bridges.get(0), LocalDate.now(), LocalTime.of(8, 0)));
        long waitlistBefore = waitlistRepo.count();

        assertTrue(slotService.expireNoShows() >= 1);
        assertEquals(SlotStatus.NO_SHOW, slotRepo.findById(expired.getId()).orElseThrow().getStatus());
        assertTrue(waitlistRepo.count() > waitlistBefore,
                "no slot left today, so the engine should auto-join tomorrow's waitlist");
    }

    @Test
    void waitlistRejectedWhileSlotsFree() {
        Mandi mandi = mandiRepo.findAll().get(0);
        User f = farmer("9000000031", "Wait Farmer");
        assertThrows(IllegalArgumentException.class, () ->
                slotService.joinWaitlist(f, new WaitlistRequest(mandi.getId(),
                        LocalDate.now().plusDays(3).toString(), "MP09WW0001", "Wheat", 1.0)));
    }

    @Test
    void ivrVoiceFlowBooksSlotAndReportsPrices() {
        IvrService.Response w = ivrService.handle("9000000041", null, null);
        assertEquals("welcome", w.node());
        assertEquals("gather", w.action());

        IvrService.Response menu = ivrService.handle("9000000041", w.node(), "1");
        assertEquals("menu_en", menu.node());
        assertTrue(menu.prompt().toLowerCase().contains("book"));

        IvrService.Response prices = ivrService.handle("9000000041", menu.node(), "3");
        assertEquals("done", prices.node());
        assertFalse(prices.data().isEmpty(), "price list should be spoken back");

        IvrService.Response book = ivrService.handle("9000000041", menu.node(), "1");
        assertEquals("done", book.node(), book.prompt());
        assertNotNull(book.data().get("token"));

        IvrService.Response status = ivrService.handle("9000000041", menu.node(), "2");
        assertEquals("done", status.node());
        assertNotNull(status.data().get("token"));
    }

    private User farmer(String phone, String name) {
        return userRepo.findByPhone(phone).orElseGet(() -> {
            User u = new User();
            u.setPhone(phone);
            u.setName(name);
            u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUV1234567");
            return userRepo.save(u);
        });
    }

    private Slot slotFor(User farmer, Mandi mandi, Weighbridge bridge, LocalDate date, LocalTime time) {
        Slot s = new Slot();
        s.setToken("TST" + date.toString().replace("-", "") + "-" + time.toString().replace(":", "") + "-" + farmer.getId());
        s.setFarmer(farmer);
        s.setMandi(mandi);
        s.setWeighbridge(bridge);
        s.setDate(date);
        s.setStartTime(time);
        s.setEndTime(time.plusMinutes(15));
        s.setVehicleNumber("MPTEST01");
        s.setCrop("Wheat");
        s.setQuantityTons(1.0);
        s.setStatus(SlotStatus.BOOKED);
        return s;
    }
}
