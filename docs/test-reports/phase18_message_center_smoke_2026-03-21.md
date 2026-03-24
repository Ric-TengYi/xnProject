# Phase 18 Message Center Smoke (2026-03-21)

- Total: 9
- Passed: 9
- Failed: 0

## Checks
- [PASS] SQL patch apply / `035_message_center_foundation.sql` executed successfully
- [PASS] API login / userId=6
- [PASS] Message summary / total=4 unread=3 read=1
- [PASS] Message keyword query / total=1 status=UNREAD priority=HIGH
- [PASS] Message mark read / status=READ readTime populated
- [PASS] Message status filter / filteredTotal=1 status=READ
- [PASS] Frontend route registration / `xngl-web/src/App.tsx:98`
- [PASS] Frontend menu and api bindings / `xngl-web/src/layouts/MainLayout.tsx:97`, `xngl-web/src/utils/messageApi.ts:64`
- [PASS] Build verification / backend `mvn -q -DskipTests install` and frontend `npm run build`

## Notes
- Smoke inserted a temporary message row for `admin` and deleted it after verification.
- The message center now filters records by the current logged-in user plus `ALL` broadcasts and supports summary, list, and mark-read actions.
