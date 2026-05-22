package com.wanderai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class TripDto {

    @Data
    public static class TripRequest {
        @NotBlank(message = "Destination is required")
        private String destination;

        @NotNull @Min(1)
        private Integer durationDays;

        @NotNull @Min(0)
        private Double budget;

        @NotBlank
        private String currency;

        private String travelStyle;       // luxury, budget, backpacker, family
        private String interests;         // culture, adventure, food, nature
        private String accommodationType; // hotel, hostel, airbnb, resort
        private String specialRequests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripResponse {
        private Long id;
        private String destination;
        private Integer durationDays;
        private Double budget;
        private String currency;
        private String travelStyle;
        private String status;
        private List<ItineraryResponse> itineraries;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryResponse {
        private Long id;
        private Integer dayNumber;
        private String morningActivity;
        private String afternoonActivity;
        private String eveningActivity;
        private String hotelSuggestion;
        private String restaurantSuggestions;
        private String travelTips;
        private Double estimatedDayCost;
    }
}
