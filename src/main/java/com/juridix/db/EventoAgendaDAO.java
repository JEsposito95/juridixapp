package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.EventoAgenda;
import com.juridix.model.TipoEvento;
import com.juridix.model.EstadoEvento;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventoAgendaDAO {

    // ==================== CREATE ====================

    public EventoAgenda guardar(EventoAgenda evento) throws SQLException {
        String sql = """
            INSERT INTO eventos_agenda (
                titulo, descripcion, fecha_hora, duracion_minutos, tipo,
                expediente_id, ubicacion, estado, recordatorio_minutos,
                color, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, evento.getTitulo());
            ps.setString(2, evento.getDescripcion());
            ps.setString(3, evento.getFechaHoraAsString());
            ps.setInt(4, evento.getDuracionMinutos());
            ps.setString(5, evento.getTipo().name());

            if (evento.getExpedienteId() != null) {
                ps.setInt(6, evento.getExpedienteId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, evento.getUbicacion());
            ps.setString(8, evento.getEstado().name());
            ps.setInt(9, evento.getRecordatorioMinutos());
            ps.setString(10, evento.getColor());
            ps.setInt(11, evento.getUsuarioId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el evento");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    evento.setId(generatedKeys.getInt(1));
                }
            }

            return evento;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar evento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<EventoAgenda> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM eventos_agenda WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearEvento(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar evento: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarTodos() throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            ORDER BY fecha_hora DESC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                eventos.add(mapearEvento(rs));
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar eventos: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarPorUsuario(Integer usuarioId) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE usuario_id = ?
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar eventos por usuario: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarPorFecha(LocalDate fecha) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE DATE(fecha_hora) = ?
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar eventos por fecha: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarPorRangoFechas(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE DATE(fecha_hora) BETWEEN ? AND ?
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar por rango: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE expediente_id = ?
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar por expediente: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarProximos(Integer usuarioId, int dias) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE usuario_id = ? 
            AND fecha_hora >= datetime('now', 'localtime')
            AND fecha_hora <= datetime('now', 'localtime', '+' || ? || ' days')
            AND estado = 'PENDIENTE'
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, usuarioId);
            ps.setInt(2, dias);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar próximos: " + e.getMessage());
            throw e;
        }
    }

    public List<EventoAgenda> listarPendientes(Integer usuarioId) throws SQLException {
        String sql = """
            SELECT * FROM eventos_agenda 
            WHERE usuario_id = ? AND estado = 'PENDIENTE'
            ORDER BY fecha_hora ASC
        """;

        List<EventoAgenda> eventos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapearEvento(rs));
                }
            }

            return eventos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar pendientes: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public EventoAgenda actualizar(EventoAgenda evento) throws SQLException {
        String sql = """
            UPDATE eventos_agenda SET
                titulo = ?,
                descripcion = ?,
                fecha_hora = ?,
                duracion_minutos = ?,
                tipo = ?,
                expediente_id = ?,
                ubicacion = ?,
                estado = ?,
                recordatorio_minutos = ?,
                color = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, evento.getTitulo());
            ps.setString(2, evento.getDescripcion());
            ps.setString(3, evento.getFechaHoraAsString());
            ps.setInt(4, evento.getDuracionMinutos());
            ps.setString(5, evento.getTipo().name());

            if (evento.getExpedienteId() != null) {
                ps.setInt(6, evento.getExpedienteId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, evento.getUbicacion());
            ps.setString(8, evento.getEstado().name());
            ps.setInt(9, evento.getRecordatorioMinutos());
            ps.setString(10, evento.getColor());
            ps.setInt(11, evento.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el evento");
            }

            return evento;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar evento: " + e.getMessage());
            throw e;
        }
    }

    public void cambiarEstado(Integer id, EstadoEvento nuevoEstado) throws SQLException {
        String sql = "UPDATE eventos_agenda SET estado = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo cambiar el estado");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al cambiar estado: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM eventos_agenda WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo eliminar el evento");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar evento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarPorEstado(Integer usuarioId, EstadoEvento estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM eventos_agenda WHERE usuario_id = ? AND estado = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, usuarioId);
            ps.setString(2, estado.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar eventos: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private EventoAgenda mapearEvento(ResultSet rs) throws SQLException {
        EventoAgenda evento = new EventoAgenda();

        evento.setId(rs.getInt("id"));
        evento.setTitulo(rs.getString("titulo"));
        evento.setDescripcion(rs.getString("descripcion"));
        evento.setFechaHoraFromString(rs.getString("fecha_hora"));
        evento.setDuracionMinutos(rs.getInt("duracion_minutos"));
        evento.setTipo(TipoEvento.fromString(rs.getString("tipo")));

        int expedienteId = rs.getInt("expediente_id");
        if (!rs.wasNull()) {
            evento.setExpedienteId(expedienteId);
        }

        evento.setUbicacion(rs.getString("ubicacion"));
        evento.setEstado(EstadoEvento.fromString(rs.getString("estado")));
        evento.setRecordatorioMinutos(rs.getInt("recordatorio_minutos"));
        evento.setColor(rs.getString("color"));
        evento.setUsuarioId(rs.getInt("usuario_id"));
        evento.setFechaCreacionFromString(rs.getString("fecha_creacion"));

        return evento;
    }
}