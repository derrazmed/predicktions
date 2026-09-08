package com.ven.predicktions.controller;

import com.ven.predicktions.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/errors")
class ExceptionTestController {

    @GetMapping("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException("Test resource not found");
    }

    @PostMapping("/validation")
    public void validation(@Valid @RequestBody TestRequest request) {
    }

    record TestRequest(
            @NotBlank String name
    ) {
    }

    @GetMapping("/internal")
    public void internal() {
        throw new RuntimeException("THIS MUST NOT BE EXPOSED");
    }
}