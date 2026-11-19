package com.paytrack.security;

import java.util.UUID;

/**
 * ThreadLocal qui porte l'ID du tenant courant pour la duree de la requete HTTP.
 * Alimente par JwtAuthenticationFilter, consomme par les services.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        UUID id = CURRENT_TENANT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant in context — request not authenticated?");
        }
        return id;
    }

    public static UUID getOrNull() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}