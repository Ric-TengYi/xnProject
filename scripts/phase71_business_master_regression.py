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
REPORT_STEM = f"phase71_business_master_regression_{REPORT_DATE}"
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


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def run_case(name: str, fn):
    try:
        detail = fn()
        record(name, True, detail)
    except Exception as exc:
        record(name, False, str(exc))


def sql_quote(value):
    if value is None:
        return "NULL"
    escaped = str(value).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{escaped}'"


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


def verify_units_runtime():
    unit_code = f"PHASE71_UNIT_{suffix}"

    def summary_and_query():
        summary = api_get("/units/summary")["data"]
        page = api_get("/units", {"pageNo": 1, "pageSize": 5})["data"]
        assert int(summary["totalUnits"]) >= 1
        assert int(page["total"]) >= 1
        first = page["records"][0]
        detail = api_get(f"/units/{first['id']}")["data"]
        projects = api_get(f"/units/{first['id']}/projects")["data"]
        groups = api_get(f"/units/{first['id']}/contract-groups")["data"]
        assert detail["orgName"]
        assert isinstance(projects, list)
        assert isinstance(groups, list)
        return f"unit_id={first['id']}, total_units={summary['totalUnits']}"

    def create_update_cleanup():
        created = api_post(
            "/units",
            {
                "orgName": "Phase71 单位",
                "orgType": "CONSTRUCTION_UNIT",
                "orgCode": unit_code,
                "contactPerson": "phase71",
                "contactPhone": "0571-87000071",
                "address": "杭州测试地址71号",
                "unifiedSocialCode": f"91330000PHASE71{suffix[-4:]}",
                "remark": "phase71 unit",
                "status": "ENABLED",
            },
        )["data"]
        unit_id = int(created["id"])
        updated = api_put(
            f"/units/{unit_id}",
            {
                "orgName": "Phase71 单位-更新",
                "orgType": "BUILDER_UNIT",
                "orgCode": unit_code,
                "contactPerson": "phase71-updated",
                "contactPhone": "0571-87000072",
                "address": "杭州测试地址72号",
                "unifiedSocialCode": f"91330000PHASE71U{suffix[-3:]}",
                "remark": "phase71 unit updated",
                "status": "DISABLED",
            },
        )["data"]
        query = api_get("/units", {"keyword": unit_code, "pageNo": 1, "pageSize": 10})["data"]
        mysql_exec(
            "DELETE FROM sys_org "
            f"WHERE tenant_id = {tenant_id} AND org_code = {sql_quote(unit_code)};"
        )
        after = api_get("/units", {"keyword": unit_code, "pageNo": 1, "pageSize": 10})["data"]
        assert updated["orgName"].endswith("更新")
        assert int(query["total"]) >= 1
        assert int(after["total"]) == 0
        return f"temp_unit_id={unit_id}"

    run_case("business_units_summary_query_runtime", summary_and_query)
    run_case("business_units_upsert_cleanup_runtime", create_update_cleanup)


def verify_projects_runtime():
    def project_list_and_detail():
        page = api_get("/projects", {"pageNo": 1, "pageSize": 5})["data"]
        assert int(page["total"]) >= 1
        first = page["records"][0]
        detail = api_get(f"/projects/{first['id']}")["data"]
        assert detail["name"]
        assert isinstance(detail.get("contractDetails") or [], list)
        assert isinstance(detail.get("siteDetails") or [], list)
        return f"project_id={first['id']}, contracts={len(detail.get('contractDetails') or [])}"

    run_case("business_projects_list_detail_runtime", project_list_and_detail)


def verify_project_payments_runtime():
    payment_no = f"PHASE71-PAY-{suffix}"

    def list_summary_create_cancel_cleanup():
        projects = api_get("/projects", {"pageNo": 1, "pageSize": 5})["data"]["records"]
        assert projects, "未查询到项目数据"
        project_id = str(projects[0]["id"])
        summary = api_get(f"/project-payments/{project_id}/payments/summary")["data"]
        listed_before = api_get(
            "/project-payments/payments",
            {"projectId": project_id, "pageNo": 1, "pageSize": 10},
        )["data"]
        created = api_post(
            f"/project-payments/{project_id}/payments",
            {
                "paymentNo": payment_no,
                "paymentType": "MANUAL",
                "amount": 1234.56,
                "paymentDate": REPORT_DATE,
                "voucherNo": f"V-{suffix}",
                "sourceType": "MANUAL",
                "remark": "phase71 payment",
            },
        )["data"]
        payment_id = int(created["paymentId"])
        listed_after = api_get(
            "/project-payments/payments",
            {"projectId": project_id, "pageNo": 1, "pageSize": 50},
        )["data"]
        cancelled = api_post(f"/project-payments/payments/{payment_id}/cancel", {"reason": "phase71 cancel"})["data"]
        mysql_exec(
            "DELETE FROM biz_project_payment_record "
            f"WHERE tenant_id = {tenant_id} AND payment_no = {sql_quote(payment_no)};"
        )
        cleanup_check = api_get(
            "/project-payments/payments",
            {"projectId": project_id, "keyword": payment_no, "pageNo": 1, "pageSize": 10},
        )["data"]
        assert summary["projectId"] == project_id
        assert any((item.get("paymentNo") or "") == payment_no for item in listed_after["records"])
        assert cancelled["summary"]["projectId"] == project_id
        assert int(cleanup_check["total"]) == 0
        return f"project_id={project_id}, before_total={listed_before['total']}, payment_id={payment_id}"

    run_case("business_project_payments_runtime", list_summary_create_cancel_cleanup)


def verify_permits_runtime():
    def list_and_detail():
        rows = api_get("/disposal-permits", {"pageNo": 1, "pageSize": 5})["data"]
        assert rows, "未查询到处置证数据"
        first = rows[0]
        detail = api_get(f"/disposal-permits/{first['id']}")["data"]
        assert detail["permitNo"]
        assert detail["projectId"] is not None
        return f"permit_id={first['id']}, status={detail['status']}"

    run_case("business_disposal_permits_runtime", list_and_detail)


def verify_checkins_runtime():
    def list_void_restore():
        page = api_get("/checkins", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(page["total"]) >= 1
        candidate_line = mysql_exec(
            "SELECT id, status, IFNULL(remark,'') "
            "FROM biz_contract_ticket "
            f"WHERE tenant_id = {tenant_id} AND status <> 'CANCELLED' "
            "ORDER BY id DESC LIMIT 1;"
        )
        assert candidate_line, "未找到可作废打卡记录"
        ticket_id, original_status, original_remark = candidate_line.split("\t", 2)
        voided = api_put(f"/checkins/{ticket_id}/void", {"reason": "phase71 void"})["data"]
        mysql_exec(
            "UPDATE biz_contract_ticket SET "
            f"status = {sql_quote(original_status)}, "
            f"remark = {sql_quote(original_remark) if original_remark else 'NULL'} "
            f"WHERE id = {ticket_id};"
        )
        restored = api_get("/checkins", {"keyword": voided["ticketNo"], "pageNo": 1, "pageSize": 5})["data"]["records"]
        assert voided["status"] == "CANCELLED"
        assert restored
        return f"ticket_id={ticket_id}, ticket_no={voided['ticketNo']}"

    run_case("business_checkins_void_restore_runtime", list_void_restore)


def verify_disposals_runtime():
    def list_and_filter():
        page = api_get("/disposals", {"pageNo": 1, "pageSize": 10})["data"]
        assert int(page["total"]) >= 1
        first = page["records"][0]
        filtered = api_get(
            "/disposals",
            {"keyword": first["ticketNo"], "pageNo": 1, "pageSize": 5},
        )["data"]
        assert int(filtered["total"]) >= 1
        return f"disposal_id={first['id']}, ticket_no={first['ticketNo']}"

    run_case("business_disposals_query_runtime", list_and_filter)


def verify_sites_runtime():
    def sites_and_site_disposals():
        sites = api_get("/sites")["data"]
        assert sites, "未查询到场地数据"
        first = sites[0]
        detail = api_get(f"/sites/{first['id']}")["data"]
        map_layers = api_get("/sites/map-layers")["data"]
        disposals = api_get("/sites/disposals", {"pageNo": 1, "pageSize": 5})["data"]
        assert detail["name"]
        assert isinstance(map_layers, list)
        assert "records" in disposals
        return f"site_id={first['id']}, map_layers={len(map_layers)}"

    run_case("business_sites_runtime", sites_and_site_disposals)


def verify_vehicles_runtime():
    def stats_list_export():
        stats = api_get("/vehicles/stats")["data"]
        page = api_get("/vehicles", {"pageNo": 1, "pageSize": 5})["data"]
        companies = api_get("/vehicles/company-capacity")["data"]
        fleets = api_get("/vehicles/fleets")["data"]
        blob, _ = api_get("/vehicles/export", {"pageNo": 1, "pageSize": 5}, expect_blob=True)
        text = blob.decode("utf-8")
        assert int(stats["totalVehicles"]) >= 1
        assert int(page["total"]) >= 1
        assert isinstance(companies, list)
        assert isinstance(fleets, list)
        assert "车牌号" in text
        return f"vehicles={page['total']}, companies={len(companies)}, fleets={len(fleets)}"

    run_case("business_vehicles_runtime", stats_list_export)


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
            ("/settings/units", "单位管理"),
            ("/projects", "消纳项目清单"),
            ("/projects/payments", "项目交款管理"),
            ("/projects/permits", "处置证清单"),
            ("/queries/checkins", "打卡数据"),
            ("/queries/disposals", "消纳信息"),
            ("/sites", "消纳场地管理"),
            ("/sites/disposals", "全局消纳清单"),
            ("/sites/basic-info", "场地基础信息"),
            ("/vehicles", "车辆与运力资源"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        record("business_master_pages_visible", True, " / ".join(visible))
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
    verify_units_runtime()
    verify_projects_runtime()
    verify_project_payments_runtime()
    verify_permits_runtime()
    verify_checkins_runtime()
    verify_disposals_runtime()
    verify_sites_runtime()
    verify_vehicles_runtime()
    verify_ui()
    write_reports()
