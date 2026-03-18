# Task Plan: xnProject User System Comprehensive Testing

## Goal
对消纳平台（xnProject）的用户体系进行全面测试，包括组织管理、角色管理、用户管理、菜单管理、权限控制和多租户隔离，发现问题并修复。

## Current Phase
Phase 1

## Phases

### Phase 1: 环境准备与项目启动
- [ ] 检查端口占用情况（后端8090、前端5173）
- [ ] 启动后端服务 xngl-service
- [ ] 启动前端服务 xngl-web
- [ ] 验证服务可用性
- **Status:** in_progress

### Phase 2: 组织管理测试
- [ ] 组织增删改查测试
- [ ] 组织层级树测试
- [ ] 跨组织数据隔离测试
- **Status:** pending

### Phase 3: 角色管理测试
- [ ] 角色增删改测试
- [ ] 角色权限分配测试
- [ ] 系统内置角色 vs 自定义角色测试
- **Status:** pending

### Phase 4: 用户管理测试
- [ ] 用户增删改查测试
- [ ] 用户绑定角色（多角色）测试
- [ ] 用户跨组织分配测试
- **Status:** pending

### Phase 5: 菜单管理测试
- [ ] 菜单配置测试
- [ ] 菜单权限控制测试
- [ ] 动态菜单渲染测试
- **Status:** pending

### Phase 6: 权限控制测试
- [ ] 页面级权限（路由守卫）测试
- [ ] 按钮级权限（显隐控制）测试
- [ ] API级权限（后端拦截）测试
- [ ] 数据级权限（行级隔离）测试
- **Status:** pending

### Phase 7: 多租户测试
- [ ] 租户隔离测试
- [ ] 租户配置测试
- [ ] 数据归属测试
- **Status:** pending

### Phase 8: 问题修复与验收
- [ ] 汇总问题清单
- [ ] 修复发现的Bug
- [ ] 回归测试验收
- **Status:** pending

## Key Questions
1. 当前系统是否支持多租户架构？
2. 权限系统是RBAC模型还是更复杂的模型？
3. 是否有超级管理员可以跨组织操作？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 使用 Playwright 进行前端测试 | 可视化测试更直观 |
| 并行测试前后端 | 提高测试效率 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|        | 1       |            |

## Notes
- 后端端口: 8090 (Spring Boot)
- 前端端口: 5173 (Vite)
- 数据库: MySQL 3306
- Redis: 6379