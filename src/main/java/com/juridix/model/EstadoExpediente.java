package com.juridix.model;

public enum EstadoExpediente {
    ACTIVO("Activo"),
    ARCHIVADO("Archivado"),
    SUSPENDIDO("Suspendido"),
    FINALIZADO("Finalizado");

    private final String displayName;

    EstadoExpediente(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Método para obtener el enum desde string
    public static EstadoExpediente fromString(String text) {
        if (text != null) {
            for (EstadoExpediente estado : EstadoExpediente.values()) {
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