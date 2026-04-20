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
REPORT_STEM = f"phase72_contract_settlement_regression_{REPORT_DATE}"
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
tenant_id = "1"
suffix = str(int(time.time() * 1000))
effective_contract_id = None


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
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = resp.read()
            if expect_blob:
                return payload, resp.headers.get("Content-Type", "")
            return json.loads(payload.decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {exc.code}: {detail[:400]}")


def api_get(path: str, params=None, expect_blob: bool = False):
    query = ""
    if params:
        cleaned = {k: v for k, v in params.items() if v is not None and v != ""}
        query = "?" + urllib.parse.urlencode(cleaned, doseq=True)
    return api_request("GET", path + query, use_auth=True, expect_blob=expect_blob)


def api_post(path: str, data=None):
    return api_request("POST", path, data=data, use_auth=True)


def api_put(path: str, data=None):
    return api_request("PUT", path, data=data, use_auth=True)


def login_api():
    global token, user_info, tenant_id
    payload = api_request(
        "POST",
        "/auth/login",
        data={"tenantId": "1", "username": "admin", "password": "admin"},
        use_auth=False,
    )
    token = payload["data"]["token"]
    user_info = payload["data"]["user"]
    tenant_id = str(user_info.get("tenantId") or "1")
    record("login", bool(token), "admin/admin 登录成功")


def prepare_contract_ref():
    global effective_contract_id
    page = api_get("/contracts", {"contractStatus": "EFFECTIVE", "pageNo": 1, "pageSize": 5})["data"]
    assert int(page["total"]) >= 1, "未查询到生效合同"
    effective_contract_id = str(page["records"][0]["id"])


def verify_contracts_runtime():
    def list_stats_and_detail_assets():
        page = api_get("/contracts", {"pageNo": 1, "pageSize": 5})["data"]
        stats = api_get("/contracts/stats")["data"]
        detail = api_get(f"/contracts/{effective_contract_id}/detail")["data"]
        approvals = api_get(f"/contracts/{effective_contract_id}/approval-records")["data"]
        materials = api_get(f"/contracts/{effective_contract_id}/materials")["data"]
        invoices = api_get(f"/contracts/{effective_contract_id}/invoices")["data"]
        tickets = api_get(f"/contracts/{effective_contract_id}/tickets")["data"]
        receipts = api_get(f"/contracts/{effective_contract_id}/receipts")["data"]
        assert int(page["total"]) >= 1
        assert int(stats["totalContracts"]) >= 1
        assert detail["contractNo"]
        assert isinstance(approvals, list)
        assert isinstance(materials, list)
        assert isinstance(invoices, list)
        assert isinstance(tickets, list)
        assert isinstance(receipts, list)
        return f"contract_id={effective_contract_id}, approvals={len(approvals)}, materials={len(materials)}"

    run_case("contracts_list_detail_assets_runtime", list_stats_and_detail_assets)


def verify_contract_receipts_runtime():
    voucher_no = f"PH72-V-{suffix}"
    bank_flow_no = f"PH72-B-{suffix}"

    def create_cancel_cleanup():
        before = api_get(f"/contracts/{effective_contract_id}/receipts")["data"]
        created = api_post(
            f"/contracts/{effective_contract_id}/receipts",
            {
                "amount": 321.45,
                "receiptDate": REPORT_DATE,
                "receiptType": "BANK",
                "voucherNo": voucher_no,
                "bankFlowNo": bank_flow_no,
                "remark": "phase72 receipt",
            },
        )["data"]
        detail = api_get(f"/contracts/receipts/{created}")["data"]
        cancelled = api_put(f"/contracts/receipts/{created}/cancel", {"remark": "phase72 cancel"})["data"]
        after_cancel = api_get(f"/contracts/{effective_contract_id}/receipts")["data"]
        mysql_exec(f"DELETE FROM biz_contract_receipt WHERE id = {created};")
        cleaned = api_get(f"/contracts/{effective_contract_id}/receipts")["data"]
        assert detail["id"] == str(created)
        assert detail["status"] == "NORMAL"
        assert str(cancelled) == str(created)
        assert any(item["id"] == str(created) and item["status"] == "CANCELLED" for item in after_cancel)
        assert len(cleaned) == len(before)
        return f"receipt_id={created}, before={len(before)}, after_cancel={len(after_cancel)}"

    run_case("contracts_receipts_create_cancel_cleanup_runtime", create_cancel_cleanup)


def verify_contract_reports_runtime():
    def reports_and_export_task():
        monthly_summary = api_get("/reports/contracts/monthly/summary")["data"]
        monthly_trend = api_get("/reports/contracts/monthly/trend", {"months": 12})["data"]
        monthly_types = api_get("/reports/contracts/monthly/types")["data"]
        daily_summary = api_get("/reports/contracts/daily/summary", {"date": REPORT_DATE})["data"]
        yearly_summary = api_get("/reports/contracts/yearly/summary", {"year": 2026})["data"]
        custom_summary = api_get(
            "/reports/contracts/custom/summary",
            {"startDate": "2026-03-01", "endDate": REPORT_DATE},
        )["data"]
        export_task = api_post("/reports/contracts/monthly/export", {"month": "2026-03"})["data"]
        task_detail = api_get(f"/export-tasks/{export_task['taskId']}")["data"]
        assert monthly_summary["month"]
        assert len(monthly_trend) == 12
        assert len(monthly_types) >= 1
        assert daily_summary["date"] == REPORT_DATE
        assert yearly_summary["year"] == 2026
        assert custom_summary["startDate"] == "2026-03-01"
        assert task_detail["bizType"] == "MONTHLY_REPORT"
        return f"export_task_id={export_task['taskId']}, trend_months={len(monthly_trend)}"

    run_case("contracts_reports_runtime", reports_and_export_task)


def verify_site_settlements_runtime():
    def stats_list_generate_detail_cleanup():
        stats = api_get("/settlements/stats")["data"]
        site_page = api_get(
            "/settlements",
            {"settlementType": "SITE", "pageNo": 1, "pageSize": 10},
        )["data"]
        assert int(site_page["total"]) >= 1
        first_site = site_page["records"][0]
        existing_detail = api_get(f"/settlements/{first_site['id']}")["data"]
        created = api_post(
            "/settlements/site/generate",
            {
                "targetId": 1,
                "periodStart": "2026-10-01",
                "periodEnd": "2026-10-31",
                "remark": "phase72 site settlement",
            },
        )["data"]
        created_detail = api_get(f"/settlements/{created}")["data"]
        mysql_exec(
            f"DELETE FROM biz_settlement_item WHERE settlement_order_id = {created};"
            f"DELETE FROM biz_settlement_order WHERE id = {created};"
        )
        cleanup_page = api_get(
            "/settlements",
            {"settlementType": "SITE", "pageNo": 1, "pageSize": 50},
        )["data"]
        assert int(stats["totalOrders"]) >= 1
        assert existing_detail["settlementType"] == "SITE"
        assert created_detail["targetSiteId"] == "1"
        assert not any(str(item["id"]) == str(created) for item in cleanup_page["records"])
        return f"existing_site_settlement={first_site['id']}, temp_site_settlement={created}"

    run_case("site_settlements_runtime", stats_list_generate_detail_cleanup)


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
            ("/contracts", "合同与财务结算"),
            (f"/contracts/{effective_contract_id}", "合同详情:"),
            ("/contracts/payments", "合同入账管理"),
            ("/contracts/monthly-report", "月报统计"),
            ("/contracts/settlements", "结算管理"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        record("contracts_settlement_pages_visible", True, " / ".join(visible))
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
    prepare_contract_ref()
    verify_contracts_runtime()
    verify_contract_receipts_runtime()
    verify_contract_reports_runtime()
    verify_site_settlements_runtime()
    verify_ui()
    write_reports()
