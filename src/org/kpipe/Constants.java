package org.kpipe;

public class Constants {
    public static final String WORKDIR = "/workdir";
    public static final String WORKDIR_STATE = WORKDIR+"/state";
    public static final String WORKDIR_DONE = WORKDIR+"/done";
    public static final String WORKDIR_OUTPUT = WORKDIR+"/output";
    public static final String WORKDIR_INPUT = WORKDIR+"/input";
    public static final String WORKDIR_OUTPUT_LOG = WORKDIR_OUTPUT+"/log";
    public static final String WORKDIR_OUTPUT_ERROR = WORKDIR_OUTPUT+"/error";
    public static final String META_STARTED = "Started ";

    public static final String WRAPPER_PREFIX = "WRAPPER>";
    public static final String COMMAND_PREFIX = "COMMAND>";
    public static final String OUT_PREFIX = "OUT>";
    public static final String ERROR_PREFIX = "ERROR>";
    public static final String PROGRESS_PREFIX = "PROGRESS>";
    public static final String FAILED_PREFIX = "FAILED>";
    public static final String SUCCESS_PREFIX = "SUCCESS>";

    public static final String RETURNED_EXIT_CODE = "Returned exit code ";
    public static final String TERMINATED = "Terminated ";
    public static final String VERSION = "0.0.1";
    public static final long WAIT_TIME_OUT = 10;
    public static final String TIME_FORMAT = "TIME_FORMAT";
    public static final String MILLIS = "UNIX_MILLIS";
    public static final String RELATIVE_TIME_FORMAT = "SECONDS_SINCE_START";
    public static final String LOG_TIMEOUT_MINUTES = "LOG_TIMEOUT_MINUTES";
    public static final String PROCESS_TIMEOUT_MINUTES = "PROCESS_TIMEOUT_MINUTES";
    public static final String OVERALL_TIMEOUT_MINUTES = "TOTAL_TIMEOUT_MINUTES";
    public static final long WATCHDOG_SLEEP = 30000;
    public static final String ISO = "ISO";

    public static final long READER_THREAD_JOIN_TIMEOUT_SECONDS = 5;

}
