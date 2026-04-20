# Phase 15 Checkin Records Smoke (2026-03-21)

- Total: 8
- Passed: 8
- Failed: 0

## Checks
- [PASS] API login / userId=6
- [PASS] Checkin list query / total=1 id=5 status=正常
- [PASS] Checkin void api / id=5 status=已作废 reason=phase15-auto-void
- [PASS] Checkin void verify / filteredTotal=1 exception=异常打卡
- [PASS] Frontend route registration / `xngl-web/src/App.tsx:65`
- [PASS] Frontend menu registration / `xngl-web/src/layouts/MainLayout.tsx:62`
- [PASS] Frontend api bindings / `xngl-web/src/utils/checkinApi.ts:68` and `xngl-web/src/utils/checkinApi.ts:76`
- [PASS] Build verification / backend `mvn -q -DskipTests install` and frontend `npm run build`

## Notes
- During this phase, the backend API path `/api/checkins` was smoke-tested with a temporary ticket row and the row was cleaned up after verification.
- Local browser automation with the system Chrome binary was not stable in this sandbox session, so frontend verification used route/menu/API wiring plus successful production build as the integration baseline.
