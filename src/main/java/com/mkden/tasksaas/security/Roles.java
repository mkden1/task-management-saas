package com.mkden.tasksaas.security;

/** Method-security expressions shared by the controllers. VIEWER is read-only. */
public final class Roles {

    public static final String CAN_WRITE = "hasAnyRole('ADMIN','MEMBER')";
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private Roles() {
    }
}
