package com.juridix.model;

public enum TipoMovimiento {
    PRESENTACION("Presentación"),
    AUDIENCIA("Audiencia"),
    RESOLUCION("Resolución"),
    NOTIFICACION("Notificación"),
    OTRO("Otro");

    private final String displayName;

    TipoMovimiento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TipoMovimiento fromString(String text) {
        if (text != null) {
            for (TipoMovimiento tipo : TipoMovimiento.values()) {
                if (text.equalsIgnoreCase(tipo.name())) {
                    return tipo;
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