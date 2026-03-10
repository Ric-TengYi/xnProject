package com.xngl.infrastructure.schema;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.schema-sync")
public class SchemaSyncProperties {

  /** 是否在启动时执行 SQL 校对。稳定上线后建议关闭，避免启动变慢 */
  private boolean enabled = true;

  /**
   * 大表或敏感表：缺少字段时只打日志提示，不执行 ALTER。
   * 例如: biz_vehicle, vehicle_gps
   */
  private List<String> warnOnlyTables = new ArrayList<>(List.of("biz_vehicle", "vehicle_gps"));

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getWarnOnlyTables() {
    return warnOnlyTables;
  }

  public void setWarnOnlyTables(List<String> warnOnlyTables) {
    this.warnOnlyTables = warnOnlyTables;
  }
}
