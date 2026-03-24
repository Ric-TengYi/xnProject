import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase69_alerts_events_unified_regression_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

results = []
token = None
user_info = None


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def run_case(name: str, fn):
    try:
        detail = fn()
        record(name, True, detail)
    except Exception as exc:
        record(name, False, str(exc))


def api_request(method: str, path: str, data=None, use_auth: bool = True, expect_blob: bool = False):
    url = BASE_API + path
    headers = {}
    body = None
    if data is not None and not expect_blob:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if use_auth and token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = resp.read()
        if expect_blob:
            return payload, resp.headers.get("Content-Type", "")
        return json.loads(payload.decode("utf-8"))


def api_get(path: str, params=None, expect_blob: bool = False):
    query = ""
    if params:
        cleaned = {k: v for k, v in params.items() if v is not None and v != ""}
        query = "?" + urllib.parse.urlencode(cleaned)
    return api_request("GET", path + query, use_auth=True, expect_blob=expect_blob)


def api_post(path: str, data=None):
    return api_request("POST", path, data=data, use_auth=True)


def api_put(path: str, data=None):
    return api_request("PUT", path, data=data, use_auth=True)


def login_api():
    global token, user_info
    payload = api_request(
        "POST",
        "/auth/login",
        data={"tenantId": "1", "username": "admin", "password": "admin"},
        use_auth=False,
    )
    token = payload["data"]["token"]
    user_info = payload["data"]["user"]
    record("login", bool(token), "admin/admin 登录成功")


def verify_manual_events():
    created_event_id = None
    suffix = str(int(time.time() * 1000))
    event_title = f"phase69 事件联调-{suffix}"

    def create_event():
        nonlocal created_event_id
        detail = api_post(
            "/events",
            {
                "eventType": "SITE_EXCEPTION",
                "title": event_title,
                "content": "phase69 创建事件",
                "sourceChannel": "WEB",
                "projectId": 1,
                "siteId": 1,
                "vehicleId": 1,
                "contactPhone": "13800000001",
                "priority": "HIGH",
                "status": "DRAFT",
                "currentAuditNode": "MANUAL_EVENT_AUDIT",
                "occurTime": "2026-03-23T18:30:00",
                "deadlineTime": "2026-03-24T18:30:00",
                "attachmentUrls": "https://example.com/phase69-event.png",
                "assigneeName": "调度员A",
                "assigneePhone": "13800000002",
                "dispatchRemark": "phase69 分派",
                "reportAddress": "测试场地一期",
            },
        )["data"]
        created_event_id = int(detail["record"]["id"])
        assert detail["record"]["status"] == "DRAFT"
        assert len(detail["auditLogs"]) >= 1
        return f"event_id={created_event_id}"

    def update_event():
        detail = api_put(
            f"/events/{created_event_id}",
            {
                "eventType": "SITE_EXCEPTION",
                "title": event_title + "-更新",
                "content": "phase69 更新事件",
                "sourceChannel": "WEB",
                "projectId": 1,
                "siteId": 1,
                "vehicleId": 1,
                "contactPhone": "13800000003",
                "priority": "HIGH",
                "status": "DRAFT",
                "currentAuditNode": "MANUAL_EVENT_AUDIT",
                "occurTime": "2026-03-23T18:31:00",
                "deadlineTime": "2026-03-24T18:40:00",
                "attachmentUrls": "https://example.com/phase69-event-updated.png",
                "assigneeName": "调度员B",
                "assigneePhone": "13800000004",
                "dispatchRemark": "phase69 更新分派",
                "reportAddress": "测试场地二期",
            },
        )["data"]
        assert detail["record"]["title"].endswith("更新")
        assert len(detail["auditLogs"]) >= 2
        return detail["record"]["title"]

    def reject_then_resubmit():
        api_post(f"/events/{created_event_id}/submit")
        pending = api_get("/events/pending-audits")["data"]
        assert any(str(item["id"]) == str(created_event_id) for item in pending)
        api_post(f"/events/{created_event_id}/reject", {"comment": "phase69 退回"})
        detail = api_get(f"/events/{created_event_id}")["data"]
        assert detail["record"]["status"] == "REJECTED"
        api_put(
            f"/events/{created_event_id}",
            {
                "eventType": "SITE_EXCEPTION",
                "title": event_title + "-二提",
                "content": "phase69 二次提交",
                "sourceChannel": "WEB",
                "projectId": 1,
                "siteId": 1,
                "vehicleId": 1,
                "contactPhone": "13800000005",
                "priority": "HIGH",
                "status": "REJECTED",
                "currentAuditNode": "APPLICANT_REWORK",
                "occurTime": "2026-03-23T18:32:00",
                "deadlineTime": "2026-03-24T18:50:00",
                "assigneeName": "调度员C",
                "assigneePhone": "13800000006",
                "dispatchRemark": "phase69 二提分派",
                "reportAddress": "测试场地三期",
            },
        )
        api_post(f"/events/{created_event_id}/submit")
        api_post(f"/events/{created_event_id}/approve", {"comment": "phase69 通过"})
        api_post(f"/events/{created_event_id}/close", {"comment": "phase69 关闭"})
        detail = api_get(f"/events/{created_event_id}")["data"]
        assert detail["record"]["status"] == "CLOSED"
        assert len(detail["auditLogs"]) >= 6
        return f"audit_logs={len(detail['auditLogs'])}"

    def summary_and_export():
        summary = api_get("/events/summary", {"keyword": event_title})["data"]
        assert int(summary["total"]) >= 1
        blob, content_type = api_get("/events/export", {"keyword": event_title}, expect_blob=True)
        text = blob.decode("utf-8")
        assert "事件编号" in text
        assert event_title in text
        return content_type or "blob-ok"

    run_case("manual_event_create_runtime", create_event)
    run_case("manual_event_update_runtime", update_event)
    run_case("manual_event_flow_runtime", reject_then_resubmit)
    run_case("manual_event_summary_export_runtime", summary_and_export)


def verify_alerts_runtime():
    handled_alert_id = None

    def generate_alerts():
        data = api_post("/alerts/generate", {"targetTypes": ["PROJECT", "SITE", "VEHICLE", "CONTRACT", "USER"]})["data"]
        assert "createdCount" in data
        assert "updatedCount" in data
        assert "closedCount" in data
        return f"created={data['createdCount']}, updated={data['updatedCount']}, closed={data['closedCount']}"

    def query_alerts():
        nonlocal handled_alert_id
        rows = api_get("/alerts", {"status": "PENDING"})["data"]
        if not rows:
            rows = api_get("/alerts", {"status": "PROCESSING"})["data"]
        if not rows:
            rows = api_get("/alerts")["data"]
        assert rows, "未查询到预警数据"
        handled_alert_id = int(rows[0]["id"])
        return f"alert_id={handled_alert_id}, total_rows={len(rows)}"

    def handle_and_close_alert():
        api_post(f"/alerts/{handled_alert_id}/handle", {"status": "PROCESSING", "handleRemark": "phase69 跟进"})
        detail = api_get(f"/alerts/{handled_alert_id}")["data"]
        assert detail["status"] == "PROCESSING"
        api_post(f"/alerts/{handled_alert_id}/close", {"handleRemark": "phase69 关闭"})
        detail = api_get(f"/alerts/{handled_alert_id}")["data"]
        assert detail["status"] == "CLOSED"
        return detail["title"]

    def alert_summary_analytics_export():
        summary = api_get("/alerts/summary")["data"]
        analytics = api_get("/alerts/analytics")["data"]
        top_risk = api_get("/alerts/top-risk")["data"]
        top_contract = api_get("/alerts/top-risk-targets", {"targetType": "CONTRACT"})["data"]
        fence_status = api_get("/alerts/fence-status")["data"]
        blob, content_type = api_get("/alerts/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert int(summary["total"]) >= 1
        assert len(analytics["levelBuckets"]) >= 1
        assert "预警编号" in text
        assert isinstance(top_risk, list)
        assert isinstance(top_contract, list)
        assert len(fence_status) >= 1
        return f"summary_total={summary['total']}, fences={len(fence_status)}"

    run_case("alerts_generate_runtime", generate_alerts)
    run_case("alerts_query_runtime", query_alerts)
    run_case("alerts_handle_close_runtime", handle_and_close_alert)
    run_case("alerts_summary_analytics_export_runtime", alert_summary_analytics_export)


def verify_alert_configs():
    suffix = str(int(time.time() * 1000))
    created_rule_id = None
    created_fence_id = None
    created_push_id = None

    def alert_rule_flow():
        nonlocal created_rule_id
        created = api_post(
            "/alert-rules",
            {
                "ruleCode": f"PHASE69_RULE_{suffix}",
                "ruleName": "Phase69 规则",
                "ruleScene": "VEHICLE",
                "metricCode": "phase69_metric",
                "thresholdJson": "{\"threshold\":18,\"unit\":\"count\"}",
                "level": "L2",
                "scopeType": "GLOBAL",
                "remark": "phase69 rule",
            },
        )["data"]
        created_rule_id = int(created["id"])
        updated = api_put(
            f"/alert-rules/{created_rule_id}",
            {
                "ruleCode": f"PHASE69_RULE_{suffix}",
                "ruleName": "Phase69 规则-更新",
                "ruleScene": "VEHICLE",
                "metricCode": "phase69_metric_v2",
                "thresholdJson": "{\"threshold\":21,\"unit\":\"count\"}",
                "level": "L3",
                "scopeType": "GLOBAL",
                "status": "ENABLED",
                "remark": "phase69 rule updated",
            },
        )["data"]
        api_put(f"/alert-rules/{created_rule_id}/status", {"status": "DISABLED"})
        api_put(f"/alert-rules/{created_rule_id}/status", {"status": "ENABLED"})
        rows = api_get("/alert-rules", {"ruleScene": "VEHICLE"})["data"]
        target = next(item for item in rows if int(item["id"]) == created_rule_id)
        assert updated["ruleName"].endswith("更新")
        assert target["status"] == "ENABLED"
        return f"rule_id={created_rule_id}"

    def alert_fence_flow():
        nonlocal created_fence_id
        created = api_post(
            "/alert-fences",
            {
                "ruleCode": "VEHICLE_ROUTE_DEVIATION",
                "fenceCode": f"PHASE69-FENCE-{suffix}",
                "fenceName": "Phase69 围栏",
                "fenceType": "ENTRY",
                "geoJson": "{\"center\":[120.180,30.280],\"radius\":260}",
                "bufferMeters": 35,
                "bizScope": "SITE:1",
                "activeTimeRange": "00:00-23:59",
                "directionRule": "IN",
            },
        )["data"]
        created_fence_id = int(created["id"])
        updated = api_put(
            f"/alert-fences/{created_fence_id}",
            {
                "ruleCode": "VEHICLE_ROUTE_DEVIATION",
                "fenceCode": f"PHASE69-FENCE-{suffix}",
                "fenceName": "Phase69 围栏-更新",
                "fenceType": "STAY",
                "geoJson": "{\"center\":[120.181,30.281],\"radius\":300}",
                "bufferMeters": 55,
                "bizScope": "ROAD:PHASE69",
                "activeTimeRange": "06:00-20:00",
                "directionRule": "BOTH",
                "status": "ENABLED",
            },
        )["data"]
        api_put(f"/alert-fences/{created_fence_id}/status", {"status": "DISABLED"})
        api_put(f"/alert-fences/{created_fence_id}/status", {"status": "ENABLED"})
        rows = api_get("/alert-fences")["data"]
        target = next(item for item in rows if int(item["id"]) == created_fence_id)
        assert updated["fenceName"].endswith("更新")
        assert target["status"] == "ENABLED"
        return f"fence_id={created_fence_id}"

    def alert_push_flow():
        nonlocal created_push_id
        created = api_post(
            "/alert-push-rules",
            {
                "ruleCode": f"PHASE69_RULE_{suffix}",
                "level": "L2",
                "channelTypes": "INBOX,SMS",
                "receiverType": "ROLE",
                "receiverExpr": "manager,dispatcher",
                "pushTimeRule": "IMMEDIATE",
                "escalationMinutes": 15,
            },
        )["data"]
        created_push_id = int(created["id"])
        updated = api_put(
            f"/alert-push-rules/{created_push_id}",
            {
                "ruleCode": f"PHASE69_RULE_{suffix}",
                "level": "L3",
                "channelTypes": "INBOX,SMS",
                "receiverType": "ROLE",
                "receiverExpr": "admin,manager",
                "pushTimeRule": "IMMEDIATE",
                "escalationMinutes": 30,
                "status": "ENABLED",
            },
        )["data"]
        api_put(f"/alert-push-rules/{created_push_id}/status", {"status": "DISABLED"})
        api_put(f"/alert-push-rules/{created_push_id}/status", {"status": "ENABLED"})
        rows = api_get("/alert-push-rules")["data"]
        target = next(item for item in rows if int(item["id"]) == created_push_id)
        assert updated["level"] == "L3"
        assert target["status"] == "ENABLED"
        return f"push_id={created_push_id}"

    run_case("alert_rule_flow_runtime", alert_rule_flow)
    run_case("alert_fence_flow_runtime", alert_fence_flow)
    run_case("alert_push_flow_runtime", alert_push_flow)


def verify_ui():
    with sync_playwright() as p:
        try:
            browser = p.chromium.launch(headless=True)
        except Exception:
            browser = p.chromium.launch(
                headless=True,
                executable_path="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            )

        context = browser.new_context()
        context.add_init_script(
            f"""
            (() => {{
              localStorage.setItem('token', {json.dumps(token)});
              localStorage.setItem('userInfo', {json.dumps(json.dumps(user_info, ensure_ascii=False))});
            }})();
            """
        )
        page = context.new_page()
        cases = [
            ("/alerts", "预警与监控中心"),
            ("/alerts/events", "事件管理"),
            ("/alerts/config", "预警配置"),
            ("/alerts/security", "安全台账管理"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        record("alerts_events_pages_visible", True, " / ".join(visible))
        context.close()
        browser.close()


def write_reports():
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    summary = {
        "report": REPORT_STEM,
        "date": REPORT_DATE,
        "results": results,
        "passed": sum(1 for item in results if item["status"] == "PASS"),
        "failed": sum(1 for item in results if item["status"] == "FAIL"),
    }
    JSON_PATH.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [
        f"# {REPORT_STEM}",
        "",
        f"- 日期：{REPORT_DATE}",
        f"- 通过：{summary['passed']}",
        f"- 失败：{summary['failed']}",
        "",
        "## 结果",
        "",
        "| 用例 | 状态 | 说明 |",
        "|---|---|---|",
    ]
    for item in results:
        lines.append(f"| {item['name']} | {item['status']} | {item['detail']} |")
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    login_api()
    verify_manual_events()
    verify_alerts_runtime()
    verify_alert_configs()
    verify_ui()
    write_reports()
