package com.mohamed.barcodelabel.workflow.csv;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public final class CsvImportService {
    public CsvDataset read(Path path) throws IOException { return read(path, StandardCharsets.UTF_8); }

    public CsvDataset read(Path path, Charset charset) throws IOException {
        String content = Files.readString(path, charset);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') content = content.substring(1);
        char delimiter = detectDelimiter(content);
        List<List<String>> records = parse(content, delimiter);
        records.removeIf(CsvImportService::isBlankRecord);
        if (records.isEmpty()) throw new IllegalArgumentException("CSV file is empty");

        List<String> headers = records.getFirst().stream().map(String::trim).toList();
        validateHeaders(headers);
        List<java.util.Map<String, String>> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> values = records.get(rowIndex);
            if (values.size() > headers.size()) {
                throw new IllegalArgumentException("CSV row " + (rowIndex + 1) + " has more fields than the header");
            }
            var row = new LinkedHashMap<String, String>();
            for (int column = 0; column < headers.size(); column++) {
                row.put(headers.get(column), column < values.size() ? values.get(column) : "");
            }
            rows.add(row);
        }
        return new CsvDataset(headers, rows, delimiter);
    }

    private static void validateHeaders(List<String> headers) {
        if (headers.isEmpty() || headers.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("CSV headers must not be empty");
        }
        if (new LinkedHashSet<>(headers).size() != headers.size()) {
            throw new IllegalArgumentException("CSV headers must be unique");
        }
    }

    static char detectDelimiter(String content) {
        char[] candidates = {',', ';', '\t'};
        int best = -1;
        char selected = ',';
        for (char candidate : candidates) {
            int count = countInFirstRecord(content, candidate);
            if (count > best) { best = count; selected = candidate; }
        }
        return selected;
    }

    private static int countInFirstRecord(String content, char target) {
        boolean quoted = false;
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') i++;
                else quoted = !quoted;
            } else if (!quoted && (c == '\r' || c == '\n')) break;
            else if (!quoted && c == target) count++;
        }
        return count;
    }

    static List<List<String>> parse(String content, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(c);
            } else if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == delimiter) {
                row.add(field.toString()); field.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0); records.add(row); row = new ArrayList<>();
            } else field.append(c);
        }
        if (quoted) throw new IllegalArgumentException("CSV contains an unclosed quoted field");
        if (!field.isEmpty() || !row.isEmpty()) { row.add(field.toString()); records.add(row); }
        return records;
    }

    private static boolean isBlankRecord(List<String> record) { return record.stream().allMatch(String::isBlank); }
}
