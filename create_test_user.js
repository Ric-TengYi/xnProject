async function main() {
  const res = await fetch('http://127.0.0.1:8090/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin' })
  });
  const data = await res.json();
  const token = data.data.accessToken;

  const createRes = await fetch('http://127.0.0.1:8090/api/users', {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      tenantId: '1',
      orgId: '1',
      username: 'test_org_user',
      password: 'password123',
      name: 'Test Org User',
      userType: 'NORMAL',
      status: 'ENABLED'
    })
  });
  console.log(await createRes.text());
}
main();
