package com.kisanbandhu.repo;

import com.kisanbandhu.domain.Slot;
import com.kisanbandhu.domain.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepo extends JpaRepository<Slot, Long> {

    List<Slot> findByMandiIdAndDateAndStartTimeBetweenOrderByStartTime(Long mandiId, LocalDate date, LocalTime from, LocalTime to);

    List<Slot> findByFarmerIdAndDateOrderByStartTime(Long farmerId, LocalDate date);

    List<Slot> findByDateAndStatusIn(LocalDate date, List<SlotStatus> statuses);

    Optional<Slot> findByToken(String token);

    boolean existsByWeighbridgeIdAndDateAndStartTimeAndStatusIn(
            Long weighbridgeId, LocalDate date, LocalTime startTime, List<SlotStatus> statuses);

    long countByMandiIdAndDateAndStatus(Long mandiId, LocalDate date, SlotStatus status);

    long countByDateAndStatus(LocalDate date, SlotStatus status);

    long countByDate(LocalDate date);

    List<Slot> findByDateAndStatus(LocalDate date, SlotStatus status);

    List<Slot> findByDateAndStatusOrderByStartTime(LocalDate date, SlotStatus status);

    @Query("""
            select s from Slot s
            where s.status = :status and s.date = :date and s.startTime < :cutoff
            """)
    List<Slot> findExpiredBookings(@Param("status") SlotStatus status,
                                   @Param("date") LocalDate date,
                                   @Param("cutoff") LocalTime cutoff);

    @Query("""
            select s from Slot s
            where s.mandi.id = :mandiId and s.date = :date
              and s.startTime >= :from and s.startTime < :until
              and s.status = :status
            order by s.startTime
            """)
    List<Slot> findFreeCandidates(@Param("mandiId") Long mandiId,
                                  @Param("date") LocalDate date,
                                  @Param("from") LocalTime from,
                                  @Param("until") LocalTime until,
                                  @Param("status") SlotStatus status);
}
