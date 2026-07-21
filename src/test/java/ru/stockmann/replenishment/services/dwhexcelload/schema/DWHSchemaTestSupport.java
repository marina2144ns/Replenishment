package ru.stockmann.replenishment.services.dwhexcelload.schema;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DWHSchemaTestSupport {

    private DWHSchemaTestSupport() {
    }

    static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    static String normalizeSql(String sql) {
        return sql
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)--.*$", " ")
                .replace('[', ' ')
                .replace(']', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    static List<ColumnDef> tableColumns(String ddlPath, String tableName) throws IOException {
        String sql = read(ddlPath);
        String normalizedTable = tableName.toLowerCase(Locale.ROOT);
        Pattern createTable = Pattern.compile(
                "(?i)create\\s+table\\s+" + Pattern.quote(normalizedTable) + "\\s*\\("
        );
        Matcher matcher = createTable.matcher(sql.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            throw new AssertionError("CREATE TABLE not found: " + tableName);
        }

        int open = sql.indexOf('(', matcher.start());
        int close = matchingParen(sql, open);
        String body = sql.substring(open + 1, close);
        List<ColumnDef> columns = new ArrayList<>();
        for (String part : splitTopLevel(body)) {
            String normalized = normalizeSql(part);
            if (normalized.isBlank() || isTableConstraint(normalized)) {
                continue;
            }

            String[] tokens = normalized.split(" ", 3);
            if (tokens.length < 2) {
                continue;
            }
            columns.add(new ColumnDef(tokens[0], tokens[1], normalized));
        }
        return columns;
    }

    static List<String> names(List<ColumnDef> columns) {
        return columns.stream().map(ColumnDef::name).toList();
    }

    static List<String> businessColumns(List<ColumnDef> columns) {
        return names(columns).stream()
                .filter(column -> !List.of("id", "loadsessionid", "excelrownum", "createdat").contains(column))
                .toList();
    }

    static List<String> definitionColumns(ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition definition) {
        return definition.columns().stream()
                .map(c -> normalizeColumnName(c.rawColumnName()))
                .toList();
    }

    static List<String> recordComponents(Class<? extends Record> recordClass) {
        List<String> components = new ArrayList<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            components.add(component.getName().toLowerCase(Locale.ROOT));
        }
        return components;
    }

    static List<String> selectColumns(String sourcePath, String tableName) throws IOException {
        String source = read(sourcePath);
        Pattern pattern = Pattern.compile("(?is)select\\s+(.*?)\\s+from\\s+" + Pattern.quote(tableName));
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new AssertionError("SELECT not found for " + tableName + " in " + sourcePath);
        }
        return splitTopLevel(matcher.group(1)).stream()
                .map(DWHSchemaTestSupport::normalizeSelectColumn)
                .toList();
    }

    static List<String> insertColumns(String sourcePath, String tableName) throws IOException {
        String source = read(sourcePath);
        Pattern pattern = Pattern.compile("(?is)insert\\s+into\\s+" + Pattern.quote(tableName));
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new AssertionError("INSERT not found for " + tableName + " in " + sourcePath);
        }
        int open = source.indexOf('(', matcher.end());
        int close = matchingParen(source, open);
        return splitTopLevel(source.substring(open + 1, close)).stream()
                .map(DWHSchemaTestSupport::normalizeColumnName)
                .toList();
    }

    static int placeholderCount(String sourcePath, String tableName) throws IOException {
        String source = read(sourcePath);
        Pattern pattern = Pattern.compile("(?is)insert\\s+into\\s+" + Pattern.quote(tableName));
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new AssertionError("INSERT not found for " + tableName + " in " + sourcePath);
        }
        int values = source.toLowerCase(Locale.ROOT).indexOf("values", matcher.end());
        int open = source.indexOf('(', values);
        int close = matchingParen(source, open);
        String placeholders = source.substring(open + 1, close);
        int count = 0;
        for (int i = 0; i < placeholders.length(); i++) {
            if (placeholders.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    static String normalizeColumnName(String value) {
        String normalized = value
                .replace("[", "")
                .replace("]", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        int asIndex = normalized.indexOf(" as ");
        if (asIndex >= 0) {
            normalized = normalized.substring(asIndex + 4).trim();
        }
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
        }
        return normalized;
    }

    static String camelToSql(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String normalizeSelectColumn(String value) {
        String normalized = value.trim();
        Matcher alias = Pattern.compile("(?is).*\\s+as\\s+(.+)$").matcher(normalized);
        if (alias.matches()) {
            normalized = alias.group(1);
        }
        return normalizeColumnName(normalized);
    }

    private static boolean isTableConstraint(String normalized) {
        return normalized.startsWith("constraint ")
                || normalized.startsWith("primary key")
                || normalized.startsWith("foreign key")
                || normalized.startsWith("check ")
                || normalized.startsWith("unique ");
    }

    private static int matchingParen(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new AssertionError("Matching parenthesis not found");
    }

    private static List<String> splitTopLevel(String text) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }

    record ColumnDef(String name, String type, String normalizedDefinition) {
        boolean contains(String value) {
            return normalizedDefinition.contains(value.toLowerCase(Locale.ROOT));
        }
    }
}
