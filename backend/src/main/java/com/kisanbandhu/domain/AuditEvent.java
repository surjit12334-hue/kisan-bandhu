package com.kisanbandhu.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 300)
    private String detail;

    @Column(name = "actor_phone", length = 15)
    private String actorPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static AuditEvent of(String type, String detail, String actorPhone) {
        AuditEvent e = new AuditEvent();
        e.type = type;
        e.detail = detail;
        e.actorPhone = actorPhone;
        return e;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getActorPhone() { return actorPhone; }
    public void setActorPhone(String actorPhone) { this.actorPhone = actorPhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
