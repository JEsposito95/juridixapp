package com.juridix.service;

import com.juridix.db.DocumentoClienteDAO;
import com.juridix.model.DocumentoCliente;
import com.juridix.model.TipoDocumentoCliente;

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

public class DocumentoClienteService {

    private final DocumentoClienteDAO documentoDAO;
    private static final String DIRECTORIO_BASE = "documentos/clientes";

    public DocumentoClienteService() {
        this.documentoDAO = new DocumentoClienteDAO();
        crearDirectorioBase();
    }

    public DocumentoClienteService(DocumentoClienteDAO documentoDAO) {
        this.documentoDAO = documentoDAO;
        crearDirectorioBase();
    }

    // ==================== CREATE ====================

    public DocumentoCliente subirDocumento(Integer clienteId, File archivo, TipoDocumentoCliente tipo,
                                           String descripcion, Integer usuarioId) throws SQLException, IOException {

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }

        if (archivo == null || !archivo.exists()) {
            throw new IllegalArgumentException("El archivo no existe");
        }

        if (!archivo.isFile()) {
            throw new IllegalArgumentException("Debe ser un archivo válido");
        }

        // Validar tamaño (máximo 10MB)
        long tamanioMB = archivo.length() / (1024 * 1024);
        if (tamanioMB > 10) {
            throw new IllegalArgumentException("El archivo no puede superar los 10MB");
        }

        // Crear directorio del cliente
        Path directorioCliente = Paths.get(DIRECTORIO_BASE, clienteId.toString());
        Files.createDirectories(directorioCliente);

        // Generar nombre único para el archivo
        String extension = obtenerExtension(archivo.getName());
        String nombreUnico = generarNombreUnico(extension);
        Path rutaDestino = directorioCliente.resolve(nombreUnico);

        // Copiar archivo
        Files.copy(archivo.toPath(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        // Crear registro en BD
        DocumentoCliente documento = new DocumentoCliente();
        documento.setClienteId(clienteId);
        documento.setNombreArchivo(nombreUnico);
        documento.setNombreOriginal(archivo.getName());
        documento.setRutaArchivo(rutaDestino.toString());
        documento.setTipoDocumento(tipo);
        documento.setDescripcion(descripcion);
        documento.setTamanioBytes(archivo.length());
        documento.setExtension(extension);
        documento.setUsuarioId(usuarioId);

        return documentoDAO.guardar(documento);
    }

    // ==================== READ ====================

    public Optional<DocumentoCliente> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return documentoDAO.buscarPorId(id);
    }

    public List<DocumentoCliente> listarPorCliente(Integer clienteId) throws SQLException {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }
        return documentoDAO.listarPorCliente(clienteId);
    }

    public List<DocumentoCliente> buscarPorTipo(Integer clienteId, TipoDocumentoCliente tipo) throws SQLException {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo");
        }
        return documentoDAO.buscarPorTipo(clienteId, tipo);
    }

    // ==================== UPDATE ====================

    public DocumentoCliente actualizarDocumento(DocumentoCliente documento) throws SQLException {
        if (documento == null || documento.getId() == null) {
            throw new IllegalArgumentException("El documento debe tener un ID");
        }

        Optional<DocumentoCliente> existente = documentoDAO.buscarPorId(documento.getId());
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

        Optional<DocumentoCliente> documento = documentoDAO.buscarPorId(id);
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
        Optional<DocumentoCliente> documento = documentoDAO.buscarPorId(documentoId);
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

    public int contarPorCliente(Integer clienteId) throws SQLException {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }
        return documentoDAO.contarPorCliente(clienteId);
    }

    // ==================== UTILIDADES ====================

    private void crearDirectorioBase() {
        try {
            Path path = Paths.get(DIRECTORIO_BASE);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("✅ Directorio de documentos creado: " + path.toAbsolutePath());
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
                "txt", "xls", "xlsx", "zip", "rar"
        };

        for (String ext : extensionesPermitidas) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }
}