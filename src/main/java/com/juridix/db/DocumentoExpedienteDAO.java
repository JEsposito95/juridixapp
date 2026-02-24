package com.juridix.db;

import com.juridix.model.DocumentoExpediente;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentoExpedienteDAO {

    public DocumentoExpediente guardar(DocumentoExpediente documento) throws SQLException {
        String sql = """
            INSERT INTO documentos_expediente (
                expediente_id, nombre_archivo, nombre_original, ruta_archivo,
                tipo_documento, descripcion, tamanio_bytes, extension, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, documento.getExpedienteId());
            ps.setString(2, documento.getNombreArchivo());
            ps.setString(3, documento.getNombreOriginal());
            ps.setString(4, documento.getRutaArchivo());
            ps.setString(5, documento.getTipoDocumento());
            ps.setString(6, documento.getDescripcion());
            ps.setLong(7, documento.getTamanioBytes());
            ps.setString(8, documento.getExtension());

            if (documento.getUsuarioId() != null) {
                ps.setInt(9, documento.getUsuarioId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    documento.setId(rs.getInt(1));
                }
            }

            return documento;
        }
    }

    public Optional<DocumentoExpediente> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM documentos_expediente WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearDocumento(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<DocumentoExpediente> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = "SELECT * FROM documentos_expediente WHERE expediente_id = ? ORDER BY fecha_subida DESC";
        List<DocumentoExpediente> documentos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    documentos.add(mapearDocumento(rs));
                }
            }
        }

        return documentos;
    }

    public List<DocumentoExpediente> buscarPorTipo(Integer expedienteId, String tipoDocumento) throws SQLException {
        String sql = "SELECT * FROM documentos_expediente WHERE expediente_id = ? AND tipo_documento = ? ORDER BY fecha_subida DESC";
        List<DocumentoExpediente> documentos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);
            ps.setString(2, tipoDocumento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    documentos.add(mapearDocumento(rs));
                }
            }
        }

        return documentos;
    }

    public DocumentoExpediente actualizar(DocumentoExpediente documento) throws SQLException {
        String sql = """
            UPDATE documentos_expediente SET
                tipo_documento = ?, descripcion = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documento.getTipoDocumento());
            ps.setString(2, documento.getDescripcion());
            ps.setInt(3, documento.getId());

            ps.executeUpdate();
            return documento;
        }
    }

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM documentos_expediente WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int contarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM documentos_expediente WHERE expediente_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private DocumentoExpediente mapearDocumento(ResultSet rs) throws SQLException {
        DocumentoExpediente doc = new DocumentoExpediente();

        doc.setId(rs.getInt("id"));
        doc.setExpedienteId(rs.getInt("expediente_id"));
        doc.setNombreArchivo(rs.getString("nombre_archivo"));
        doc.setNombreOriginal(rs.getString("nombre_original"));
        doc.setRutaArchivo(rs.getString("ruta_archivo"));
        doc.setTipoDocumento(rs.getString("tipo_documento"));
        doc.setDescripcion(rs.getString("descripcion"));
        doc.setTamanioBytes(rs.getLong("tamanio_bytes"));
        doc.setExtension(rs.getString("extension"));

        Integer usuarioId = (Integer) rs.getObject("usuario_id");
        doc.setUsuarioId(usuarioId);

        String fechaSubida = rs.getString("fecha_subida");
        if (fechaSubida != null) {
            doc.setFechaSubida(LocalDateTime.parse(fechaSubida.replace(" ", "T")));
        }

        return doc;
    }
}