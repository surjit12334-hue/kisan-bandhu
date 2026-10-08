package com.kisanbandhu.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "slots", indexes = {
        @Index(name = "idx_slot_bridge_date_time", columnList = "weighbridge_id,date,start_time"),
        @Index(name = "idx_slot_farmer_date", columnList = "farmer_id,date")
})
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id")
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandi_id")
    private Mandi mandi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weighbridge_id")
    private Weighbridge weighbridge;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "vehicle_number", nullable = false, length = 16)
    private String vehicleNumber;

    @Column(nullable = false, length = 60)
    private String crop;

    @Column(name = "quantity_tons", nullable = false)
    private double quantityTons;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status = SlotStatus.BOOKED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescheduled_from")
    private Slot rescheduledFrom;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "sms_sent", nullable = false)
    private boolean smsSent = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getFarmer() { return farmer; }
    public void setFarmer(User farmer) { this.farmer = farmer; }
    public Mandi getMandi() { return mandi; }
    public void setMandi(Mandi mandi) { this.mandi = mandi; }
    public Weighbridge getWeighbridge() { return weighbridge; }
    public void setWeighbridge(Weighbridge weighbridge) { this.weighbridge = weighbridge; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public double getQuantityTons() { return quantityTons; }
    public void setQuantityTons(double quantityTons) { this.quantityTons = quantityTons; }
    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }
    public Slot getRescheduledFrom() { return rescheduledFrom; }
    public void setRescheduledFrom(Slot rescheduledFrom) { this.rescheduledFrom = rescheduledFrom; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public boolean isSmsSent() { return smsSent; }
    public void setSmsSent(boolean smsSent) { this.smsSent = smsSent; }
}
