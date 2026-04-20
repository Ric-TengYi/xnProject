import json
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase61_plan5_pending_acceptance_regression_{REPORT_DATE}"
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
      content_type = resp.headers.get("Content-Type", "")
      if expect_blob:
        return payload, content_type
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
    record("login", bool(token), "admin/admin 登录成功并获取 token")


def verify_platform_acceptance():
    run_case(
        "platform_overview",
        lambda: f"enabled={api_get('/platform-integrations/overview')['data']['enabledCount']}",
    )
    run_case(
        "platform_configs",
        lambda: f"configs={len(api_get('/platform-integrations/configs')['data'])}",
    )
    run_case(
        "platform_video_channels",
        lambda: f"channels={len(api_get('/platform-integrations/video/channels')['data'])}",
    )
    run_case(
        "platform_sync_logs",
        lambda: f"logs={len(api_get('/platform-integrations/sync-logs')['data'])}",
    )
    run_case(
        "platform_gov_sync_logs",
        lambda: f"govLogs={len(api_get('/platform-integrations/gov/sync-logs')['data'])}",
    )
    run_case(
        "platform_weighbridge_records",
        lambda: f"records={len(api_get('/platform-integrations/weighbridge/records')['data'])}",
    )

    def create_ticket():
      data = api_post(
          "/platform-integrations/sso/tickets",
          {"targetPlatform": "PLAN5", "redirectUri": "https://example.com/plan5"},
      )["data"]
      assert data["ticket"]
      return f"ticket={data['ticket'][:8]}..., target={data.get('targetPlatform')}"

    run_case("platform_sso_ticket", create_ticket)


def verify_contract_acceptance():
    contract_list = api_get("/contracts", {"pageNo": 1, "pageSize": 20})
    records = contract_list["data"]["records"]
    contract_id = records[0]["id"] if records else None

    run_case("contracts_stats", lambda: f"effective={api_get('/contracts/stats')['data']['effectiveContracts']}")
    record("contracts_list", contract_list["code"] == 200, f"records={len(records)}")

    if contract_id:
      run_case(
          "contracts_detail",
          lambda: f"contractNo={api_get(f'/contracts/{contract_id}')['data']['contractNo']}",
      )
      run_case(
          "contracts_approval_records",
          lambda: f"records={len(api_get(f'/contracts/{contract_id}/approval-records')['data'])}",
      )
      run_case(
          "contracts_materials",
          lambda: f"materials={len(api_get(f'/contracts/{contract_id}/materials')['data'])}",
      )
      run_case(
          "contracts_invoices",
          lambda: f"invoices={len(api_get(f'/contracts/{contract_id}/invoices')['data'])}",
      )
      run_case(
          "contracts_tickets",
          lambda: f"tickets={len(api_get(f'/contracts/{contract_id}/tickets')['data'])}",
      )
      run_case(
          "contracts_receipts_by_contract",
          lambda: f"receipts={len(api_get(f'/contracts/{contract_id}/receipts')['data'])}",
      )

    def export_contracts():
      payload = api_post("/contracts/export", {"exportType": "CSV", "pageNo": 1, "pageSize": 20})["data"]
      task_id = payload["taskId"]
      task = api_get(f"/export-tasks/{task_id}")["data"]
      blob, content_type = api_get(f"/export-tasks/{task_id}/download", expect_blob=True)
      assert "text/csv" in content_type and len(blob) > 0
      return f"taskId={task_id}, file={task['fileName']}, bytes={len(blob)}"

    run_case("contracts_export", export_contracts)

    def import_preview():
      data = api_post(
          "/contracts/import-preview",
          {
              "fileName": "plan5_contract_import.csv",
              "rows": [
                  {
                      "合同编号": "HT-PLAN5-001",
                      "合同名称": "Plan5历史合同",
                      "合同类型": "DISPOSAL",
                      "项目ID": "1",
                      "场地ID": "1",
                      "建设单位ID": "1",
                      "运输单位ID": "1",
                      "签订日期": "2026-03-01",
                      "生效日期": "2026-03-01",
                      "到期日期": "2026-12-31",
                      "约定方量": "100",
                      "合同单价": "12.5",
                      "合同金额": "1250",
                      "三方合同": "否",
                      "备注": "Plan5回归",
                      "区内单价": "12.5",
                      "区外单价": "13.0",
                  }
              ],
          },
      )["data"]
      return f"batchId={data['batchId']}, valid={data['validCount']}, errors={data['errorCount']}"

    run_case("contracts_import_preview", import_preview)
    run_case(
        "contract_change_list",
        lambda: f"records={len(api_get('/contracts/change-applications', {'pageNo': 1, 'pageSize': 20})['data']['records'])}",
    )
    run_case(
        "contract_extension_list",
        lambda: f"records={len(api_get('/contracts/extensions', {'pageNo': 1, 'pageSize': 20})['data']['records'])}",
    )
    run_case(
        "contract_transfer_list",
        lambda: f"records={len(api_get('/contracts/transfers', {'pageNo': 1, 'pageSize': 20})['data']['records'])}",
    )
    run_case(
        "contract_receipts_list",
        lambda: f"records={len(api_get('/contracts/receipts', {'pageNo': 1, 'pageSize': 20})['data']['records'])}",
    )
    run_case(
        "settlements_stats",
        lambda: f"totalOrders={api_get('/settlements/stats')['data']['totalOrders']}",
    )
    run_case(
        "settlements_list",
        lambda: f"records={len(api_get('/settlements', {'pageNo': 1, 'pageSize': 20})['data']['records'])}",
    )
    run_case(
        "contract_monthly_summary",
        lambda: f"month={api_get('/reports/contracts/monthly/summary')['data']['month']}",
    )
    run_case(
        "contract_monthly_trend",
        lambda: f"points={len(api_get('/reports/contracts/monthly/trend', {'months': 6})['data'])}",
    )
    run_case(
        "contract_monthly_types",
        lambda: f"rows={len(api_get('/reports/contracts/monthly/types')['data'])}",
    )


def verify_permit_acceptance():
    permit_list = api_get("/disposal-permits")
    rows = permit_list["data"]
    record("permits_list", permit_list["code"] == 200, f"records={len(rows)}")

    if rows:
      permit_id = rows[0]["id"]
      run_case(
          "permits_detail",
          lambda: f"permitNo={api_get(f'/disposal-permits/{permit_id}')['data']['permitNo']}",
      )


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

      ui_cases = [
          ("ui_platform_integrations", "/settings/platform-integrations", "平台对接中心"),
          ("ui_contracts", "/contracts", "合同与财务结算"),
          ("ui_contract_transfers", "/contracts/transfers", "内拨申请"),
          ("ui_contract_payments", "/contracts/payments", "合同入账管理"),
          ("ui_settlements", "/contracts/settlements", "结算管理"),
          ("ui_monthly_report", "/contracts/monthly-report", "月报统计"),
          ("ui_project_permits", "/projects/permits", "处置证清单"),
      ]

      for name, path, text in ui_cases:
        def check_page(target=path, title=text):
          page.goto(f"{BASE_WEB}{target}")
          page.wait_for_load_state("networkidle")
          page.wait_for_timeout(1200)
          visible = page.locator(f'text="{title}"').first.is_visible()
          table_count = page.locator(".ant-table").count()
          return f"title={title}, tables={table_count}, url={page.url}" if visible else (_ for _ in ()).throw(Exception(f"title {title} not visible"))

        run_case(name, check_page)

      context.close()
      browser.close()


def write_reports():
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    passed = sum(1 for item in results if item["status"] == "PASS")
    failed = len(results) - passed
    payload = {
        "date": REPORT_DATE,
        "phase": "Phase61",
        "scope": ["平台对接统一回归", "合同管理统一回归", "处置证统一回归"],
        "passed": passed,
        "failed": failed,
        "results": results,
    }
    JSON_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [
        "# Phase61 Plan5 待验收模块统一回归",
        "",
        f"- 日期：{REPORT_DATE}",
        "- 覆盖需求：`Plan 5 第一批 = 平台对接 / 合同管理 / 处置证统一页面回归与补验收`",
        f"- 结果：`{passed} / {len(results)} PASS`",
        "",
        "## 覆盖内容",
        "- 平台对接：概览、配置、视频通道、同步日志、SSO 票据、地磅记录",
        "- 合同管理：合同清单/详情、导出、导入预检、变更/延期/内拨列表、入账、结算、月报",
        "- 处置证：清单、详情、页面可达性",
        "",
        "## 测试结果",
        "| 用例 | 结果 | 说明 |",
        "|---|---|---|",
    ]
    for item in results:
      lines.append(f"| {item['name']} | {item['status']} | {item['detail']} |")
    lines.extend(
        [
            "",
            "## 验证命令",
            "```bash",
            "python3 scripts/phase61_plan5_pending_acceptance_regression.py",
            "```",
        ]
    )
    MD_PATH.write_text("\n".join(lines), encoding="utf-8")


def main():
    login_api()
    run_case("health", lambda: str(api_request("GET", "/health", use_auth=False)["data"]))
    verify_platform_acceptance()
    verify_contract_acceptance()
    verify_permit_acceptance()
    verify_ui()
    write_reports()
    print(f"report json => {JSON_PATH}")
    print(f"report md   => {MD_PATH}")
    if any(item["status"] != "PASS" for item in results):
      raise SystemExit(1)


if __name__ == "__main__":
    main()
