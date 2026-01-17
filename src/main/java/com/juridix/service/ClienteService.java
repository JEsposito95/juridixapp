package com.juridix.service;

import com.juridix.db.ClienteDAO;
import com.juridix.model.Cliente;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClienteService {

    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this.clienteDAO = new ClienteDAO();
    }

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    // ==================== CREATE ====================

    public Cliente crearCliente(Cliente cliente) throws SQLException {
        validarCliente(cliente);

        // Verificar si ya existe un cliente con el mismo DNI
        if (cliente.getDni() != null && !cliente.getDni().trim().isEmpty()) {
            Optional<Cliente> existente = clienteDAO.buscarPorDni(cliente.getDni());
            if (existente.isPresent()) {
                throw new IllegalArgumentException("Ya existe un cliente con el DNI: " + cliente.getDni());
            }
        }

        return clienteDAO.guardar(cliente);
    }

    // ==================== READ ====================

    public Optional<Cliente> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return clienteDAO.buscarPorId(id);
    }

    public Optional<Cliente> buscarPorDni(String dni) throws SQLException {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        return clienteDAO.buscarPorDni(dni.trim());
    }

    public List<Cliente> listarTodos() throws SQLException {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> listarActivos() throws SQLException {
        return clienteDAO.listarActivos();
    }

    public List<Cliente> buscarPorNombre(String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        return clienteDAO.buscarPorNombre(nombre.trim());
    }

    public List<Cliente> buscarPorCriterios(String busqueda, Boolean soloActivos) throws SQLException {
        return clienteDAO.buscarPorCriterios(busqueda, soloActivos);
    }

    // ==================== UPDATE ====================

    public Cliente actualizarCliente(Cliente cliente) throws SQLException {
        validarCliente(cliente);

        if (cliente.getId() == null) {
            throw new IllegalArgumentException("El cliente debe tener un ID para actualizarse");
        }

        Optional<Cliente> existente = clienteDAO.buscarPorId(cliente.getId());
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No existe un cliente con el ID: " + cliente.getId());
        }

        // Verificar DNI duplicado (excepto el mismo cliente)
        if (cliente.getDni() != null && !cliente.getDni().trim().isEmpty()) {
            Optional<Cliente> otroCLiente = clienteDAO.buscarPorDni(cliente.getDni());
            if (otroCLiente.isPresent() && !otroCLiente.get().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("Ya existe otro cliente con el DNI: " + cliente.getDni());
            }
        }

        return clienteDAO.actualizar(cliente);
    }

    public void desactivarCliente(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<Cliente> cliente = clienteDAO.buscarPorId(id);
        if (cliente.isEmpty()) {
            throw new IllegalArgumentException("No existe un cliente con el ID: " + id);
        }

        clienteDAO.cambiarEstado(id, false);
    }

    public void activarCliente(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<Cliente> cliente = clienteDAO.buscarPorId(id);
        if (cliente.isEmpty()) {
            throw new IllegalArgumentException("No existe un cliente con el ID: " + id);
        }

        clienteDAO.cambiarEstado(id, true);
    }

    // ==================== DELETE ====================

    public void eliminarCliente(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<Cliente> cliente = clienteDAO.buscarPorId(id);
        if (cliente.isEmpty()) {
            throw new IllegalArgumentException("No existe un cliente con el ID: " + id);
        }

        // Aquí podrías verificar si tiene expedientes asociados antes de eliminar
        // Por ahora lo dejamos simple

        clienteDAO.eliminar(id);
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarTotal() throws SQLException {
        return clienteDAO.contarTotal();
    }

    public int contarActivos() throws SQLException {
        return clienteDAO.contarActivos();
    }

    public EstadisticasClientes obtenerEstadisticas() throws SQLException {
        int total = clienteDAO.contarTotal();
        int activos = clienteDAO.contarActivos();
        int inactivos = total - activos;

        return new EstadisticasClientes(total, activos, inactivos);
    }

    // ==================== VALIDACIONES ====================

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }

        if (cliente.getNombreCompleto() == null || cliente.getNombreCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }

        // Validar email si existe
        if (cliente.getEmail() != null && !cliente.getEmail().trim().isEmpty()) {
            if (!cliente.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("El email no tiene un formato válido");
            }
        }

        // Validar DNI si existe
        if (cliente.getDni() != null && !cliente.getDni().trim().isEmpty()) {
            String dni = cliente.getDni().replaceAll("[^0-9]", "");
            if (dni.length() < 7 || dni.length() > 8) {
                throw new IllegalArgumentException("El DNI debe tener entre 7 y 8 dígitos");
            }
        }

        // Validar CUIT si existe
        if (cliente.getCuitCuil() != null && !cliente.getCuitCuil().trim().isEmpty()) {
            String cuit = cliente.getCuitCuil().replaceAll("[^0-9]", "");
            if (cuit.length() != 11) {
                throw new IllegalArgumentException("El CUIT/CUIL debe tener 11 dígitos");
            }
        }
    }

    // ==================== CLASE INTERNA ====================

    public static class EstadisticasClientes {
        private final int total;
        private final int activos;
        private final int inactivos;

        public EstadisticasClientes(int total, int activos, int inactivos) {
            this.total = total;
            this.activos = activos;
            this.inactivos = inactivos;
        }

        public int getTotal() { return total; }
        public int getActivos() { return activos; }
        public int getInactivos() { return inactivos; }

        @Override
        public String toString() {
            return String.format("Total: %d | Activos: %d | Inactivos: %d", total, activos, inactivos);
        }
    }
}