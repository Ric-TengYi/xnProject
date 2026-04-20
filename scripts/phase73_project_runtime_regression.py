import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase73_project_runtime_regression_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

results = []
token = None
user_info = None
project_id = None


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def run_case(name: str, fn):
    try:
        detail = fn()
        record(name, True, detail)
    except Exception as exc:
        record(name, False, str(exc))


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


def prepare_project_ref():
    global project_id
    page = api_get("/projects", {"pageNo": 1, "pageSize": 10})["data"]
    assert int(page["total"]) >= 1, "未查询到项目数据"
    project_id = str(page["records"][0]["id"])


def verify_project_daily_runtime():
    def daily_list_and_export():
        base_page = api_get(
            "/reports/projects/daily",
            {"date": REPORT_DATE, "pageNo": 1, "pageSize": 20},
        )["data"]
        assert int(base_page["total"]) >= 1, "项目日报无数据"
        first = base_page["records"][0]
        keyword_page = api_get(
            "/reports/projects/daily",
            {"date": REPORT_DATE, "keyword": first["projectName"], "pageNo": 1, "pageSize": 20},
        )["data"]
        assert any(row["projectId"] == first["projectId"] for row in keyword_page["records"])
        task = api_post(
            "/reports/projects/daily/export",
            {"date": REPORT_DATE, "keyword": first["projectName"]},
        )["data"]
        task_detail = api_get(f"/export-tasks/{task['taskId']}")["data"]
        assert task_detail["bizType"] == "PROJECT_DAILY_REPORT"
        return f"project_id={first['projectId']}, export_task_id={task['taskId']}"

    run_case("project_daily_runtime", daily_list_and_export)


def verify_project_config_runtime():
    def config_fields():
        detail = api_get(f"/projects/{project_id}")["data"]
        config = detail["config"]
        assert detail["id"] == project_id
        assert config is not None, "项目配置为空"
        assert bool(config["checkinEnabled"]) is True
        assert config["checkinAccount"]
        assert bool(config["locationCheckRequired"]) is True
        assert float(config["locationRadiusMeters"] or 0) > 0
        assert float(config["preloadVolume"] or 0) > 0
        return (
            f"checkin={config['checkinAccount']}, "
            f"radius={config['locationRadiusMeters']}, preload={config['preloadVolume']}"
        )

    def route_and_violation():
        detail = api_get(f"/projects/{project_id}")["data"]
        config = detail["config"]
        route = json.loads(config["routeGeoJson"])
        fence = json.loads(config["violationFenceGeoJson"])
        assert route["type"] == "LineString"
        assert len(route["coordinates"]) >= 2
        assert bool(config["violationRuleEnabled"]) is True
        assert config["violationFenceCode"]
        assert config["violationFenceName"]
        assert fence["type"] == "Polygon"
        assert len(fence["coordinates"][0]) >= 4
        return (
            f"route_points={len(route['coordinates'])}, "
            f"fence={config['violationFenceCode']}"
        )

    run_case("project_config_checkin_location_preload_runtime", config_fields)
    run_case("project_config_route_violation_runtime", route_and_violation)


def verify_project_reports_runtime():
    def summary_list_trend_export():
        summary = api_get(
            "/reports/projects/summary",
            {"periodType": "MONTH", "date": REPORT_DATE},
        )["data"]
        day_summary = api_get(
            "/reports/projects/summary",
            {"periodType": "DAY", "date": REPORT_DATE},
        )["data"]
        year_summary = api_get(
            "/reports/projects/summary",
            {"periodType": "YEAR", "date": REPORT_DATE},
        )["data"]
        listing = api_get(
            "/reports/projects/list",
            {"periodType": "MONTH", "date": REPORT_DATE, "pageNo": 1, "pageSize": 20},
        )["data"]
        trend = api_get(
            "/reports/projects/trend",
            {"periodType": "MONTH", "date": REPORT_DATE, "limit": 6},
        )["data"]
        task = api_post(
            "/reports/projects/export",
            {"periodType": "MONTH", "date": REPORT_DATE},
        )["data"]
        task_detail = api_get(f"/export-tasks/{task['taskId']}")["data"]
        assert summary["periodType"] == "MONTH"
        assert day_summary["periodType"] == "DAY"
        assert year_summary["periodType"] == "YEAR"
        assert int(listing["total"]) >= 1
        assert len(trend) == 6
        assert task_detail["bizType"] == "PROJECT_REPORT"
        return f"list_total={listing['total']}, trend_points={len(trend)}, export_task_id={task['taskId']}"

    run_case("project_reports_runtime", summary_list_trend_export)


def verify_project_violation_runtime():
    def violation_analysis():
        data = api_get(
            "/reports/projects/violations",
            {"periodType": "MONTH", "date": REPORT_DATE},
        )["data"]
        keyword = None
        if data["byPlate"]:
            keyword = data["byPlate"][0]["name"]
        elif data["byTeam"]:
            keyword = data["byTeam"][0]["name"]
        elif data["byFleet"]:
            keyword = data["byFleet"][0]["name"]
        if keyword:
            filtered = api_get(
                "/reports/projects/violations",
                {"periodType": "MONTH", "date": REPORT_DATE, "keyword": keyword},
            )["data"]
            assert (
                filtered["byFleet"] or filtered["byPlate"] or filtered["byTeam"]
            ), "违规分析关键字过滤后无结果"
        assert data["summary"]["reportPeriod"]
        assert data["summary"]["totalViolations"] >= 0
        return (
            f"violations={data['summary']['totalViolations']}, "
            f"fleet_rows={len(data['byFleet'])}, plate_rows={len(data['byPlate'])}"
        )

    run_case("project_violation_runtime", violation_analysis)


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
            ("/projects/daily-report", "项目日报"),
            ("/projects/reports", "项目报表"),
            (f"/projects/{project_id}?tab=config", "项目配置"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        page.goto(BASE_WEB + f"/projects/{project_id}?tab=config")
        page.wait_for_load_state("networkidle")
        page.get_by_text("线路与违规围栏预览").first.wait_for(timeout=10000)
        page.get_by_text("当前项目配置已收口打卡配置、位置判断、出土预扣值、线路配置与违规围栏 5 类数据。").first.wait_for(timeout=10000)
        record("project_pages_visible", True, " / ".join(visible))
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
    prepare_project_ref()
    verify_project_daily_runtime()
    verify_project_config_runtime()
    verify_project_reports_runtime()
    verify_project_violation_runtime()
    verify_ui()
    write_reports()
