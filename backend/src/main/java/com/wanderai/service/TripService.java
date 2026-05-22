package com.wanderai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanderai.dto.TripDto;
import com.wanderai.entity.Itinerary;
import com.wanderai.entity.Trip;
import com.wanderai.entity.User;
import com.wanderai.repository.ItineraryRepository;
import com.wanderai.repository.TripRepository;
import com.wanderai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TripDto.TripResponse createAndGenerateTrip(TripDto.TripRequest request) {
        User user = getCurrentUser();

        if (user.getCreditsBalance() < 1) {
            throw new RuntimeException("Insufficient credits. Please purchase more credits to generate an itinerary.");
        }

        // Build preferences JSON
        Map<String, String> prefs = new HashMap<>();
        prefs.put("travelStyle", request.getTravelStyle() != null ? request.getTravelStyle() : "");
        prefs.put("interests", request.getInterests() != null ? request.getInterests() : "");
        prefs.put("accommodationType", request.getAccommodationType() != null ? request.getAccommodationType() : "");
        prefs.put("specialRequests", request.getSpecialRequests() != null ? request.getSpecialRequests() : "");

        String preferencesJson;
        try {
            preferencesJson = objectMapper.writeValueAsString(prefs);
        } catch (Exception e) {
            preferencesJson = "{}";
        }

        Trip trip = Trip.builder()
                .user(user)
                .destination(request.getDestination())
                .durationDays(request.getDurationDays())
                .budget(request.getBudget())
                .currency(request.getCurrency())
                .preferences(preferencesJson)
                .status(Trip.TripStatus.PENDING)
                .build();

        trip = tripRepository.save(trip);

        // Generate AI itinerary
        List<Itinerary> itineraries = aiService.generateItinerary(trip);
        itineraryRepository.saveAll(itineraries);

        // Deduct 1 credit
        user.setCreditsBalance(user.getCreditsBalance() - 1);
        userRepository.save(user);

        trip.setStatus(Trip.TripStatus.UNLOCKED);
        tripRepository.save(trip);

        return mapToResponse(trip, itineraries);
    }

    public List<TripDto.TripResponse> getUserTrips() {
        User user = getCurrentUser();
        List<Trip> trips = tripRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return trips.stream().map(trip -> {
            List<Itinerary> itineraries = itineraryRepository.findByTripIdOrderByDayNumber(trip.getId());
            return mapToResponse(trip, itineraries);
        }).collect(Collectors.toList());
    }

    public TripDto.TripResponse getTripById(Long tripId) {
        User user = getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (!trip.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<Itinerary> itineraries = itineraryRepository.findByTripIdOrderByDayNumber(tripId);
        return mapToResponse(trip, itineraries);
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        User user = getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (!trip.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        itineraryRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);
    }

    private TripDto.TripResponse mapToResponse(Trip trip, List<Itinerary> itineraries) {
        List<TripDto.ItineraryResponse> itineraryResponses = itineraries.stream()
                .map(i -> TripDto.ItineraryResponse.builder()
                        .id(i.getId())
                        .dayNumber(i.getDayNumber())
                        .morningActivity(i.getMorningActivity())
                        .afternoonActivity(i.getAfternoonActivity())
                        .eveningActivity(i.getEveningActivity())
                        .hotelSuggestion(i.getHotelSuggestion())
                        .restaurantSuggestions(i.getRestaurantSuggestions())
                        .travelTips(i.getTravelTips())
                        .estimatedDayCost(i.getEstimatedDayCost())
                        .build())
                .collect(Collectors.toList());

        return TripDto.TripResponse.builder()
                .id(trip.getId())
                .destination(trip.getDestination())
                .durationDays(trip.getDurationDays())
                .budget(trip.getBudget())
                .currency(trip.getCurrency())
                .status(trip.getStatus().name())
                .itineraries(itineraryResponses)
                .createdAt(trip.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
