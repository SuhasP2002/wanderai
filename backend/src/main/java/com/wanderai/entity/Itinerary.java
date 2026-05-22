package com.wanderai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itineraries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(columnDefinition = "TEXT")
    private String morningActivity;

    @Column(columnDefinition = "TEXT")
    private String afternoonActivity;

    @Column(columnDefinition = "TEXT")
    private String eveningActivity;

    @Column(columnDefinition = "TEXT")
    private String hotelSuggestion;

    @Column(columnDefinition = "TEXT")
    private String restaurantSuggestions;

    @Column(columnDefinition = "TEXT")
    private String travelTips;

    private Double estimatedDayCost;
}
