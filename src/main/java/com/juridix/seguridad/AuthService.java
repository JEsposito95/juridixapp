package com.juridix.seguridad;

import com.juridix.model.Usuario;
import com.juridix.db.UsuarioDAO;

public class AuthService {

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Usuario login(String username, String password) {

        Usuario usuario = usuarioDAO.buscarPorUsername(username);

        if (usuario == null) {
            return null;
        }

        if (PasswordUtil.verificar(password, usuario.getPasswordHash())) {
            return usuario;
        }

        return null;
    }
}
