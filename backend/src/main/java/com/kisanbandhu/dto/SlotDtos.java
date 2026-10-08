package com.kisanbandhu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public final class SlotDtos {

    private SlotDtos() {}

    public record BookRequest(
            @NotNull Long mandiId,
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
            @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String startTime,
            @NotBlank String vehicleNumber,
            @NotBlank String crop,
            @NotNull @Min(value = 1) Double quantityTons) {}

    public record WaitlistRequest(
            @NotNull Long mandiId,
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
            @NotBlank String vehicleNumber,
            @NotBlank String crop,
            @NotNull @Min(value = 1) Double quantityTons) {}

    public record SlotCell(String time, boolean available, String token, String status) {}

    public record BridgeAvailability(String code, List<SlotCell> cells) {}

    public record AvailabilityResponse(
            Long mandiId, String mandiName, String district, String date,
            List<BridgeAvailability> bridges,
            int waitlistCount, long estimatedWaitMinutes, String tollFreeNumber) {}

    public record SlotResponse(
            Long id, String token, String date, String startTime, String endTime, String status,
            Long mandiId, String mandiName, String bridgeCode,
            String vehicleNumber, String crop, double quantityTons,
            String farmerName, String farmerPhone,
            String checkedInAt, String completedAt, String rescheduledFromToken) {}

    public record WaitlistResponse(
            Long id, Long mandiId, String mandiName, String date, String status,
            int position, String crop, double quantityTons, String vehicleNumber) {}
}
