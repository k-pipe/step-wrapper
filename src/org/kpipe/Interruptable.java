package org.kpipe;

@FunctionalInterface
interface Interruptable {
    void run() throws InterruptedException;
}
