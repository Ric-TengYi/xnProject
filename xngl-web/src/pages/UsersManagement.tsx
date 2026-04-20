import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  message,
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';
import { fetchOrgTree, type OrgTreeNode } from '../utils/orgApi';

type UserRecord = {
  id: string;
  name?: string;
  username?: string;
  mainOrgName?: string;
  roleNames?: string[];
  status?: string;
  lastLoginTime?: string;
};

type UserDetailRecord = {
  id: string;
  username?: string;
  name?: string;
  mobile?: string;
  email?: string;
  userType?: string;
  mainOrgId?: string;
  mainOrgName?: string;
  orgs?: Array<{ id: string; orgCode?: string; orgName?: string }>;
  roles?: Array<{ id: string; roleCode?: string; roleName?: string }>;
  status?: string;
  needResetPassword?: number;
  lockStatus?: number;
  lastLoginTime?: string;
};

type RoleSelectOption = { label: string; value: string };

type UserFormValues = {
  username: string;
  name: string;
  mobile?: string;
  email?: string;
  password?: string;
  userType: string;
  mainOrgId: string;
  orgIds?: string[];
  roleIds?: string[];
};

type TreeSelectNode = { title: string; value: string; children?: TreeSelectNode[] };

const buildTreeSelect = (nodes: OrgTreeNode[]): TreeSelectNode[] =>
  nodes.map((n) => ({
    title: n.orgName,
    value: String(n.id),
    children: n.children ? buildTreeSelect(n.children) : [],
  }));

const UsersManagement: React.FC = () => {
  const [searchText, setSearchText] = useState('');
  const [selectedOrgId, setSelectedOrgId] = useState<string | undefined>(undefined);
  const [usersData, setUsersData] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [orgTreeData, setOrgTreeData] = useState<TreeSelectNode[]>([]);
  const [roleOptions, setRoleOptions] = useState<RoleSelectOption[]>([]);
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [userSubmitting, setUserSubmitting] = useState(false);
  const [userDrawerOpen, setUserDrawerOpen] = useState(false);
  const [userDetailLoading, setUserDetailLoading] = useState(false);
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [selectedUserDetail, setSelectedUserDetail] = useState<UserDetailRecord | null>(null);
  const [userForm] = Form.useForm<UserFormValues>();

  const tenantId = useMemo(() => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    return userInfo.tenantId || '1';
  }, []);

  const fetchOrgs = async () => {
    try {
      const nodes = await fetchOrgTree();
      setOrgTreeData(buildTreeSelect(nodes));
    } catch {
      // ignore
    }
  };

  const fetchRoles = async () => {
    try {
      const res = await request.get('/roles', { params: { tenantId, pageNo: 1, pageSize: 100 } });
      if (res.code === 200) {
        setRoleOptions(
          (res.data.records || []).map((item: any) => ({
            value: String(item.id),
            label: `${item.roleName}${item.roleCode ? ` (${item.roleCode})` : ''}`,
          })),
        );
      }
    } catch {
      // ignore
    }
  };

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { pageNo, pageSize, tenantId };
      if (selectedOrgId) params.orgId = selectedOrgId;
      if (searchText.trim()) params.keyword = searchText.trim();
      const res = await request.get('/users', { params });
      if (res.code === 200) {
        setUsersData(res.data.records || []);
        setTotal(res.data.total || 0);
      }
    } catch {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchOrgs();
    void fetchRoles();
  }, []);

  useEffect(() => {
    void fetchUsers();
  }, [selectedOrgId, searchText, pageNo, pageSize]);

  const fetchUserDetail = async (userId: string, openDrawer = false) => {
    setUserDetailLoading(true);
    try {
      const res = await request.get(`/users/${userId}`);
      if (res.code === 200) {
        setSelectedUserDetail(res.data);
        if (openDrawer) setUserDrawerOpen(true);
        return res.data as UserDetailRecord;
      }
    } catch {
      message.error('获取用户详情失败');
    } finally {
      setUserDetailLoading(false);
    }
    return null;
  };

  const openCreateUser = () => {
    setEditingUserId(null);
    userForm.resetFields();
    userForm.setFieldsValue({
      userType: 'TENANT_USER',
      mainOrgId: selectedOrgId || undefined,
      orgIds: selectedOrgId ? [selectedOrgId] : [],
      roleIds: [],
    });
    setUserModalOpen(true);
  };

  const openEditUser = async (record: Pick<UserRecord, 'id'>) => {
    const detail = await fetchUserDetail(record.id);
    if (!detail) return;
    setEditingUserId(record.id);
    userForm.setFieldsValue({
      username: detail.username || '',
      name: detail.name || '',
      mobile: detail.mobile || '',
      email: detail.email || '',
      password: undefined,
      userType: detail.userType || 'TENANT_USER',
      mainOrgId: detail.mainOrgId || undefined,
      orgIds: detail.orgs?.map((item) => item.id) || [],
      roleIds: detail.roles?.map((item) => item.id) || [],
    });
    setUserModalOpen(true);
  };

  const handleSubmitUser = async () => {
    try {
      const values = await userForm.validateFields();
      setUserSubmitting(true);
      const mergedOrgIds = Array.from(
        new Set([values.mainOrgId, ...(values.orgIds || [])].filter(Boolean)),
      );
      const payload = {
        tenantId,
        username: values.username.trim(),
        name: values.name.trim(),
        mobile: values.mobile?.trim() || undefined,
        email: values.email?.trim() || undefined,
        password: values.password?.trim() || undefined,
        userType: values.userType,
        mainOrgId: values.mainOrgId,
        orgIds: mergedOrgIds,
        roleIds: values.roleIds || [],
      };
      const res = editingUserId
        ? await request.put(`/users/${editingUserId}`, payload)
        : await request.post('/users', payload);
      if (res.code === 200) {
        const currentEditingUserId = editingUserId;
        message.success(
          editingUserId
            ? '用户已更新'
            : values.password?.trim()
              ? '用户创建成功'
              : '用户创建成功，默认初始密码为 123456',
        );
        setUserModalOpen(false);
        setEditingUserId(null);
        userForm.resetFields();
        await fetchUsers();
        if (currentEditingUserId && selectedUserDetail?.id === currentEditingUserId) {
          await fetchUserDetail(currentEditingUserId, true);
        }
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error(editingUserId ? '更新用户失败' : '新增用户失败');
    } finally {
      setUserSubmitting(false);
    }
  };

  const handleDeleteUser = async (record: UserRecord) => {
    try {
      const res = await request.delete(`/users/${record.id}`);
      if (res.code === 200) {
        message.success('用户已删除');
        if (selectedUserDetail?.id === record.id) {
          setUserDrawerOpen(false);
          setSelectedUserDetail(null);
        }
        await fetchUsers();
      }
    } catch {
      message.error('删除用户失败');
    }
  };

  const userColumns = [
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: UserRecord) => (
        <a className="g-text-primary font-bold" onClick={() => void fetchUserDetail(record.id, true)}>
          {text}
        </a>
      ),
    },
    {
      title: '账号',
      dataIndex: 'username',
      key: 'username',
      render: (text: string) => <span className="g-text-secondary font-mono">{text}</span>,
    },
    {
      title: '所属组织',
      dataIndex: 'mainOrgName',
      key: 'mainOrgName',
      render: (text: string) => <span className="g-text-secondary">{text || '-'}</span>,
    },
    {
      title: '角色',
      dataIndex: 'roleNames',
      key: 'roleNames',
      render: (roleNames: string[]) => (
        <Space size={[0, 4]} wrap>
          {roleNames?.map((name: string, index: number) => (
            <Tag color="blue" key={index} className="border-none">{name}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ENABLED' ? 'success' : 'error'} className="border-none">
          {status === 'ENABLED' ? '正常' : '停用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: UserRecord) => (
        <Space size="middle">
          <a className="g-text-primary-link" onClick={() => void fetchUserDetail(record.id, true)}>查看</a>
          <a className="g-text-primary-link" onClick={() => void openEditUser(record)}>
            <EditOutlined /> 编辑
          </a>
          <Popconfirm title="确认删除该用户？" onConfirm={() => void handleDeleteUser(record)}>
            <a className="g-text-error"><DeleteOutlined /> 删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={openCreateUser}>
          新增用户
        </Button>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 justify-between g-bg-toolbar">
          <div className="flex gap-4 flex-wrap">
            <Input
              placeholder="搜索姓名/账号"
              prefix={<SearchOutlined className="g-text-secondary" />}
              className="w-64 bg-white g-border-panel border g-text-primary"
              value={searchText}
              onChange={(e) => { setSearchText(e.target.value); setPageNo(1); }}
            />
            <TreeSelect
              treeData={orgTreeData}
              placeholder="按组织筛选"
              allowClear
              showSearch
              treeNodeFilterProp="title"
              style={{ width: 220 }}
              value={selectedOrgId}
              onChange={(value) => { setSelectedOrgId(value); setPageNo(1); }}
            />
          </div>
        </div>
        <Table
          columns={userColumns}
          dataSource={usersData}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (p, ps) => { setPageNo(p); setPageSize(ps); },
            className: 'pr-4 pb-2',
          }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>

      {/* User Create / Edit Modal */}
      <Modal
        title={editingUserId ? '编辑用户' : '新增用户'}
        open={userModalOpen}
        onCancel={() => { setUserModalOpen(false); setEditingUserId(null); userForm.resetFields(); }}
        onOk={() => void handleSubmitUser()}
        confirmLoading={userSubmitting}
        width={720}
      >
        <Form form={userForm} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
              <Input placeholder="请输入姓名" />
            </Form.Item>
            <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
              <Input placeholder="请输入登录账号" />
            </Form.Item>
            <Form.Item name="userType" label="用户类型" rules={[{ required: true, message: '请选择用户类型' }]}>
              <Select
                options={[
                  { label: '租户管理员', value: 'TENANT_ADMIN' },
                  { label: '普通租户用户', value: 'TENANT_USER' },
                  { label: '司机', value: 'DRIVER' },
                ]}
              />
            </Form.Item>
            <Form.Item name="mainOrgId" label="主组织" rules={[{ required: true, message: '请选择主组织' }]}>
              <TreeSelect
                treeData={orgTreeData}
                showSearch
                treeNodeFilterProp="title"
                placeholder="请选择主组织"
              />
            </Form.Item>
            <Form.Item name="orgIds" label="所属组织">
              <TreeSelect
                treeData={orgTreeData}
                multiple
                showSearch
                treeNodeFilterProp="title"
                placeholder="可额外选择多个组织"
              />
            </Form.Item>
            <Form.Item name="roleIds" label="角色">
              <Select
                mode="multiple"
                showSearch
                optionFilterProp="label"
                placeholder="请选择角色"
                options={roleOptions}
              />
            </Form.Item>
            <Form.Item name="mobile" label="手机号">
              <Input placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item name="email" label="邮箱">
              <Input placeholder="请输入邮箱" />
            </Form.Item>
          </div>
          <Form.Item name="password" label={editingUserId ? '重置密码（可选）' : '初始密码'}>
            <Input.Password placeholder={editingUserId ? '不填则不修改当前密码' : '不填则默认 123456，并要求首次修改'} />
          </Form.Item>
        </Form>
      </Modal>

      {/* User Detail Drawer */}
      <Drawer
        title={selectedUserDetail ? `用户详情 · ${selectedUserDetail.name || selectedUserDetail.username}` : '用户详情'}
        open={userDrawerOpen}
        onClose={() => setUserDrawerOpen(false)}
        width={640}
      >
        {userDetailLoading && <div className="g-text-secondary">详情加载中...</div>}
        {selectedUserDetail && (
          <div className="space-y-6">
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="姓名">{selectedUserDetail.name || '-'}</Descriptions.Item>
              <Descriptions.Item label="账号">{selectedUserDetail.username || '-'}</Descriptions.Item>
              <Descriptions.Item label="用户类型">{selectedUserDetail.userType || '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedUserDetail.status === 'ENABLED' ? 'success' : 'error'} className="border-none">
                  {selectedUserDetail.status === 'ENABLED' ? '正常' : '停用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="主组织">{selectedUserDetail.mainOrgName || '-'}</Descriptions.Item>
              <Descriptions.Item label="最近登录">{selectedUserDetail.lastLoginTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="手机号">{selectedUserDetail.mobile || '-'}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{selectedUserDetail.email || '-'}</Descriptions.Item>
              <Descriptions.Item label="首次改密">{selectedUserDetail.needResetPassword === 1 ? '是' : '否'}</Descriptions.Item>
              <Descriptions.Item label="锁定状态">{selectedUserDetail.lockStatus === 1 ? '已锁定' : '正常'}</Descriptions.Item>
            </Descriptions>

            <div>
              <div className="mb-2 font-semibold g-text-primary">所属组织</div>
              <Space size={[0, 8]} wrap>
                {(selectedUserDetail.orgs || []).map((item) => (
                  <Tag key={item.id} color={item.id === selectedUserDetail.mainOrgId ? 'blue' : 'default'} className="border-none">
                    {item.orgName || item.orgCode || item.id}
                    {item.id === selectedUserDetail.mainOrgId ? '（主组织）' : ''}
                  </Tag>
                ))}
              </Space>
            </div>

            <div>
              <div className="mb-2 font-semibold g-text-primary">角色</div>
              <Space size={[0, 8]} wrap>
                {(selectedUserDetail.roles || []).map((item) => (
                  <Tag key={item.id} color="purple" className="border-none">
                    {item.roleName || item.roleCode || item.id}
                  </Tag>
                ))}
              </Space>
            </div>

            <Space>
              <Button onClick={() => selectedUserDetail && void openEditUser({ id: selectedUserDetail.id })}>编辑</Button>
              <Popconfirm
                title="确认删除该用户？"
                onConfirm={() => selectedUserDetail && void handleDeleteUser({ id: selectedUserDetail.id } as UserRecord)}
              >
                <Button danger>删除</Button>
              </Popconfirm>
            </Space>
          </div>
        )}
      </Drawer>
    </motion.div>
  );
};
export default UsersManagement;
