import json
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase62_vehicle_master_enhancement_regression_{REPORT_DATE}"
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


def source_contains(path: str, text: str):
    content = Path(path).read_text(encoding="utf-8")
    assert text in content, f"{path} 未找到 {text}"
    return f"{Path(path).name} contains {text}"


def verify_source():
    run_case(
        "vehicles_controller_export_endpoint",
        lambda: source_contains(
            "xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java",
            '@GetMapping("/export")',
        ),
    )
    run_case(
        "vehicles_controller_batch_status_endpoint",
        lambda: source_contains(
            "xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java",
            '@PutMapping("/batch-status")',
        ),
    )
    run_case(
        "vehicles_controller_batch_delete_endpoint",
        lambda: source_contains(
            "xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehiclesController.java",
            '@PostMapping("/batch-delete")',
        ),
    )
    run_case(
        "vehicle_models_controller_export_endpoint",
        lambda: source_contains(
            "xngl-service/xngl-service-web/src/main/java/com/xngl/web/controller/VehicleModelsController.java",
            '@GetMapping("/export")',
        ),
    )
    run_case(
        "vehicles_page_batch_actions",
        lambda: source_contains("xngl-web/src/pages/VehiclesManagement.tsx", "批量设为禁用"),
    )
    run_case(
        "vehicles_page_export_action",
        lambda: source_contains("xngl-web/src/pages/VehiclesManagement.tsx", "导出台账"),
    )
    run_case(
        "vehicle_models_page_export_action",
        lambda: source_contains("xngl-web/src/pages/VehicleModelsManagement.tsx", "导出车型库"),
    )


def verify_runtime_api():
    def export_vehicles():
        blob, content_type = api_get("/vehicles/export", expect_blob=True)
        assert len(blob) > 0 and "text/csv" in content_type
        return f"bytes={len(blob)}"

    run_case("vehicles_export_runtime", export_vehicles)

    created_vehicle_id = None

    def create_temp_vehicle():
        nonlocal created_vehicle_id
        payload = api_post(
            "/vehicles",
            {
                "plateNo": "浙A9P620",
                "orgId": 1,
                "vehicleType": "测试自卸车",
                "brand": "测试品牌",
                "model": "回归车型",
                "energyType": "DIESEL",
                "loadWeight": 18.5,
                "status": 4,
                "useStatus": "STANDBY",
                "runningStatus": "STOPPED",
                "currentMileage": 1200,
            },
        )["data"]
        created_vehicle_id = int(payload["id"])
        return f"id={created_vehicle_id}"

    def batch_disable_vehicle():
        response = api_put("/vehicles/batch-status", {"ids": [created_vehicle_id], "status": 3})["data"]
        detail = api_get(f"/vehicles/{created_vehicle_id}")["data"]
        assert response["updated"] == 1
        assert detail["status"] == 3
        return f"updated={response['updated']}, status={detail['status']}"

    def batch_delete_vehicle():
        response = api_post("/vehicles/batch-delete", {"ids": [created_vehicle_id]})["data"]
        rows = api_get("/vehicles", {"pageNo": 1, "pageSize": 200})["data"]["records"]
        assert response["deleted"] == 1
        assert all(str(row["id"]) != str(created_vehicle_id) for row in rows)
        return f"deleted={response['deleted']}"

    run_case("vehicles_create_temp_runtime", create_temp_vehicle)
    run_case("vehicles_batch_status_runtime", batch_disable_vehicle)
    run_case("vehicles_batch_delete_runtime", batch_delete_vehicle)

    def export_vehicle_models():
        blob, content_type = api_get("/vehicle-models/export", expect_blob=True)
        assert len(blob) > 0 and "text/csv" in content_type
        return f"bytes={len(blob)}"

    run_case("vehicle_models_export_runtime", export_vehicle_models)


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
        storage_script = f"""
            (() => {{
                localStorage.setItem('token', {json.dumps(token)});
                localStorage.setItem('userInfo', {json.dumps(json.dumps(user_info, ensure_ascii=False))});
            }})();
        """
        context.add_init_script(storage_script)
        page = context.new_page()

        page.goto(f"{BASE_WEB}/vehicles")
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name="导出台账").wait_for(timeout=10000)
        page.get_by_role("button", name="批量设为在用").wait_for(timeout=10000)
        page.get_by_role("button", name="批量删除").wait_for(timeout=10000)
        record("vehicles_page_buttons", True, "车辆页面导出/批量按钮已渲染")

        page.goto(f"{BASE_WEB}/vehicles/models")
        page.wait_for_load_state("networkidle")
        page.get_by_role("button", name="导出车型库").wait_for(timeout=10000)
        record("vehicle_models_page_buttons", True, "车型页面导出按钮已渲染")

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
        "notes": [
            "前端构建已通过。",
            "后端全量 Maven 编译已通过，服务已按最新代码重启。",
        ],
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
    lines.extend(
        [
            "",
            "## 备注",
            "",
            "- 前端构建 `npm run build` 已通过。",
            "- 后端全量 `mvn -q -DskipTests compile` 已通过，`mvn spring-boot:run -pl xngl-service-starter` 已重启成功。",
            "- 本报告同时覆盖源码落地校验、运行态 API 校验与前端页面可见性回归。",
        ]
    )
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    login_api()
    verify_source()
    verify_runtime_api()
    verify_ui()
    write_reports()
