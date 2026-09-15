package com.ven.predicktions.exception;

public class CannotRemoveOwnAdminRoleException extends RuntimeException {

    public CannotRemoveOwnAdminRoleException() {
        super("Administrators cannot remove their own admin role.");
    }
}
