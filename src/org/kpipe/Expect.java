package org.kpipe;

public class Expect {
    public static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void isTrue(boolean value, String message) {
        if (!value) {
            fail(message);
        }
    }

}
