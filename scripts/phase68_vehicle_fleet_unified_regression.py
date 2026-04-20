import json
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase68_vehicle_fleet_unified_regression_{REPORT_DATE}"
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


def verify_vehicle_master():
    def vehicles():
        data = api_get("/vehicles", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"vehicles_total={data['total']}"

    def vehicle_stats():
        data = api_get("/vehicles/stats")["data"]
        assert data is not None
        return f"stats_keys={len(data.keys())}"

    def vehicle_export():
        blob, content_type = api_get("/vehicles/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "车牌号" in text
        return content_type or "blob-ok"

    def vehicle_models():
        data = api_get("/vehicle-models", {"pageNo": 1, "pageSize": 10})["data"]
        assert len(data) >= 1
        return f"vehicle_models={len(data)}"

    def vehicle_model_export():
        blob, content_type = api_get("/vehicle-models/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "品牌" in text
        return content_type or "blob-ok"

    run_case("vehicle_master_list_runtime", vehicles)
    run_case("vehicle_master_stats_runtime", vehicle_stats)
    run_case("vehicle_master_export_runtime", vehicle_export)
    run_case("vehicle_model_list_runtime", vehicle_models)
    run_case("vehicle_model_export_runtime", vehicle_model_export)


def verify_vehicle_ops():
    def insurance_list():
        data = api_get("/vehicle-insurances", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"insurance_total={data['total']}"

    def insurance_summary():
        data = api_get("/vehicle-insurances/summary")["data"]
        assert int(data["totalPolicies"]) >= 1
        return f"expiring={data['expiringPolicies']}, expired={data['expiredPolicies']}"

    def insurance_export():
        blob, content_type = api_get("/vehicle-insurances/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "保单号" in text
        return content_type or "blob-ok"

    def maintenance_plans():
        data = api_get("/vehicle-maintenance-plans", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"maintenance_plans={data['total']}"

    def maintenance_summary():
        data = api_get("/vehicle-maintenance-plans/summary")["data"]
        assert data is not None
        return f"plans={data['totalPlans']}, overdue={data['overduePlans']}"

    def maintenance_records_export():
        blob, content_type = api_get("/vehicle-maintenance-plans/export/records", expect_blob=True)
        text = blob.decode("utf-8")
        assert "执行编号" in text
        return content_type or "blob-ok"

    def repair_list():
        data = api_get("/vehicle-repairs", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"repairs_total={data['total']}"

    def repair_summary():
        data = api_get("/vehicle-repairs/summary")["data"]
        assert int(data["totalOrders"]) >= 1
        return f"approved={data['approvedOrders']}, completed={data['completedOrders']}"

    def repair_export():
        blob, content_type = api_get("/vehicle-repairs/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "维修单号" in text
        return content_type or "blob-ok"

    run_case("vehicle_insurance_list_runtime", insurance_list)
    run_case("vehicle_insurance_summary_runtime", insurance_summary)
    run_case("vehicle_insurance_export_runtime", insurance_export)
    run_case("vehicle_maintenance_plans_runtime", maintenance_plans)
    run_case("vehicle_maintenance_summary_runtime", maintenance_summary)
    run_case("vehicle_maintenance_records_export_runtime", maintenance_records_export)
    run_case("vehicle_repair_list_runtime", repair_list)
    run_case("vehicle_repair_summary_runtime", repair_summary)
    run_case("vehicle_repair_export_runtime", repair_export)


def verify_cards_and_fleet():
    def card_list():
        data = api_get("/vehicle-cards", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"vehicle_cards={data['total']}"

    def card_summary():
        data = api_get("/vehicle-cards/summary")["data"]
        assert int(data["totalCards"]) >= 1
        return f"balance={data['totalBalance']}"

    def card_txn_summary():
        data = api_get("/vehicle-cards/transactions/summary")["data"]
        assert data is not None
        return f"recharge={data['totalRechargeAmount']}, consume={data['totalConsumeAmount']}"

    def card_export():
        blob, content_type = api_get("/vehicle-cards/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "卡号" in text
        return content_type or "blob-ok"

    def card_txn_export():
        blob, content_type = api_get("/vehicle-cards/transactions/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "发生时间" in text
        return content_type or "blob-ok"

    def personnel_list():
        data = api_get("/vehicle-personnel-certificates", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"personnel_certificates={data['total']}"

    def personnel_summary():
        data = api_get("/vehicle-personnel-certificates/summary")["data"]
        assert int(data["totalPersons"]) >= 1
        return f"unpaid={data['unpaidAmount']}"

    def personnel_export():
        blob, content_type = api_get("/vehicle-personnel-certificates/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "人员姓名" in text
        return content_type or "blob-ok"

    def fleet_finance():
        data = api_get("/fleet-management/finance-records", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(data["total"]) >= 1
        return f"fleet_finance={data['total']}"

    def fleet_finance_summary():
        data = api_get("/fleet-management/finance-records/summary")["data"]
        assert int(data["totalRecords"]) >= 1
        return f"outstanding={data['totalOutstandingAmount']}, profit={data['totalProfitAmount']}"

    def fleet_finance_export():
        blob, content_type = api_get("/fleet-management/finance-records/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "账期" in text
        return content_type or "blob-ok"

    def fleet_report():
        data = api_get("/fleet-management/report")["data"]
        assert len(data) >= 1
        return f"fleet_report_rows={len(data)}"

    def fleet_report_export():
        blob, content_type = api_get("/fleet-management/report/export", expect_blob=True)
        text = blob.decode("utf-8")
        assert "车队" in text
        return content_type or "blob-ok"

    run_case("vehicle_cards_list_runtime", card_list)
    run_case("vehicle_cards_summary_runtime", card_summary)
    run_case("vehicle_cards_transaction_summary_runtime", card_txn_summary)
    run_case("vehicle_cards_export_runtime", card_export)
    run_case("vehicle_cards_transaction_export_runtime", card_txn_export)
    run_case("vehicle_personnel_list_runtime", personnel_list)
    run_case("vehicle_personnel_summary_runtime", personnel_summary)
    run_case("vehicle_personnel_export_runtime", personnel_export)
    run_case("fleet_finance_list_runtime", fleet_finance)
    run_case("fleet_finance_summary_runtime", fleet_finance_summary)
    run_case("fleet_finance_export_runtime", fleet_finance_export)
    run_case("fleet_report_runtime", fleet_report)
    run_case("fleet_report_export_runtime", fleet_report_export)


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
            ("/vehicles", "车辆与运力资源"),
            ("/vehicles/models", "车型管理"),
            ("/vehicles/maintenance", "维保计划"),
            ("/vehicles/repairs", "维修管理"),
            ("/vehicles/cards", "油电卡管理"),
            ("/vehicles/personnel-certificates", "人证管理"),
            ("/vehicles/fleet", "车队管理"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        record("vehicle_fleet_pages_visible", True, " / ".join(visible))
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
    verify_vehicle_master()
    verify_vehicle_ops()
    verify_cards_and_fleet()
    verify_ui()
    write_reports()
