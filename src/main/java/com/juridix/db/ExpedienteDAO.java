package com.juridix.db;

import com.juridix.model.Expediente;
import com.juridix.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpedienteDAO {

    public void guardar(Expediente e) {

        String sql = """
                INSERT INTO expedientes
                (numero, caratula, cliente, estado, fecha_inicio, creador_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getNumero());
            ps.setString(2, e.getCaratula());
            ps.setString(3, e.getCliente());
            ps.setString(4, e.getEstado());
            ps.setDate(5, Date.valueOf(e.getFechaInicio()));
            ps.setInt(6, e.getCreador().getId());

            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Expediente> listar() {

        List<Expediente> lista = new ArrayList<>();

        String sql = """
                SELECT e.*, u.username
                FROM expedientes e
                JOIN usuarios u ON e.creador_id = u.id
                """;

        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Usuario creador = new Usuario();
                creador.setId(rs.getInt("creador_id"));
                creador.setUsername(rs.getString("username"));

                Expediente e = new Expediente(
                        rs.getString("numero"),
                        rs.getString("caratula"),
                        rs.getString("cliente"),
                        rs.getString("estado"),
                        creador
                );

                e.setId(rs.getInt("id"));

                lista.add(e);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lista;
    }
}
