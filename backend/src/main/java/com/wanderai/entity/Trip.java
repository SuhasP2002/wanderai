package com.wanderai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Double budget;

    @Column(nullable = false)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String preferences; // JSON string: travel style, interests, accommodation type

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TripStatus status = TripStatus.PENDING;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Itinerary> itineraries;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum TripStatus {
        PENDING,     // Created but AI not yet called
        GENERATED,   // AI itinerary generated (locked behind credits)
        UNLOCKED,    // User paid credits and can view full itinerary
        SAVED        // User saved to their dashboard
    }
}
