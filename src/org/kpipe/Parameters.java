package org.kpipe;

public class Parameters {

    public static final int LOG_TIMEOUT_MINUTES = parseEnvInt(Constants.LOG_TIMEOUT_MINUTES);
    public static final int PROCESS_TIMEOUT_MINUTES = parseEnvInt(Constants.PROCESS_TIMEOUT_MINUTES);
    public static final int OVERALL_TIMEOUT_MINUTES = parseEnvInt(Constants.OVERALL_TIMEOUT_MINUTES);
    public static final String TIME_FORMAT = System.getenv(Constants.TIME_FORMAT);

    private static int parseEnvInt(final String envkey) {
        String value = System.getenv(envkey);
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Main.fail("Illegal integer specified for parameter "+envkey+": "+value);
            return 0;
        }
    }

}
