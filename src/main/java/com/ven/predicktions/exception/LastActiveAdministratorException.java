package com.ven.predicktions.exception;

public class LastActiveAdministratorException extends RuntimeException {

    public LastActiveAdministratorException() {
        super("Cannot disable the last active administrator.");
    }
}
