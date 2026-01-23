package com.juridix.model;

public enum TipoHonorario {
    PORCENTAJE("Porcentaje sobre monto"),
    MONTO_FIJO("Monto fijo"),
    REGULACION_JUDICIAL("Regulación judicial");

    private final String displayName;

    TipoHonorario(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TipoHonorario fromString(String text) {
        if (text != null) {
            for (TipoHonorario tipo : TipoHonorario.values()) {
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