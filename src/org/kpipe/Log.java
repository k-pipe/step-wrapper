package org.kpipe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

public class Log {

    public static final PrintStream SYS_OUT = System.out;
    public static final PrintStream SYS_ERR = System.err;

    private static PrintStream out;
    private static PrintStream err;

    private static final Supplier<String> timeSupplier = TimeFormatter.parseFormat();

    /**
     * Executes a runnable, capturing logs and error output into separate files
     *
     * @param runnable runnable to be executed
     */
    public static int doMirroringLogs(Supplier<Integer> runnable) {
        boolean createdDir = ensureOutputDir();
        int result = -1;
        try (PrintStream errorStream = new PrintStream(new FileOutputStream(Constants.WORKDIR_OUTPUT_ERROR))) {
            err = errorStream;
            try (PrintStream outStream = new PrintStream(new FileOutputStream(Constants.WORKDIR_OUTPUT_LOG))) {
                out = outStream;
                initialLogs(createdDir);
                result = runnable.get();
                finalLogs(result);
            } catch (IOException e) {
                Main.fail("Exception writing log file", e);
            }
        } catch (IOException e) {
            Main.fail("Exception writing error file", e);
        }
        out = null;
        err = null;
        return result;
    }

    private static boolean ensureOutputDir() {
        if (!new File(Constants.WORKDIR).exists()) {
            Main.fail("Workdir directory not mounted under "+Constants.WORKDIR, null);
        }
        boolean createDir = !new File(Constants.WORKDIR_OUTPUT).exists();
        if (createDir && !new File(Constants.WORKDIR_OUTPUT).mkdir()) {
            Main.fail("Could not create directory "+Constants.WORKDIR_OUTPUT, null);
        }
        return createDir;
    }

    /** A log produced by the wrapper code */
    public static void log(final String line) {
        log(out, Constants.WRAPPER_PREFIX, line);
    }

    /** A log mirroring the output of the wrapped process */
    public static void mirrorLog(final String line, boolean isErrorOut) {
        log(out, isErrorOut ? Constants.ERROR_PREFIX : Constants.OUT_PREFIX, line);
        if (isErrorOut) {
            println(err,  line);
        }
    }

    public static void command(final String line) {
        log(out, Constants.COMMAND_PREFIX, line);
        SYS_OUT.println(line);
    }

    /** issue error message to log file, error file and std error output */
    public static void error(final String line) {
        log(out, Constants.ERROR_PREFIX, line);
        println(err,  line);
        SYS_ERR.println(line);
    }

    public static void progress(final String line) {
        log(out, Constants.PROGRESS_PREFIX, line);
    }
    public static void failed(final String line) {
        log(out, Constants.FAILED_PREFIX, line);
    }

    public static void success(final String line) {
        log(out, Constants.SUCCESS_PREFIX, line);
    }

    public static void exception(Exception e) {
        e.printStackTrace(out);
        e.printStackTrace(err);
        e.printStackTrace(SYS_ERR);
    }

    private static void initialLogs(final boolean createDir) {
        log("Wrapped by "+StepWrapper.class.getPackage().getName()+":"+Constants.VERSION);
        log( "Start time: "+ Main.getStartTime());
        if (createDir) {
            log("Created directory " + Constants.WORKDIR_OUTPUT);
        }
    }

    private static void finalLogs(final int exitcode) {
        if (exitcode == 0) {
            Log.success("Succeeded");
        } else {
            Log.failed(Constants.RETURNED_EXIT_CODE + exitcode);
        }
        Log.log(Constants.TERMINATED + ZonedDateTime.now());
        Log.log("Shutting down with exit code "+ exitcode);
    }

    private static void log(final PrintStream log, final String prefix, final String line) {
        println(log == null ? SYS_ERR : log, (timeSupplier == null ? "" :  timeSupplier.get()+" ")+prefix+" "+line);
    }
    private static void println(final PrintStream out, final String line) {
        synchronized(out) {
            out.println(line);
        }
        out.flush();
    }
}

