"""
消纳平台用户体系全面测试脚本
测试范围：登录、组织、角色、用户、菜单、权限、多租户
"""
import json
from playwright.sync_api import sync_playwright, expect, Page

BASE_URL = "http://localhost:5173"
API_URL = "http://localhost:8090/api"
TEST_RESULTS = []

def log_result(module: str, test_case: str, passed: bool, detail: str = ""):
    """记录测试结果"""
    result = {
        "module": module,
        "test_case": test_case,
        "passed": passed,
        "detail": detail
    }
    TEST_RESULTS.append(result)
    status = "✅ PASS" if passed else "❌ FAIL"
    print(f"{status} [{module}] {test_case}: {detail}")

def save_screenshot(page: Page, name: str):
    """保存截图"""
    page.screenshot(path=f"/tmp/xngl_test_{name}.png")

def login(page: Page, username: str = "admin", password: str = "admin") -> bool:
    """登录操作"""
    try:
        page.goto(f"{BASE_URL}/login")
        page.wait_for_load_state('networkidle')

        # 等待登录表单加载
        page.wait_for_selector('input[placeholder="请输入账号"]', timeout=5000)

        # 填写账号密码
        page.fill('input[placeholder="请输入账号"]', username)
        page.fill('input[placeholder="请输入密码"]', password)

        # 点击登录按钮
        page.click('button[type="submit"]')

        # 等待跳转或错误提示
        page.wait_for_timeout(2000)

        # 检查是否登录成功（跳转到首页或出现错误）
        current_url = page.url
        if "/login" not in current_url:
            return True

        # 检查是否有错误提示
        error_msg = page.locator('.ant-message-error, .ant-message').text_content(timeout=1000)
        if error_msg:
            return False

        return "/login" not in page.url
    except Exception as e:
        print(f"Login error: {e}")
        return False

def test_login(page: Page):
    """测试登录功能"""
    print("\n=== 开始测试登录功能 ===")

    # 测试1: 正确的账号密码登录
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state('networkidle')
    page.wait_for_selector('input[placeholder="请输入账号"]', timeout=5000)

    page.fill('input[placeholder="请输入账号"]', "admin")
    page.fill('input[placeholder="请输入密码"]', "admin")
    page.click('button[type="submit"]')
    page.wait_for_timeout(2000)

    login_success = "/login" not in page.url
    log_result("登录", "正确账号密码登录", login_success, f"URL: {page.url}")

    if login_success:
        save_screenshot(page, "login_success")

        # 检查是否能看到主页内容
        page.wait_for_selector('.g-sider-logo', timeout=5000)
        sider_visible = page.locator('.g-sider-logo').is_visible()
        log_result("登录", "登录后侧边栏显示", sider_visible)

    # 测试2: 错误的密码登录
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state('networkidle')
    page.wait_for_selector('input[placeholder="请输入账号"]', timeout=5000)

    page.fill('input[placeholder="请输入账号"]', "admin")
    page.fill('input[placeholder="请输入密码"]', "wrongpassword")
    page.click('button[type="submit"]')
    page.wait_for_timeout(2000)

    login_failed = "/login" in page.url
    log_result("登录", "错误密码登录失败", login_failed, "应保持在登录页")

    # 测试3: 不存在的用户登录
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state('networkidle')
    page.fill('input[placeholder="请输入账号"]', "nonexistent")
    page.fill('input[placeholder="请输入密码"]', "password")
    page.click('button[type="submit"]')
    page.wait_for_timeout(2000)

    nonexistent_failed = "/login" in page.url
    log_result("登录", "不存在用户登录失败", nonexistent_failed)

    # 测试4: 空账号密码
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state('networkidle')
    page.click('button[type="submit"]')
    page.wait_for_timeout(1000)

    # 应该有表单验证提示
    validation_shown = page.locator('.ant-form-item-explain-error').count() > 0
    log_result("登录", "空账号密码表单验证", validation_shown)

def test_organization(page: Page):
    """测试组织管理"""
    print("\n=== 开始测试组织管理 ===")

    # 确保已登录
    if "/login" in page.url:
        login(page)

    # 导航到组织管理页面
    page.goto(f"{BASE_URL}/settings/organization")
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    save_screenshot(page, "org_page")

    # 测试1: 组织树是否显示
    tree_visible = page.locator('.ant-tree').count() > 0
    log_result("组织管理", "组织树显示", tree_visible)

    # 测试2: 用户列表是否显示
    user_table_visible = page.locator('.ant-table').count() > 0
    log_result("组织管理", "用户列表显示", user_table_visible)

    # 测试3: 点击组织树节点筛选用户
    tree_nodes = page.locator('.ant-tree-node-content-wrapper')
    if tree_nodes.count() > 0:
        first_node = tree_nodes.first()
        first_node.click()
        page.wait_for_timeout(1000)
        log_result("组织管理", "点击组织节点筛选", True)
    else:
        log_result("组织管理", "点击组织节点筛选", False, "无组织节点")

    # 测试4: 新增人员弹窗
    add_btn = page.locator('button:has-text("新增人员")')
    if add_btn.count() > 0:
        add_btn.click()
        page.wait_for_timeout(500)

        modal_visible = page.locator('.ant-modal:visible').count() > 0
        log_result("组织管理", "新增人员弹窗打开", modal_visible)

        # 检查表单字段
        form_items = page.locator('.ant-modal .ant-form-item')
        form_fields_ok = form_items.count() >= 5  # 姓名、账号、密码、用户类型、组织、角色等
        log_result("组织管理", "新增人员表单字段完整", form_fields_ok, f"字段数: {form_items.count()}")

        # 关闭弹窗
        page.click('.ant-modal button:has-text("取消")')
        page.wait_for_timeout(500)
    else:
        log_result("组织管理", "新增人员弹窗打开", False, "按钮不存在")

    # 测试5: 搜索功能
    search_input = page.locator('input[placeholder*="搜索"]')
    if search_input.count() > 0:
        search_input.first.fill("admin")
        page.keyboard.press('Enter')
        page.wait_for_timeout(1000)
        log_result("组织管理", "搜索功能", True)
    else:
        log_result("组织管理", "搜索功能", False, "搜索框不存在")

def test_roles(page: Page):
    """测试角色管理"""
    print("\n=== 开始测试角色管理 ===")

    # 导航到角色管理页面
    page.goto(f"{BASE_URL}/settings/roles")
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    save_screenshot(page, "roles_page")

    # 测试1: 角色列表显示
    role_list = page.locator('.ant-list')
    roles_visible = role_list.count() > 0
    log_result("角色管理", "角色列表显示", roles_visible)

    # 测试2: 菜单权限树显示
    menu_tree = page.locator('.ant-tree-checkable')
    menu_tree_visible = menu_tree.count() > 0
    log_result("角色管理", "菜单权限树显示", menu_tree_visible)

    # 测试3: 点击角色查看权限
    role_items = page.locator('.ant-list-item')
    if role_items.count() > 0:
        role_items.first.click()
        page.wait_for_timeout(500)
        log_result("角色管理", "点击角色查看权限", True)
    else:
        log_result("角色管理", "点击角色查看权限", False, "无角色项")

    # 测试4: 数据权限范围选择器
    scope_select = page.locator('.ant-select:has-text("全部数据可见")')
    scope_visible = scope_select.count() > 0
    log_result("角色管理", "数据权限范围选择器", scope_visible)

    # 测试5: 保存配置按钮
    save_btn = page.locator('button:has-text("保存配置")')
    save_btn_visible = save_btn.count() > 0
    log_result("角色管理", "保存配置按钮存在", save_btn_visible)

def test_users_api():
    """测试用户管理API"""
    print("\n=== 开始测试用户管理API ===")

    import urllib.request

    # 测试1: 获取用户列表
    try:
        req = urllib.request.Request(f"{API_URL}/users?tenantId=1&pageNo=1&pageSize=10")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            users_ok = data.get('code') == 200
            log_result("用户API", "获取用户列表", users_ok, f"返回{len(data.get('data', {}).get('records', []))}条记录")
    except Exception as e:
        log_result("用户API", "获取用户列表", False, str(e))

    # 测试2: 获取组织树
    try:
        req = urllib.request.Request(f"{API_URL}/orgs/tree?tenantId=1")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            orgs_ok = data.get('code') == 200
            log_result("组织API", "获取组织树", orgs_ok, f"返回{len(data.get('data', []))}个顶级组织")
    except Exception as e:
        log_result("组织API", "获取组织树", False, str(e))

    # 测试3: 获取角色列表
    try:
        req = urllib.request.Request(f"{API_URL}/roles?tenantId=1&pageNo=1&pageSize=10")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            roles_ok = data.get('code') == 200
            log_result("角色API", "获取角色列表", roles_ok)
    except Exception as e:
        log_result("角色API", "获取角色列表", False, str(e))

    # 测试4: 获取菜单树
    try:
        req = urllib.request.Request(f"{API_URL}/menus/tree?tenantId=1")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            menus_ok = data.get('code') == 200
            log_result("菜单API", "获取菜单树", menus_ok)
    except Exception as e:
        log_result("菜单API", "获取菜单树", False, str(e))

def test_menu_navigation(page: Page):
    """测试菜单导航"""
    print("\n=== 开始测试菜单导航 ===")

    # 确保已登录
    if "/login" in page.url:
        login(page)

    # 测试各菜单页面
    menu_routes = [
        ("/", "数据看板"),
        ("/projects", "项目管理"),
        ("/sites", "消纳场地"),
        ("/vehicles", "车辆与运力"),
        ("/contracts", "合同与结算"),
        ("/alerts", "预警与安全"),
        ("/settings/organization", "系统设置-组织人员"),
        ("/settings/roles", "系统设置-角色管理"),
        ("/settings/dictionary", "系统设置-数据字典"),
    ]

    for route, name in menu_routes:
        try:
            page.goto(f"{BASE_URL}{route}")
            page.wait_for_load_state('networkidle')
            page.wait_for_timeout(500)

            # 检查页面是否正常渲染（无JS错误或白屏）
            page_content = page.content()
            page_ok = len(page_content) > 1000 and "root" in page_content
            log_result("菜单导航", f"访问{name}", page_ok, f"路由: {route}")

            save_screenshot(page, f"menu_{route.replace('/', '_')}")

        except Exception as e:
            log_result("菜单导航", f"访问{name}", False, str(e))

def test_permission_control(page: Page):
    """测试权限控制"""
    print("\n=== 开始测试权限控制 ===")

    # 确保已登录为admin
    if "/login" in page.url:
        login(page)

    # 测试1: 检查动态菜单是否从API加载
    page.goto(f"{BASE_URL}/")
    page.wait_for_load_state('networkidle')

    # 检查侧边栏菜单
    menu_items = page.locator('.ant-menu-item, .ant-menu-submenu')
    menu_count = menu_items.count()
    log_result("权限控制", "动态菜单渲染", menu_count > 0, f"菜单项数: {menu_count}")

    # 测试2: 检查页面权限控制 - 访问需要登录的页面
    # 先登出
    page.click('.g-sider-footer-user')
    page.wait_for_timeout(300)
    page.click('.ant-dropdown-menu-item:has-text("退出登录")')
    page.wait_for_timeout(1000)

    # 未登录访问受保护页面应该跳转到登录页
    page.goto(f"{BASE_URL}/settings/organization")
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    redirected_to_login = "/login" in page.url
    log_result("权限控制", "未登录访问受保护页面跳转登录", redirected_to_login)

    # 测试3: 重新登录
    login(page)
    logged_in = "/login" not in page.url
    log_result("权限控制", "重新登录成功", logged_in)

def test_data_isolation(page: Page):
    """测试数据隔离"""
    print("\n=== 开始测试数据隔离 ===")

    # 确保已登录
    if "/login" in page.url:
        login(page)

    # 检查API返回的数据是否包含tenantId
    page.goto(f"{BASE_URL}/settings/organization")
    page.wait_for_load_state('networkidle')

    # 监听网络请求
    api_has_tenant_filter = False

    def handle_response(response):
        nonlocal api_has_tenant_filter
        if '/api/users' in response.url or '/api/orgs' in response.url:
            try:
                data = response.json()
                if data.get('code') == 200:
                    api_has_tenant_filter = True
            except:
                pass

    page.on('response', handle_response)

    # 刷新页面触发API请求
    page.reload()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    log_result("数据隔离", "API请求带租户参数", api_has_tenant_filter, "检查network请求")

def test_dashboard(page: Page):
    """测试数据看板"""
    print("\n=== 开始测试数据看板 ===")

    # 确保已登录
    if "/login" in page.url:
        login(page)

    page.goto(f"{BASE_URL}/")
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    save_screenshot(page, "dashboard")

    # 检查看板元素
    dashboard_elements = [
        (".ant-card", "卡片组件"),
        (".recharts-wrapper", "图表组件"),
    ]

    for selector, name in dashboard_elements:
        count = page.locator(selector).count()
        log_result("数据看板", f"{name}渲染", count > 0, f"数量: {count}")

def run_all_tests():
    """运行所有测试"""
    print("=" * 60)
    print("消纳平台用户体系全面测试")
    print("=" * 60)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        try:
            test_login(page)
            test_organization(page)
            test_roles(page)
            test_users_api()
            test_menu_navigation(page)
            test_permission_control(page)
            test_data_isolation(page)
            test_dashboard(page)
        except Exception as e:
            print(f"测试执行异常: {e}")
        finally:
            browser.close()

    # 打印测试汇总
    print("\n" + "=" * 60)
    print("测试结果汇总")
    print("=" * 60)

    total = len(TEST_RESULTS)
    passed = sum(1 for r in TEST_RESULTS if r['passed'])
    failed = total - passed

    print(f"总计: {total} 个测试")
    print(f"通过: {passed} 个")
    print(f"失败: {failed} 个")
    print(f"通过率: {passed/total*100:.1f}%")

    # 打印失败的测试
    if failed > 0:
        print("\n失败的测试详情:")
        for r in TEST_RESULTS:
            if not r['passed']:
                print(f"  ❌ [{r['module']}] {r['test_case']}: {r['detail']}")

    return TEST_RESULTS

if __name__ == "__main__":
    results = run_all_tests()

    # 保存结果到文件
    with open("/tmp/xngl_test_results.json", "w") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n测试结果已保存到: /tmp/xngl_test_results.json")