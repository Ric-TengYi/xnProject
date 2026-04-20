import json
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase77_contract_message_jump_regression_{REPORT_DATE}"
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
source_contract_id = None
target_contract_id = None
site_id = None
transfer_id = None
transfer_no = None
settlement_id = None
settlement_no = None
transfer_reason = f"phase77-transfer-{suffix}"
settlement_reason = f"phase77-settlement-{suffix}"


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
            payload = resp.read()
            return json.loads(payload.decode("utf-8")) if payload else {"code": 200, "data": None}
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


def build_unique_period():
    month = int(suffix[-2:]) % 12 + 1
    day = int(suffix[-4:-2]) % 28 + 1
    date_value = datetime(2030, month, day).strftime("%Y-%m-%d")
    return date_value, date_value


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


def prepare_refs():
    global source_contract_id, target_contract_id, site_id
    contract_page = api_get("/contracts", {"contractStatus": "EFFECTIVE", "pageNo": 1, "pageSize": 20})["data"]
    contract_records = contract_page["records"]
    assert len(contract_records) >= 2, "未找到至少两个生效合同"
    source_contract_id = str(contract_records[0]["id"])
    target_contract_id = str(next(item["id"] for item in contract_records if str(item["id"]) != source_contract_id))
    site_records = api_get("/sites")["data"]
    assert len(site_records) >= 1, "未找到可用场地"
    site_id = str(site_records[0]["id"])


def verify_transfer_message_runtime():
    def transfer_flow():
        global transfer_id, transfer_no
        created = api_post(
            "/contracts/transfers",
            {
                "sourceContractId": int(source_contract_id),
                "targetContractId": int(target_contract_id),
                "transferAmount": 12.34,
                "reason": transfer_reason,
            },
        )["data"]
        transfer_id = str(created)
        api_post(f"/contracts/transfers/{transfer_id}/submit")
        api_post(f"/contracts/transfers/{transfer_id}/reject", {"reason": transfer_reason})
        detail = api_get(f"/contracts/transfers/{transfer_id}")["data"]
        transfer_no = detail["transferNo"]
        rows = api_get("/messages", {"keyword": transfer_no, "pageNo": 1, "pageSize": 20})["data"]["records"]
        assert detail["approvalStatus"] == "REJECTED"
        assert any(
            row["bizType"] == "CONTRACT_TRANSFER"
            and row["bizId"] == transfer_id
            and row["linkUrl"] == f"/contracts/transfers?transferId={transfer_id}"
            for row in rows
        )
        return f"transfer_id={transfer_id}, transfer_no={transfer_no}, message_count={len(rows)}"

    run_case("contract_transfer_message_runtime", transfer_flow)


def verify_settlement_message_runtime():
    def settlement_flow():
        global settlement_id, settlement_no
        period_start, period_end = build_unique_period()
        created = api_post(
            "/settlements/site/generate",
            {
                "targetId": int(site_id),
                "periodStart": period_start,
                "periodEnd": period_end,
                "remark": settlement_reason,
            },
        )["data"]
        settlement_id = str(created)
        api_post(f"/settlements/{settlement_id}/submit")
        api_post(f"/settlements/{settlement_id}/reject", {"reason": settlement_reason})
        detail = api_get(f"/settlements/{settlement_id}")["data"]
        settlement_no = detail["settlementNo"]
        rows = api_get("/messages", {"keyword": settlement_no, "pageNo": 1, "pageSize": 20})["data"]["records"]
        assert detail["approvalStatus"] == "REJECTED"
        assert any(
            row["bizType"] == "SETTLEMENT"
            and row["bizId"] == settlement_id
            and row["linkUrl"] == f"/contracts/settlements?settlementId={settlement_id}"
            for row in rows
        )
        return f"settlement_id={settlement_id}, settlement_no={settlement_no}, message_count={len(rows)}"

    run_case("settlement_message_runtime", settlement_flow)


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

        page.goto(BASE_WEB + "/messages")
        page.wait_for_load_state("networkidle")
        page.get_by_placeholder("搜索标题/内容/分类/发送人").fill(transfer_no)
        page.wait_for_timeout(1200)
        page.get_by_role("button", name="查看业务").first.click()
        page.wait_for_url(f"**/contracts/transfers?transferId={transfer_id}")
        page.get_by_text("内拨申请详情").first.wait_for(timeout=10000)
        page.get_by_text(transfer_no).first.wait_for(timeout=10000)

        page.goto(BASE_WEB + "/messages")
        page.wait_for_load_state("networkidle")
        page.get_by_placeholder("搜索标题/内容/分类/发送人").fill(settlement_no)
        page.wait_for_timeout(1200)
        page.get_by_role("button", name="查看业务").first.click()
        page.wait_for_url(f"**/contracts/settlements?settlementId={settlement_id}")
        page.get_by_text("结算单详情").first.wait_for(timeout=10000)
        page.get_by_text(settlement_no).first.wait_for(timeout=10000)

        record("contract_message_jump_pages_visible", True, "内拨申请消息跳转 / 结算消息跳转")
        context.close()
        browser.close()


def cleanup():
    if transfer_id:
        mysql_exec(
            f"DELETE FROM biz_message_record WHERE biz_type = 'CONTRACT_TRANSFER' AND biz_id = '{transfer_id}';"
        )
        mysql_exec(f"DELETE FROM biz_contract_transfer_apply WHERE id = {transfer_id};")
    if settlement_id:
        mysql_exec(
            f"DELETE FROM biz_message_record WHERE biz_type = 'SETTLEMENT' AND biz_id = '{settlement_id}';"
        )
        mysql_exec(f"DELETE FROM biz_settlement_item WHERE settlement_order_id = {settlement_id};")
        mysql_exec(f"DELETE FROM biz_settlement_order WHERE id = {settlement_id};")


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
        prepare_refs()
        verify_transfer_message_runtime()
        verify_settlement_message_runtime()
        verify_ui()
    finally:
        cleanup()
        write_reports()
