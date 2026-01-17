package com.juridix.model;

public enum TipoDocumentoCliente {
    DNI("DNI/Documento"),
    PODER("Poder"),
    CUIT("Constancia CUIT/CUIL"),
    CONTRATO("Contrato de Honorarios"),
    CERTIFICADO("Certificado"),
    ESCRITURA("Escritura"),
    OTRO("Otro");

    private final String displayName;

    TipoDocumentoCliente(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TipoDocumentoCliente fromString(String text) {
        if (text != null) {
            for (TipoDocumentoCliente tipo : TipoDocumentoCliente.values()) {
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