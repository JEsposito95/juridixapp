package com.juridix.model;

public enum RolUsuario {
    ADMIN,
    ABOGADO,
    SECRETARIA;

public boolean esAdmin() {
    return this == ADMIN;
}

public boolean esUsuario() {
    return this == ABOGADO || this == SECRETARIA;
}
}