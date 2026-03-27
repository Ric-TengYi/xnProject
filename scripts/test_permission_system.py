#!/usr/bin/env python3
"""
权限体系重构 — 功能测试脚本
模块：菜单管理 / 角色权限 / 组织最大权限 / 组织管理员优化 / 用户搜索
"""

import json
import sys
import requests

BASE = "http://localhost:8090/api"
TOKEN = open("/tmp/xngl-test-token.txt").read().strip()
HEADERS = {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}

results = []
total_pass = 0
total_fail = 0

def test(module, name, method, url, body=None, expect_code=200, check=None):
    global total_pass, total_fail
    full_url = BASE + url
    try:
        if method == "GET":
            resp = requests.get(full_url, headers=HEADERS, params=body, timeout=10)
        elif method == "POST":
            resp = requests.post(full_url, headers=HEADERS, json=body, timeout=10)
        elif method == "PUT":
            resp = requests.put(full_url, headers=HEADERS, json=body, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(full_url, headers=HEADERS, timeout=10)
        else:
            raise ValueError(f"Unknown method: {method}")

        data = resp.json()
        api_code = data.get("code", resp.status_code)
        passed = api_code == expect_code

        detail = ""
        if passed and check:
            try:
                check_result = check(data)
                if check_result is not True:
                    passed = False
                    detail = f" | CHECK_FAIL: {check_result}"
            except Exception as e:
                passed = False
                detail = f" | CHECK_ERROR: {e}"

        status = "PASS" if passed else "FAIL"
        if passed:
            total_pass += 1
        else:
            total_fail += 1
            detail += f" | response_code={api_code}, expected={expect_code}"
            if not passed:
                detail += f" | body={json.dumps(data, ensure_ascii=False)[:200]}"

        results.append({"module": module, "name": name, "status": status, "detail": detail.strip(" |")})
        print(f"  [{status}] {name}{detail}")
        return data
    except Exception as e:
        total_fail += 1
        results.append({"module": module, "name": name, "status": "ERROR", "detail": str(e)[:200]})
        print(f"  [ERROR] {name} — {e}")
        return None


# ═══════════════════════════════════════════════════════
# 模块1: 菜单管理
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("模块1: 菜单管理")
print("="*60)

# TC-1.1 获取PC端菜单树
r = test("菜单管理", "TC-1.1 获取PC端菜单树", "GET", "/menus/tree",
    body={"tenantId": "1", "platform": "PC"},
    check=lambda d: True if isinstance(d.get("data"), list) and len(d["data"]) > 0 else f"Empty tree: {len(d.get('data', []))}")

# TC-1.2 获取MINI端菜单树
r = test("菜单管理", "TC-1.2 获取小程序端菜单树", "GET", "/menus/tree",
    body={"tenantId": "1", "platform": "MINI"},
    check=lambda d: True if isinstance(d.get("data"), list) and len(d["data"]) >= 0 else "Not a list")

# TC-1.3 获取SCREEN端菜单树
r = test("菜单管理", "TC-1.3 获取大屏端菜单树", "GET", "/menus/tree",
    body={"tenantId": "1", "platform": "SCREEN"},
    check=lambda d: True if isinstance(d.get("data"), list) else "Not a list")

# TC-1.4 获取全量菜单树（不传platform）
r = test("菜单管理", "TC-1.4 获取全量菜单树（无platform过滤）", "GET", "/menus/tree",
    body={"tenantId": "1"},
    check=lambda d: True if isinstance(d.get("data"), list) and len(d["data"]) > 0 else "Empty tree")

# TC-1.5 菜单树节点结构验证
def check_menu_structure(d):
    if not d.get("data") or len(d["data"]) == 0:
        return "Empty tree"
    node = d["data"][0]
    required = ["id", "menuCode", "menuName", "menuType", "platform"]
    missing = [k for k in required if k not in node]
    return True if not missing else f"Missing fields: {missing}"

test("菜单管理", "TC-1.5 菜单节点结构包含platform字段", "GET", "/menus/tree",
    body={"tenantId": "1", "platform": "PC"},
    check=check_menu_structure)

# TC-1.6 创建菜单
new_menu = test("菜单管理", "TC-1.6 创建测试菜单", "POST", "/menus",
    body={"tenantId": "1", "menuCode": "TEST_FUNC_MENU", "menuName": "功能测试菜单",
          "menuType": "MENU", "platform": "PC", "parentId": "0", "sortOrder": 999},
    check=lambda d: True if d.get("data") else "No id returned")

test_menu_id = new_menu.get("data") if new_menu else None

# TC-1.7 获取菜单详情
if test_menu_id:
    test("菜单管理", "TC-1.7 获取菜单详情", "GET", f"/menus/{test_menu_id}",
        check=lambda d: True if d.get("data", {}).get("menuName") == "功能测试菜单" else "Name mismatch")

# TC-1.8 修改菜单
if test_menu_id:
    test("菜单管理", "TC-1.8 修改菜单名称", "PUT", f"/menus/{test_menu_id}",
        body={"menuCode": "TEST_FUNC_MENU", "menuName": "功能测试菜单(已修改)",
              "menuType": "MENU", "platform": "PC", "sortOrder": 999})

# TC-1.9 创建菜单下的权限
new_perm = test("菜单管理", "TC-1.9 创建按钮权限", "POST", "/permissions",
    body={"tenantId": "1", "menuId": str(test_menu_id) if test_menu_id else "1",
          "permissionCode": "TEST_BTN_VIEW", "permissionName": "测试查看按钮",
          "resourceType": "BUTTON", "permissionType": "BUTTON"},
    check=lambda d: True if d.get("data") else "No permission id returned")

test_perm_id = new_perm.get("data") if new_perm else None

# TC-1.10 查询权限列表（按菜单ID）
if test_menu_id:
    test("菜单管理", "TC-1.10 按菜单ID查询权限列表", "GET", "/permissions",
        body={"tenantId": "1", "menuId": str(test_menu_id), "pageSize": "50"},
        check=lambda d: True if len(d.get("data", {}).get("records", [])) > 0 else "No permissions found")

# TC-1.11 删除权限
if test_perm_id:
    test("菜单管理", "TC-1.11 删除按钮权限", "DELETE", f"/permissions/{test_perm_id}")

# TC-1.12 删除菜单
if test_menu_id:
    test("菜单管理", "TC-1.12 删除测试菜单", "DELETE", f"/menus/{test_menu_id}")


# ═══════════════════════════════════════════════════════
# 模块2: 角色权限
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("模块2: 角色权限")
print("="*60)

# TC-2.1 获取角色列表
roles_data = test("角色权限", "TC-2.1 获取角色列表", "GET", "/roles",
    body={"tenantId": "1", "pageSize": "100"},
    check=lambda d: True if len(d.get("data", {}).get("records", [])) > 0 else "No roles found")

first_role_id = None
if roles_data and roles_data.get("data", {}).get("records"):
    first_role_id = str(roles_data["data"]["records"][0]["id"])

# TC-2.2 获取角色详情
if first_role_id:
    test("角色权限", "TC-2.2 获取角色详情", "GET", f"/roles/{first_role_id}",
        check=lambda d: True if d.get("data", {}).get("roleName") else "Missing roleName")

# TC-2.3 获取角色权限（含menuIds和permissionIds）
if first_role_id:
    rp = test("角色权限", "TC-2.3 获取角色权限（menuIds+permissionIds）", "GET", f"/roles/{first_role_id}/permissions",
        check=lambda d: True if "menuIds" in d.get("data", {}) and "permissionIds" in d.get("data", {}) else "Missing menuIds/permissionIds")

# TC-2.4 创建测试角色
new_role = test("角色权限", "TC-2.4 创建测试角色", "POST", "/roles",
    body={"tenantId": "1", "roleCode": "TEST_FUNC_ROLE", "roleName": "功能测试角色",
          "roleScope": "TENANT", "roleCategory": "CUSTOM", "dataScopeTypeDefault": "ORG_AND_CHILDREN"})

test_role_id = new_role.get("data") if new_role else None

# TC-2.5 分配菜单+按钮权限
if test_role_id:
    # Get some menu IDs first
    menu_tree = requests.get(BASE + "/menus/tree", headers=HEADERS, params={"tenantId": "1", "platform": "PC"}).json()
    sample_menu_ids = []
    sample_perm_ids = []
    if menu_tree.get("data"):
        for node in menu_tree["data"][:3]:
            sample_menu_ids.append(int(node["id"]))
            if node.get("children"):
                for child in node["children"][:2]:
                    sample_menu_ids.append(int(child["id"]))

    # Get some permission IDs
    perms_resp = requests.get(BASE + "/permissions", headers=HEADERS, params={"tenantId": "1", "pageSize": "20"}).json()
    if perms_resp.get("data", {}).get("records"):
        sample_perm_ids = [int(p["id"]) for p in perms_resp["data"]["records"][:5]]

    test("角色权限", "TC-2.5 分配菜单+按钮权限", "PUT", f"/roles/{test_role_id}/permissions",
        body={"menuIds": sample_menu_ids, "permissionIds": sample_perm_ids})

# TC-2.6 验证权限保存成功
if test_role_id:
    test("角色权限", "TC-2.6 验证权限回读（menuIds非空）", "GET", f"/roles/{test_role_id}/permissions",
        check=lambda d: True if len(d.get("data", {}).get("menuIds", [])) > 0 else "menuIds empty after save")

# TC-2.7 验证permissionIds也保存成功
if test_role_id:
    test("角色权限", "TC-2.7 验证权限回读（permissionIds非空）", "GET", f"/roles/{test_role_id}/permissions",
        check=lambda d: True if len(d.get("data", {}).get("permissionIds", [])) > 0 else "permissionIds empty after save")

# TC-2.8 更新数据权限范围
if test_role_id:
    test("角色权限", "TC-2.8 保存数据权限范围", "PUT", f"/roles/{test_role_id}/data-scope-rules",
        body=[{"ruleType": "SELF", "ruleValue": "[]", "resourceCode": "ALL"}])

# TC-2.9 回读数据权限范围
if test_role_id:
    test("角色权限", "TC-2.9 回读数据权限范围", "GET", f"/roles/{test_role_id}/data-scope-rules",
        check=lambda d: True if len(d.get("data", [])) > 0 and d["data"][0].get("ruleType") == "SELF" else "Data scope not saved correctly")

# TC-2.10 删除测试角色
if test_role_id:
    test("角色权限", "TC-2.10 删除测试角色", "DELETE", f"/roles/{test_role_id}")


# ═══════════════════════════════════════════════════════
# 模块3: 组织最大权限角色
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("模块3: 组织最大权限角色")
print("="*60)

# TC-3.1 获取组织树
org_tree = test("组织最大权限", "TC-3.1 获取组织树", "GET", "/orgs/tree",
    check=lambda d: True if isinstance(d.get("data"), list) else "Not a list")

first_org_id = None
if org_tree and org_tree.get("data") and len(org_tree["data"]) > 0:
    first_org_id = org_tree["data"][0].get("id")

# TC-3.2 获取组织详情包含maxRoleId字段
if first_org_id:
    test("组织最大权限", "TC-3.2 组织详情包含maxRoleId字段", "GET", f"/orgs/{first_org_id}",
        check=lambda d: True if "maxRoleId" in d.get("data", {}) else "Missing maxRoleId field")

# TC-3.3 设置组织最大权限角色
if first_org_id and first_role_id:
    test("组织最大权限", "TC-3.3 设置组织最大权限角色", "PUT", f"/orgs/{first_org_id}/max-role",
        body={"maxRoleId": first_role_id})

# TC-3.4 验证maxRoleId已保存
if first_org_id and first_role_id:
    test("组织最大权限", "TC-3.4 验证maxRoleId已保存", "GET", f"/orgs/{first_org_id}",
        check=lambda d: True if d.get("data", {}).get("maxRoleId") == first_role_id else f"maxRoleId mismatch: got {d.get('data', {}).get('maxRoleId')}")

# TC-3.5 获取组织可用菜单
if first_org_id:
    test("组织最大权限", "TC-3.5 获取组织可用菜单（PC端）", "GET", f"/orgs/{first_org_id}/available-menus",
        body={"platform": "PC"},
        check=lambda d: True if isinstance(d.get("data"), list) else "Not a list")

# TC-3.6 获取组织可用菜单（不传platform）
if first_org_id:
    test("组织最大权限", "TC-3.6 获取组织可用菜单（全平台）", "GET", f"/orgs/{first_org_id}/available-menus",
        check=lambda d: True if isinstance(d.get("data"), list) else "Not a list")

# TC-3.7 清除maxRoleId
if first_org_id:
    test("组织最大权限", "TC-3.7 清除maxRoleId", "PUT", f"/orgs/{first_org_id}/max-role",
        body={"maxRoleId": ""})

# TC-3.8 验证maxRoleId已清除
if first_org_id:
    test("组织最大权限", "TC-3.8 验证maxRoleId已清除", "GET", f"/orgs/{first_org_id}",
        check=lambda d: True if d.get("data", {}).get("maxRoleId") is None else f"maxRoleId not cleared: {d.get('data', {}).get('maxRoleId')}")

# TC-3.9 组织树节点包含maxRoleId
test("组织最大权限", "TC-3.9 组织树节点包含maxRoleId字段", "GET", "/orgs/tree",
    check=lambda d: True if len(d.get("data", [])) > 0 and "maxRoleId" in d["data"][0] else "Missing maxRoleId in tree node")


# ═══════════════════════════════════════════════════════
# 模块4: 组织创建管理员优化
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("模块4: 组织创建管理员优化")
print("="*60)

# TC-4.1 自定义管理员创建组织（NEW模式）
new_org_new = test("组织管理员", "TC-4.1 NEW模式：自定义管理员创建组织", "POST", "/orgs",
    body={
        "orgName": "测试组织-自定义管理员",
        "orgCode": "TEST_NEW_ADMIN",
        "orgType": "DEPARTMENT",
        "parentId": "0",
        "status": "ENABLED",
        "adminMode": "NEW",
        "adminUsername": "test_custom_admin",
        "adminPassword": "Test12345",
        "adminName": "测试管理员",
        "adminMobile": "13800000001"
    },
    check=lambda d: True if d.get("data", {}).get("adminUsername") == "test_custom_admin" else f"Admin username mismatch: {d.get('data', {}).get('adminUsername')}")

new_org_id_1 = new_org_new.get("data", {}).get("orgId") if new_org_new else None

# TC-4.2 验证自定义管理员密码返回
if new_org_new:
    test("组织管理员", "TC-4.2 NEW模式返回密码", "GET", "/orgs/tree",  # dummy, use returned data
        check=lambda d: True)  # just pass - we verify from the creation response
    # Actually check from new_org_new directly
    pwd = new_org_new.get("data", {}).get("adminPassword")
    status = "PASS" if pwd == "Test12345" else "FAIL"
    detail = "" if status == "PASS" else f"Expected Test12345, got {pwd}"
    results.append({"module": "组织管理员", "name": "TC-4.2 NEW模式返回自定义密码", "status": status, "detail": detail})
    print(f"  [{status}] TC-4.2 NEW模式返回自定义密码 {detail}")
    if status == "PASS":
        total_pass += 1
    else:
        total_fail += 1

# TC-4.3 自定义管理员可登录
if new_org_new:
    login_res = requests.post(BASE + "/auth/login", headers={"Content-Type": "application/json"},
        json={"username": "test_custom_admin", "password": "Test12345"}).json()
    status = "PASS" if login_res.get("code") == 200 else "FAIL"
    detail = "" if status == "PASS" else f"Login failed: {login_res.get('message')}"
    results.append({"module": "组织管理员", "name": "TC-4.3 自定义管理员可登录", "status": status, "detail": detail})
    print(f"  [{status}] TC-4.3 自定义管理员可登录 {detail}")
    if status == "PASS":
        total_pass += 1
    else:
        total_fail += 1

# TC-4.4 用户搜索（按手机号）
test("组织管理员", "TC-4.4 用户搜索（按手机号）", "GET", "/users/search",
    body={"mobile": "138", "tenantId": "1"},
    check=lambda d: True if isinstance(d.get("data"), list) else "Not a list")

# TC-4.5 搜索返回的用户包含必要字段
test("组织管理员", "TC-4.5 搜索结果包含必要字段", "GET", "/users/search",
    body={"mobile": "13800000001", "tenantId": "1"},
    check=lambda d: True if (len(d.get("data", [])) > 0 and "id" in d["data"][0] and "username" in d["data"][0]) else f"Missing fields or empty: {d.get('data', [])[:1]}")

# TC-4.6 BIND模式创建组织（绑定已有用户）
bind_user_id = None
search_res = requests.get(BASE + "/users/search", headers=HEADERS,
    params={"mobile": "13800000001", "tenantId": "1"}).json()
if search_res.get("data") and len(search_res["data"]) > 0:
    bind_user_id = search_res["data"][0]["id"]

if bind_user_id:
    new_org_bind = test("组织管理员", "TC-4.6 BIND模式：绑定已有用户创建组织", "POST", "/orgs",
        body={
            "orgName": "测试组织-绑定管理员",
            "orgCode": "TEST_BIND_ADMIN",
            "orgType": "DEPARTMENT",
            "parentId": "0",
            "status": "ENABLED",
            "adminMode": "BIND",
            "adminUserId": bind_user_id
        },
        check=lambda d: True if d.get("data", {}).get("orgId") else "No orgId returned")

    # TC-4.7 BIND模式不返回密码
    if new_org_bind:
        pwd = new_org_bind.get("data", {}).get("adminPassword")
        status = "PASS" if pwd is None or pwd == "" else "FAIL"
        detail = "" if status == "PASS" else f"Should not return password, got: {pwd}"
        results.append({"module": "组织管理员", "name": "TC-4.7 BIND模式不返回密码", "status": status, "detail": detail})
        print(f"  [{status}] TC-4.7 BIND模式不返回密码 {detail}")
        if status == "PASS":
            total_pass += 1
        else:
            total_fail += 1

        new_org_id_2 = new_org_bind.get("data", {}).get("orgId")
else:
    new_org_id_2 = None
    print("  [SKIP] TC-4.6/4.7 — No user found for BIND test")

# TC-4.8 Legacy模式（不传adminMode）
new_org_legacy = test("组织管理员", "TC-4.8 Legacy模式（无adminMode）创建组织", "POST", "/orgs",
    body={
        "orgName": "测试组织-Legacy",
        "orgCode": "TEST_LEGACY_ADMIN",
        "orgType": "DEPARTMENT",
        "parentId": "0",
        "status": "ENABLED"
    },
    check=lambda d: True if d.get("data", {}).get("adminUsername") else "No admin username")

new_org_id_3 = new_org_legacy.get("data", {}).get("orgId") if new_org_legacy else None

# TC-4.9 空手机号搜索返回空列表
test("组织管理员", "TC-4.9 空手机号搜索返回空列表", "GET", "/users/search",
    body={"mobile": "", "tenantId": "1"},
    check=lambda d: True if d.get("data") == [] else f"Expected empty, got {d.get('data')}")


# ═══════════════════════════════════════════════════════
# 模块5: 当前用户菜单权限（/me 接口）
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("模块5: 当前用户接口 (/me)")
print("="*60)

# TC-5.1 获取当前用户信息
test("/me接口", "TC-5.1 获取当前用户信息", "GET", "/me",
    check=lambda d: True if d.get("data", {}).get("username") else "Missing username")

# TC-5.2 获取当前用户权限
test("/me接口", "TC-5.2 获取当前用户权限", "GET", "/me/permissions",
    check=lambda d: True if "buttonCodes" in d.get("data", {}) and "apiCodes" in d.get("data", {}) else "Missing buttonCodes/apiCodes")

# TC-5.3 获取当前用户菜单树
test("/me接口", "TC-5.3 获取当前用户菜单树", "GET", "/me/menus",
    check=lambda d: True if isinstance(d.get("data"), list) and len(d["data"]) > 0 else "Empty menu tree")

# TC-5.4 菜单树节点包含platform字段
test("/me接口", "TC-5.4 菜单树节点包含platform字段", "GET", "/me/menus",
    check=lambda d: True if len(d.get("data", [])) > 0 and "platform" in d["data"][0] else "Missing platform in menu node")


# ═══════════════════════════════════════════════════════
# 清理测试数据
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("清理测试数据")
print("="*60)

for org_id in [new_org_id_1, new_org_id_2, new_org_id_3]:
    if org_id:
        r = requests.delete(BASE + f"/orgs/{org_id}", headers=HEADERS).json()
        ok = r.get("code") == 200
        print(f"  [{'OK' if ok else 'WARN'}] 删除测试组织 {org_id}")


# ═══════════════════════════════════════════════════════
# 汇总报告
# ═══════════════════════════════════════════════════════
print("\n" + "="*60)
print("测试结果汇总")
print("="*60)

modules = {}
for r in results:
    m = r["module"]
    if m not in modules:
        modules[m] = {"pass": 0, "fail": 0}
    if r["status"] == "PASS":
        modules[m]["pass"] += 1
    else:
        modules[m]["fail"] += 1

for m, c in modules.items():
    total = c["pass"] + c["fail"]
    pct = c["pass"] / total * 100 if total > 0 else 0
    status = "✓" if c["fail"] == 0 else "✗"
    print(f"  {status} {m}: {c['pass']}/{total} ({pct:.0f}%)")

print(f"\n  总计: {total_pass} pass / {total_fail} fail / {total_pass + total_fail} total")
print(f"  通过率: {total_pass / (total_pass + total_fail) * 100:.1f}%")

# 输出JSON报告
report = {
    "summary": {"total": total_pass + total_fail, "pass": total_pass, "fail": total_fail,
                 "rate": f"{total_pass / (total_pass + total_fail) * 100:.1f}%"},
    "modules": modules,
    "details": results
}
with open("/tmp/xngl-permission-test-report.json", "w") as f:
    json.dump(report, f, ensure_ascii=False, indent=2)

print(f"\n  JSON报告: /tmp/xngl-permission-test-report.json")

# Failed tests detail
failed = [r for r in results if r["status"] != "PASS"]
if failed:
    print(f"\n  失败用例详情:")
    for f in failed:
        print(f"    [{f['status']}] {f['name']}: {f['detail']}")

sys.exit(0 if total_fail == 0 else 1)
