package com.xngl.infrastructure.config;

/**
 * 基于 ThreadLocal 的租户上下文，供 MyBatis-Plus TenantLineInnerInterceptor 读取当前租户 ID。
 * <p>
 * 由 JwtAuthFilter 在请求进入时 set，请求结束时 clear。
 */
public final class TenantContextHolder {

  private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

  /** 超级管理员角色标识 */
  public static final String SUPER_ADMIN = "SUPER_ADMIN";

  private TenantContextHolder() {}

  public static void setTenantId(Long tenantId) {
    TENANT_ID.set(tenantId);
  }

  public static Long getTenantId() {
    return TENANT_ID.get();
  }

  public static void setRole(String role) {
    ROLE.set(role);
  }

  public static String getRole() {
    return ROLE.get();
  }

  /**
   * 判断当前用户是否为超级管理员
   * 超级管理员可访问所有租户数据
   */
  public static boolean isSuperAdmin() {
    return SUPER_ADMIN.equals(getRole());
  }

  public static void clear() {
    TENANT_ID.remove();
    ROLE.remove();
  }

  // 兼容旧方法
  public static void set(Long tenantId) {
    setTenantId(tenantId);
  }

  public static Long get() {
    return getTenantId();
  }
}