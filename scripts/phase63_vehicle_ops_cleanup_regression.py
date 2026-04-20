import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase63_vehicle_ops_cleanup_regression_{REPORT_DATE}"
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


def api_put(path: str, data=None):
    return api_request("PUT", path, data=data, use_auth=True)


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


def verify_runtime():
    insurance_id = None
    plan_id = None
    repair_id = None
    vehicle_id = 1
    suffix = str(int(time.time()))

    def create_insurance():
      nonlocal insurance_id
      data = api_post(
          "/vehicle-insurances",
          {
              "vehicleId": vehicle_id,
              "policyNo": f"POL-REG-063-{suffix}",
              "insuranceType": "商业险",
              "insurerName": "回归保险公司",
              "coverageAmount": 1000000,
              "premiumAmount": 5000,
              "claimAmount": 0,
              "startDate": "2026-03-01",
              "endDate": "2027-03-01",
              "remark": "phase63",
          },
      )["data"]
      insurance_id = int(data["id"])
      return f"id={insurance_id}"

    def delete_insurance():
      api_delete(f"/vehicle-insurances/{insurance_id}")
      rows = api_get("/vehicle-insurances", {"pageNo": 1, "pageSize": 200})["data"]["records"]
      assert all(str(row["id"]) != str(insurance_id) for row in rows)
      return f"deleted={insurance_id}"

    def create_plan():
      nonlocal plan_id
      data = api_post(
          "/vehicle-maintenance-plans",
          {
              "vehicleId": vehicle_id,
              "planType": f"季度保养-{suffix}",
              "cycleType": "MONTH",
              "cycleValue": 3,
              "lastMaintainDate": "2026-02-01",
              "nextMaintainDate": "2026-06-01",
              "lastOdometer": 1000,
              "nextOdometer": 3000,
              "responsibleName": "回归机务",
              "status": "ACTIVE",
              "remark": "phase63",
          },
      )["data"]
      plan_id = int(data["id"])
      return f"id={plan_id}"

    def delete_plan():
      api_delete(f"/vehicle-maintenance-plans/{plan_id}")
      rows = api_get("/vehicle-maintenance-plans", {"pageNo": 1, "pageSize": 200})["data"]["records"]
      assert all(str(row["id"]) != str(plan_id) for row in rows)
      return f"deleted={plan_id}"

    def create_repair():
      nonlocal repair_id
      data = api_post(
          "/vehicle-repairs",
          {
              "vehicleId": vehicle_id,
              "urgencyLevel": "LOW",
              "repairReason": f"phase63 清理回归-{suffix}",
              "repairContent": "测试删除链路",
              "budgetAmount": 600,
              "applyDate": "2026-03-23",
              "applicantName": "回归测试",
              "status": "DRAFT",
              "remark": "phase63",
          },
      )["data"]
      repair_id = int(data["id"])
      return f"id={repair_id}"

    def delete_repair():
      api_delete(f"/vehicle-repairs/{repair_id}")
      rows = api_get("/vehicle-repairs", {"pageNo": 1, "pageSize": 200})["data"]["records"]
      assert all(str(row["id"]) != str(repair_id) for row in rows)
      return f"deleted={repair_id}"

    run_case("insurance_create_runtime", create_insurance)
    run_case("insurance_delete_runtime", delete_insurance)
    run_case("maintenance_plan_create_runtime", create_plan)
    run_case("maintenance_plan_delete_runtime", delete_plan)
    run_case("repair_create_runtime", create_repair)
    run_case("repair_delete_runtime", delete_repair)


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

      for path, button_name in [
          ("/vehicles/insurances", "导出台账"),
          ("/vehicles/maintenance", "导出计划"),
          ("/vehicles/repairs", "导出台账"),
      ]:
        page.goto(BASE_WEB + path)
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name=button_name).wait_for(timeout=10000)
      record("vehicle_ops_pages_visible", True, "保险/维保/维修页面已正常渲染")

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
