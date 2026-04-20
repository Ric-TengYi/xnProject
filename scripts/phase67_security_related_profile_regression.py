import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase67_security_related_profile_regression_{REPORT_DATE}"
REPORT_DIR = Path("docs/test-reports")
JSON_PATH = REPORT_DIR / f"{REPORT_STEM}.json"
MD_PATH = REPORT_DIR / f"{REPORT_STEM}.md"

results = []
token = None
user_info = None
created_security_ids = []
ui_keyword = None


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


def ensure_person_certificate():
    rows = api_get(
        "/vehicle-personnel-certificates",
        {"pageNo": 1, "pageSize": 20, "keyword": "Local Admin"},
    )["data"]["records"]
    if rows:
        return int(rows[0]["id"])
    created = api_post(
        "/vehicle-personnel-certificates",
        {
            "personName": "Local Admin",
            "mobile": "13800000001",
            "roleType": "SAFETY_OFFICER",
            "vehicleId": 1,
            "driverLicenseNo": "ADM-LIC-0001",
            "driverLicenseExpireDate": "2026-04-15",
            "transportLicenseNo": "ADM-TRN-0001",
            "transportLicenseExpireDate": "2026-04-20",
            "feeAmount": 1800,
            "paidAmount": 600,
            "feeDueDate": "2026-03-20",
            "status": "ACTIVE",
            "remark": "phase67 安全台账关联验证",
        },
    )["data"]
    return int(created["id"])


def verify_runtime():
    global created_security_ids, ui_keyword
    suffix = str(int(time.time()))
    created_ids = []
    ui_keyword = f"phase67 安全检查-{suffix}"
    ensure_person_certificate()

    def create_person_security():
        data = api_post(
            "/security/inspections",
            {
                "objectType": "PERSON",
                "objectId": 6,
                "userId": 6,
                "title": f"{ui_keyword}-PERSON",
                "checkScene": "PERSONNEL",
                "checkType": "DAILY",
                "hazardCategory": "CERTIFICATE",
                "resultLevel": "FAIL",
                "dangerLevel": "HIGH",
                "issueCount": 1,
                "status": "OPEN",
                "rectifyOwner": "人员安全负责人",
                "rectifyOwnerPhone": "13800000001",
                "description": "phase67 人员安全档案校验",
                "rectifyDeadline": "2026-03-28T18:00:00",
                "nextCheckTime": "2026-03-29T09:00:00",
            },
        )["data"]
        created_ids.append(int(data["id"]))
        profile = data.get("relatedProfile") or {}
        assert int(profile.get("certificateCount") or 0) >= 1
        assert int(profile.get("learningCount") or 0) >= 1
        assert "证照" in (data.get("relatedProfileSummary") or "")
        return f"id={data['id']}, cert={profile.get('certificateCount')}, learning={profile.get('learningCount')}"

    def create_vehicle_security():
        data = api_post(
            "/security/inspections",
            {
                "objectType": "VEHICLE",
                "objectId": 1,
                "vehicleId": 1,
                "siteId": 1,
                "title": f"{ui_keyword}-VEHICLE",
                "checkScene": "VEHICLE_OPERATION",
                "checkType": "SPECIAL",
                "hazardCategory": "OPERATION",
                "resultLevel": "FAIL",
                "dangerLevel": "MEDIUM",
                "issueCount": 2,
                "status": "RECTIFYING",
                "rectifyOwner": "车队安全员",
                "rectifyOwnerPhone": "13800000002",
                "description": "phase67 车辆安全档案校验",
                "rectifyDeadline": "2026-03-28T18:00:00",
                "nextCheckTime": "2026-03-30T10:00:00",
            },
        )["data"]
        created_ids.append(int(data["id"]))
        profile = data.get("relatedProfile") or {}
        assert int(profile.get("insuranceCount") or 0) >= 1
        assert int(profile.get("maintenanceCount") or 0) >= 1
        assert "保险" in (data.get("relatedProfileSummary") or "")
        return f"id={data['id']}, insurance={profile.get('insuranceCount')}, maintenance={profile.get('maintenanceCount')}"

    def create_site_security():
        data = api_post(
            "/security/inspections",
            {
                "objectType": "SITE",
                "objectId": 1,
                "siteId": 1,
                "title": f"{ui_keyword}-SITE",
                "checkScene": "SITE_OPERATION",
                "checkType": "SPECIAL",
                "hazardCategory": "FIRE",
                "resultLevel": "FAIL",
                "dangerLevel": "HIGH",
                "issueCount": 3,
                "status": "OPEN",
                "rectifyOwner": "场地负责人",
                "rectifyOwnerPhone": "13800000003",
                "description": "phase67 场地安全档案校验",
                "rectifyDeadline": "2026-03-28T18:00:00",
                "nextCheckTime": "2026-03-31T10:00:00",
            },
        )["data"]
        created_ids.append(int(data["id"]))
        profile = data.get("relatedProfile") or {}
        assert int(profile.get("documentCount") or 0) >= 1
        assert int(profile.get("deviceCount") or 0) >= 1
        assert "资料" in (data.get("relatedProfileSummary") or "")
        return f"id={data['id']}, docs={profile.get('documentCount')}, devices={profile.get('deviceCount')}"

    def verify_detail_profiles():
        details = []
        for inspection_id in created_ids:
            detail = api_get(f"/security/inspections/{inspection_id}")["data"]
            details.append(detail)
        person_detail = next(item for item in details if item["objectType"] == "PERSON")
        vehicle_detail = next(item for item in details if item["objectType"] == "VEHICLE")
        site_detail = next(item for item in details if item["objectType"] == "SITE")
        assert int((person_detail.get("relatedProfile") or {}).get("certificateCount") or 0) >= 1
        assert int((vehicle_detail.get("relatedProfile") or {}).get("insuranceCount") or 0) >= 1
        assert int((site_detail.get("relatedProfile") or {}).get("documentCount") or 0) >= 1
        return "person/vehicle/site 详情安全档案已回填"

    def verify_export():
        blob, content_type = api_get(
            "/security/inspections/export",
            {"keyword": ui_keyword},
            expect_blob=True,
        )
        text = blob.decode("utf-8")
        assert "安全档案摘要" in text
        assert "证照数" in text
        assert "保险数" in text
        assert "场地设备数" in text
        return content_type or "blob-ok"

    run_case("security_person_profile_runtime", create_person_security)
    run_case("security_vehicle_profile_runtime", create_vehicle_security)
    run_case("security_site_profile_runtime", create_site_security)
    run_case("security_related_profile_detail_runtime", verify_detail_profiles)
    run_case("security_related_profile_export_runtime", verify_export)
    created_security_ids = created_ids


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
        page.get_by_text("安全台账管理").wait_for(timeout=10000)
        page.get_by_placeholder("搜索编号 / 标题 / 检查人 / 描述").fill(ui_keyword or "phase67 安全检查")
        page.wait_for_timeout(1200)
        page.get_by_role("button", name="详情").first.click()
        page.get_by_text("处理时间线").wait_for(timeout=10000)
        profile_visible = False
        for text in ["人员安全档案", "车辆安全档案", "场地安全档案"]:
            locator = page.get_by_text(text)
            if locator.count() > 0:
                locator.first.wait_for(timeout=5000)
                profile_visible = True
                break
        assert profile_visible, "未看到对象安全档案区块"
        record("security_related_profile_ui_visible", True, "安全台账详情抽屉已展示对象安全档案区块")

        context.close()
        browser.close()


def cleanup_runtime():
    def cleanup():
        for inspection_id in created_security_ids:
            api_delete(f"/security/inspections/{inspection_id}")
        rows = api_get("/security/inspections", {"keyword": ui_keyword})["data"]
        assert not rows
        return f"deleted={len(created_security_ids)}"

    run_case("security_related_profile_cleanup_runtime", cleanup)


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
    cleanup_runtime()
    write_reports()
