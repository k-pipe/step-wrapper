package org.kpipe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandExecutor {

    public static final String WITH = "WITH:";
    public static final String CALL = "CALL:";

    public static final String FOR_FILES = "FOR_FILES:";

    public static final String FILE = "file";

    public static final String WITH_LINES = "WITH_LINES:";

    private static final char COMMENT_CHAR = '#';

    public CommandExecutor() {
    }

    public WrapperState execute(String[] args) {
        return executeRecursively(List.of(args), new LinkedHashMap<>());
    }

    public WrapperState executeRecursively(List<String> commands, Map<String,String> variables) {
        if (commands.isEmpty()) {
            return WrapperState.SUCCESS;
        }
        String first = resolve(variables, commands.get(0));
        List<String> rest = commands.subList(1, commands.size());
        if (first.startsWith(WITH)) {
            String path = Constants.WORKDIR_INPUT+"/"+first.substring(WITH.length());
            readVariables(path, variables);
            return executeRecursively(rest, variables);
        }
        if (first.startsWith(CALL)) {
            String path = first.substring(CALL.length());
            WrapperState res = executeRecursively(split(mergeLinesRemovingComments(path)), variables);
            if (res.equals(WrapperState.SUCCESS) && !rest.isEmpty()) {
                res = executeRecursively(rest, variables);
            }
            return res;
        }
        if (first.startsWith(FOR_FILES)) {
            Pattern pattern = Pattern.compile(first.substring(FOR_FILES.length()));
            WrapperState res = WrapperState.SUCCESS;
            String[] files = new File(Constants.WORKDIR_INPUT).list((dir, fn) -> pattern.matcher(fn).matches());
            if (files == null) {
                Log.error("Files could not be read from folder: "+Constants.WORKDIR_INPUT);
                return WrapperState.ERROR;
            }
            for (String file : files) {
                variables.put(FILE, file);
                res = executeRecursively(rest, variables);
                if (!res.equals(WrapperState.SUCCESS)) {
                    return res;
                }
            }
            variables.remove(FILE);
            return res;
        }
        if (first.startsWith(WITH_LINES)) {
            String file = first.substring(WITH_LINES.length());
            try {
                WrapperState res = WrapperState.SUCCESS;
                for (String line : Files.readAllLines(Path.of(Constants.WORKDIR_INPUT, file))) {
                    LinkedHashMap<String, String> variablesClone = new LinkedHashMap<>(variables);
                    variablesClone.putAll(Json.asMap(line));
                    res = executeRecursively(rest, variablesClone);
                    if (!res.equals(WrapperState.SUCCESS)) {
                        return res;
                    }
                }
                return res;
            } catch (IOException e) {
                Log.exception(e);
                return WrapperState.ERROR;
            }
        }
        return new ProcessExecutor(resolve(variables, commands)).execute();
    }

    private List<String> split(String string) {
        return List.of(string.split(" "));
    }

    private void readVariables(String path, Map<String, String> variables) {
        variables.putAll(Json.asMap(mergeLinesRemovingComments(path)));
    }

    private String mergeLinesRemovingComments(String file) {
        StringBuilder sb = new StringBuilder();
        try {
            for (String line : Files.readAllLines(Path.of(file))) {
                String cleaned = removeComment(line).trim();
                if (!cleaned.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(cleaned);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Expect.fail("Could not read file "+file+", exception: "+e.getMessage());
        }
        return sb.toString();
    }

    private String removeComment(String line) {
        int pos = line.indexOf(COMMENT_CHAR);
        return pos < 0 ? line : line.substring(0, pos);
    }

    private List<String> resolve(Map<String, String> variables, List<String> list) {
        return list.stream().map(s -> resolve(variables, s)).collect(Collectors.toList());
    }

    private String resolve(Map<String, String> variables, String string) {
        String resolved = string;
        boolean done = false;
        while (!done) {
            String before = resolved;
            for (Map.Entry<String, String> e : variables.entrySet()) {
                resolved = resolved.replaceAll("\\{\\{"+e.getKey()+"}}", e.getValue());
            }
            done = resolved.equals(before);
        }
        return resolved;
    }

}
