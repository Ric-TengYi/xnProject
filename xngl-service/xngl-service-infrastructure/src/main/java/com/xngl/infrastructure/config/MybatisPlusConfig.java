package com.xngl.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xngl.infrastructure.persistence.mapper")
public class MybatisPlusConfig {

  /** 系统表不需要租户隔离 */
  private static final Set<String> IGNORE_TABLES = Set.of(
      "sys_tenant",     // 租户表
      "sys_config",    // 系统配置
      "sys_dict",      // 字典表
      "sys_dict_item", // 字典项
      "sys_menu"       // 菜单（全局）
  );

  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 租户拦截器必须放在分页拦截器之前
    interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
  }

  private TenantLineInnerInterceptor tenantLineInnerInterceptor() {
    return new TenantLineInnerInterceptor(
        new TenantLineHandler() {
          @Override
          public Expression getTenantId() {
            // 超级管理员不限制租户，可访问所有数据
            if (TenantContextHolder.isSuperAdmin()) {
              return new NullValue(); // 不添加租户条件
            }
            Long tenantId = TenantContextHolder.getTenantId();
            return tenantId != null ? new LongValue(tenantId) : new NullValue();
          }

          @Override
          public String getTenantIdColumn() {
            return "tenant_id";
          }

          @Override
          public boolean ignoreTable(String tableName) {
            return IGNORE_TABLES.contains(tableName.toLowerCase());
          }
        });
  }
}