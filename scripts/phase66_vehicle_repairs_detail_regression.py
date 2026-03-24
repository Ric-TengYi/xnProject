import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase66_vehicle_repairs_detail_regression_{REPORT_DATE}"
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


def pick_vehicle_id() -> int:
    rows = api_get("/vehicles", {"pageNo": 1, "pageSize": 20})["data"]["records"]
    assert rows, "未查询到车辆数据"
    return int(rows[0]["id"])


def verify_runtime():
    vehicle_id = pick_vehicle_id()
    suffix = str(int(time.time()))
    repair_id = None

    def create_order():
        nonlocal repair_id
        data = api_post(
            "/vehicle-repairs",
            {
                "vehicleId": vehicle_id,
                "urgencyLevel": "HIGH",
                "repairReason": f"phase66 维修申请-{suffix}",
                "repairContent": "检查制动系统并更换磨损配件",
                "diagnosisResult": "初检发现制动片磨损严重，伴随异响",
                "safetyImpact": "高",
                "budgetAmount": 2600,
                "applyDate": "2026-03-23",
                "applicantName": "phase66 申请人",
                "status": "PENDING_APPROVAL",
                "remark": "phase66 新建维修单",
            },
        )["data"]
        repair_id = int(data["id"])
        assert data["diagnosisResult"] == "初检发现制动片磨损严重，伴随异响"
        return f"repair_id={repair_id}"

    def approve_order():
        data = api_post(f"/vehicle-repairs/{repair_id}/approve", {"comment": "同意进厂维修"})["data"]
        assert data["status"] == "APPROVED"
        return f"approved_by={data['approvedBy']}"

    def complete_order():
        data = api_post(
            f"/vehicle-repairs/{repair_id}/complete",
            {
                "completedDate": "2026-03-23",
                "vendorName": "phase66 修理厂",
                "repairManager": "phase66 维修主管",
                "technicianName": "phase66 技师",
                "acceptanceResult": "维修完成，路试正常",
                "signoffStatus": "SIGNED",
                "attachmentUrls": "https://example.com/r1.jpg",
                "actualAmount": 0,
                "partsCost": 1200,
                "laborCost": 680,
                "otherCost": 100,
                "remark": "phase66 完工",
            },
        )["data"]
        assert data["status"] == "COMPLETED"
        assert abs(float(data["actualAmount"]) - 1980.0) < 0.01
        assert data["signoffStatusLabel"] == "已签字"
        return f"actual={data['actualAmount']}"

    def detail_order():
        data = api_get(f"/vehicle-repairs/{repair_id}")["data"]
        assert data["repairManager"] == "phase66 维修主管"
        assert data["technicianName"] == "phase66 技师"
        assert abs(float(data["costVariance"]) - (-620.0)) < 0.01
        return f"variance={data['costVariance']}"

    def export_orders():
        blob, content_type = api_get("/vehicle-repairs/export", {"vehicleId": vehicle_id}, expect_blob=True)
        text = blob.decode("utf-8")
        assert "配件费" in text
        assert "签字状态" in text
        return content_type or "blob-ok"

    run_case("repair_create_runtime", create_order)
    run_case("repair_approve_runtime", approve_order)
    run_case("repair_complete_runtime", complete_order)
    run_case("repair_detail_runtime", detail_order)
    run_case("repair_export_runtime", export_orders)


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
        page.goto(BASE_WEB + "/vehicles/repairs")
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name="新增维修单").wait_for(timeout=10000)
        page.get_by_role("button", name="详情").first.click()
        page.get_by_text("维修单详情").wait_for(timeout=10000)
        record("vehicle_repairs_page_visible", True, "维修管理页面详情抽屉可见")
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
