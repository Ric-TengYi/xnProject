import json
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_WEB = "http://127.0.0.1:5173"
BASE_API = "http://127.0.0.1:8090/api"
REPORT_DATE = "2026-03-23"
REPORT_STEM = f"phase70_governance_unified_regression_{REPORT_DATE}"
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
current_user_id = None
suffix = str(int(time.time() * 1000))

menu_id = None
created_role_id = None
created_org_id = None
created_user_id = None
created_dict_id = None
created_rule_id = None
created_material_id = None
created_flow_id = None
created_sys_param_id = None
created_message_titles = []


def record(name: str, ok: bool, detail: str):
    results.append({"name": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"{'PASS' if ok else 'FAIL'} {name}: {detail}")


def run_case(name: str, fn):
    try:
        detail = fn()
        record(name, True, detail)
    except Exception as exc:
        record(name, False, str(exc))


def _raise_http_error(exc: urllib.error.HTTPError):
    payload = exc.read().decode("utf-8", errors="ignore")
    raise RuntimeError(f"HTTP {exc.code}: {payload[:400]}")


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
        _raise_http_error(exc)


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


def api_delete(path: str):
    return api_request("DELETE", path, use_auth=True)


def mysql_exec(sql: str) -> str:
    completed = subprocess.run(
        MYSQL_CMD + ["-e", sql],
        capture_output=True,
        text=True,
        check=True,
    )
    return completed.stdout.strip()


def login_api():
    global token, user_info, tenant_id, current_user_id
    payload = api_request(
        "POST",
        "/auth/login",
        data={"tenantId": "1", "username": "admin", "password": "admin"},
        use_auth=False,
    )
    token = payload["data"]["token"]
    user_info = payload["data"]["user"]
    tenant_id = str(user_info.get("tenantId") or "1")
    current_user_id = str(user_info.get("id") or "6")
    record("login", bool(token), "admin/admin 登录成功")


def prepare_base_refs():
    global menu_id
    menu_tree = api_get("/menus/tree", {"tenantId": tenant_id})["data"]

    def pick_menu(nodes):
        for node in nodes:
            if str(node.get("menuType") or "").upper() == "MENU":
                return int(node["id"])
            child = pick_menu(node.get("children") or [])
            if child:
                return child
        return None

    menu_id = pick_menu(menu_tree)
    assert menu_id is not None, "未找到菜单数据"


def verify_org_role_user_runtime():
    role_code = f"PHASE70_ROLE_{suffix}"
    org_code = f"PHASE70_ORG_{suffix}"
    username = f"phase70_user_{suffix}"

    def role_create_and_configure():
        global created_role_id
        created_role_id = int(
            api_post(
                "/roles",
                {
                    "tenantId": tenant_id,
                    "roleCode": role_code,
                    "roleName": "Phase70 角色",
                    "roleScope": "TENANT",
                    "roleCategory": "CUSTOM",
                    "description": "phase70 governance runtime role",
                    "dataScopeTypeDefault": "ORG_AND_CHILDREN",
                },
            )["data"]
        )
        api_put(
            f"/roles/{created_role_id}/permissions",
            {"menuIds": [menu_id], "permissionIds": []},
        )
        api_put(
            f"/roles/{created_role_id}/data-scope-rules",
            [{"ruleType": "ORG_AND_CHILDREN", "ruleValue": "[]", "resourceCode": "ALL"}],
        )
        detail = api_get(f"/roles/{created_role_id}")["data"]
        permissions = api_get(f"/roles/{created_role_id}/permissions")["data"]
        rules = api_get(f"/roles/{created_role_id}/data-scope-rules")["data"]
        assert detail["roleCode"] == role_code
        assert str(menu_id) in [str(item) for item in permissions["menuIds"]]
        assert any((rule.get("ruleType") or "") == "ORG_AND_CHILDREN" for rule in rules)
        return f"role_id={created_role_id}"

    def role_update():
        updated = api_put(
            f"/roles/{created_role_id}",
            {
                "tenantId": tenant_id,
                "roleCode": role_code,
                "roleName": "Phase70 角色-更新",
                "roleScope": "TENANT",
                "roleCategory": "CUSTOM",
                "description": "phase70 governance runtime role updated",
                "dataScopeTypeDefault": "SELF",
            },
        )
        detail = api_get(f"/roles/{created_role_id}")["data"]
        api_put(
            f"/roles/{created_role_id}/data-scope-rules",
            [{"ruleType": "SELF", "ruleValue": "[]", "resourceCode": "ALL"}],
        )
        rules = api_get(f"/roles/{created_role_id}/data-scope-rules")["data"]
        assert updated["code"] == 200
        assert detail["roleName"].endswith("更新")
        assert any((rule.get("ruleType") or "") == "SELF" for rule in rules)
        return detail["roleName"]

    def org_create_update():
        global created_org_id
        created_org_id = int(
            api_post(
                "/orgs",
                {
                    "tenantId": tenant_id,
                    "parentId": "1",
                    "orgCode": org_code,
                    "orgName": "Phase70 组织",
                    "orgType": "DEPARTMENT",
                    "sortOrder": 70,
                },
            )["data"]
        )
        api_put(
            f"/orgs/{created_org_id}",
            {
                "tenantId": tenant_id,
                "parentId": "1",
                "orgCode": org_code,
                "orgName": "Phase70 组织-更新",
                "orgType": "DEPARTMENT",
                "sortOrder": 71,
            },
        )
        api_put(f"/orgs/{created_org_id}/leader", {"leaderUserId": current_user_id})
        api_put(f"/orgs/{created_org_id}/status", {"status": "DISABLED"})
        api_put(f"/orgs/{created_org_id}/status", {"status": "ENABLED"})
        detail = api_get(f"/orgs/{created_org_id}")["data"]
        tree = api_get("/orgs/tree", {"tenantId": tenant_id})["data"]

        def contains_org(nodes):
            for node in nodes:
                if str(node.get("id")) == str(created_org_id):
                    return True
                if contains_org(node.get("children") or []):
                    return True
            return False

        assert detail["orgName"].endswith("更新")
        assert detail["status"] == "ENABLED"
        assert contains_org(tree)
        return f"org_id={created_org_id}"

    def user_create_update_bindings():
        global created_user_id
        created_user_id = int(
            api_post(
                "/users",
                {
                    "tenantId": tenant_id,
                    "username": username,
                    "name": "Phase70 人员",
                    "mobile": "13900000070",
                    "email": "phase70@example.com",
                    "password": "phase70-pass",
                    "userType": "STAFF",
                    "mainOrgId": str(created_org_id),
                    "orgIds": [str(created_org_id)],
                    "roleIds": [str(created_role_id)],
                },
            )["data"]
        )
        api_put(
            f"/users/{created_user_id}",
            {
                "tenantId": tenant_id,
                "username": username,
                "name": "Phase70 人员-更新",
                "mobile": "13900000071",
                "email": "phase70-updated@example.com",
                "userType": "STAFF",
                "mainOrgId": str(created_org_id),
                "orgIds": [str(created_org_id)],
                "roleIds": [str(created_role_id)],
            },
        )
        api_put(f"/users/{created_user_id}/status", {"status": "DISABLED"})
        api_put(f"/users/{created_user_id}/status", {"status": "ENABLED"})
        api_put(f"/users/{created_user_id}/password", {"newPassword": "phase70-reset-pass"})
        roles = api_get(f"/users/{created_user_id}/roles")["data"]
        orgs = api_get(f"/users/{created_user_id}/orgs")["data"]
        detail = api_get(f"/users/{created_user_id}")["data"]
        list_rows = api_get(
            "/users",
            {"tenantId": tenant_id, "keyword": username, "pageNo": 1, "pageSize": 10},
        )["data"]["records"]
        assert detail["name"].endswith("更新")
        assert any(str(item["id"]) == str(created_role_id) for item in roles)
        assert any(str(item["id"]) == str(created_org_id) for item in orgs)
        assert any(str(item["id"]) == str(created_user_id) for item in list_rows)
        return f"user_id={created_user_id}"

    def cleanup_org_role_user():
        api_delete(f"/users/{created_user_id}")
        api_delete(f"/orgs/{created_org_id}")
        api_delete(f"/roles/{created_role_id}")
        rows = api_get(
            "/users",
            {"tenantId": tenant_id, "keyword": username, "pageNo": 1, "pageSize": 10},
        )["data"]["records"]
        assert not any(str(item["id"]) == str(created_user_id) for item in rows)
        return "user/org/role cleanup ok"

    run_case("governance_role_create_runtime", role_create_and_configure)
    run_case("governance_role_update_runtime", role_update)
    run_case("governance_org_create_update_runtime", org_create_update)
    run_case("governance_user_create_update_runtime", user_create_update_bindings)
    run_case("governance_org_role_user_cleanup_runtime", cleanup_org_role_user)


def verify_dict_runtime():
    dict_type = f"PHASE70_DICT_{suffix}"
    dict_code = f"PHASE70_ITEM_{suffix}"

    def create_update_toggle_export_delete():
        global created_dict_id
        created = api_post(
            "/data-dicts",
            {
                "dictType": dict_type,
                "dictCode": dict_code,
                "dictLabel": "Phase70 字典项",
                "dictValue": "PHASE70_VALUE",
                "sort": 70,
                "status": "ENABLED",
                "remark": "phase70 governance dict",
            },
        )["data"]
        created_dict_id = int(created["id"])
        updated = api_put(
            f"/data-dicts/{created_dict_id}",
            {
                "dictType": dict_type,
                "dictCode": dict_code,
                "dictLabel": "Phase70 字典项-更新",
                "dictValue": "PHASE70_VALUE_UPDATED",
                "sort": 71,
                "status": "ENABLED",
                "remark": "phase70 governance dict updated",
            },
        )["data"]
        api_put(f"/data-dicts/{created_dict_id}/status", {"status": "DISABLED"})
        api_put(f"/data-dicts/{created_dict_id}/status", {"status": "ENABLED"})
        rows = api_get("/data-dicts", {"dictType": dict_type})["data"]
        blob, _ = api_get("/data-dicts/export", {"dictType": dict_type}, expect_blob=True)
        text = blob.decode("utf-8")
        api_delete(f"/data-dicts/{created_dict_id}")
        assert updated["dictLabel"].endswith("更新")
        assert any(int(item["id"]) == created_dict_id for item in rows)
        assert dict_type in text and dict_code in text
        return f"dict_id={created_dict_id}"

    run_case("governance_data_dict_crud_runtime", create_update_toggle_export_delete)


def verify_approval_runtime():
    process_key = f"PHASE70_PROCESS_{suffix}"

    def actor_rule_flow():
        global created_rule_id
        created_rule_id = int(
            api_post(
                "/approval-actor-rules",
                {
                    "tenantId": tenant_id,
                    "processKey": process_key,
                    "ruleName": "Phase70 审批规则",
                    "ruleType": "ROLE",
                    "ruleExpression": "phase70-role",
                    "priority": 70,
                },
            )["data"]
        )
        api_put(
            f"/approval-actor-rules/{created_rule_id}",
            {
                "tenantId": tenant_id,
                "processKey": process_key,
                "ruleName": "Phase70 审批规则-更新",
                "ruleType": "USER",
                "ruleExpression": current_user_id,
                "priority": 71,
            },
        )
        api_put(f"/approval-actor-rules/{created_rule_id}/status", {"status": "DISABLED"})
        api_put(f"/approval-actor-rules/{created_rule_id}/status", {"status": "ENABLED"})
        detail = api_get(f"/approval-actor-rules/{created_rule_id}")["data"]
        rows = api_get(
            "/approval-actor-rules",
            {"tenantId": tenant_id, "processKey": process_key, "pageNo": 1, "pageSize": 20},
        )["data"]["records"]
        blob, _ = api_get(
            "/approval-actor-rules/export",
            {"tenantId": tenant_id, "processKey": process_key},
            expect_blob=True,
        )
        text = blob.decode("utf-8")
        assert detail["ruleName"].endswith("更新")
        assert any(int(item["id"]) == created_rule_id for item in rows)
        assert process_key in text
        return f"rule_id={created_rule_id}"

    def material_flow():
        global created_material_id
        created_material_id = int(
            api_post(
                "/approval-material-configs",
                {
                    "processKey": process_key,
                    "materialCode": f"PHASE70_MATERIAL_{suffix}",
                    "materialName": "Phase70 材料",
                    "materialType": "PDF",
                    "required": True,
                    "sortOrder": 10,
                    "status": "ENABLED",
                    "remark": "phase70 material",
                },
            )["data"]
        )
        api_put(
            f"/approval-material-configs/{created_material_id}",
            {
                "processKey": process_key,
                "materialCode": f"PHASE70_MATERIAL_{suffix}",
                "materialName": "Phase70 材料-更新",
                "materialType": "IMAGE",
                "required": False,
                "sortOrder": 11,
                "status": "ENABLED",
                "remark": "phase70 material updated",
            },
        )
        api_put(f"/approval-material-configs/{created_material_id}/status", {"status": "DISABLED"})
        api_put(f"/approval-material-configs/{created_material_id}/status", {"status": "ENABLED"})
        detail = api_get(f"/approval-material-configs/{created_material_id}")["data"]
        rows = api_get("/approval-material-configs", {"processKey": process_key})["data"]
        blob, _ = api_get(
            "/approval-material-configs/export",
            {"processKey": process_key},
            expect_blob=True,
        )
        text = blob.decode("utf-8")
        assert detail["materialName"].endswith("更新")
        assert any(int(item["id"]) == created_material_id for item in rows)
        assert process_key in text
        return f"material_id={created_material_id}"

    def flow_config_flow():
        global created_flow_id
        created = api_post(
            "/approval-configs",
            {
                "processKey": process_key,
                "configName": "Phase70 流程",
                "approvalType": "SERIAL",
                "nodeCode": f"PHASE70_NODE_{suffix}",
                "nodeName": "Phase70 节点",
                "approvers": "role:phase70",
                "conditions": "{\"amount\":\">=0\"}",
                "formTemplateCode": "PHASE70_FORM",
                "timeoutHours": 12,
                "sortOrder": 5,
                "status": "ENABLED",
                "remark": "phase70 flow",
            },
        )["data"]
        created_flow_id = int(created["id"])
        updated = api_put(
            f"/approval-configs/{created_flow_id}",
            {
                "processKey": process_key,
                "configName": "Phase70 流程-更新",
                "approvalType": "PARALLEL",
                "nodeCode": f"PHASE70_NODE_{suffix}",
                "nodeName": "Phase70 节点-更新",
                "approvers": "user:6",
                "conditions": "{\"amount\":\">=100\"}",
                "formTemplateCode": "PHASE70_FORM_UPDATED",
                "timeoutHours": 18,
                "sortOrder": 6,
                "status": "ENABLED",
                "remark": "phase70 flow updated",
            },
        )["data"]
        api_put(f"/approval-configs/{created_flow_id}/status", {"status": "DISABLED"})
        api_put(f"/approval-configs/{created_flow_id}/status", {"status": "ENABLED"})
        detail = api_get(f"/approval-configs/{created_flow_id}")["data"]
        rows = api_get("/approval-configs", {"processKey": process_key})["data"]
        blob, _ = api_get("/approval-configs/export", {"processKey": process_key}, expect_blob=True)
        text = blob.decode("utf-8")
        assert updated["configName"].endswith("更新")
        assert detail["approvalType"] == "PARALLEL"
        assert any(int(item["id"]) == created_flow_id for item in rows)
        assert process_key in text
        return f"flow_id={created_flow_id}"

    def cleanup_approval():
        api_delete(f"/approval-configs/{created_flow_id}")
        api_delete(f"/approval-material-configs/{created_material_id}")
        api_delete(f"/approval-actor-rules/{created_rule_id}")
        rows = api_get(
            "/approval-actor-rules",
            {"tenantId": tenant_id, "processKey": process_key, "pageNo": 1, "pageSize": 20},
        )["data"]["records"]
        assert not any(int(item["id"]) == created_rule_id for item in rows)
        return "approval cleanup ok"

    run_case("governance_approval_actor_rule_runtime", actor_rule_flow)
    run_case("governance_approval_material_runtime", material_flow)
    run_case("governance_approval_flow_runtime", flow_config_flow)
    run_case("governance_approval_cleanup_runtime", cleanup_approval)


def verify_sys_params_runtime():
    param_key = f"phase70.param.{suffix}"

    def create_update_export_delete():
        global created_sys_param_id
        created = api_post(
            "/sys-params",
            {
                "paramKey": param_key,
                "paramName": "Phase70 参数",
                "paramValue": "70",
                "paramType": "NUMBER",
                "status": "ENABLED",
                "remark": "phase70 sys param",
            },
        )["data"]
        created_sys_param_id = int(created["id"])
        updated = api_put(
            f"/sys-params/{created_sys_param_id}",
            {
                "paramKey": param_key,
                "paramName": "Phase70 参数-更新",
                "paramValue": "{\"value\":71}",
                "paramType": "JSON",
                "status": "ENABLED",
                "remark": "phase70 sys param updated",
            },
        )["data"]
        api_put(f"/sys-params/{created_sys_param_id}/status", {"status": "DISABLED"})
        api_put(f"/sys-params/{created_sys_param_id}/status", {"status": "ENABLED"})
        rows = api_get("/sys-params", {"keyword": param_key})["data"]
        blob, _ = api_get("/sys-params/export", {"keyword": param_key}, expect_blob=True)
        text = blob.decode("utf-8")
        api_delete(f"/sys-params/{created_sys_param_id}")
        assert updated["paramName"].endswith("更新")
        assert any(int(item["id"]) == created_sys_param_id for item in rows)
        assert param_key in text
        return f"sys_param_id={created_sys_param_id}"

    run_case("governance_sys_param_crud_runtime", create_update_export_delete)


def verify_logs_and_messages_runtime():
    title1 = f"PHASE70_MSG_{suffix}_1"
    title2 = f"PHASE70_MSG_{suffix}_2"
    created_message_titles.extend([title1, title2])

    def logs_and_exports():
        login_rows = api_get(
            "/login-logs",
            {"tenantId": tenant_id, "pageNo": 1, "pageSize": 5},
        )["data"]["records"]
        operation_rows = api_get(
            "/operation-logs",
            {"tenantId": tenant_id, "pageNo": 1, "pageSize": 5},
        )["data"]["records"]
        error_rows = api_get(
            "/error-logs",
            {"tenantId": tenant_id, "pageNo": 1, "pageSize": 5},
        )["data"]["records"]
        login_csv = api_get("/login-logs/export", {"tenantId": tenant_id}, expect_blob=True)[0].decode("utf-8")
        operation_csv = api_get("/operation-logs/export", {"tenantId": tenant_id}, expect_blob=True)[0].decode("utf-8")
        error_csv = api_get("/error-logs/export", {"tenantId": tenant_id}, expect_blob=True)[0].decode("utf-8")
        assert len(login_rows) >= 1
        assert "登录账号" in login_csv
        assert "操作人" in operation_csv
        assert "记录时间" in error_csv
        return f"login={len(login_rows)}, operate={len(operation_rows)}, error={len(error_rows)}"

    def message_flow():
        mysql_exec(
            "INSERT INTO biz_message_record "
            "(tenant_id, receiver_type, receiver_id, title, content, category, channel, status, priority, sender_name, send_time, create_time, update_time, deleted) "
            f"VALUES ({tenant_id}, 'USER', {current_user_id}, '{title1}', 'phase70 unread message 1', '系统回归', 'SYSTEM', 'UNREAD', 'HIGH', 'phase70', NOW(), NOW(), NOW(), 0), "
            f"({tenant_id}, 'USER', {current_user_id}, '{title2}', 'phase70 unread message 2', '系统回归', 'SMS', 'UNREAD', 'NORMAL', 'phase70', NOW(), NOW(), NOW(), 0);"
        )
        summary = api_get("/messages/summary")["data"]
        unread_page = api_get(
            "/messages",
            {"keyword": "PHASE70_MSG_", "status": "UNREAD", "pageNo": 1, "pageSize": 10},
        )["data"]["records"]
        assert len(unread_page) >= 2
        target_id = int(unread_page[0]["id"])
        read_result = api_put(f"/messages/{target_id}/read")["data"]
        assert read_result["status"] == "READ"
        updated = api_put("/messages/read-all")["data"]
        final_rows = api_get(
            "/messages",
            {"keyword": "PHASE70_MSG_", "pageNo": 1, "pageSize": 10},
        )["data"]["records"]
        final_summary = api_get("/messages/summary")["data"]
        export_text = api_get(
            "/messages/export",
            {"keyword": "PHASE70_MSG_"},
            expect_blob=True,
        )[0].decode("utf-8")
        assert summary["total"] >= 2
        assert updated["updated"] >= 1
        assert all((item.get("status") or "") == "READ" for item in final_rows)
        assert title1 in export_text and title2 in export_text
        assert final_summary["unread"] == 0
        return f"messages={len(final_rows)}"

    def cleanup_messages():
        mysql_exec(
            "DELETE FROM biz_message_record "
            f"WHERE tenant_id = {tenant_id} AND receiver_id = {current_user_id} "
            f"AND title IN ('{title1}', '{title2}');"
        )
        rows = api_get(
            "/messages",
            {"keyword": "PHASE70_MSG_", "pageNo": 1, "pageSize": 10},
        )["data"]["records"]
        assert not any(item.get("title") in {title1, title2} for item in rows)
        return "message cleanup ok"

    run_case("governance_logs_export_runtime", logs_and_exports)
    run_case("governance_messages_runtime", message_flow)
    run_case("governance_messages_cleanup_runtime", cleanup_messages)


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
            ("/settings/organization", "组织与人员管理"),
            ("/settings/roles", "角色与权限管理"),
            ("/settings/dictionary", "数据字典"),
            ("/settings/approvals", "审核审批配置"),
            ("/settings/system-params", "系统参数"),
            ("/settings/logs", "系统日志"),
            ("/messages", "消息管理"),
        ]
        visible = []
        for route, title in cases:
            page.goto(BASE_WEB + route)
            page.wait_for_load_state("networkidle")
            page.get_by_text(title).first.wait_for(timeout=10000)
            visible.append(title)
        record("governance_pages_visible", True, " / ".join(visible))
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
    prepare_base_refs()
    verify_org_role_user_runtime()
    verify_dict_runtime()
    verify_approval_runtime()
    verify_sys_params_runtime()
    verify_logs_and_messages_runtime()
    verify_ui()
    write_reports()
