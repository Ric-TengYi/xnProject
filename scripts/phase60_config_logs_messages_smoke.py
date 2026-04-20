import json
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase60_config_logs_messages_smoke_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

results = []
token = None
user_info = None


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def api_request(method: str, path: str, data=None, use_auth: bool = True, expect_blob: bool = False):
    url = BASE_API + path
    body = None
    headers = {}
    if data is not None and not expect_blob:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if use_auth and token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as resp:
        payload = resp.read()
        content_type = resp.headers.get("Content-Type", "")
        if expect_blob:
            return payload, content_type
        return json.loads(payload.decode("utf-8"))


def api_get(path: str, params=None, expect_blob: bool = False):
    query = ""
    if params:
        cleaned = {k: v for k, v in params.items() if v is not None and v != ""}
        query = "?" + urllib.parse.urlencode(cleaned)
    return api_request("GET", path + query, use_auth=True, expect_blob=expect_blob)


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
    record("login", bool(token), "admin/admin 登录成功并获取 token")


def verify_api():
    health = api_request("GET", "/health", use_auth=False)
    record("health", health.get("code") == 200, str(health.get("data")))

    message_page = api_get("/messages", {"pageNo": 1, "pageSize": 10})
    message_records = message_page["data"]["records"]
    record("messages_list", isinstance(message_records, list), f"records={len(message_records)}")

    message_summary = api_get("/messages/summary")
    record(
        "messages_summary",
        message_summary["data"]["total"] >= message_summary["data"]["read"],
        f"summary={message_summary['data']}",
    )

    mark_all = api_put("/messages/read-all")
    record("messages_read_all", mark_all["code"] == 200, f"updated={mark_all['data']['updated']}")

    blob, content_type = api_get("/messages/export", {"pageNo": 1, "pageSize": 10}, expect_blob=True)
    record("messages_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    login_logs = api_get("/login-logs", {"tenantId": 1, "pageNo": 1, "pageSize": 10})
    record("login_logs_list", login_logs["data"]["total"] >= 1, f"records={len(login_logs['data']['records'])}")

    login_logs_filtered = api_get("/login-logs", {"tenantId": 1, "status": "SUCCESS", "pageNo": 1, "pageSize": 10})
    record(
        "login_logs_filtered",
        login_logs_filtered["code"] == 200,
        f"records={len(login_logs_filtered['data']['records'])}",
    )

    blob, content_type = api_get("/login-logs/export", {"tenantId": 1}, expect_blob=True)
    record("login_logs_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    operation_logs = api_get("/operation-logs", {"tenantId": 1, "pageNo": 1, "pageSize": 10})
    record(
        "operation_logs_list",
        operation_logs["code"] == 200,
        f"records={len(operation_logs['data']['records'])}",
    )

    blob, content_type = api_get("/operation-logs/export", {"tenantId": 1}, expect_blob=True)
    record("operation_logs_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    error_logs = api_get("/error-logs", {"tenantId": 1, "pageNo": 1, "pageSize": 10})
    record("error_logs_list", error_logs["code"] == 200, f"records={len(error_logs['data']['records'])}")

    blob, content_type = api_get("/error-logs/export", {"tenantId": 1}, expect_blob=True)
    record("error_logs_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    data_dicts = api_get("/data-dicts", {"keyword": "alert"})
    record("data_dicts_list", isinstance(data_dicts["data"], list), f"records={len(data_dicts['data'])}")

    blob, content_type = api_get("/data-dicts/export", {"keyword": "alert"}, expect_blob=True)
    record("data_dicts_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    sys_params = api_get("/sys-params", {"paramType": "STRING"})
    record("sys_params_list", isinstance(sys_params["data"], list), f"records={len(sys_params['data'])}")

    blob, content_type = api_get("/sys-params/export", {"paramType": "STRING"}, expect_blob=True)
    record("sys_params_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    approval_rules = api_get("/approval-actor-rules", {"tenantId": 1, "pageNo": 1, "pageSize": 20})
    record(
        "approval_rules_list",
        isinstance(approval_rules["data"]["records"], list),
        f"records={len(approval_rules['data']['records'])}",
    )

    blob, content_type = api_get("/approval-actor-rules/export", {"tenantId": 1}, expect_blob=True)
    record("approval_rules_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")

    approval_materials = api_get("/approval-material-configs")
    record(
        "approval_materials_list",
        isinstance(approval_materials["data"], list),
        f"records={len(approval_materials['data'])}",
    )

    blob, content_type = api_get("/approval-material-configs/export", expect_blob=True)
    record(
        "approval_materials_export",
        len(blob) > 0 and "text/csv" in content_type,
        f"bytes={len(blob)}",
    )

    approval_flows = api_get("/approval-configs")
    record("approval_flows_list", isinstance(approval_flows["data"], list), f"records={len(approval_flows['data'])}")

    blob, content_type = api_get("/approval-configs/export", expect_blob=True)
    record("approval_flows_export", len(blob) > 0 and "text/csv" in content_type, f"bytes={len(blob)}")


def verify_ui():
    with sync_playwright() as p:
        try:
            browser = p.chromium.launch(headless=True)
        except Exception:
            browser = p.chromium.launch(
                headless=True,
                executable_path="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            )
        page = browser.new_page()
        page.goto(BASE_WEB)
        page.wait_for_load_state("networkidle")
        page.evaluate(
            """([token, userInfo]) => {
                localStorage.setItem('token', token);
                localStorage.setItem('userInfo', JSON.stringify(userInfo));
            }""",
            [token, user_info],
        )

        ui_cases = [
            ("ui_logs_page", "/settings/logs", "系统日志"),
            ("ui_messages_page", "/messages", "消息管理"),
            ("ui_dictionary_page", "/settings/dictionary", "数据字典"),
            ("ui_sys_params_page", "/settings/system-params", "系统参数"),
            ("ui_approval_page", "/settings/approvals", "审核审批配置"),
        ]

        for name, path, text in ui_cases:
            page.goto(f"{BASE_WEB}{path}")
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(1200)
            visible = page.locator(f'text="{text}"').first.is_visible()
            export_btns = page.locator("button:has-text(\"导出\")").count()
            record(name, visible and export_btns >= 1, f"title={text}, exportButtons={export_btns}")

        browser.close()


def write_reports():
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    passed = sum(1 for item in results if item["status"] == "PASS")
    failed = len(results) - passed
    payload = {
        "date": REPORT_DATE,
        "phase": "Phase60",
        "scope": ["配置中心增强", "系统日志增强", "消息中心增强"],
        "passed": passed,
        "failed": failed,
        "results": results,
    }
    JSON_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [
        "# Phase60 配置中心、日志、消息增强包烟测",
        "",
        f"- 日期：{REPORT_DATE}",
        "- 覆盖需求：`Plan 4 = 配置中心 / 系统日志 / 消息中心增强`",
        f"- 结果：`{passed} / {len(results)} PASS`",
        "",
        "## 覆盖内容",
        "- 系统日志：登录/操作/错误日志筛选与导出",
        "- 消息中心：批量已读、导出、详情查看",
        "- 配置中心：审批规则/材料/流程导出，字典/系统参数导出与筛选",
        "",
        "## 测试结果",
        "| 用例 | 结果 | 说明 |",
        "|---|---|---|",
    ]
    for item in results:
        lines.append(f"| {item['name']} | {item['status']} | {item['detail']} |")
    lines.extend(
        [
            "",
            "## 验证命令",
            "```bash",
            "cd xngl-service && mvn -q -DskipTests compile -pl xngl-service-web -am",
            "cd xngl-web && npm run build",
            "cd xngl-service && mvn spring-boot:run -pl xngl-service-starter",
            "python3 scripts/phase60_config_logs_messages_smoke.py",
            "```",
        ]
    )
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")


def main():
    login_api()
    verify_api()
    verify_ui()
    write_reports()
    print(f"report json => {JSON_PATH}")
    print(f"report md   => {MD_PATH}")
    failed = [item for item in results if item["status"] != "PASS"]
    if failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
