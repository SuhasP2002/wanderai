package com.wanderai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanderai.dto.TripDto;
import com.wanderai.entity.Itinerary;
import com.wanderai.entity.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public List<Itinerary> generateItinerary(Trip trip) {
        String systemPrompt = """
                You are WanderAI, an expert travel planner. Generate detailed, realistic travel itineraries.
                Always respond ONLY with a valid JSON array. No extra text, no markdown, no explanation.
                Each item in the array represents one day and must have these exact fields:
                {
                  "dayNumber": <integer>,
                  "morningActivity": "<detailed description>",
                  "afternoonActivity": "<detailed description>",
                  "eveningActivity": "<detailed description>",
                  "hotelSuggestion": "<hotel name and short description>",
                  "restaurantSuggestions": "<2-3 restaurant names with cuisine type>",
                  "travelTips": "<practical tip for the day>",
                  "estimatedDayCost": <number in the trip's currency>
                }
                """;

        String userPrompt = String.format("""
                Generate a %d-day travel itinerary for %s.
                Budget: %s %s total
                Travel style: %s
                Interests: %s
                Accommodation preference: %s
                Special requests: %s
                
                Make it practical, detailed, and tailored to the budget. Return ONLY the JSON array.
                """,
                trip.getDurationDays(),
                trip.getDestination(),
                trip.getBudget(),
                trip.getCurrency(),
                extractFromPreferences(trip.getPreferences(), "travelStyle"),
                extractFromPreferences(trip.getPreferences(), "interests"),
                extractFromPreferences(trip.getPreferences(), "accommodationType"),
                extractFromPreferences(trip.getPreferences(), "specialRequests")
        );

        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            ));

            String response = chatClient.call(prompt).getResult().getOutput().getContent();
            log.info("AI response received for trip to {}", trip.getDestination());

            return parseItineraryResponse(response, trip);

        } catch (Exception e) {
            log.error("Error generating itinerary: {}", e.getMessage());
            throw new RuntimeException("Failed to generate itinerary. Please try again.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Itinerary> parseItineraryResponse(String response, Trip trip) {
        try {
            // Clean response in case AI adds extra text
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
            }

            List<Map<String, Object>> days = objectMapper.readValue(cleaned, List.class);
            List<Itinerary> itineraries = new ArrayList<>();

            for (Map<String, Object> day : days) {
                Itinerary itinerary = Itinerary.builder()
                        .trip(trip)
                        .dayNumber((Integer) day.get("dayNumber"))
                        .morningActivity((String) day.get("morningActivity"))
                        .afternoonActivity((String) day.get("afternoonActivity"))
                        .eveningActivity((String) day.get("eveningActivity"))
                        .hotelSuggestion((String) day.get("hotelSuggestion"))
                        .restaurantSuggestions((String) day.get("restaurantSuggestions"))
                        .travelTips((String) day.get("travelTips"))
                        .estimatedDayCost(day.get("estimatedDayCost") instanceof Number
                                ? ((Number) day.get("estimatedDayCost")).doubleValue() : 0.0)
                        .build();
                itineraries.add(itinerary);
            }

            return itineraries;

        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI response");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractFromPreferences(String preferencesJson, String key) {
        try {
            if (preferencesJson == null) return "not specified";
            Map<String, Object> prefs = objectMapper.readValue(preferencesJson, Map.class);
            Object value = prefs.get(key);
            return value != null ? value.toString() : "not specified";
        } catch (Exception e) {
            return "not specified";
        }
    }
}
