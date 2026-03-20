# Unit Management Smoke 2026-03-20

- Passed: 9/9

## Checks
- PASS `auth_login` admin tenant 1 登录成功
- PASS `unit_summary` path=/units/summary
- PASS `unit_list` path=/units?pageNo=1&pageSize=20
- PASS `unit_detail` path=/units/10
- PASS `unit_seed_types` 建设/施工/运输三类单位已存在
- PASS `transport_vehicle_binding` 运输单位已挂接车辆
- PASS `unit_create` created=测试建设单位8917
- PASS `backend_build` mvn -q -DskipTests install 通过
- PASS `frontend_build` npm run build 通过

## Samples
- summary: `{"totalUnits": 7, "constructionUnits": 2, "builderUnits": 1, "transportUnits": 4, "totalVehicles": 8}`
- first unit: `{"id": "12", "orgCode": "ORG-CONSTRUCT-108523976", "orgName": "测试建设单位8858", "orgType": "CONSTRUCTION_UNIT", "orgTypeLabel": "建设单位", "contactPerson": "单元测试", "contactPhone": "0571-89990000", "address": null, "unifiedSocialCode": null, "status": "ENABLED", "statusLabel": "正常", "projectCount": 0, "contractCount": 0, "vehicleCount": 0, "activeVehicleCount": 0, "createTime": "2026-03-20T11:54:18", "updateTime": "2026-03-20T11:54:18"}`