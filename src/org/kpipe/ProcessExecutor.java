package org.kpipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessExecutor {

    private final AtomicReference<WrapperState> state = new AtomicReference<>(WrapperState.RUNNING);
    private final Process process;

    public ProcessExecutor(List<String> commandline) {
        process = createProcess(commandline);
    }

    public void setState(WrapperState state) {
        Log.log("Setting state to "+state);
        this.state.set(state);
    }

    public WrapperState getState() {
        return state.get();
    }

    public Process getProcess() {
        return process;
    }

    public WrapperState execute() {
        if (process == null) {
            return WrapperState.ERROR;
        }
        Watchdog.setProcessStarted(this);
        final Thread inReader = createPipeReader(process.getInputStream(), Log.SYS_OUT, false);
        final Thread errReader = createPipeReader(process.getErrorStream(), Log.SYS_ERR, true);
        Util.doInterruptable(process::waitFor);
        int exitcode = process.exitValue();
        if (getState().equals(WrapperState.RUNNING)) {
            // was not stopped, determine result based on exit code
            setState(exitcode == 0 ? WrapperState.SUCCESS : WrapperState.ERROR);
        }
        waitFor("output", inReader);
        waitFor("error", errReader);
        Watchdog.setProcessEnded();
        return getState();
    }

    private static void waitFor(final String name, final Thread readerThread) {
        Log.log("Joining "+name+" reader thread");
        //System.out.println("Joining "+name+" reader thread");
        long timeStamp = System.currentTimeMillis();
        Util.doInterruptable(() -> readerThread.join(Constants.READER_THREAD_JOIN_TIMEOUT_SECONDS*1000));
        Log.log("Joined "+name+" reader thread after "+(System.currentTimeMillis()-timeStamp)+" ms");
        //System.out.println("Joined "+name+" reader thread after "+(System.currentTimeMillis()-timeStamp)+" ms");
    }

    private static Process createProcess(final List<String> args) {
        String commandline = String.join(" ", args);
        Log.command(commandline);
        try {
            return new ProcessBuilder().command(args).start();
        } catch (IOException e) {
            Main.fail("Could not start wrapped process", e);
            return null;
        }
    }

    private static Thread createPipeReader(final InputStream in, final PrintStream mirroredStream, final boolean isError) {
        Thread reader = new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                int b;
                while ((b = in.read()) != -1) {
                    Watchdog.resetLogsTime();
                    mirroredStream.write(b);
                    if (b == '\r') {
                        Log.progress(sb.toString());
                        sb.setLength(0);
                    } else if (b == '\n') {
                        Log.mirrorLog(sb.toString(), isError);
                        sb.setLength(0);
                    } else {
                        sb.append((char)b);
                    }
                }
                if (sb.length() != 0) {
                    Log.mirrorLog(sb.toString(), isError);
                }
            } catch (IOException e) {
                Main.fail("Exception reading stream", e);
            }
        });
        reader.start();
        return reader;
    }

}
