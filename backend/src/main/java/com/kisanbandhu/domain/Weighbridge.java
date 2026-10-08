package com.kisanbandhu.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "weighbridges")
public class Weighbridge {

    public enum BridgeStatus { ACTIVE, MAINTENANCE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandi_id")
    private Mandi mandi;

    @Column(nullable = false, length = 24)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BridgeStatus status = BridgeStatus.ACTIVE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mandi getMandi() { return mandi; }
    public void setMandi(Mandi mandi) { this.mandi = mandi; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BridgeStatus getStatus() { return status; }
    public void setStatus(BridgeStatus status) { this.status = status; }
}
