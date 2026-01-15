package com.juridix.service;

import com.juridix.db.ExpedienteDAO;
import com.juridix.model.Expediente;
import com.juridix.model.EstadoExpediente;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para Expedientes
 * Maneja validaciones y operaciones complejas
 */
public class ExpedienteService {

    private final ExpedienteDAO expedienteDAO;

    public ExpedienteService() {
        this.expedienteDAO = new ExpedienteDAO();
    }

    // Constructor alternativo para inyección de dependencias (útil para testing)
    public ExpedienteService(ExpedienteDAO expedienteDAO) {
        this.expedienteDAO = expedienteDAO;
    }

    // ==================== CREATE ====================

    /**
     * Crea un nuevo expediente con validaciones de negocio
     * @param expediente El expediente a crear
     * @return El expediente creado con su ID
     * @throws IllegalArgumentException Si los datos no son válidos
     * @throws SQLException Si hay un error de base de datos
     */
    public Expediente crearExpediente(Expediente expediente) throws SQLException {
        // Validaciones de negocio
        validarExpediente(expediente);

        // Verificar que no exista el número
        if (expedienteDAO.existeNumero(expediente.getNumero())) {
            throw new IllegalArgumentException("Ya existe un expediente con el número: " + expediente.getNumero());
        }

        // Establecer valores por defecto si no están
        if (expediente.getEstado() == null) {
            expediente.setEstado(EstadoExpediente.ACTIVO);
        }

        if (expediente.getFechaInicio() == null) {
            expediente.setFechaInicio(LocalDate.now());
        }

        // Guardar en la base de datos
        return expedienteDAO.guardar(expediente);
    }

    // ==================== READ ====================

    /**
     * Busca un expediente por su ID
     * @param id El ID del expediente
     * @return Optional con el expediente si existe
     * @throws SQLException Si hay un error de base de datos
     */
    public Optional<Expediente> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return expedienteDAO.buscarPorId(id);
    }

    /**
     * Busca un expediente por su número
     * @param numero El número del expediente
     * @return Optional con el expediente si existe
     * @throws SQLException Si hay un error de base de datos
     */
    public Optional<Expediente> buscarPorNumero(String numero) throws SQLException {
        if (numero == null || numero.trim().isEmpty()) {
            throw new IllegalArgumentException("El número no puede estar vacío");
        }
        return expedienteDAO.buscarPorNumero(numero.trim().toUpperCase());
    }

    /**
     * Lista todos los expedientes
     * @return Lista de todos los expedientes
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> listarTodos() throws SQLException {
        return expedienteDAO.listarTodos();
    }

    /**
     * Lista solo los expedientes activos
     * @return Lista de expedientes activos
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> listarActivos() throws SQLException {
        return expedienteDAO.buscarPorEstado(EstadoExpediente.ACTIVO);
    }

    /**
     * Lista expedientes por estado
     * @param estado El estado a filtrar
     * @return Lista de expedientes con ese estado
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> listarPorEstado(EstadoExpediente estado) throws SQLException {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        return expedienteDAO.buscarPorEstado(estado);
    }

    /**
     * Busca expedientes por cliente
     * @param cliente Nombre del cliente (búsqueda parcial)
     * @return Lista de expedientes que coinciden
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> buscarPorCliente(String cliente) throws SQLException {
        if (cliente == null || cliente.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        return expedienteDAO.buscarPorCliente(cliente.trim());
    }

    /**
     * Busca expedientes en un rango de fechas
     * @param desde Fecha inicial
     * @param hasta Fecha final
     * @return Lista de expedientes en ese rango
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> buscarPorRangoFechas(LocalDate desde, LocalDate hasta) throws SQLException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return expedienteDAO.buscarPorRangoFechas(desde, hasta);
    }

    /**
     * Búsqueda avanzada por múltiples criterios
     * @param numero Número (puede ser null)
     * @param cliente Cliente (puede ser null)
     * @param estado Estado (puede ser null)
     * @param fuero Fuero (puede ser null)
     * @return Lista de expedientes que coinciden
     * @throws SQLException Si hay un error de base de datos
     */
    public List<Expediente> buscarPorCriterios(String numero, String cliente,
                                               EstadoExpediente estado, String fuero) throws SQLException {
        return expedienteDAO.buscarPorCriterios(numero, cliente, estado, fuero);
    }

    // ==================== UPDATE ====================

    /**
     * Actualiza un expediente existente
     * @param expediente El expediente con los datos actualizados
     * @return El expediente actualizado
     * @throws SQLException Si hay un error de base de datos
     */
    public Expediente actualizarExpediente(Expediente expediente) throws SQLException {
        // Validaciones
        validarExpediente(expediente);

        if (expediente.getId() == null) {
            throw new IllegalArgumentException("No se puede actualizar un expediente sin ID");
        }

        // Verificar que el expediente existe
        Optional<Expediente> existente = expedienteDAO.buscarPorId(expediente.getId());
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No existe un expediente con el ID: " + expediente.getId());
        }

        // Verificar que no haya otro expediente con el mismo número
        if (expedienteDAO.existeNumeroExceptoId(expediente.getNumero(), expediente.getId())) {
            throw new IllegalArgumentException("Ya existe otro expediente con el número: " + expediente.getNumero());
        }

        // Actualizar
        return expedienteDAO.actualizar(expediente);
    }

    /**
     * Cambia el estado de un expediente
     * @param id ID del expediente
     * @param nuevoEstado Nuevo estado
     * @throws SQLException Si hay un error de base de datos
     */
    public void cambiarEstado(Integer id, EstadoExpediente nuevoEstado) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        // Verificar que existe
        Optional<Expediente> expediente = expedienteDAO.buscarPorId(id);
        if (expediente.isEmpty()) {
            throw new IllegalArgumentException("No existe un expediente con el ID: " + id);
        }

        expedienteDAO.cambiarEstado(id, nuevoEstado);
    }

    /**
     * Archiva un expediente (cambia su estado a ARCHIVADO)
     * @param id ID del expediente
     * @throws SQLException Si hay un error de base de datos
     */
    public void archivarExpediente(Integer id) throws SQLException {
        cambiarEstado(id, EstadoExpediente.ARCHIVADO);
    }

    /**
     * Reactiva un expediente archivado
     * @param id ID del expediente
     * @throws SQLException Si hay un error de base de datos
     */
    public void reactivarExpediente(Integer id) throws SQLException {
        cambiarEstado(id, EstadoExpediente.ACTIVO);
    }

    /**
     * Finaliza un expediente
     * @param id ID del expediente
     * @throws SQLException Si hay un error de base de datos
     */
    public void finalizarExpediente(Integer id) throws SQLException {
        // Buscar el expediente
        Optional<Expediente> optExpediente = expedienteDAO.buscarPorId(id);
        if (optExpediente.isEmpty()) {
            throw new IllegalArgumentException("No existe un expediente con el ID: " + id);
        }

        Expediente expediente = optExpediente.get();
        expediente.setEstado(EstadoExpediente.FINALIZADO);
        expediente.setFechaFinalizacion(LocalDate.now());

        expedienteDAO.actualizar(expediente);
    }

    // ==================== DELETE ====================

    /**
     * Elimina un expediente de forma permanente
     * ADVERTENCIA: Esta acción no se puede deshacer
     * @param id ID del expediente a eliminar
     * @throws SQLException Si hay un error de base de datos
     */
    public void eliminarExpediente(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        // Verificar que existe
        Optional<Expediente> expediente = expedienteDAO.buscarPorId(id);
        if (expediente.isEmpty()) {
            throw new IllegalArgumentException("No existe un expediente con el ID: " + id);
        }

        // En lugar de eliminar, se podría archivar
        // Pero si realmente quieres eliminar:
        expedienteDAO.eliminar(id);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtiene el total de expedientes
     * @return Total de expedientes
     * @throws SQLException Si hay un error de base de datos
     */
    public int contarTotal() throws SQLException {
        return expedienteDAO.contarTotal();
    }

    /**
     * Cuenta expedientes por estado
     * @param estado Estado a contar
     * @return Total de expedientes en ese estado
     * @throws SQLException Si hay un error de base de datos
     */
    public int contarPorEstado(EstadoExpediente estado) throws SQLException {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        return expedienteDAO.contarPorEstado(estado);
    }

    /**
     * Obtiene estadísticas resumidas de expedientes
     * @return Objeto con estadísticas
     * @throws SQLException Si hay un error de base de datos
     */
    public EstadisticasExpedientes obtenerEstadisticas() throws SQLException {
        return new EstadisticasExpedientes(
                expedienteDAO.contarTotal(),
                expedienteDAO.contarPorEstado(EstadoExpediente.ACTIVO),
                expedienteDAO.contarPorEstado(EstadoExpediente.ARCHIVADO),
                expedienteDAO.contarPorEstado(EstadoExpediente.SUSPENDIDO),
                expedienteDAO.contarPorEstado(EstadoExpediente.FINALIZADO)
        );
    }

    // ==================== VALIDACIONES ====================

    /**
     * Valida que un expediente tenga todos los datos obligatorios
     * @param expediente El expediente a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validarExpediente(Expediente expediente) {
        if (expediente == null) {
            throw new IllegalArgumentException("El expediente no puede ser nulo");
        }

        // Número
        if (expediente.getNumero() == null || expediente.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("El número de expediente es obligatorio");
        }

        // Carátula
        if (expediente.getCaratula() == null || expediente.getCaratula().trim().isEmpty()) {
            throw new IllegalArgumentException("La carátula es obligatoria");
        }

        // Cliente
        if (expediente.getCliente() == null || expediente.getCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }

        // Fecha de inicio
        if (expediente.getFechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }

        // No permitir fechas futuras
        if (expediente.getFechaInicio().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser futura");
        }

        // Validar fecha de finalización si existe
        if (expediente.getFechaFinalizacion() != null) {
            if (expediente.getFechaFinalizacion().isBefore(expediente.getFechaInicio())) {
                throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio");
            }
        }

        // Validar monto si existe
        if (expediente.getMontoEstimado() != null && expediente.getMontoEstimado() < 0) {
            throw new IllegalArgumentException("El monto estimado no puede ser negativo");
        }

        // Creador ID
        if (expediente.getCreadorId() == null) {
            throw new IllegalArgumentException("El expediente debe tener un creador asignado");
        }
    }

    // ==================== CLASE INTERNA PARA ESTADÍSTICAS ====================

    /**
     * Clase para encapsular estadísticas de expedientes
     */
    public static class EstadisticasExpedientes {
        private final int total;
        private final int activos;
        private final int archivados;
        private final int suspendidos;
        private final int finalizados;

        public EstadisticasExpedientes(int total, int activos, int archivados,
                                       int suspendidos, int finalizados) {
            this.total = total;
            this.activos = activos;
            this.archivados = archivados;
            this.suspendidos = suspendidos;
            this.finalizados = finalizados;
        }

        public int getTotal() { return total; }
        public int getActivos() { return activos; }
        public int getArchivados() { return archivados; }
        public int getSuspendidos() { return suspendidos; }
        public int getFinalizados() { return finalizados; }

        @Override
        public String toString() {
            return String.format(
                    "Total: %d | Activos: %d | Archivados: %d | Suspendidos: %d | Finalizados: %d",
                    total, activos, archivados, suspendidos, finalizados
            );
        }
    }
}