package com.wanderai.controller;

import com.wanderai.dto.TripDto;
import com.wanderai.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping("/generate")
    public ResponseEntity<TripDto.TripResponse> generateTrip(@Valid @RequestBody TripDto.TripRequest request) {
        return ResponseEntity.ok(tripService.createAndGenerateTrip(request));
    }

    @GetMapping
    public ResponseEntity<List<TripDto.TripResponse>> getUserTrips() {
        return ResponseEntity.ok(tripService.getUserTrips());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripDto.TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.ok("Trip deleted successfully");
    }
}
