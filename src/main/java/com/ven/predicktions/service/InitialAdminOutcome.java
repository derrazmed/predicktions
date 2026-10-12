package com.ven.predicktions.service;

/**
 * Result of an attempt to create the initial administrator account at startup.
 */
public enum InitialAdminOutcome {

    /** An administrator account was created. */
    CREATED,

    /** At least one of the configuration values was blank, so nothing was created. */
    SKIPPED_NOT_CONFIGURED,

    /** The database already contains an administrator, so nothing was created. */
    SKIPPED_ADMIN_EXISTS,

    /** The configured email already belongs to an account, so nothing was created. */
    SKIPPED_EMAIL_IN_USE,

    /** The configured username already belongs to an account, so nothing was created. */
    SKIPPED_USERNAME_IN_USE,

    /** A concurrent instance or registration inserted the same username/email first. */
    SKIPPED_CONFLICT,

    /** The database could not be written for another reason. */
    FAILED
}
