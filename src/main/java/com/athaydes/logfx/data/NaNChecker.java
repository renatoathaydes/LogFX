package com.athaydes.logfx.data;

public class NaNChecker {
    public static double checkNaN( double n ) throws NaNException {
        if ( Double.isNaN( n ) ) {
            throw new NaNException();
        }
        return n;
    }

    public static final class NaNException extends Exception {
    }
}
