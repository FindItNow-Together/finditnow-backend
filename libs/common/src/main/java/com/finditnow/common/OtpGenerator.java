package com.finditnow.common;

import java.security.SecureRandom;

public class OtpGenerator {
    //generates 4 character long random otp
    public static String generateSecureOtp(){
        return generateSecureOtp(4);
    }

    public static String generateSecureOtp(int length) {
        SecureRandom random = new SecureRandom();
        int max = (int) Math.pow(10, length);
        int num = random.nextInt(max);  // random number from 0 to max-1
        return String.format("%0" + length + "d", num);
    }
}
