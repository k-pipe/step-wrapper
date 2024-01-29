package org.kpipe;

public class Main {

    private static long starttime;

    public static void main(String... args)  {
        starttime = System.currentTimeMillis();
        int exitCode = Log.doMirroringLogs(() -> new StepWrapper().execute(args));
        if (exitCode != 0) {
            System.out.println("Exiting with error code "+exitCode);
            System.exit(exitCode);
        } else {
            System.out.println("Terminating main thread normally");
        }
    }

    public static long getStartTime() {
        return starttime;
    }

    public static void fail(String message) {
        fail(message, null);
    }

    public static void fail(String message, Exception e) {
        Log.error(message);
        if (e != null) {
            Log.exception(e);
        }
        System.exit(-1);
    }

}
