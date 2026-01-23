package com.juridix.model;

public enum EstadoHonorario {
    PENDIENTE("Pendiente de cobro"),
    PARCIAL("Parcialmente cobrado"),
    COBRADO("Cobrado");

    private final String displayName;

    EstadoHonorario(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EstadoHonorario fromString(String text) {
        if (text != null) {
            for (EstadoHonorario estado : EstadoHonorario.values()) {
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