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
REPORT_STEM = f"phase74_fleet_core_regression_{REPORT_DATE}"
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
temp_profile_id = None
temp_plan_id = None
temp_dispatch_id = None


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
            return json.loads(resp.read().decode("utf-8"))
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


def verify_fleet_query_runtime():
    def summary_and_list():
        summary = api_get("/fleet-management/summary")["data"]
        profiles = api_get("/fleet-management/profiles", {"pageNo": 1, "pageSize": 10})["data"]
        plans = api_get("/fleet-management/transport-plans", {"pageNo": 1, "pageSize": 10})["data"]
        dispatch_orders = api_get("/fleet-management/dispatch-orders", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(summary["totalFleets"]) >= 1
        assert int(profiles["total"]) >= 1
        assert int(plans["total"]) >= 1
        assert int(dispatch_orders["total"]) >= 1
        return (
            f"fleets={summary['totalFleets']}, plans={summary['totalPlans']}, "
            f"pending_dispatch={summary['pendingDispatchOrders']}"
        )

    run_case("fleet_query_runtime", summary_and_list)


def verify_fleet_profile_runtime():
    def create_update_query_cleanup():
        global temp_profile_id
        fleet_name = f"联调车队-{suffix}"
        created = api_post(
            "/fleet-management/profiles",
            {
                "fleetName": fleet_name,
                "captainName": "联调队长",
                "captainPhone": "13800000001",
                "driverCountPlan": 12,
                "vehicleCountPlan": 8,
                "status": "ENABLED",
                "attendanceMode": "MANUAL",
                "remark": "phase74 profile",
            },
        )["data"]
        temp_profile_id = str(created["id"])
        updated = api_put(
            f"/fleet-management/profiles/{temp_profile_id}",
            {
                "fleetName": fleet_name,
                "captainName": "联调队长-更新",
                "captainPhone": "13800000002",
                "driverCountPlan": 15,
                "vehicleCountPlan": 9,
                "status": "DISABLED",
                "attendanceMode": "AUTO",
                "remark": "phase74 profile updated",
            },
        )["data"]
        filtered = api_get(
            "/fleet-management/profiles",
            {"keyword": fleet_name, "pageNo": 1, "pageSize": 10},
        )["data"]
        mysql_exec(f"DELETE FROM biz_fleet_profile WHERE id = {temp_profile_id};")
        after = api_get(
            "/fleet-management/profiles",
            {"keyword": fleet_name, "pageNo": 1, "pageSize": 10},
        )["data"]
        temp_profile_id = None
        assert created["fleetName"] == fleet_name
        assert updated["captainName"] == "联调队长-更新"
        assert updated["status"] == "DISABLED"
        assert int(filtered["total"]) >= 1
        assert int(after["total"]) == 0
        return f"profile_id={created['id']}, keyword={fleet_name}"

    run_case("fleet_profile_maintain_runtime", create_update_query_cleanup)


def verify_fleet_transport_plan_runtime():
    def create_update_query_cleanup():
        global temp_profile_id, temp_plan_id
        fleet_name = f"联调计划车队-{suffix}"
        profile = api_post(
            "/fleet-management/profiles",
            {
                "fleetName": fleet_name,
                "captainName": "计划队长",
                "driverCountPlan": 10,
                "vehicleCountPlan": 6,
                "status": "ENABLED",
                "remark": "phase74 plan profile",
            },
        )["data"]
        temp_profile_id = str(profile["id"])
        plan_no = f"FTP-PH74-{suffix}"
        created = api_post(
            "/fleet-management/transport-plans",
            {
                "fleetId": int(temp_profile_id),
                "planNo": plan_no,
                "planDate": REPORT_DATE,
                "sourcePoint": "项目-001",
                "destinationPoint": "消纳场-001",
                "cargoType": "工程渣土",
                "plannedTrips": 18,
                "plannedVolume": 3600,
                "status": "ACTIVE",
                "remark": "phase74 transport plan",
            },
        )["data"]
        temp_plan_id = str(created["id"])
        updated = api_put(
            f"/fleet-management/transport-plans/{temp_plan_id}",
            {
                "fleetId": int(temp_profile_id),
                "planNo": plan_no,
                "planDate": REPORT_DATE,
                "sourcePoint": "项目-001东区",
                "destinationPoint": "消纳场-002",
                "cargoType": "回填土",
                "plannedTrips": 20,
                "plannedVolume": 4000,
                "status": "COMPLETED",
                "remark": "phase74 transport plan updated",
            },
        )["data"]
        filtered = api_get(
            "/fleet-management/transport-plans",
            {"keyword": plan_no, "fleetId": temp_profile_id, "pageNo": 1, "pageSize": 10},
        )["data"]
        mysql_exec(f"DELETE FROM biz_fleet_transport_plan WHERE id = {temp_plan_id};")
        mysql_exec(f"DELETE FROM biz_fleet_profile WHERE id = {temp_profile_id};")
        temp_plan_id = None
        temp_profile_id = None
        after = api_get(
            "/fleet-management/transport-plans",
            {"keyword": plan_no, "pageNo": 1, "pageSize": 10},
        )["data"]
        assert created["planNo"] == plan_no
        assert updated["destinationPoint"] == "消纳场-002"
        assert updated["status"] == "COMPLETED"
        assert int(filtered["total"]) >= 1
        assert int(after["total"]) == 0
        return f"plan_id={created['id']}, plan_no={plan_no}"

    run_case("fleet_transport_plan_runtime", create_update_query_cleanup)


def verify_fleet_dispatch_runtime():
    def create_update_approve_cleanup():
        global temp_profile_id, temp_plan_id, temp_dispatch_id
        fleet_name = f"联调调度车队-{suffix}"
        profile = api_post(
            "/fleet-management/profiles",
            {
                "fleetName": fleet_name,
                "captainName": "调度队长",
                "driverCountPlan": 14,
                "vehicleCountPlan": 10,
                "status": "ENABLED",
                "remark": "phase74 dispatch profile",
            },
        )["data"]
        temp_profile_id = str(profile["id"])
        plan_no = f"FTP-DISPATCH-{suffix}"
        plan = api_post(
            "/fleet-management/transport-plans",
            {
                "fleetId": int(temp_profile_id),
                "planNo": plan_no,
                "planDate": REPORT_DATE,
                "sourcePoint": "项目-002",
                "destinationPoint": "消纳场-003",
                "cargoType": "渣土",
                "plannedTrips": 12,
                "plannedVolume": 2400,
                "status": "ACTIVE",
                "remark": "phase74 dispatch plan",
            },
        )["data"]
        temp_plan_id = str(plan["id"])
        created = api_post(
            "/fleet-management/dispatch-orders",
            {
                "fleetId": int(temp_profile_id),
                "relatedPlanNo": plan_no,
                "applyDate": REPORT_DATE,
                "requestedVehicleCount": 6,
                "requestedDriverCount": 6,
                "urgencyLevel": "HIGH",
                "status": "PENDING_APPROVAL",
                "applicantName": "联调申请人",
                "remark": "phase74 dispatch",
            },
        )["data"]
        temp_dispatch_id = str(created["id"])
        updated = api_put(
            f"/fleet-management/dispatch-orders/{temp_dispatch_id}",
            {
                "fleetId": int(temp_profile_id),
                "relatedPlanNo": plan_no,
                "applyDate": REPORT_DATE,
                "requestedVehicleCount": 8,
                "requestedDriverCount": 7,
                "urgencyLevel": "MEDIUM",
                "status": "PENDING_APPROVAL",
                "applicantName": "联调申请人-更新",
                "remark": "phase74 dispatch updated",
            },
        )["data"]
        approved = api_post(
            f"/fleet-management/dispatch-orders/{temp_dispatch_id}/approve",
            {"comment": "phase74 approve"},
        )["data"]
        filtered = api_get(
            "/fleet-management/dispatch-orders",
            {"keyword": created["orderNo"], "status": "APPROVED", "pageNo": 1, "pageSize": 10},
        )["data"]
        mysql_exec(f"DELETE FROM biz_fleet_dispatch_order WHERE id = {temp_dispatch_id};")
        mysql_exec(f"DELETE FROM biz_fleet_transport_plan WHERE id = {temp_plan_id};")
        mysql_exec(f"DELETE FROM biz_fleet_profile WHERE id = {temp_profile_id};")
        temp_dispatch_id = None
        temp_plan_id = None
        temp_profile_id = None
        after = api_get(
            "/fleet-management/dispatch-orders",
            {"keyword": created['orderNo'], "pageNo": 1, "pageSize": 10},
        )["data"]
        assert updated["requestedVehicleCount"] == 8
        assert approved["status"] == "APPROVED"
        assert approved["approvedBy"]
        assert int(filtered["total"]) >= 1
        assert int(after["total"]) == 0
        return f"dispatch_id={created['id']}, order_no={created['orderNo']}"

    run_case("fleet_dispatch_approval_runtime", create_update_approve_cleanup)


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
        page.goto(BASE_WEB + "/vehicles/fleet")
        page.wait_for_load_state("networkidle")
        for title in ["车队管理", "车队维护", "运输计划", "调度审批"]:
            page.get_by_text(title).first.wait_for(timeout=10000)
        record("fleet_pages_visible", True, "车队管理 / 车队维护 / 运输计划 / 调度审批")
        context.close()
        browser.close()


def cleanup_orphans():
    if temp_dispatch_id:
        mysql_exec(f"DELETE FROM biz_fleet_dispatch_order WHERE id = {temp_dispatch_id};")
    if temp_plan_id:
        mysql_exec(f"DELETE FROM biz_fleet_transport_plan WHERE id = {temp_plan_id};")
    if temp_profile_id:
        mysql_exec(f"DELETE FROM biz_fleet_profile WHERE id = {temp_profile_id};")


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
        verify_fleet_query_runtime()
        verify_fleet_profile_runtime()
        verify_fleet_transport_plan_runtime()
        verify_fleet_dispatch_runtime()
        verify_ui()
    finally:
        cleanup_orphans()
        write_reports()
