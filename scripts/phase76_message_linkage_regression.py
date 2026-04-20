import json
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase76_message_linkage_regression_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

MYSQL_CMD = [
    "mysql",
    "-h127.0.0.1",
    "-P3306",
    "-uroot",
    "-pForkliftDev2025",
    "-D",
    "xngl",
    "-N",
    "-B",
]

results = []
token = None
user_info = None
suffix = str(int(time.time() * 1000))
event_id = None
inspection_id = None
event_title = f"PH76-事件联动-{suffix}"
inspection_title = f"PH76-台账联动-{suffix}"


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def run_case(name: str, fn):
    try:
        detail = fn()
        record(name, True, detail)
    except Exception as exc:
        record(name, False, str(exc))


def mysql_exec(sql: str) -> str:
    completed = subprocess.run(
        MYSQL_CMD + ["-e", sql],
        capture_output=True,
        text=True,
        check=True,
    )
    return completed.stdout.strip()


def api_request(method: str, path: str, data=None, use_auth: bool = True):
    url = BASE_API + path
    headers = {}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if use_auth and token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = resp.read()
            return json.loads(payload.decode("utf-8")) if payload else {"code": 200, "data": None}
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {exc.code}: {detail[:400]}")


def api_get(path: str, params=None):
    query = ""
    if params:
        cleaned = {k: v for k, v in params.items() if v is not None and v != ""}
        query = "?" + urllib.parse.urlencode(cleaned, doseq=True)
    return api_request("GET", path + query, use_auth=True)


def api_post(path: str, data=None):
    return api_request("POST", path, data=data, use_auth=True)


def api_delete(path: str):
    return api_request("DELETE", path, use_auth=True)


def resolve_user_id() -> int:
    if not user_info:
        raise RuntimeError("登录用户信息缺失")
    raw = user_info.get("id") or user_info.get("userId")
    if raw is None:
        raise RuntimeError(f"登录响应未返回可用用户ID: {user_info}")
    return int(raw)


def extract_id(payload):
    if payload is None:
        raise RuntimeError("接口未返回数据")
    if isinstance(payload, dict):
        for key in ("id", "bizId", "recordId"):
            value = payload.get(key)
            if value is not None:
                return str(value)
        record = payload.get("record")
        if isinstance(record, dict):
            value = record.get("id")
            if value is not None:
                return str(value)
    raise RuntimeError(f"无法从响应中提取ID: {payload}")


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


def verify_event_message_runtime():
    def event_flow():
        global event_id
        created = api_post(
            "/events",
            {
                "eventType": "OTHER",
                "title": event_title,
                "content": "phase76 manual event linkage",
                "priority": "HIGH",
                "sourceChannel": "WEB",
            },
        )["data"]
        event_id = str(created["record"]["id"])
        api_post(f"/events/{event_id}/submit")
        api_post(f"/events/{event_id}/approve", {"comment": "phase76 approve"})
        api_post(f"/events/{event_id}/close", {"comment": "phase76 close"})
        detail = api_get(f"/events/{event_id}")["data"]
        messages = api_get("/messages", {"keyword": event_title, "pageNo": 1, "pageSize": 20})["data"]
        rows = messages["records"]
        assert detail["record"]["status"] == "CLOSED"
        assert len(rows) >= 3
        assert any(
            row["category"] == "事件通知"
            and row["bizType"] == "MANUAL_EVENT"
            and row["bizId"] == event_id
            and row["linkUrl"] == f"/alerts/events?eventId={event_id}"
            for row in rows
        )
        return f"event_id={event_id}, message_count={len(rows)}"

    run_case("event_message_runtime", event_flow)


def verify_security_message_runtime():
    def security_flow():
        global inspection_id
        user_id = resolve_user_id()
        created = api_post(
            "/security/inspections",
            {
                "objectType": "PERSON",
                "objectId": user_id,
                "userId": user_id,
                "title": inspection_title,
                "checkScene": "DAILY",
                "checkType": "SAFE_CHECK",
                "hazardCategory": "消防设施",
                "resultLevel": "FAIL",
                "dangerLevel": "HIGH",
                "issueCount": 1,
                "status": "OPEN",
                "rectifyOwner": "联动责任人",
                "rectifyOwnerPhone": "13800000003",
                "description": "phase76 security linkage",
            },
        )["data"]
        inspection_id = extract_id(created)
        api_post(
            f"/security/inspections/{inspection_id}/rectify",
            {
                "status": "CLOSED",
                "resultLevel": "PASS",
                "rectifyRemark": "phase76 rectify",
            },
        )
        detail = api_get(f"/security/inspections/{inspection_id}")["data"]
        messages = api_get("/messages", {"keyword": inspection_title, "pageNo": 1, "pageSize": 20})["data"]
        rows = messages["records"]
        assert detail["status"] == "CLOSED"
        assert len(rows) >= 1
        assert any(
            row["category"] == "安全台账通知"
            and row["bizType"] == "SECURITY_INSPECTION"
            and row["bizId"] == inspection_id
            and row["linkUrl"] == f"/alerts/security?inspectionId={inspection_id}"
            for row in rows
        )
        return f"inspection_id={inspection_id}, message_count={len(rows)}"

    run_case("security_message_runtime", security_flow)


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

        page.goto(BASE_WEB + "/messages")
        page.wait_for_load_state("networkidle")
        page.get_by_placeholder("搜索标题/内容/分类/发送人").fill(event_title)
        page.wait_for_timeout(1200)
        page.get_by_role("button", name="查看业务").first.click()
        page.wait_for_url(f"**/alerts/events?eventId={event_id}")
        page.get_by_text("事件详情").first.wait_for(timeout=10000)
        page.get_by_text(event_title).first.wait_for(timeout=10000)

        page.goto(BASE_WEB + "/messages")
        page.wait_for_load_state("networkidle")
        page.get_by_placeholder("搜索标题/内容/分类/发送人").fill(inspection_title)
        page.wait_for_timeout(1200)
        page.get_by_role("button", name="查看业务").first.click()
        page.wait_for_url(f"**/alerts/security?inspectionId={inspection_id}")
        page.get_by_text("检查详情").first.wait_for(timeout=10000)
        page.get_by_text(inspection_title).first.wait_for(timeout=10000)

        record("message_jump_pages_visible", True, "事件消息跳转 / 安全台账消息跳转")
        context.close()
        browser.close()


def cleanup():
    if event_id:
        mysql_exec(f"DELETE FROM biz_manual_event_audit_log WHERE event_id = {event_id};")
        mysql_exec(f"DELETE FROM biz_manual_event WHERE id = {event_id};")
    if inspection_id:
        try:
            api_delete(f"/security/inspections/{inspection_id}")
        except Exception:
            mysql_exec(f"DELETE FROM biz_security_inspection_action WHERE inspection_id = {inspection_id};")
            mysql_exec(f"DELETE FROM biz_security_inspection WHERE id = {inspection_id};")


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
    try:
        login_api()
        verify_event_message_runtime()
        verify_security_message_runtime()
        verify_ui()
    finally:
        cleanup()
        write_reports()
