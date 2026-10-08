package com.kisanbandhu.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "mandis")
public class Mandi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 80)
    private String district;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(name = "toll_free_number", length = 20)
    private String tollFreeNumber;

    @Column(name = "open_time", nullable = false, length = 5)
    private String openTime = "08:00";

    @Column(name = "close_time", nullable = false, length = 5)
    private String closeTime = "17:00";

    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes = 15;

    @Column(name = "grace_minutes", nullable = false)
    private int graceMinutes = 20;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getTollFreeNumber() { return tollFreeNumber; }
    public void setTollFreeNumber(String tollFreeNumber) { this.tollFreeNumber = tollFreeNumber; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }
    public int getGraceMinutes() { return graceMinutes; }
    public void setGraceMinutes(int graceMinutes) { this.graceMinutes = graceMinutes; }
}
