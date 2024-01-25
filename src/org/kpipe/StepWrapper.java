package org.kpipe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class StepWrapper {

    public static final PrintStream OUT = System.out;
    public static final PrintStream ERR = System.err;
    private static final long READER_THREAD_JOIN_TIMEOUT_SECONDS = 5;

    public enum WrapperState {
        RUNNING, SUCCESS, ERROR, LOG_TIMEOUT, OVERALL_TIMEOUT, POD_SHUTTING_DOWN
    }

    public static final String WORKDIR = "/workdir";
    public static final String WORKDIR_STATE = WORKDIR+"/state";
    public static final String WORKDIR_DONE = WORKDIR+"/done";
    public static final String WORKDIR_OUTPUT = WORKDIR+"/output";
    public static final String WORKDIR_OUTPUT_LOG = WORKDIR_OUTPUT+"/log";
    public static final String WORKDIR_OUTPUT_ERROR = WORKDIR_OUTPUT+"/error";
    public static final String META_STARTED = "Started ";

    public static final String WRAPPER_PREFIX = "WRAPPER>";
    public static final String LOG_PREFIX = "OUT>";
    public static final String ERROR_PREFIX = "ERROR>";
    public static final String PROGRESS_PREFIX = "PROGRESS>";
    public static final String FAILED_PREFIX = "FAILED>";
    public static final String SUCCESS_PREFIX = "SUCCESS>";

    public static final String RETURNED_EXIT_CODE = "Returned exit code ";
    public static final String TERMINATED = "Terminated ";
    private static final long WAIT_TIME_OUT = 10;
    public static final String TIME_FORMAT = "TIME_FORMAT";
    public static final String MILLIS = "UNIX_MILLIS";
    public static final String RELATIVE_TIME_FORMAT = "SECONDS_SINCE_START";
    private static final String LOG_TIMEOUT_MINUTES = "LOG_TIMEOUT_MINUTES";
    private static final String OVERALL_TIMEOUT_MINUTES = "LOG_TIMEOUT_MINUTES";
    private static final long WATCHDOG_SLEEP = 30000;
    private static final String ISO = "ISO";

    private static long starttime;
    private static long lastLogTime;
    private static WrapperState state;

    private static Supplier<String> timeSupplier;
    private static int logTimeoutMinutes;
    private static int overallTimeoutMinutes;

    public static void main(String... args)  {
        boolean createDir = initialize(args);
        int exitcode;
        try (PrintStream log = new PrintStream(new FileOutputStream(WORKDIR_OUTPUT_LOG))) {
            try (PrintStream error = new PrintStream(new FileOutputStream(WORKDIR_OUTPUT_ERROR))) {
                exitcode = runProcess(createDir, log, error, args);
                if (exitcode != 0) {
                    System.out.println("Exiting with code "+exitcode);
                    System.exit(exitcode);
                } else {
                    System.out.println("Ending main thread");
                }
            }
        } catch (IOException e) {
            fail("Exception writing log files", e);
        }
    }

    private static int runProcess(final boolean createDir, final PrintStream log,
                                  final PrintStream error, final String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);
        initialLogs(createDir, log);
        Process process = createProcess(log, args);
        if (process == null) {
            return -1;
        }
        installShutdownHandler(log, process, running);
        if ((overallTimeoutMinutes > 0) || (logTimeoutMinutes > 0)) {
            installWatchdog(running, log, process);
        }
        state(log, WrapperState.RUNNING);
        final Thread inReader = createPipeReader(process.getInputStream(), LOG_PREFIX, log, OUT);
        final Thread errReader = createPipeReader(process.getErrorStream(), ERROR_PREFIX, log, ERR, error);
        doInterruptable(process::waitFor);
        running.set(false);
        int exitcode = process.exitValue();
        if (state.equals(WrapperState.RUNNING)) {
            // was not stopped, determine result based on exit code
            state(log, exitcode == 0 ? WrapperState.SUCCESS : WrapperState.ERROR);
        }
        waitFor(log,"output", inReader);
        waitFor(log,"error", errReader);
        finalLogs(exitcode, log);
        done();
        return exitcode;
    }

    private static void waitFor(final PrintStream log, final String name, final Thread readerThread) {
        log(log, "Joining "+name+" reader thread");
        //System.out.println("Joining "+name+" reader thread");
        long timeStamp = System.currentTimeMillis();
        doInterruptable(() -> readerThread.join(READER_THREAD_JOIN_TIMEOUT_SECONDS*1000));
        log(log, "Joined "+name+" reader thread after "+(System.currentTimeMillis()-timeStamp)+" ms");
        //System.out.println("Joined "+name+" reader thread after "+(System.currentTimeMillis()-timeStamp)+" ms");
    }

    private static void finalLogs(final int exitcode, final PrintStream log) {
        if (exitcode == 0) {
            log(log, SUCCESS_PREFIX, "Succeeded");
        } else {
            log(log, FAILED_PREFIX, RETURNED_EXIT_CODE + exitcode);
        }
        log(log, TERMINATED + ZonedDateTime.now());
        log(log, "Shutting down with exit code "+ exitcode);
    }

    private static void initialLogs(final boolean createDir, final PrintStream log) {
        String info = "Wrapped by "+StepWrapper.class.getPackage().getName();
        log(log, info);
        log(log, META_STARTED + ZonedDateTime.now());
        if (createDir) {
            log(log, "Created directory " + WORKDIR_OUTPUT);
        }
    }

    private static Process createProcess(final PrintStream log, final String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(" ");
            sb.append(arg);
        }
        log(log, "Creating process:"+sb);
        try {
            return new ProcessBuilder().command(args).start();
        } catch (IOException e) {
            fail("Could not start wrapped process", e);
            return null;
        }
    }

    private static boolean initialize(final String[] args) {
        starttime = System.currentTimeMillis();
        lastLogTime = starttime;
        timeSupplier = parseFormat(System.getenv(TIME_FORMAT));
        logTimeoutMinutes = parseEnvInt(LOG_TIMEOUT_MINUTES);
        overallTimeoutMinutes = parseEnvInt(OVERALL_TIMEOUT_MINUTES);
        if (args.length == 0) {
            fail("No commandline arguments specified", null);
        }
        if (!new File(WORKDIR).exists()) {
            fail("Workdir directory not mounted under "+WORKDIR, null);
        }
        boolean createDir = !new File(WORKDIR_OUTPUT).exists();
        if (createDir && !new File(WORKDIR_OUTPUT).mkdir()) {
            fail("Could not create directory "+WORKDIR_OUTPUT, null);
        }
        return createDir;
    }

    private static int parseEnvInt(final String envkey) {
        String value = System.getenv(envkey);
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            fail("Illegal integer specified: "+value, e);
            return 0;
        }
    }

    private static Supplier<String> parseFormat(final String timeformat) {
        if (timeformat == null) {
            return null;
        }
        if (timeformat.equals(RELATIVE_TIME_FORMAT)) {
            return StepWrapper::relativeTimeStamp;
        }
        if (timeformat.equals(MILLIS)) {
            return () -> Long.toString(System.currentTimeMillis());
        }
        if (timeformat.equals(ISO)) {
            return () -> DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeformat);
            return () -> formatter.format(LocalDateTime.now());
        } catch(IllegalArgumentException e) {
            fail("Could not parse time format: "+timeformat, e);
            return null;
        }
    }

    private static void installShutdownHandler(final PrintStream log, final Process process, final AtomicBoolean running) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(running, log, process)));
        log(log, "Shutdown handler installed");
    }

    private static void shutdown(final AtomicBoolean running, final PrintStream log,
                                 final Process process) {
        if (!running.get()) {
            // process not running anymore
            return;
        }
        running.set(false);
        log(log, "Shutdown handler called. Stopping process");
        stopProcess(log, process, WrapperState.POD_SHUTTING_DOWN);
    }

    private static void installWatchdog(final AtomicBoolean running, final PrintStream log, final Process process) {
        new Thread(() -> {
            while (running.get()) {
                doInterruptable(() -> Thread.sleep(WATCHDOG_SLEEP));
                if (checkLogTimeout()) {
                    running.set(false);
                    log(log, "Watchdog: Received no logs since "+logTimeoutMinutes+" minutes. Stopping process");
                    stopProcess(log, process, WrapperState.LOG_TIMEOUT);
                }
                if (checkOverallTimeout()) {
                    running.set(false);
                    log(log, "Watchdog: Process running for "+overallTimeoutMinutes+" minutes. Stopping process");
                    stopProcess(log, process, WrapperState.OVERALL_TIMEOUT);
                }
            }
        }).start();
        log(log, "Watchdog installed");
    }

    private static boolean checkLogTimeout() {
        return System.currentTimeMillis() > lastLogTime + logTimeoutMinutes*60_000L;
    }

    private static boolean checkOverallTimeout() {
        return System.currentTimeMillis() > starttime + overallTimeoutMinutes*60_000L;
    }

    private static void stopProcess(final PrintStream log, final Process process,
                                    final WrapperState stoppedReason) {
        process.destroy();
        if (process.isAlive()) {
            log(log, "Waiting for process to terminate");
            doInterruptable(() -> process.waitFor(WAIT_TIME_OUT, TimeUnit.SECONDS));
        }
        if (process.isAlive()) {
            log(log, "Process still alive, terminating forcibly");
            process.destroyForcibly();
            doInterruptable(() -> process.waitFor(WAIT_TIME_OUT, TimeUnit.SECONDS));
        }
        if (process.isAlive()) {
            log(log, "Process still running, shutdown anyway");
        } else {
            log(log, "Process has terminated");
        }
        state(log, stoppedReason);
        log(log, "System shutdown");
    }

    private static Thread createPipeReader(final InputStream in, final String prefix, final PrintStream log, final PrintStream... outputs) {
        Thread reader = new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                int b;
                while ((b = in.read()) != -1) {
                    lastLogTime = System.currentTimeMillis();
                    for (PrintStream out : outputs) {
                        out.write(b);
                    }
                    if (b == '\r') {
                        log(log, PROGRESS_PREFIX, sb.toString());
                        sb.setLength(0);
                    } else if (b == '\n') {
                        log(log, prefix, sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append((char)b);
                    }
                }
                if (sb.length() != 0) {
                    log(log, prefix, sb.toString());
                }
            } catch (IOException e) {
                fail("Exception reading stream", e);
            }
        });
        reader.start();
        return reader;
    }

    private static void println(final PrintStream out, final String line) {
        synchronized(out) {
            out.println(line);
        }
        out.flush();
    }

    private static void log(final PrintStream log, final String line) {
        log(log, WRAPPER_PREFIX, line);
    }

    private static void log(final PrintStream log, final String prefix, final String line) {
        println(log, prefix+(timeSupplier == null ? " " :  " "+timeSupplier.get()+" ")+line);
    }

    private static String relativeTimeStamp() {
        long time = System.currentTimeMillis() - starttime;
        int secs = (int) (time / 1000);
        int milis = (int) (time % 1000);
        int digits;
        if (secs >= 1000) {
            digits = 0;
        } else if (secs >= 100) {
            digits = 1;
        } else if (secs >= 10) {
            digits = 2;
        } else {
            digits = 3;
        }
        String timestr = Integer.toString(milis);
        while (timestr.length() < 3) {
            timestr = "0" + timestr;
        }
        timestr = secs + "." + timestr.substring(0, digits);
        while (timestr.length() < 5) {
            timestr = " " + timestr;
        }
        return timestr;
    }

    private static void done() {
        try (FileOutputStream done = new FileOutputStream(WORKDIR_DONE)) {
        } catch (FileNotFoundException e) {
            fail("could not create file "+WORKDIR_DONE, e);
        } catch (IOException e) {
            fail("could not write to file "+WORKDIR_DONE, e);
        }
    }

    private static void state(final PrintStream log, WrapperState newState)  {
        log(log, "Setting state file to "+newState);
        try (PrintStream state = new PrintStream(new FileOutputStream(WORKDIR_STATE))) {
            state.println(newState);
        } catch (FileNotFoundException e) {
            fail("could not create file "+WORKDIR_STATE, e);
        }
        state = newState;
    }

    private static void fail(String message, Exception e) {
        ERR.println(message);
        if (e != null) {
            e.printStackTrace();
        }
        System.exit(-1);
    }

    @FunctionalInterface
    interface Interruptable {
        void run() throws InterruptedException;
    }

    public static void doInterruptable(Interruptable interruptable) {
        try {
            interruptable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
