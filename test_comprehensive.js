const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();

  console.log('=== 多租户SaaS系统完整测试 ===\n');

  try {
    // 测试1: 超级管理员登录
    console.log('【测试1】超级管理员登录');
    await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle2' });
    await page.type('input[placeholder*="账号"]', 'admin');
    await page.type('input[placeholder*="密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });

    const adminUrl = page.url();
    console.log(`✅ 登录成功，重定向到: ${adminUrl}`);

    // 检查用户菜单
    await page.waitForSelector('.g-sider-footer-user', { timeout: 5000 });
    console.log('✅ 用户菜单显示');

    // 点击用户菜单
    await page.click('.g-sider-footer-user');
    await page.waitForTimeout(500);

    const menuItems = await page.$$eval('.ant-dropdown-menu-item', items =>
      items.map(item => item.textContent)
    );
    console.log(`✅ 菜单项: ${menuItems.join(', ')}`);

    // 检查是否有用户管理选项
    if (menuItems.includes('用户管理')) {
      console.log('✅ 超级管理员可见用户管理');
    } else {
      console.log('⚠️ 超级管理员未见用户管理');
    }

    // 关闭菜单
    await page.keyboard.press('Escape');
    await page.waitForTimeout(300);

    // 测试2: 进入角色管理，检查分页
    console.log('\n【测试2】角色管理分页');
    await page.click('a[href="/roles"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });

    // 等待角色列表加载
    await page.waitForSelector('.ant-list-item', { timeout: 5000 });

    // 检查分页控件
    const paginationExists = await page.$('.ant-pagination');
    if (paginationExists) {
      console.log('✅ 分页控件存在');

      const paginationText = await page.$eval('.ant-pagination', el => el.textContent);
      console.log(`✅ 分页信息: ${paginationText}`);
    } else {
      console.log('⚠️ 分页控件未找到');
    }

    // 检查列表项数量
    const roleItems = await page.$$('.ant-list-item');
    console.log(`✅ 显示角色数: ${roleItems.length}`);

    // 测试3: 检查页面标签
    console.log('\n【测试3���页面标签导航');
    const tabsContainer = await page.$('.ant-tabs-nav-list');
    if (tabsContainer) {
      const tabs = await page.$$('.ant-tabs-tab');
      console.log(`✅ 页面标签数: ${tabs.length}`);
    } else {
      console.log('⚠️ 页面标签容器未找到');
    }

    // 测试4: 进入用户管理
    console.log('\n【测试4】用户管理');
    await page.click('.g-sider-footer-user');
    await page.waitForTimeout(300);

    const userMgmtBtn = await page.$('.ant-dropdown-menu-item:has-text("用户管理")');
    if (userMgmtBtn) {
      await userMgmtBtn.click();
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
      console.log(`✅ 进入用户管理，URL: ${page.url()}`);
    } else {
      console.log('⚠️ 用户管理按钮未找到');
    }

    // 测试5: 修改密码
    console.log('\n【测试5】修改密码功能');
    await page.goto('http://localhost:5173/roles', { waitUntil: 'networkidle2' });
    await page.click('.g-sider-footer-user');
    await page.waitForTimeout(300);

    const changePasswordBtn = await page.$('.ant-dropdown-menu-item:has-text("修改密码")');
    if (changePasswordBtn) {
      await changePasswordBtn.click();
      await page.waitForTimeout(500);

      const modalTitle = await page.$('.ant-modal-title');
      if (modalTitle) {
        const title = await page.evaluate(el => el.textContent, modalTitle);
        console.log(`✅ 修改密码弹窗打开: ${title}`);
      }

      // 关闭弹窗
      await page.keyboard.press('Escape');
    } else {
      console.log('⚠️ 修改密码按钮未找到');
    }

    // 测试6: 退出登录
    console.log('\n【测试6】退出登录');
    await page.click('.g-sider-footer-user');
    await page.waitForTimeout(300);

    const logoutBtn = await page.$('.ant-dropdown-menu-item:has-text("退出登录")');
    if (logoutBtn) {
      await logoutBtn.click();
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
      console.log(`✅ 退出成功，重定向到: ${page.url()}`);
    }

    console.log('\n=== 测试完成 ===');

  } catch (error) {
    console.error('❌ 测试失败:', error.message);
  } finally {
    await browser.close();
  }
})();
