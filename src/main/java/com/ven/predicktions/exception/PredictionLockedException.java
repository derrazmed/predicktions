package com.ven.predicktions.exception;

public class PredictionLockedException extends RuntimeException {

    public PredictionLockedException(String message) {
        super(message);
    }
}