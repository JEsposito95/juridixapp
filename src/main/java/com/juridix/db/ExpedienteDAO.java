package com.juridix.db;

import com.juridix.model.Expediente;
import com.juridix.model.EstadoExpediente;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpedienteDAO {

    // ==================== CREATE ====================

    /**
     * Guarda un nuevo expediente en la base de datos
     * @param expediente El expediente a guardar
     * @return El expediente guardado con su ID generado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public Expediente guardar(Expediente expediente) throws SQLException {
        String sql = """
            INSERT INTO expedientes (
                numero, caratula, cliente, demandado, fuero, juzgado, 
                secretaria, estado, fecha_inicio, fecha_finalizacion, 
                monto_estimado, observaciones, creador_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, expediente.getNumero());
            ps.setString(2, expediente.getCaratula());
            ps.setString(3, expediente.getCliente());
            ps.setString(4, expediente.getDemandado());
            ps.setString(5, expediente.getFuero());
            ps.setString(6, expediente.getJuzgado());
            ps.setString(7, expediente.getSecretaria());
            ps.setString(8, expediente.getEstado().name());
            ps.setString(9, expediente.getFechaInicioAsString());
            ps.setString(10, expediente.getFechaFinalizacionAsString());

            if (expediente.getMontoEstimado() != null) {
                ps.setDouble(11, expediente.getMontoEstimado());
            } else {
                ps.setNull(11, Types.DOUBLE);
            }

            ps.setString(12, expediente.getObservaciones());
            ps.setInt(13, expediente.getCreadorId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el expediente");
            }

            // Obtener el ID generado
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expediente.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID del expediente");
                }
            }

            return expediente;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar expediente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    /**
     * Busca un expediente por su ID
     * @param id El ID del expediente
     * @return Optional con el expediente si existe, Optional.empty() si no
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public Optional<Expediente> buscarPorId(Integer id) throws SQLException {
        String sql = """
            SELECT * FROM expedientes WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearExpediente(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar expediente por ID: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca un expediente por su número
     * @param numero El número del expediente
     * @return Optional con el expediente si existe, Optional.empty() si no
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public Optional<Expediente> buscarPorNumero(String numero) throws SQLException {
        String sql = """
            SELECT * FROM expedientes WHERE numero = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, numero);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearExpediente(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar expediente por número: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica si existe un expediente con el número dado
     * @param numero El número a verificar
     * @return true si existe, false si no
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public boolean existeNumero(String numero) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM expedientes WHERE numero = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, numero);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error al verificar existencia de número: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica si existe un expediente con el número dado, excluyendo un ID específico
     * Útil para validar al actualizar
     */
    public boolean existeNumeroExceptoId(String numero, Integer idExcluir) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM expedientes WHERE numero = ? AND id != ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, numero);
            ps.setInt(2, idExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error al verificar existencia de número: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lista todos los expedientes
     * @return Lista de expedientes
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<Expediente> listarTodos() throws SQLException {
        String sql = """
            SELECT * FROM expedientes 
            ORDER BY fecha_creacion DESC
        """;

        List<Expediente> expedientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                expedientes.add(mapearExpediente(rs));
            }

            return expedientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar expedientes: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca expedientes por estado
     * @param estado El estado a buscar
     * @return Lista de expedientes con ese estado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<Expediente> buscarPorEstado(EstadoExpediente estado) throws SQLException {
        String sql = """
            SELECT * FROM expedientes 
            WHERE estado = ?
            ORDER BY fecha_creacion DESC
        """;

        List<Expediente> expedientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expedientes.add(mapearExpediente(rs));
                }
            }

            return expedientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por estado: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca expedientes por cliente (búsqueda parcial, case-insensitive)
     * @param cliente Nombre del cliente (o parte de él)
     * @return Lista de expedientes que coinciden
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<Expediente> buscarPorCliente(String cliente) throws SQLException {
        String sql = """
            SELECT * FROM expedientes 
            WHERE cliente LIKE ?
            ORDER BY fecha_creacion DESC
        """;

        List<Expediente> expedientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + cliente + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expedientes.add(mapearExpediente(rs));
                }
            }

            return expedientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por cliente: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca expedientes por rango de fechas
     * @param desde Fecha inicial (inclusive)
     * @param hasta Fecha final (inclusive)
     * @return Lista de expedientes en ese rango
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<Expediente> buscarPorRangoFechas(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
            SELECT * FROM expedientes 
            WHERE fecha_inicio BETWEEN ? AND ?
            ORDER BY fecha_inicio DESC
        """;

        List<Expediente> expedientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expedientes.add(mapearExpediente(rs));
                }
            }

            return expedientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por rango de fechas: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca expedientes por múltiples criterios
     * @param numero Número (puede ser null)
     * @param cliente Cliente (puede ser null)
     * @param estado Estado (puede ser null)
     * @param fuero Fuero (puede ser null)
     * @return Lista de expedientes que coinciden
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<Expediente> buscarPorCriterios(String numero, String cliente,
                                               EstadoExpediente estado, String fuero) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM expedientes WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (numero != null && !numero.trim().isEmpty()) {
            sql.append(" AND numero LIKE ?");
            parametros.add("%" + numero.trim() + "%");
        }

        if (cliente != null && !cliente.trim().isEmpty()) {
            sql.append(" AND cliente LIKE ?");
            parametros.add("%" + cliente.trim() + "%");
        }

        if (estado != null) {
            sql.append(" AND estado = ?");
            parametros.add(estado.name());
        }

        if (fuero != null && !fuero.trim().isEmpty()) {
            sql.append(" AND fuero = ?");
            parametros.add(fuero.trim());
        }

        sql.append(" ORDER BY fecha_creacion DESC");

        List<Expediente> expedientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expedientes.add(mapearExpediente(rs));
                }
            }

            return expedientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por criterios: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    /**
     * Actualiza un expediente existente
     * @param expediente El expediente con los datos actualizados
     * @return El expediente actualizado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public Expediente actualizar(Expediente expediente) throws SQLException {
        String sql = """
            UPDATE expedientes SET
                numero = ?,
                caratula = ?,
                cliente = ?,
                demandado = ?,
                fuero = ?,
                juzgado = ?,
                secretaria = ?,
                estado = ?,
                fecha_inicio = ?,
                fecha_finalizacion = ?,
                monto_estimado = ?,
                observaciones = ?,
                fecha_modificacion = datetime('now', 'localtime')
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, expediente.getNumero());
            ps.setString(2, expediente.getCaratula());
            ps.setString(3, expediente.getCliente());
            ps.setString(4, expediente.getDemandado());
            ps.setString(5, expediente.getFuero());
            ps.setString(6, expediente.getJuzgado());
            ps.setString(7, expediente.getSecretaria());
            ps.setString(8, expediente.getEstado().name());
            ps.setString(9, expediente.getFechaInicioAsString());
            ps.setString(10, expediente.getFechaFinalizacionAsString());

            if (expediente.getMontoEstimado() != null) {
                ps.setDouble(11, expediente.getMontoEstimado());
            } else {
                ps.setNull(11, Types.DOUBLE);
            }

            ps.setString(12, expediente.getObservaciones());
            ps.setInt(13, expediente.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el expediente, ID no encontrado: " + expediente.getId());
            }

            return expediente;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar expediente: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cambia el estado de un expediente
     * @param id ID del expediente
     * @param nuevoEstado Nuevo estado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public void cambiarEstado(Integer id, EstadoExpediente nuevoEstado) throws SQLException {
        String sql = """
            UPDATE expedientes 
            SET estado = ?, 
                fecha_modificacion = datetime('now', 'localtime')
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo cambiar el estado, ID no encontrado: " + id);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al cambiar estado: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    /**
     * Elimina un expediente por su ID
     * ADVERTENCIA: Esto elimina permanentemente el expediente
     * @param id ID del expediente a eliminar
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public void eliminar(Integer id) throws SQLException {
        String sql = """
            DELETE FROM expedientes WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo eliminar el expediente, ID no encontrado: " + id);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar expediente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta expedientes por estado
     * @return Total de expedientes
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM expedientes";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar expedientes: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cuenta expedientes por estado específico
     * @param estado Estado a contar
     * @return Total de expedientes en ese estado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int contarPorEstado(EstadoExpediente estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM expedientes WHERE estado = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar por estado: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Mapea un ResultSet a un objeto Expediente
     * @param rs ResultSet posicionado en una fila
     * @return Objeto Expediente mapeado
     * @throws SQLException Si ocurre un error al leer el ResultSet
     */
    private Expediente mapearExpediente(ResultSet rs) throws SQLException {
        Expediente expediente = new Expediente();

        expediente.setId(rs.getInt("id"));
        expediente.setNumero(rs.getString("numero"));
        expediente.setCaratula(rs.getString("caratula"));
        expediente.setCliente(rs.getString("cliente"));
        expediente.setDemandado(rs.getString("demandado"));
        expediente.setFuero(rs.getString("fuero"));
        expediente.setJuzgado(rs.getString("juzgado"));
        expediente.setSecretaria(rs.getString("secretaria"));

        String estadoStr = rs.getString("estado");
        expediente.setEstado(EstadoExpediente.fromString(estadoStr));

        expediente.setFechaInicioFromString(rs.getString("fecha_inicio"));
        expediente.setFechaFinalizacionFromString(rs.getString("fecha_finalizacion"));

        double monto = rs.getDouble("monto_estimado");
        if (!rs.wasNull()) {
            expediente.setMontoEstimado(monto);
        }

        expediente.setObservaciones(rs.getString("observaciones"));
        expediente.setCreadorId(rs.getInt("creador_id"));
        expediente.setFechaCreacionFromString(rs.getString("fecha_creacion"));
        expediente.setFechaModificacionFromString(rs.getString("fecha_modificacion"));

        return expediente;
    }
}