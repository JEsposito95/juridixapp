package com.juridix.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DocumentoCliente {
    private Integer id;
    private Integer clienteId;
    private String nombreArchivo;
    private String nombreOriginal;
    private String rutaArchivo;
    private TipoDocumentoCliente tipoDocumento;
    private String descripcion;
    private Long tamanioBytes;
    private String extension;
    private Integer usuarioId;
    private LocalDateTime fechaSubida;

    // Formateador
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructores
    public DocumentoCliente() {
    }

    public DocumentoCliente(Integer clienteId, String nombreArchivo, String rutaArchivo) {
        this.clienteId = clienteId;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
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

    public TipoDocumentoCliente getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumentoCliente tipoDocumento) {
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

    // Métodos de conversión
    public String getFechaSubidaAsString() {
        return fechaSubida != null ? fechaSubida.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaSubidaFromString(String fecha) {
        this.fechaSubida = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    // Métodos de utilidad
    public String getTamanioFormateado() {
        if (tamanioBytes == null) return "0 KB";

        if (tamanioBytes < 1024) {
            return tamanioBytes + " B";
        } else if (tamanioBytes < 1024 * 1024) {
            return String.format("%.2f KB", tamanioBytes / 1024.0);
        } else {
            return String.format("%.2f MB", tamanioBytes / (1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return nombreOriginal != null ? nombreOriginal : nombreArchivo;
    }
}