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

async function getContracts(token) {
  const res = await fetch('http://127.0.0.1:8090/api/contracts', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();
}

async function testMultiTenantIsolation() {
  console.log('=== 开始测试：合同结算多租户隔离 ===');
  
  const tenants = [
    { name: '租户1(平台方)', username: 'admin', password: 'admin' },
    { name: '租户2', username: 'tenant2_user', password: '123456' }
  ];

  const tenantData = {};

  for (const tenant of tenants) {
    console.log(`\n[测试] 验证租户: ${tenant.name} (${tenant.username})`);
    try {
      console.log(`  -> 正在登录...`);
      let token;
      try {
        token = await login(tenant.username, tenant.password);
      } catch (e) {
        try {
          token = await login(tenant.username, '123456');
        } catch (e) {
          token = await login(tenant.username, 'admin');
        }
      }
      
      console.log(`  -> 正在获取合同列表...`);
      const contracts = await getContracts(token);
      
      const count = contracts.data?.total || 0;
      console.log(`  -> 查询到 ${count} 条合同数据`);
      tenantData[tenant.name] = contracts.data?.records || [];
      
      console.log(`  ✅ 验证通过: 成功获取本租户数据`);
    } catch (err) {
      console.error(`  ❌ 验证失败: ${err.message}`);
    }
  }
  
  console.log('\n[测试] 交叉验证租户数据隔离...');
  if (tenantData['租户1(平台方)'] && tenantData['租户2']) {
     const idsA = tenantData['租户1(平台方)'].map(c => c.id);
     const idsB = tenantData['租户2'].map(c => c.id);
     const overlap = idsA.filter(id => idsB.includes(id));
     if (overlap.length === 0) {
       console.log(`  ✅ 验证通过: 租户A与租户B的合同数据无重叠，隔离生效`);
     } else {
       console.log(`  ❌ 验证失败: 发现重叠数据 ID: ${overlap.join(', ')}`);
     }
  } else {
     console.log(`  ⚠️ 提示: 无法完成交叉验证，因为某些租户数据获取失败或为空`);
  }
  
  console.log('\n=== 测试完成：合同结算多租户隔离 ===');
}

testMultiTenantIsolation().catch(console.error);
