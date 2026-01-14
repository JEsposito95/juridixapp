package com.juridix.model;

public class Usuario {
    private int id;
    private String username;
    private String passwordHash;
    private RolUsuario rol;
    private boolean activo;

    public Usuario() {
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public Usuario(int id, String username, String passwordHash, RolUsuario rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public Usuario(String username, String passwordHash, RolUsuario rol) {
        this(0, username, passwordHash, rol, true);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public RolUsuario getRol() { return rol; }
    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
