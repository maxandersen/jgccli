package dk.xam.jgccli.cli;

import java.util.List;

public class OutputFormatter {

    public static void printTable(String[] headers, List<String[]> rows) {
        System.out.println(String.join("\t", headers));
        for (String[] row : rows) {
            System.out.println(String.join("\t", row));
        }
    }

    public static void printKeyValue(String key, String value) {
        System.out.println(key + ": " + value);
    }

    public static void printKeyValue(String key, Object value) {
        printKeyValue(key, value != null ? value.toString() : "");
    }

    public static String orEmpty(String value) {
        return value != null ? value : "";
    }

    public static String orDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
