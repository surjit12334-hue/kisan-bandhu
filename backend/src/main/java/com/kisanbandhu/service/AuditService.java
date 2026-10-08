package com.kisanbandhu.service;

import com.kisanbandhu.domain.AuditEvent;
import com.kisanbandhu.repo.AuditEventRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepo auditEventRepo;

    public AuditService(AuditEventRepo auditEventRepo) {
        this.auditEventRepo = auditEventRepo;
    }

    public void log(String type, String detail, String actorPhone) {
        auditEventRepo.save(AuditEvent.of(type, detail, actorPhone));
    }

    public List<AuditEvent> recent() {
        return auditEventRepo.findTop100ByOrderByCreatedAtDesc();
    }
}
