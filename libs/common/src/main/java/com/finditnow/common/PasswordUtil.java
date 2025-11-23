package com.finditnow.common;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String plainPwd) {
        return BCrypt.hashpw(plainPwd, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String plainPwd, String pwdHash) {
        return BCrypt.checkpw(plainPwd, pwdHash);
    }

    /**
     *
     * @param plainPwd plain password string
     * @return returns the boolean checking the regex for password
     */
    public static boolean checkPwdString(String plainPwd) {
        return plainPwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$\n");
    }
}

