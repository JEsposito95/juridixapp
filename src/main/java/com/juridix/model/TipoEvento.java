package com.juridix.model;

public enum TipoEvento {
    AUDIENCIA("Audiencia", "#e74c3c"),
    VENCIMIENTO("Vencimiento", "#f39c12"),
    REUNION("Reunión", "#3498db"),
    PRESENTACION("Presentación", "#9b59b6"),
    OTRO("Otro", "#95a5a6");

    private final String displayName;
    private final String colorPredeterminado;

    TipoEvento(String displayName, String colorPredeterminado) {
        this.displayName = displayName;
        this.colorPredeterminado = colorPredeterminado;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorPredeterminado() {
        return colorPredeterminado;
    }

    public static TipoEvento fromString(String text) {
        if (text != null) {
            for (TipoEvento tipo : TipoEvento.values()) {
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