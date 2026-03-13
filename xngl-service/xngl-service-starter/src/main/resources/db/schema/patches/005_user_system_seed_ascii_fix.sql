-- Normalize bootstrap demo data to ASCII so CLI-based init keeps values stable across locales.

UPDATE sys_tenant
SET tenant_name = 'Demo Tenant',
    contact_name = 'System Admin',
    remark = 'Minimal bootstrap data for user system'
WHERE tenant_code = 'demo';

UPDATE sys_org
SET name = 'Root Org',
    org_name = 'Root Org'
WHERE org_code = 'ROOT';

UPDATE sys_role
SET name = 'Tenant Admin',
    role_name = 'Tenant Admin',
    description = 'Bootstrap admin role for demo tenant'
WHERE role_code = 'TENANT_ADMIN';

UPDATE sys_menu
SET menu_name = 'Dashboard'
WHERE menu_code = 'dashboard';

UPDATE sys_permission
SET permission_name = 'View current user info'
WHERE permission_code = 'api:me:read';

UPDATE sys_permission
SET permission_name = 'View user list'
WHERE permission_code = 'user:list';

UPDATE sys_user
SET name = 'Demo Admin'
WHERE username = 'demo';
