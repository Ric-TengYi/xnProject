import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase64_security_ledger_detail_regression_{REPORT_DATE}"
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
    with urllib.request.urlopen(req, timeout=20) as resp:
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


def api_delete(path: str):
    return api_request("DELETE", path, use_auth=True)


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


def pick_person_user_id() -> int:
    rows = api_get("/users", {"pageNo": 1, "pageSize": 50, "status": "ENABLED"})["data"]["records"]
    assert rows, "未查询到可用用户"
    return int(rows[0]["id"])


def verify_runtime():
    inspection_id = None
    person_user_id = pick_person_user_id()
    suffix = str(int(time.time()))

    def create_inspection():
        nonlocal inspection_id
        data = api_post(
            "/security/inspections",
            {
                "objectType": "PERSON",
                "objectId": person_user_id,
                "userId": person_user_id,
                "title": f"phase64 安全检查-{suffix}",
                "checkScene": "PERSONNEL",
                "checkType": "SPECIAL",
                "hazardCategory": "CERTIFICATE",
                "resultLevel": "FAIL",
                "dangerLevel": "HIGH",
                "issueCount": 2,
                "status": "OPEN",
                "rectifyOwner": "回归责任人",
                "rectifyOwnerPhone": "13800000000",
                "description": "phase64 创建记录",
                "rectifyDeadline": "2026-03-24T18:00:00",
                "nextCheckTime": "2026-03-25T09:00:00",
            },
        )["data"]
        inspection_id = int(data["id"])
        assert str(data.get("userId")) == str(person_user_id)
        assert data.get("objectName")
        assert len(data.get("actions", [])) >= 1
        return f"id={inspection_id}, objectName={data['objectName']}"

    def verify_filtering():
        rows = api_get(
            "/security/inspections",
            {
                "userId": person_user_id,
                "hazardCategory": "CERTIFICATE",
                "nextCheckTimeFrom": "2026-03-25T00:00:00",
                "nextCheckTimeTo": "2026-03-25T23:59:59",
            },
        )["data"]
        assert any(str(row["id"]) == str(inspection_id) for row in rows)
        return f"filtered_rows={len(rows)}"

    def verify_detail_timeline():
        detail = api_get(f"/security/inspections/{inspection_id}")["data"]
        assert detail["objectLabel"].startswith("PERSON")
        assert detail["userName"]
        assert len(detail.get("actions", [])) >= 1
        return f"timeline_count={len(detail['actions'])}"

    def rectify_inspection():
        api_post(
            f"/security/inspections/{inspection_id}/rectify",
            {
                "status": "RECTIFYING",
                "resultLevel": "RECTIFYING",
                "rectifyRemark": "phase64 整改中",
                "nextCheckTime": "2026-03-26T10:00:00",
            },
        )
        detail = api_get(f"/security/inspections/{inspection_id}")["data"]
        assert detail["status"] == "RECTIFYING"
        assert len(detail.get("actions", [])) >= 2
        return f"timeline_count={len(detail['actions'])}"

    def export_inspections():
        blob, content_type = api_get(
            "/security/inspections/export",
            {"userId": person_user_id, "hazardCategory": "CERTIFICATE"},
            expect_blob=True,
        )
        text = blob.decode("utf-8")
        assert "关联对象名称" in text
        assert "phase64 安全检查" in text
        return content_type or "blob-ok"

    def delete_inspection():
        api_delete(f"/security/inspections/{inspection_id}")
        rows = api_get("/security/inspections", {"userId": person_user_id})["data"]
        assert all(str(row["id"]) != str(inspection_id) for row in rows)
        return f"deleted={inspection_id}"

    run_case("security_create_runtime", create_inspection)
    run_case("security_filter_runtime", verify_filtering)
    run_case("security_detail_timeline_runtime", verify_detail_timeline)
    run_case("security_rectify_runtime", rectify_inspection)
    run_case("security_export_runtime", export_inspections)
    run_case("security_delete_runtime", delete_inspection)


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
        page.goto(BASE_WEB + "/alerts/security")
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name="新增检查记录").wait_for(timeout=10000)
        page.get_by_text("安全台账管理").wait_for(timeout=10000)
        page.get_by_role("button", name="详情").first.click()
        page.get_by_text("处理时间线").wait_for(timeout=10000)
        record("security_ledger_page_visible", True, "安全台账页面与详情时间线区块可见")

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
    verify_runtime()
    verify_ui()
    write_reports()
