package com.juridix.seguridad;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verificar(String passwordPlano, String hash) {
        return BCrypt.checkpw(passwordPlano, hash);
    }
}
