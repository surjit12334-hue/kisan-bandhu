package com.kisanbandhu.service;

import com.kisanbandhu.domain.*;
import com.kisanbandhu.dto.SlotDtos.*;
import com.kisanbandhu.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SlotService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<SlotStatus> ACTIVE = List.of(SlotStatus.BOOKED, SlotStatus.CHECKED_IN, SlotStatus.IN_PROGRESS);

    private final MandiRepo mandiRepo;
    private final WeighbridgeRepo weighbridgeRepo;
    private final SlotRepo slotRepo;
    private final WaitlistRepo waitlistRepo;
    private final UserRepo userRepo;
    private final SmsService smsService;
    private final AuditService auditService;

    public SlotService(MandiRepo mandiRepo, WeighbridgeRepo weighbridgeRepo, SlotRepo slotRepo,
                       WaitlistRepo waitlistRepo, UserRepo userRepo, SmsService smsService,
                       AuditService auditService) {
        this.mandiRepo = mandiRepo;
        this.weighbridgeRepo = weighbridgeRepo;
        this.slotRepo = slotRepo;
        this.waitlistRepo = waitlistRepo;
        this.userRepo = userRepo;
        this.smsService = smsService;
        this.auditService = auditService;
    }

    public List<Mandi> allMandis() {
        return mandiRepo.findAll();
    }

    @Transactional
    public AvailabilityResponse availability(Long mandiId, LocalDate date) {
        Mandi mandi = mandiRepo.findById(mandiId)
                .orElseThrow(() -> new IllegalArgumentException("Mandi not found"));
        List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(mandiId, Weighbridge.BridgeStatus.ACTIVE);
        List<Slot> booked = slotRepo.findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(
                mandiId, date, LocalTime.MIDNIGHT, LocalTime.of(23, 59, 59));

        Map<String, Slot> taken = new HashMap<>();
        for (Slot s : booked) {
            if (ACTIVE.contains(s.getStatus())) {
                taken.put(s.getWeighbridge().getCode() + "|" + s.getStartTime(), s);
            }
        }

        List<LocalTime> grid = grid(mandi);
        List<BridgeAvailability> bridgeCells = new ArrayList<>();
        for (Weighbridge b : bridges) {
            List<SlotCell> cells = new ArrayList<>();
            for (LocalTime t : grid) {
                Slot s = taken.get(b.getCode() + "|" + t);
                cells.add(s == null
                        ? new SlotCell(t.format(TIME_FMT), true, null, null)
                        : new SlotCell(t.format(TIME_FMT), false, mask(s.getToken()), s.getStatus().name()));
            }
            bridgeCells.add(new BridgeAvailability(b.getCode(), cells));
        }

        long waitlist = waitlistRepo.countByMandiIdAndStatus(mandiId, WaitlistStatus.WAITING);
        return new AvailabilityResponse(mandi.getId(), mandi.getName(), mandi.getDistrict(),
                date.toString(), bridgeCells, (int) waitlist,
                estimateWaitMinutes(mandi, date, bridges.size(), (int) waitlist),
                mandi.getTollFreeNumber());
    }

    @Transactional
    public SlotResponse book(User farmer, BookRequest req) {
        Mandi mandi = mandiRepo.findById(req.mandiId())
                .orElseThrow(() -> new IllegalArgumentException("Mandi not found"));
        LocalDate date = LocalDate.parse(req.date());
        LocalTime time = LocalTime.parse(req.startTime());

        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Cannot book a past date");
        if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("That time has already passed today — please pick a later slot");
        }
        if (!grid(mandi).contains(time)) throw new IllegalArgumentException("Selected time is outside mandi hours");

        List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(mandi.getId(), Weighbridge.BridgeStatus.ACTIVE);
        if (bridges.isEmpty()) throw new IllegalArgumentException("No active weighbridge at this mandi");

        Weighbridge bridge = bridges.get(0);
        for (Weighbridge b : bridges) {
            if (!slotRepo.existsByWeighbridgeIdAndDateAndStartTimeAndStatusIn(b.getId(), date, time, ACTIVE)) {
                bridge = b;
                break;
            }
        }
        if (slotRepo.existsByWeighbridgeIdAndDateAndStartTimeAndStatusIn(bridge.getId(), date, time, ACTIVE)) {
            throw new IllegalArgumentException("All weighbridges are booked for " + time.format(TIME_FMT) + ". Join the waitlist.");
        }

        boolean farmerBusy = slotRepo.findByFarmerIdAndDateOrderByStartTime(farmer.getId(), date).stream()
                .anyMatch(s -> ACTIVE.contains(s.getStatus()));
        if (farmerBusy) throw new IllegalArgumentException("You already hold an active slot for this date");

        Slot slot = new Slot();
        slot.setToken(nextToken(date));
        slot.setFarmer(farmer);
        slot.setMandi(mandi);
        slot.setWeighbridge(bridge);
        slot.setDate(date);
        slot.setStartTime(time);
        slot.setEndTime(time.plusMinutes(mandi.getSlotMinutes()));
        slot.setVehicleNumber(req.vehicleNumber().toUpperCase());
        slot.setCrop(req.crop());
        slot.setQuantityTons(req.quantityTons());
        slot.setStatus(SlotStatus.BOOKED);
        slotRepo.save(slot);

        smsService.send(farmer.getPhone(),
                "Kisan Bandhu: Token " + slot.getToken() + " booked at " + mandi.getName()
                        + " on " + date + " " + time.format(TIME_FMT) + " (" + bridge.getCode()
                        + "). Reach 10 min early. Toll-free IVR: " + mandi.getTollFreeNumber());
        auditService.log("BOOK", "Token " + slot.getToken() + " booked by " + farmer.getPhone(), farmer.getPhone());
        return toResponse(slot, false);
    }

    @Transactional
    public SlotResponse cancel(User farmer, Long slotId) {
        Slot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!slot.getFarmer().getId().equals(farmer.getId())) throw new IllegalArgumentException("Not your slot");
        if (slot.getStatus() != SlotStatus.BOOKED) throw new IllegalArgumentException("Only booked slots can be cancelled");
        slot.setStatus(SlotStatus.CANCELLED);
        slotRepo.save(slot);
        smsService.send(farmer.getPhone(),
                "Kisan Bandhu: Token " + slot.getToken() + " cancelled. You can book a new slot anytime.");
        auditService.log("CANCEL", "Token " + slot.getToken() + " cancelled", farmer.getPhone());
        return toResponse(slot, false);
    }

    @Transactional
    public SlotResponse checkIn(String token, String phone) {
        Slot slot = slotRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (!slot.getFarmer().getPhone().equals(phone)) throw new IllegalArgumentException("Token does not belong to this phone");
        if (slot.getStatus() != SlotStatus.BOOKED) throw new IllegalArgumentException("Slot is " + slot.getStatus());
        if (!slot.getDate().equals(LocalDate.now())) throw new IllegalArgumentException("Check-in is only allowed on the slot date");
        slot.setStatus(SlotStatus.CHECKED_IN);
        slot.setCheckedInAt(LocalDateTime.now());
        slotRepo.save(slot);
        auditService.log("CHECKIN", "Token " + token + " checked in", phone);
        return toResponse(slot, false);
    }

    @Transactional
    public SlotResponse checkout(Long slotId) {
        Slot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (slot.getStatus() != SlotStatus.CHECKED_IN && slot.getStatus() != SlotStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Slot is not checked in");
        }
        slot.setStatus(SlotStatus.COMPLETED);
        slot.setCompletedAt(LocalDateTime.now());
        slotRepo.save(slot);
        smsService.send(slot.getFarmer().getPhone(),
                "Kisan Bandhu: Token " + slot.getToken() + " weighing complete. Thank you.");
        auditService.log("CHECKOUT", "Token " + token(slot) + " completed", slot.getFarmer().getPhone());
        return toResponse(slot, false);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> mySlots(String phone) {
        User farmer = user(phone);
        LocalDate today = LocalDate.now();
        List<Slot> all = new ArrayList<>();
        all.addAll(slotRepo.findByFarmerIdAndDateOrderByStartTime(farmer.getId(), today));
        all.addAll(slotRepo.findByFarmerIdAndDateOrderByStartTime(farmer.getId(), today.plusDays(1)));
        return all.stream().map(s -> toResponse(s, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> adminSlots(Long mandiId, LocalDate date) {
        Mandi mandi = mandiRepo.findById(mandiId)
                .orElseThrow(() -> new IllegalArgumentException("Mandi not found"));
        return slotRepo.findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(
                        mandi.getId(), date, LocalTime.MIDNIGHT, LocalTime.of(23, 59, 59))
                .stream().map(s -> toResponse(s, true)).toList();
    }

    @Transactional
    public WaitlistResponse joinWaitlist(User farmer, WaitlistRequest req) {
        Mandi mandi = mandiRepo.findById(req.mandiId())
                .orElseThrow(() -> new IllegalArgumentException("Mandi not found"));
        LocalDate date = LocalDate.parse(req.date());

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw new IllegalArgumentException("Cannot join the waitlist for a past date");
        LocalTime now = LocalTime.now();
        boolean anyFree = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(mandi.getId(), Weighbridge.BridgeStatus.ACTIVE)
                .stream().anyMatch(b -> grid(mandi).stream().anyMatch(t ->
                        (date.isAfter(today) || !t.isBefore(now))
                                && !slotRepo.existsByWeighbridgeIdAndDateAndStartTimeAndStatusIn(b.getId(), date, t, ACTIVE)));
        if (anyFree) throw new IllegalArgumentException("A free slot exists — please book directly instead of waitlisting");

        WaitlistEntry e = new WaitlistEntry();
        e.setMandi(mandi);
        e.setFarmer(farmer);
        e.setPreferredDate(date);
        e.setCrop(req.crop());
        e.setQuantityTons(req.quantityTons());
        e.setVehicleNumber(req.vehicleNumber().toUpperCase());
        waitlistRepo.save(e);
        smsService.send(farmer.getPhone(),
                "Kisan Bandhu: Added to waitlist for " + mandi.getName() + " on " + date
                        + ". You will get an SMS when a slot frees up.");
        auditService.log("WAITLIST", "Farmer joined waitlist at " + mandi.getCode(), farmer.getPhone());
        return waitlistResponse(e, positionOf(e));
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> myWaitlist(String phone) {
        User farmer = user(phone);
        return waitlistRepo.findAll().stream()
                .filter(w -> w.getFarmer().getId().equals(farmer.getId()))
                .filter(w -> w.getStatus() == WaitlistStatus.WAITING)
                .sorted(Comparator.comparing(WaitlistEntry::getCreatedAt))
                .map(w -> waitlistResponse(w, positionOf(w)))
                .toList();
    }

    @Transactional
    public int expireNoShows() {
        LocalDateTime now = LocalDateTime.now();
        int affected = 0;
        for (Mandi mandi : mandiRepo.findAll()) {
            LocalTime cutoff = now.toLocalTime().minusMinutes(mandi.getGraceMinutes());
            for (Slot slot : slotRepo.findExpiredBookings(SlotStatus.BOOKED, now.toLocalDate(), cutoff)) {
                if (!slot.getMandi().getId().equals(mandi.getId())) continue;
                slot.setStatus(SlotStatus.NO_SHOW);
                slotRepo.save(slot);
                affected++;
                auditService.log("NOSHOW", "Token " + slot.getToken() + " marked no-show", slot.getFarmer().getPhone());
                autoReschedule(slot, mandi, now);
            }
        }
        return affected;
    }

    @Transactional
    public int fillGaps() {
        LocalDateTime now = LocalDateTime.now();
        int promoted = 0;
        for (Mandi mandi : mandiRepo.findAll()) {
            List<WaitlistEntry> waiting = waitlistRepo
                    .findByMandiIdAndPreferredDateAndStatusOrderByCreatedAt(mandi.getId(), now.toLocalDate(), WaitlistStatus.WAITING);
            if (waiting.isEmpty()) continue;

            List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(mandi.getId(), Weighbridge.BridgeStatus.ACTIVE);
            if (bridges.isEmpty()) continue;

            LocalTime from = ceil(now.toLocalTime(), mandi.getSlotMinutes());
            LocalTime until = now.toLocalTime().plusHours(2);
            Set<String> busy = new HashSet<>();
            for (Slot s : slotRepo.findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(mandi.getId(), now.toLocalDate(), LocalTime.MIDNIGHT, LocalTime.of(23, 59))) {
                if (ACTIVE.contains(s.getStatus())) busy.add(s.getWeighbridge().getCode() + "|" + s.getStartTime());
            }

            Iterator<WaitlistEntry> it = waiting.iterator();
            for (LocalTime t : grid(mandi)) {
                if (t.isBefore(from) || !t.isBefore(until)) continue;
                if (!it.hasNext()) break;
                for (Weighbridge b : bridges) {
                    if (!it.hasNext()) break;
                    if (busy.contains(b.getCode() + "|" + t)) continue;
                    WaitlistEntry w = it.next();
                    Slot slot = createFromWaitlist(w, mandi, b, t);
                    busy.add(b.getCode() + "|" + t);
                    w.setStatus(WaitlistStatus.PROMOTED);
                    w.setAssignedSlot(slot);
                    waitlistRepo.save(w);
                    promoted++;
                    auditService.log("GAPFILL", "Waitlist promoted to token " + slot.getToken(), w.getFarmer().getPhone());
                    break;
                }
            }
        }
        return promoted;
    }

    @Transactional
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime horizon = now.toLocalTime().plusMinutes(30);
        int sent = 0;
        for (Slot slot : slotRepo.findByDateAndStatus(now.toLocalDate(), SlotStatus.BOOKED)) {
            if (slot.isSmsSent()) continue;
            if (slot.getStartTime().isBefore(now.toLocalTime()) || slot.getStartTime().isAfter(horizon)) continue;
            smsService.send(slot.getFarmer().getPhone(),
                    "Kisan Bandhu reminder: Token " + slot.getToken() + " at " + slot.getStartTime().format(TIME_FMT)
                            + " (" + slot.getWeighbridge().getCode() + "), " + slot.getMandi().getName() + ". Please reach the gate now.");
            slot.setSmsSent(true);
            slotRepo.save(slot);
            sent++;
        }
        return sent;
    }

    private void autoReschedule(Slot slot, Mandi mandi, LocalDateTime now) {
        List<Weighbridge> bridges = weighbridgeRepo.findByMandiIdAndStatusOrderByCode(mandi.getId(), Weighbridge.BridgeStatus.ACTIVE);
        Set<String> busy = new HashSet<>();
        for (Slot s : slotRepo.findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(mandi.getId(), now.toLocalDate(), LocalTime.MIDNIGHT, LocalTime.of(23, 59))) {
            if (ACTIVE.contains(s.getStatus())) busy.add(s.getWeighbridge().getCode() + "|" + s.getStartTime());
        }
        LocalTime from = ceil(now.toLocalTime(), mandi.getSlotMinutes());
        for (LocalTime t : grid(mandi)) {
            if (t.isBefore(from)) continue;
            for (Weighbridge b : bridges) {
                if (busy.contains(b.getCode() + "|" + t)) continue;
                Slot next = new Slot();
                next.setToken(nextToken(now.toLocalDate()));
                next.setFarmer(slot.getFarmer());
                next.setMandi(mandi);
                next.setWeighbridge(b);
                next.setDate(now.toLocalDate());
                next.setStartTime(t);
                next.setEndTime(t.plusMinutes(mandi.getSlotMinutes()));
                next.setVehicleNumber(slot.getVehicleNumber());
                next.setCrop(slot.getCrop());
                next.setQuantityTons(slot.getQuantityTons());
                next.setStatus(SlotStatus.BOOKED);
                next.setRescheduledFrom(slot);
                slotRepo.save(next);
                smsService.send(slot.getFarmer().getPhone(),
                        "Kisan Bandhu: You missed token " + slot.getToken() + ". Auto-rescheduled (Zero-Idle) to "
                                + next.getToken() + " at " + t.format(TIME_FMT) + " (" + b.getCode() + ").");
                auditService.log("RESCHEDULE", slot.getToken() + " -> " + next.getToken(), slot.getFarmer().getPhone());
                return;
            }
        }
        WaitlistEntry w = new WaitlistEntry();
        w.setMandi(mandi);
        w.setFarmer(slot.getFarmer());
        w.setPreferredDate(now.toLocalDate().plusDays(1));
        w.setCrop(slot.getCrop());
        w.setQuantityTons(slot.getQuantityTons());
        w.setVehicleNumber(slot.getVehicleNumber());
        waitlistRepo.save(w);
        smsService.send(slot.getFarmer().getPhone(),
                "Kisan Bandhu: You missed token " + slot.getToken() + ". No slots left today — auto-joined tomorrow's waitlist.");
    }

    private Slot createFromWaitlist(WaitlistEntry w, Mandi mandi, Weighbridge bridge, LocalTime t) {
        Slot slot = new Slot();
        slot.setToken(nextToken(w.getPreferredDate()));
        slot.setFarmer(w.getFarmer());
        slot.setMandi(mandi);
        slot.setWeighbridge(bridge);
        slot.setDate(w.getPreferredDate());
        slot.setStartTime(t);
        slot.setEndTime(t.plusMinutes(mandi.getSlotMinutes()));
        slot.setVehicleNumber(w.getVehicleNumber());
        slot.setCrop(w.getCrop());
        slot.setQuantityTons(w.getQuantityTons());
        slot.setStatus(SlotStatus.BOOKED);
        slotRepo.save(slot);
        smsService.send(w.getFarmer().getPhone(),
                "Kisan Bandhu: Slot freed up! Token " + slot.getToken() + " at " + t.format(TIME_FMT)
                        + " (" + bridge.getCode() + "), " + mandi.getName() + " on " + w.getPreferredDate() + ".");
        return slot;
    }

    private long estimateWaitMinutes(Mandi mandi, LocalDate date, int bridges, int waitlistAhead) {
        if (bridges == 0) return 0;
        LocalTime now = LocalTime.now();
        long activeAhead = slotRepo.findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(
                        mandi.getId(), date, LocalTime.MIDNIGHT, LocalTime.of(23, 59)).stream()
                .filter(s -> ACTIVE.contains(s.getStatus()))
                .filter(s -> !s.getStartTime().isBefore(now) || s.getStatus() == SlotStatus.CHECKED_IN)
                .count();
        long units = activeAhead + waitlistAhead;
        return Math.max(0, (units * mandi.getSlotMinutes()) / bridges);
    }

    private List<LocalTime> grid(Mandi mandi) {
        List<LocalTime> times = new ArrayList<>();
        LocalTime t = LocalTime.parse(mandi.getOpenTime());
        LocalTime end = LocalTime.parse(mandi.getCloseTime());
        while (t.isBefore(end)) {
            times.add(t);
            t = t.plusMinutes(mandi.getSlotMinutes());
        }
        return times;
    }

    private LocalTime ceil(LocalTime t, int minutes) {
        int m = t.getMinute();
        int rem = m % minutes;
        LocalTime base = rem == 0 ? t : t.plusMinutes(minutes - rem);
        return base.withSecond(0).withNano(0);
    }

    private String nextToken(LocalDate date) {
        String prefix = "KB" + date.toString().substring(2).replace("-", "");
        int seq = (int) slotRepo.countByDate(date) + 1;
        String token = prefix + String.format("-%04d", seq);
        while (slotRepo.findByToken(token).isPresent()) {
            seq++;
            token = prefix + String.format("-%04d", seq);
        }
        return token;
    }

    private String mask(String token) {
        return token;
    }

    public User user(String phone) {
        return userRepo.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private WaitlistResponse waitlistResponse(WaitlistEntry e, int position) {
        return new WaitlistResponse(
                e.getId(), e.getMandi().getId(), e.getMandi().getName(),
                e.getPreferredDate().toString(), e.getStatus().name(), position,
                e.getCrop(), e.getQuantityTons(), e.getVehicleNumber());
    }

    private int positionOf(WaitlistEntry e) {
        List<WaitlistEntry> queue = waitlistRepo.findByMandiIdAndPreferredDateAndStatusOrderByCreatedAt(
                e.getMandi().getId(), e.getPreferredDate(), WaitlistStatus.WAITING);
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(e.getId())) return i + 1;
        }
        return 0;
    }

    public SlotResponse toResponse(Slot s, boolean admin) {
        return new SlotResponse(
                s.getId(), s.getToken(), s.getDate().toString(),
                s.getStartTime().format(TIME_FMT), s.getEndTime().format(TIME_FMT),
                s.getStatus().name(),
                s.getMandi().getId(), s.getMandi().getName(), s.getWeighbridge().getCode(),
                s.getVehicleNumber(), s.getCrop(), s.getQuantityTons(),
                s.getFarmer().getName(),
                admin ? s.getFarmer().getPhone() : null,
                s.getCheckedInAt() == null ? null : s.getCheckedInAt().toString(),
                s.getCompletedAt() == null ? null : s.getCompletedAt().toString(),
                s.getRescheduledFrom() == null ? null : s.getRescheduledFrom().getToken());
    }

    private String token(Slot s) { return s.getToken(); }
}
