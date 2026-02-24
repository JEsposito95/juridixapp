package com.juridix.service;

import com.juridix.db.Database;
import com.juridix.db.DocumentoExpedienteDAO;
import com.juridix.model.DocumentoExpediente;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DocumentoExpedienteService {

    private final DocumentoExpedienteDAO documentoDAO;
    private static final String DIRECTORIO_BASE = Database.getAppDirectory() + File.separator + "documentos" + File.separator + "expedientes";

    public DocumentoExpedienteService() {
        this.documentoDAO = new DocumentoExpedienteDAO();
        crearDirectorioBase();
    }

    public DocumentoExpedienteService(DocumentoExpedienteDAO documentoDAO) {
        this.documentoDAO = documentoDAO;
        crearDirectorioBase();
    }

    // ==================== CREATE ====================

    public DocumentoExpediente subirDocumento(Integer expedienteId, File archivo, String tipoDocumento,
                                              String descripcion, Integer usuarioId) throws SQLException, IOException {

        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }

        if (archivo == null || !archivo.exists()) {
            throw new IllegalArgumentException("El archivo no existe");
        }

        if (!archivo.isFile()) {
            throw new IllegalArgumentException("Debe ser un archivo válido");
        }

        // Validar tamaño (máximo 50MB para documentos)
        long tamanioMB = archivo.length() / (1024 * 1024);
        if (tamanioMB > 50) {
            throw new IllegalArgumentException("El archivo no puede superar los 50MB");
        }

        // Crear directorio del expediente
        Path directorioExpediente = Paths.get(DIRECTORIO_BASE, expedienteId.toString());
        Files.createDirectories(directorioExpediente);

        // Generar nombre único para el archivo
        String extension = obtenerExtension(archivo.getName());
        String nombreUnico = generarNombreUnico(extension);
        Path rutaDestino = directorioExpediente.resolve(nombreUnico);

        // Copiar archivo
        Files.copy(archivo.toPath(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        // Crear registro en BD
        DocumentoExpediente documento = new DocumentoExpediente();
        documento.setExpedienteId(expedienteId);
        documento.setNombreArchivo(nombreUnico);
        documento.setNombreOriginal(archivo.getName());
        documento.setRutaArchivo(rutaDestino.toString());
        documento.setTipoDocumento(tipoDocumento);
        documento.setDescripcion(descripcion);
        documento.setTamanioBytes(archivo.length());
        documento.setExtension(extension);
        documento.setUsuarioId(usuarioId);

        return documentoDAO.guardar(documento);
    }

    // ==================== READ ====================

    public Optional<DocumentoExpediente> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return documentoDAO.buscarPorId(id);
    }

    public List<DocumentoExpediente> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return documentoDAO.listarPorExpediente(expedienteId);
    }

    public List<DocumentoExpediente> buscarPorTipo(Integer expedienteId, String tipo) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo");
        }
        return documentoDAO.buscarPorTipo(expedienteId, tipo);
    }

    // ==================== UPDATE ====================

    public DocumentoExpediente actualizarDocumento(DocumentoExpediente documento) throws SQLException {
        if (documento == null || documento.getId() == null) {
            throw new IllegalArgumentException("El documento debe tener un ID");
        }

        Optional<DocumentoExpediente> existente = documentoDAO.buscarPorId(documento.getId());
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No existe el documento con ID: " + documento.getId());
        }

        return documentoDAO.actualizar(documento);
    }

    // ==================== DELETE ====================

    public void eliminarDocumento(Integer id) throws SQLException, IOException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<DocumentoExpediente> documento = documentoDAO.buscarPorId(id);
        if (documento.isEmpty()) {
            throw new IllegalArgumentException("No existe el documento con ID: " + id);
        }

        // Eliminar archivo físico
        Path rutaArchivo = Paths.get(documento.get().getRutaArchivo());
        if (Files.exists(rutaArchivo)) {
            Files.delete(rutaArchivo);
        }

        // Eliminar registro de BD
        documentoDAO.eliminar(id);
    }

    // ==================== OPERACIONES DE ARCHIVO ====================

    public File obtenerArchivo(Integer documentoId) throws SQLException, IOException {
        Optional<DocumentoExpediente> documento = documentoDAO.buscarPorId(documentoId);
        if (documento.isEmpty()) {
            throw new IllegalArgumentException("No existe el documento");
        }

        File archivo = new File(documento.get().getRutaArchivo());
        if (!archivo.exists()) {
            throw new IOException("El archivo físico no existe");
        }

        return archivo;
    }

    public void abrirDocumento(Integer documentoId) throws SQLException, IOException {
        File archivo = obtenerArchivo(documentoId);

        // Abrir con la aplicación predeterminada del sistema
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (archivo.exists()) {
                desktop.open(archivo);
            }
        } else {
            throw new UnsupportedOperationException("No se puede abrir el archivo en este sistema");
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return documentoDAO.contarPorExpediente(expedienteId);
    }

    // ==================== UTILIDADES ====================

    private void crearDirectorioBase() {
        try {
            Path path = Paths.get(DIRECTORIO_BASE);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("✅ Directorio de documentos de expedientes creado: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Error al crear directorio base: " + e.getMessage());
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        int lastIndexOf = nombreArchivo.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return nombreArchivo.substring(lastIndexOf + 1).toLowerCase();
    }

    private String generarNombreUnico(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    public boolean esExtensionPermitida(String extension) {
        String[] extensionesPermitidas = {
                "pdf", "doc", "docx", "jpg", "jpeg", "png", "gif",
                "txt", "xls", "xlsx", "zip", "rar", "odt", "rtf"
        };

        for (String ext : extensionesPermitidas) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }
}