package org.kpipe;

public class Util {

    public static void doInterruptable(Interruptable interruptable) {
        try {
            interruptable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
