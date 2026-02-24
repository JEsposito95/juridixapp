package com.juridix.model;

import java.time.LocalDateTime;

public class DocumentoExpediente {

    private Integer id;
    private Integer expedienteId;
    private String nombreArchivo;
    private String nombreOriginal;
    private String rutaArchivo;
    private String tipoDocumento;
    private String descripcion;
    private Long tamanioBytes;
    private String extension;
    private Integer usuarioId;
    private LocalDateTime fechaSubida;

    // Constructores
    public DocumentoExpediente() {
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExpedienteId() {
        return expedienteId;
    }

    public void setExpedienteId(Integer expedienteId) {
        this.expedienteId = expedienteId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(Long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    // Método para obtener tamaño formateado
    public String getTamanioFormateado() {
        if (tamanioBytes == null) return "0 KB";

        double kb = tamanioBytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.2f KB", kb);
        }

        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    @Override
    public String toString() {
        return "DocumentoExpediente{" +
                "id=" + id +
                ", expedienteId=" + expedienteId +
                ", nombreOriginal='" + nombreOriginal + '\'' +
                ", tipoDocumento='" + tipoDocumento + '\'' +
                '}';
    }
}