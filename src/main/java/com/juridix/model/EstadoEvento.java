package com.juridix.model;

public enum EstadoEvento {
    PENDIENTE("Pendiente"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String displayName;

    EstadoEvento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EstadoEvento fromString(String text) {
        if (text != null) {
            for (EstadoEvento estado : EstadoEvento.values()) {
                if (text.equalsIgnoreCase(estado.name())) {
                    return estado;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}