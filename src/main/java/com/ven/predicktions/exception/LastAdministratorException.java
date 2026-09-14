package com.ven.predicktions.exception;

public class LastAdministratorException extends RuntimeException {

    public LastAdministratorException() {
        super("Cannot remove the last administrator.");
    }
}
