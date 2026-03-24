# Phase 16 Disposal Records Smoke (2026-03-21)

- Total: 8
- Passed: 8
- Failed: 0

## Checks
- [PASS] API login / userId=6
- [PASS] Disposal keyword query / total=1 project=项目-001 site=场地-001
- [PASS] Disposal site filter / total=3 volumeSum=5300.00
- [PASS] Disposal date filter / filteredTotal=1 time=2026-03-20T11:01:54
- [PASS] Frontend route registration / `xngl-web/src/App.tsx:67`
- [PASS] Frontend menu registration / `xngl-web/src/layouts/MainLayout.tsx:64`
- [PASS] Frontend api binding / `xngl-web/src/utils/disposalApi.ts:64`
- [PASS] Build verification / backend `mvn -q -DskipTests install` and frontend `npm run build`

## Notes
- `/api/disposals` uses the current contract ticket flow as the unified disposal query data source, so the smoke test focused on keyword, site and date filters over existing seed data.
- Local browser automation with the system Chrome binary was not stable in this sandbox session, so frontend verification used route/menu/API wiring plus successful production build as the integration baseline.
