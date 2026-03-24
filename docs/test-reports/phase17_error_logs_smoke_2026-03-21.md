# Phase 17 Error Logs Smoke (2026-03-21)

- Total: 8
- Passed: 8
- Failed: 0

## Checks
- [PASS] SQL patch apply / `034_error_log_foundation.sql` executed successfully
- [PASS] API login / userId=6
- [PASS] Error log list / total=2 firstLevel=WARN
- [PASS] Error log level filter / filteredTotal=1 exception=Phase17TestException
- [PASS] Error log keyword filter / filteredTotal=1 message=phase17 temporary warning record
- [PASS] Frontend error log binding / `xngl-web/src/pages/SystemLogs.tsx:77`
- [PASS] Backend error log controller / `xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/ErrorLogsController.java:19`
- [PASS] Build verification / backend `mvn -q -DskipTests install` and frontend `npm run build`

## Notes
- Smoke inserted a temporary `WARN` error log row for filter verification and deleted it after the test.
- `GlobalExceptionHandler` now persists unhandled exceptions into `sys_error_log`; this smoke focused on query/filter correctness and the persistence wiring was verified from source integration points.
