package com.kisanbandhu.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandi_id")
    private Mandi mandi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id")
    private User farmer;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(nullable = false, length = 60)
    private String crop;

    @Column(name = "quantity_tons", nullable = false)
    private double quantityTons;

    @Column(name = "vehicle_number", nullable = false, length = 16)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_slot_id")
    private Slot assignedSlot;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mandi getMandi() { return mandi; }
    public void setMandi(Mandi mandi) { this.mandi = mandi; }
    public User getFarmer() { return farmer; }
    public void setFarmer(User farmer) { this.farmer = farmer; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public double getQuantityTons() { return quantityTons; }
    public void setQuantityTons(double quantityTons) { this.quantityTons = quantityTons; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public WaitlistStatus getStatus() { return status; }
    public void setStatus(WaitlistStatus status) { this.status = status; }
    public Slot getAssignedSlot() { return assignedSlot; }
    public void setAssignedSlot(Slot assignedSlot) { this.assignedSlot = assignedSlot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
