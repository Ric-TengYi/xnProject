# phase67_security_related_profile_regression_2026-03-23

- 日期：2026-03-23
- 通过：8
- 失败：0

## 结果

| 用例 | 状态 | 说明 |
|---|---|---|
| login | PASS | admin/admin 登录成功 |
| security_person_profile_runtime | PASS | id=48, cert=1, learning=1 |
| security_vehicle_profile_runtime | PASS | id=49, insurance=2, maintenance=1 |
| security_site_profile_runtime | PASS | id=50, docs=7, devices=4 |
| security_related_profile_detail_runtime | PASS | person/vehicle/site 详情安全档案已回填 |
| security_related_profile_export_runtime | PASS | text/csv;charset=UTF-8 |
| security_related_profile_ui_visible | PASS | 安全台账详情抽屉已展示对象安全档案区块 |
| security_related_profile_cleanup_runtime | PASS | deleted=3 |