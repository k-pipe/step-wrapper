package org.kpipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandExecutor {

    public static final String WITH = "WITH(";
    public static final String CLOSE_BRACKET = ")";
    public static final String CALL = "CALL";
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
        String first = commands.get(0);
        if (first.startsWith(WITH)) {
            Expect.isTrue(first.endsWith(CLOSE_BRACKET), "Expected closing bracket in WITH statement, got "+first);
            String path = Constants.WORKDIR_INPUT+"/"+first.substring(WITH.length(), first.length()-CLOSE_BRACKET.length()).trim();
            readVariables(path, variables);
            return executeRecursively(commands.subList(1, commands.size()), variables);
        }
        if (first.equals(CALL)) {
            Expect.isTrue(commands.size() == 2, "Expected exactly one argument after CALL statement, got "+(commands.size()-1));
            return executeRecursively(split(mergeLinesRemovingComments(commands.get(1))), variables);
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
                resolved = resolved.replaceAll("{"+e.getKey()+"}", e.getValue());
            }
            done = resolved.equals(before);
        }
        return resolved;
    }

}
