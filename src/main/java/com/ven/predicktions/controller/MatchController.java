package com.ven.predicktions.controller;

import com.ven.predicktions.dto.MatchResponse;
import com.ven.predicktions.service.MatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> getMatches() {
        return matchService.findAll();
    }

    @GetMapping("/{id}")
    public MatchResponse getMatchById(@PathVariable UUID id) {
        return matchService.findById(id);
    }
}
