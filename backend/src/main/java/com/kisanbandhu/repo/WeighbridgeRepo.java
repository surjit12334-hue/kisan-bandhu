package com.kisanbandhu.repo;

import com.kisanbandhu.domain.Weighbridge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeighbridgeRepo extends JpaRepository<Weighbridge, Long> {
    List<Weighbridge> findByMandiIdAndStatusOrderByCode(Long mandiId, Weighbridge.BridgeStatus status);
    List<Weighbridge> findByMandiIdOrderByCode(Long mandiId);
}
