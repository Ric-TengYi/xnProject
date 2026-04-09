#!/bin/bash

# 多租户SaaS系统完整测试脚本

echo "=== 多租户SaaS系统完整测试 ==="
echo ""

# 测试1: 超级管理员登录和用户菜单
echo "【测试1】超级管理员登录和用户菜单"
curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq '.'

echo ""
echo "【测试2】获取当前用户信息"
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

curl -s -X GET http://localhost:8080/api/me \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "【测试3】获取用户菜单"
curl -s -X GET "http://localhost:8080/api/me/menus?platform=PC" \
  -H "Authorization: Bearer $TOKEN" | jq '.data | length'

echo ""
echo "【测试4】获取角色列表（分页）"
curl -s -X GET "http://localhost:8080/api/roles?pageNo=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN" | jq '.data | {total, pageSize: .size, records: (.records | length)}'

echo ""
echo "【测试5】获取用户列表（分页）"
curl -s -X GET "http://localhost:8080/api/users?pageNo=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN" | jq '.data | {total, pageSize: .size, records: (.records | length)}'

echo ""
echo "=== 测试完成 ==="
