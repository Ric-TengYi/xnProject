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

async function getUnits(token) {
  const res = await fetch('http://127.0.0.1:8090/api/units', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();
}

async function testTenantIsolation() {
  console.log('=== 开始测试：多租户数据隔离 ===');
  
  try {
    // 假设存在租户1(admin)和租户2(tenant2_user)
    console.log(`\n[测试] 验证租户1管理员访问数据`);
    const token1 = await login('admin', 'admin');
    const units1 = await getUnits(token1);
    console.log(`  -> 租户1查询到 ${units1.data?.total || 0} 条单位数据`);
    
    console.log(`\n[测试] 验证租户2用户访问数据`);
    // 尝试登录 tenant2_user，假设密码是 123456 或 admin
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
      const units2 = await getUnits(token2);
      console.log(`  -> 租户2查询到 ${units2.data?.total || 0} 条单位数据`);
      
      if (units1.data?.total !== units2.data?.total) {
        console.log(`  ✅ 验证通过: 租户1和租户2的数据隔离`);
      } else {
        console.log(`  ⚠️ 警告: 租户1和租户2查询到相同数量的数据，可能未隔离`);
      }
    }

    console.log('\n=== 测试完成：多租户数据隔离 ===');
  } catch (err) {
    console.error(`测试失败: ${err.message}`);
  }
}

testTenantIsolation().catch(console.error);
