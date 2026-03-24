# Phase 14 Org Types Smoke (2026-03-21)

- Total: 10
- Passed: 10
- Failed: 0

## Checks
- [PASS] API login / userId=6
- [PASS] Org type seeds / count=2 law+company=true
- [PASS] Org type create / id=2035328084848177153 code=TEST_ORG_1774095004645
- [PASS] Org type update / label=测试组织类型升级版
- [PASS] Org type status / status=DISABLED
- [PASS] Org type query / keyword=TEST_ORG_1774095004645 disabled=true
- [PASS] Org type delete api / id=2035328084848177153
- [PASS] Org type delete verify / code=TEST_ORG_1774095004645 removed=true
- [PASS] UI org types tab / tab=组织类型 seedVisible=true
- [PASS] UI org type modal / modal=新增组织类型 codeField=true
