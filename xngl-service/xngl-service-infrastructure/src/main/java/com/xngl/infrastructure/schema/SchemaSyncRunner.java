package com.xngl.infrastructure.schema;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * 启动时校对库表：执行 baseline.sql 补全缺失表；对 patch 中涉及大表的 ALTER 仅提示不执行。
 */
@Component
public class SchemaSyncRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SchemaSyncRunner.class);

  private static final String BASELINE_PATH = "db/schema/baseline.sql";
  private static final String PATCHES_DIR = "db/schema/patches/";
  private static final Pattern ALTER_TABLE_PATTERN =
      Pattern.compile("ALTER\\s+TABLE\\s+([a-zA-Z0-9_]+)\\s+ADD\\s+COLUMN", Pattern.CASE_INSENSITIVE);
  private static final Pattern CREATE_INDEX_PATTERN =
      Pattern.compile(
          "CREATE\\s+(UNIQUE\\s+)?INDEX\\s+(IF\\s+NOT\\s+EXISTS\\s+)?([a-zA-Z0-9_]+)\\s+ON\\s+([a-zA-Z0-9_]+)",
          Pattern.CASE_INSENSITIVE);

  private final DataSource dataSource;
  private final SchemaSyncProperties properties;

  public SchemaSyncRunner(DataSource dataSource, SchemaSyncProperties properties) {
    this.dataSource = dataSource;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      log.info("schema-sync 已关闭，跳过启动时 SQL 校对");
      return;
    }
    try {
      runBaseline();
      runPatches();
    } catch (Exception e) {
      log.warn("schema-sync check failed: {}", e.getMessage());
    }
  }

  private void runBaseline() throws Exception {
    ClassPathResource resource = new ClassPathResource(BASELINE_PATH);
    if (!resource.exists()) {
      log.debug("no baseline.sql found, skip");
      return;
    }
    String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    executeStatements(sql);
    log.info("schema-sync baseline executed");
  }

  private void runPatches() throws Exception {
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    org.springframework.core.io.Resource[] resources =
        resolver.getResources("classpath:" + PATCHES_DIR + "*.sql");
    if (resources.length == 0) return;

    List<String> warnOnlyTables = properties.getWarnOnlyTables();
    for (org.springframework.core.io.Resource res : resources) {
      String scriptName = res.getFilename();
      if (scriptName == null) continue;

      String content = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
      List<String> statements = splitStatements(content);

      for (String stmt : statements) {
        String s = stmt.trim();
        if (s.isEmpty() || s.startsWith("--")) continue;
        if (s.toUpperCase().startsWith("ALTER")) {
          Matcher m = ALTER_TABLE_PATTERN.matcher(s);
          if (m.find()) {
            String tableName = m.group(1);
            if (warnOnlyTables.stream().anyMatch(t -> t.equalsIgnoreCase(tableName))) {
              if (columnMissing(tableName, s)) {
                log.warn(
                    "[schema-sync] 大表 {} 缺少字段，仅提示不执行。请评估后在低峰期手动执行: {}",
                    tableName,
                    s);
              }
              continue;
            }
            if (!columnMissing(tableName, s)) continue;
          }
        }
        if (s.toUpperCase().startsWith("CREATE")) {
          Matcher indexMatcher = CREATE_INDEX_PATTERN.matcher(s);
          if (indexMatcher.find()) {
            String indexName = indexMatcher.group(3);
            String tableName = indexMatcher.group(4);
            if (indexExists(tableName, indexName)) {
              continue;
            }
            s = s.replaceFirst("(?i)\\s+IF\\s+NOT\\s+EXISTS", "");
          }
        }
        executeOne(s);
      }
    }
  }

  private boolean columnMissing(String tableName, String alterSql) {
    try {
      String columnName = extractColumnNameFromAlter(alterSql);
      if (columnName == null) return true;
      Connection conn = DataSourceUtils.getConnection(dataSource);
      try (ResultSet rs =
          conn.getMetaData()
              .getColumns(null, null, tableName, columnName)) {
        return !rs.next();
      } finally {
        DataSourceUtils.releaseConnection(conn, dataSource);
      }
    } catch (Exception e) {
      return true;
    }
  }

  private static final Pattern ADD_COLUMN_NAME = Pattern.compile("ADD\\s+COLUMN\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

  private String extractColumnNameFromAlter(String alterSql) {
    Matcher m = ADD_COLUMN_NAME.matcher(alterSql);
    return m.find() ? m.group(1) : null;
  }

  private boolean indexExists(String tableName, String indexName) {
    try {
      Connection conn = DataSourceUtils.getConnection(dataSource);
      try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
        while (rs.next()) {
          String current = rs.getString("INDEX_NAME");
          if (current != null && current.equalsIgnoreCase(indexName)) {
            return true;
          }
        }
        return false;
      } finally {
        DataSourceUtils.releaseConnection(conn, dataSource);
      }
    } catch (Exception e) {
      return false;
    }
  }

  private void executeStatements(String sql) throws Exception {
    for (String stmt : splitStatements(sql)) {
      String s = stmt.trim();
      if (s.isEmpty() || s.startsWith("--")) continue;
      executeOne(s);
    }
  }

  private void executeOne(String sql) throws Exception {
    try (Connection conn = DataSourceUtils.getConnection(dataSource)) {
      conn.createStatement().execute(sql);
    }
  }

  private List<String> splitStatements(String sql) {
    List<String> list = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    boolean inLiteral = false;
    char quote = 0;
    for (String line : sql.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("--")) continue;
      for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);
        if (!inLiteral && (c == '\'' || c == '"')) {
          inLiteral = true;
          quote = c;
        } else if (inLiteral && c == quote) {
          inLiteral = false;
        } else if (!inLiteral && c == ';') {
          String s = sb.toString().trim();
          if (StringUtils.hasText(s)) list.add(s);
          sb.setLength(0);
          continue;
        }
        sb.append(c);
      }
      sb.append('\n');
    }
    String s = sb.toString().trim();
    if (StringUtils.hasText(s)) list.add(s);
    return list;
  }
}
