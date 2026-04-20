import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase65_vehicle_maintenance_detail_regression_{REPORT_DATE}"
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
    plan_id = None
    record_id = None

    def create_plan():
      nonlocal plan_id
      data = api_post(
          "/vehicle-maintenance-plans",
          {
              "vehicleId": vehicle_id,
              "planType": f"phase65 深度维保-{suffix}",
              "cycleType": "MONTH",
              "cycleValue": 2,
              "lastMaintainDate": "2026-02-01",
              "nextMaintainDate": "2026-04-01",
              "lastOdometer": 18800,
              "nextOdometer": 22000,
              "responsibleName": "phase65 负责人",
              "status": "ACTIVE",
              "remark": "phase65 新建计划",
          },
      )["data"]
      plan_id = int(data["id"])
      assert str(data.get("recordCount", 0)) in ["0", "0.0"]
      return f"plan_id={plan_id}"

    def detail_plan():
      data = api_get(f"/vehicle-maintenance-plans/{plan_id}")["data"]
      assert data["planNo"]
      assert "recordCount" in data
      return f"plan_no={data['planNo']}"

    def execute_plan():
      nonlocal record_id
      data = api_post(
          f"/vehicle-maintenance-plans/{plan_id}/execute",
          {
              "serviceDate": "2026-03-23",
              "odometer": 20500,
              "vendorName": "phase65 机务中心",
              "costAmount": 0,
              "laborCost": 320,
              "materialCost": 860,
              "externalCost": 120,
              "items": "机油更换/滤芯保养/底盘复检",
              "issueDescription": "发现滤芯磨损和底盘螺栓松动",
              "resultSummary": "已完成滤芯更换并复紧底盘螺栓",
              "operatorName": "phase65 经办人",
              "technicianName": "phase65 技师",
              "checkerName": "phase65 验收人",
              "signoffStatus": "SIGNED",
              "attachmentUrls": "https://example.com/a.jpg\nhttps://example.com/b.jpg",
              "status": "DONE",
              "remark": "phase65 执行记录",
              "nextMaintainDate": "2026-05-23",
              "nextOdometer": 24500,
          },
      )["data"]
      record_id = int(data["id"])
      assert abs(float(data["costAmount"]) - 1300.0) < 0.01
      assert data["technicianName"] == "phase65 技师"
      assert data["signoffStatus"] == "SIGNED"
      return f"record_id={record_id}, cost={data['costAmount']}"

    def detail_record():
      data = api_get(f"/vehicle-maintenance-plans/records/{record_id}")["data"]
      assert abs(float(data["laborCost"]) - 320.0) < 0.01
      assert data["resultSummary"] == "已完成滤芯更换并复紧底盘螺栓"
      assert data["signoffStatusLabel"] == "已签字"
      return f"signoff={data['signoffStatusLabel']}"

    def history_records():
      data = api_get(f"/vehicle-maintenance-plans/{plan_id}/records")["data"]
      assert any(str(item["id"]) == str(record_id) for item in data)
      return f"history_count={len(data)}"

    def export_records():
      blob, content_type = api_get("/vehicle-maintenance-plans/export/records", {"vehicleId": vehicle_id}, expect_blob=True)
      text = blob.decode("utf-8")
      assert "人工费" in text
      assert "签字状态" in text
      return content_type or "blob-ok"

    run_case("maintenance_plan_create_runtime", create_plan)
    run_case("maintenance_plan_detail_runtime", detail_plan)
    run_case("maintenance_execute_detail_runtime", execute_plan)
    run_case("maintenance_record_detail_runtime", detail_record)
    run_case("maintenance_plan_history_runtime", history_records)
    run_case("maintenance_record_export_runtime", export_records)


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
        page.goto(BASE_WEB + "/vehicles/maintenance")
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name="新增计划").wait_for(timeout=10000)
        page.get_by_role("button", name="详情").first.click()
        page.get_by_text("执行历史").wait_for(timeout=10000)
        record("vehicle_maintenance_page_visible", True, "维保计划页面详情抽屉与执行历史可见")
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
