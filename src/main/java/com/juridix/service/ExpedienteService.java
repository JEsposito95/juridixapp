package com.juridix.service;

import com.juridix.db.ExpedienteDAO;
import com.juridix.model.Expediente;
import com.juridix.model.Usuario;

import java.util.List;

public class ExpedienteService {

    private final ExpedienteDAO dao = new ExpedienteDAO();

    public void crear(String numero, String caratula, String cliente, Usuario creador) {
        Expediente e = new Expediente(
                numero,
                caratula,
                cliente,
                "ABIERTO",
                creador
        );
        dao.guardar(e);
    }

    public List<Expediente> listar() {
        return dao.listar();
    }
}
