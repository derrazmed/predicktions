package com.ven.predicktions.exception;

public class CannotDisableSelfException extends RuntimeException {

    public CannotDisableSelfException() {
        super("Administrators cannot disable their own account.");
    }
}
