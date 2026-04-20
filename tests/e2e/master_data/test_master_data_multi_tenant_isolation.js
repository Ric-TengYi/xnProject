const http = require('http');

async function login(username, password) {
  const res = await fetch('http://127.0.0.1:8090/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const data = await res.json();
  if (data.code !== 200) {
    throw new Error(`Login failed for ${username}: ${data.message}`);
  }
  return data.data.accessToken;
}

async function getList(token, resource) {
  const res = await fetch(`http://127.0.0.1:8090/api/${resource}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();
}

async function getById(token, resource, id) {
  const res = await fetch(`http://127.0.0.1:8090/api/${resource}/${id}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return { status: res.status, data: await res.json().catch(() => null) };
}

async function testTenantIsolation() {
  console.log('=== 开始测试：主数据多租户隔离 ===');
  
  try {
    console.log(`\n[测试] 验证租户1管理员访问数据`);
    const token1 = await login('admin', 'admin');
    
    const projects1 = await getList(token1, 'projects');
    const sites1 = await getList(token1, 'sites');
    const vehicles1 = await getList(token1, 'vehicles');
    
    console.log(`  -> 租户1查询到 ${projects1.data?.total || 0} 条项目数据`);
    console.log(`  -> 租户1查询到 ${sites1.data?.total || 0} 条站点数据`);
    console.log(`  -> 租户1查询到 ${vehicles1.data?.total || 0} 条车辆数据`);
    
    console.log(`\n[测试] 验证租户2用户访问数据`);
    let token2;
    try {
      token2 = await login('tenant2_user', '123456');
    } catch (e) {
      try {
        token2 = await login('tenant2_user', 'admin');
      } catch (e) {
        console.log('  -> 租户2用户登录失败，跳过此测试');
      }
    }
    
    if (token2) {
      const projects2 = await getList(token2, 'projects');
      const sites2 = await getList(token2, 'sites');
      const vehicles2 = await getList(token2, 'vehicles');
      
      console.log(`  -> 租户2查询到 ${projects2.data?.total || 0} 条项目数据`);
      console.log(`  -> 租户2查询到 ${sites2.data?.total || 0} 条站点数据`);
      console.log(`  -> 租户2查询到 ${vehicles2.data?.total || 0} 条车辆数据`);
      
      console.log(`\n[测试] 越权访问测试`);
      const resources = [
        { name: 'projects', data: projects1.data?.records || [] },
        { name: 'sites', data: sites1.data?.records || [] },
        { name: 'vehicles', data: vehicles1.data?.records || [] }
      ];
      
      for (const res of resources) {
        if (res.data.length > 0) {
          const targetId = res.data[0].id;
          console.log(`  -> 租户2 尝试访问租户1的 ${res.name} (ID: ${targetId})`);
          const response = await getById(token2, res.name, targetId);
          if (response.status === 404 || response.data?.code === 404 || !response.data?.data) {
            console.log(`  ✅ 验证通过: 成功拦截越权访问`);
          } else {
            console.log(`  ❌ 验证失败: 成功获取了越权数据!`);
            console.log(`     响应:`, response);
          }
        } else {
          console.log(`  ⚠️ 提示: 租户1没有 ${res.name} 数据，跳过越权访问测试`);
        }
      }
    }

    console.log('\n=== 测试完成：主数据多租户隔离 ===');
  } catch (err) {
    console.error(`测试失败: ${err.message}`);
  }
}

testTenantIsolation().catch(console.error);
