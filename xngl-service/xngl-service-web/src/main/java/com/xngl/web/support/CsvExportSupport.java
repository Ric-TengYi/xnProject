package com.xngl.web.support;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class CsvExportSupport {

  private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private CsvExportSupport() {}

  public static ResponseEntity<byte[]> csvResponse(
      String filePrefix, List<String> headers, List<? extends List<?>> rows) {
    StringBuilder csv = new StringBuilder();
    csv.append(headers.stream().map(CsvExportSupport::escape).collect(Collectors.joining(","))).append('\n');
    for (List<?> row : rows) {
      csv.append(
              row.stream()
                  .map(value -> escape(value == null ? "" : String.valueOf(value)))
                  .collect(Collectors.joining(",")))
          .append('\n');
    }
    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    String fileName = filePrefix + "_" + LocalDateTime.now().format(FILE_TIME) + ".csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(bytes);
  }

  public static String value(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private static String escape(String value) {
    String escaped = value == null ? "" : value.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }
}
