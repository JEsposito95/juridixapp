package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.DocumentoCliente;
import com.juridix.model.TipoDocumentoCliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentoClienteDAO {

    // ==================== CREATE ====================

    public DocumentoCliente guardar(DocumentoCliente documento) throws SQLException {
        String sql = """
            INSERT INTO documentos_cliente (
                cliente_id, nombre_archivo, nombre_original, ruta_archivo,
                tipo_documento, descripcion, tamanio_bytes, extension, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, documento.getClienteId());
            ps.setString(2, documento.getNombreArchivo());
            ps.setString(3, documento.getNombreOriginal());
            ps.setString(4, documento.getRutaArchivo());
            ps.setString(5, documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null);
            ps.setString(6, documento.getDescripcion());

            if (documento.getTamanioBytes() != null) {
                ps.setLong(7, documento.getTamanioBytes());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            ps.setString(8, documento.getExtension());

            if (documento.getUsuarioId() != null) {
                ps.setInt(9, documento.getUsuarioId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el documento");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    documento.setId(generatedKeys.getInt(1));
                }
            }

            return documento;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar documento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<DocumentoCliente> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM documentos_cliente WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearDocumento(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar documento: " + e.getMessage());
            throw e;
        }
    }

    public List<DocumentoCliente> listarPorCliente(Integer clienteId) throws SQLException {
        String sql = """
            SELECT * FROM documentos_cliente 
            WHERE cliente_id = ?
            ORDER BY fecha_subida DESC
        """;

        List<DocumentoCliente> documentos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    documentos.add(mapearDocumento(rs));
                }
            }

            return documentos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar documentos: " + e.getMessage());
            throw e;
        }
    }

    public List<DocumentoCliente> buscarPorTipo(Integer clienteId, TipoDocumentoCliente tipo) throws SQLException {
        String sql = """
            SELECT * FROM documentos_cliente 
            WHERE cliente_id = ? AND tipo_documento = ?
            ORDER BY fecha_subida DESC
        """;

        List<DocumentoCliente> documentos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);
            ps.setString(2, tipo.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    documentos.add(mapearDocumento(rs));
                }
            }

            return documentos;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por tipo: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public DocumentoCliente actualizar(DocumentoCliente documento) throws SQLException {
        String sql = """
            UPDATE documentos_cliente SET
                nombre_original = ?,
                tipo_documento = ?,
                descripcion = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documento.getNombreOriginal());
            ps.setString(2, documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null);
            ps.setString(3, documento.getDescripcion());
            ps.setInt(4, documento.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el documento");
            }

            return documento;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar documento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM documentos_cliente WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo eliminar el documento");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar documento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarPorCliente(Integer clienteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM documentos_cliente WHERE cliente_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar documentos: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private DocumentoCliente mapearDocumento(ResultSet rs) throws SQLException {
        DocumentoCliente doc = new DocumentoCliente();

        doc.setId(rs.getInt("id"));
        doc.setClienteId(rs.getInt("cliente_id"));
        doc.setNombreArchivo(rs.getString("nombre_archivo"));
        doc.setNombreOriginal(rs.getString("nombre_original"));
        doc.setRutaArchivo(rs.getString("ruta_archivo"));

        String tipo = rs.getString("tipo_documento");
        if (tipo != null) {
            doc.setTipoDocumento(TipoDocumentoCliente.fromString(tipo));
        }

        doc.setDescripcion(rs.getString("descripcion"));

        long tamanio = rs.getLong("tamanio_bytes");
        if (!rs.wasNull()) {
            doc.setTamanioBytes(tamanio);
        }

        doc.setExtension(rs.getString("extension"));

        int usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            doc.setUsuarioId(usuarioId);
        }

        doc.setFechaSubidaFromString(rs.getString("fecha_subida"));

        return doc;
    }
}