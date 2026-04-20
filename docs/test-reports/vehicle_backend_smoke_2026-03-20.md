# Vehicle Backend Smoke 2026-03-20

- Passed: 11/11

## Checks
- PASS `auth_login` admin tenant 1 登录成功
- PASS `vehicle_stats` path=/vehicles/stats
- PASS `vehicle_list` path=/vehicles?pageNo=1&pageSize=10
- PASS `vehicle_company_capacity` path=/vehicles/company-capacity
- PASS `vehicle_fleets` path=/vehicles/fleets
- PASS `vehicle_seed_count` records=8
- PASS `vehicle_created_record` 创建接口测试记录可见
- PASS `transport_org_aggregate` orgs=4
- PASS `fleet_aggregate` fleets=7
- PASS `backend_build` mvn -q -DskipTests install 通过
- PASS `frontend_build` npm run build 通过

## Samples
- stats: `{"totalVehicles": 8, "activeVehicles": 5, "maintenanceVehicles": 1, "disabledVehicles": 1, "warningVehicles": 4, "activeRate": 62.5, "totalLoadTons": 226.5}`
- top vehicle: `{"id": "8", "plateNo": "浙A97610", "vin": null, "orgId": "6", "orgName": "宏基渣土运输公司", "vehicleType": "重型自卸货车", "brand": "测试品牌", "model": "TEST-001", "energyType": null, "axleCount": null, "loadWeight": 26.5, "driverName": "测试司机", "driverPhone": "13800990099", "fleetName": "测试联调车队", "captainName": null, "captainPhone": null, "status": 1, "statusLabel": "在用", "useStatus": "ACTIVE", "runningStatus": "STOPPED", "runningStatusLabel": "静止", "currentSpeed": 0.0, "currentMileage": 0.0, "nextMaintainDate": "2026-05-31", "annualInspectionExpireDate": null, "insuranceExpireDate": "2026-12-31", "warningLabel": "正常", "createTime": "2026-03-20T11:33:30", "updateTime": "2026-03-20T11:33:30"}`
- top capacity: `{"orgId": "6", "orgName": "宏基渣土运输公司", "totalVehicles": 4, "activeVehicles": 2, "movingVehicles": 1, "warningVehicles": 2, "disabledVehicles": 1, "totalLoadTons": 119.5, "activeRate": 50.0, "avgLoadTons": 29.88, "captainName": "张建国", "captainPhone": "13800138001"}`