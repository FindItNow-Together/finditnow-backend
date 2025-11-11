package com.finditnow.common;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String plainPwd) {
        return BCrypt.hashpw(plainPwd, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String plainPwd, String pwdHash) {
        return BCrypt.checkpw(plainPwd, pwdHash);
    }
}

