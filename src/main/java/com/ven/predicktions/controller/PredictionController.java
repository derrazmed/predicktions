package com.ven.predicktions.controller;

import com.ven.predicktions.dto.prediction.CreatePredictionRequest;
import com.ven.predicktions.dto.prediction.PredictionResponse;
import com.ven.predicktions.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PredictionResponse createPrediction(
            @Valid @RequestBody CreatePredictionRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return predictionService.createPrediction(userId, request);
    }
}