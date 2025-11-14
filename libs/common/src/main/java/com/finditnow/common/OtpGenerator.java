package com.finditnow.common;

import java.util.function.Supplier;

public class OtpGenerator {
    //generates 4 character long random otp
    public static int generateRandomOtp(){
        return generateRandomOtp(4);
    }

    public static int generateRandomOtp(int length){
        return (int)(Math.random()*Math.pow(10, length));
    }
}
