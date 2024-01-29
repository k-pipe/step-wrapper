package org.kpipe;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Watchdog {

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final AtomicLong lastLogTime = new AtomicLong(Main.getStartTime());

    private static final AtomicLong processStartTime = new AtomicLong(Main.getStartTime());

    private static final AtomicReference<ProcessExecutor> process = new AtomicReference<>();

    private static Thread thread;
    public static void initialize() {
        installShutdownHandler();
        if ((Parameters.OVERALL_TIMEOUT_MINUTES > 0) || (Parameters.PROCESS_TIMEOUT_MINUTES > 0) || (Parameters.LOG_TIMEOUT_MINUTES > 0)) {
            startWatchdogThread();
        }
    }
    public static void resetLogsTime() {
        lastLogTime.set(System.currentTimeMillis());
    }

    public static void setProcessStarted(ProcessExecutor processExecutor) {
        processStartTime.set(System.currentTimeMillis());
        process.set(processExecutor);
    }

    public static void setProcessEnded() {
        process.set(null);
    }

    private static boolean checkLogTimeout() {
        return System.currentTimeMillis() > lastLogTime.get() + Parameters.LOG_TIMEOUT_MINUTES*60_000L;
    }

    private static boolean checkProcessTimeout() {
        return System.currentTimeMillis() > processStartTime.get() + Parameters.PROCESS_TIMEOUT_MINUTES*60_000L;
    }

    private static boolean checkOverallTimeout() {
        return System.currentTimeMillis() > Main.getStartTime() + Parameters.OVERALL_TIMEOUT_MINUTES*60_000L;
    }

    private static void installShutdownHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(Watchdog::shutdown));
        Log.log("Shutdown handler installed");
    }

    private static void shutdown() {
        if (running.getAndSet(false)) {
            Log.log("Shutdown handler called");
            ProcessExecutor runningProcess = process.get();
            if (runningProcess != null) {
                Log.log("Stopping currently running process");
                stopProcess(runningProcess, WrapperState.POD_SHUTTING_DOWN);
            } else {
                Log.log("No process is running");
            }
            Log.log("Terminating watchdog thread");
            thread.interrupt();
        }
    }

    private static void startWatchdogThread() {
        thread = new Thread(() -> {
            while (running.get()) {
                ProcessExecutor runningProcess = process.get();
                if (runningProcess != null) {
                    if (checkLogTimeout()) {
                        running.set(false);
                        Log.log("Watchdog: Received no logs since " + Parameters.LOG_TIMEOUT_MINUTES + " minutes. Stopping process");
                        stopProcess(runningProcess, WrapperState.LOG_TIMEOUT);
                    } else if (checkProcessTimeout()) {
                        running.set(false);
                        Log.log("Watchdog: Process running for " + Parameters.PROCESS_TIMEOUT_MINUTES + " minutes. Stopping process");
                        stopProcess(runningProcess, WrapperState.PROCESS_TIMEOUT);
                    } else if (checkOverallTimeout()) {
                        running.set(false);
                        Log.log("Watchdog: Step running for " + Parameters.OVERALL_TIMEOUT_MINUTES + " minutes. Stopping process");
                        stopProcess(runningProcess, WrapperState.OVERALL_TIMEOUT);
                    } else {
                        Util.doInterruptable(() -> Thread.sleep(Constants.WATCHDOG_SLEEP));
                    }
                }
            }
        });
        thread.start();
        Log.log("Watchdog thread started");
    }

    private static void stopProcess(final ProcessExecutor processExecutor, final WrapperState stoppedReason) {
        Process process = processExecutor.getProcess();
        process.destroy();
        if (process.isAlive()) {
            Log.log("Waiting for process to terminate");
            Util.doInterruptable(() -> process.waitFor(Constants.WAIT_TIME_OUT, TimeUnit.SECONDS));
        }
        if (process.isAlive()) {
            Log.log("Process still alive, terminating forcibly");
            process.destroyForcibly();
            Util.doInterruptable(() -> process.waitFor(Constants.WAIT_TIME_OUT, TimeUnit.SECONDS));
        }
        if (process.isAlive()) {
            Log.error("Process still running, shutdown anyway");
        } else {
            Log.log("Process has terminated");
        }
        processExecutor.setState(stoppedReason);
    }

}
