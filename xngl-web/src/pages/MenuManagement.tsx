import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Card,
  Tree,
  Button,
  Tag,
  Select,
  message,
  Modal,
  Form,
  Input,
  InputNumber,
  Tabs,
  Space,
  Empty,
  Popconfirm,
  Table,
  TreeSelect,
  Spin,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  AppstoreOutlined,
  MobileOutlined,
  FundProjectionScreenOutlined,
  SaveOutlined,
  FolderOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import {
  fetchMenuTree,
  fetchMenuDetail,
  createMenu,
  updateMenu,
  deleteMenu,
  fetchPermissions,
  createPermission,
  deletePermission,
  type MenuTreeNode,
  type MenuDetail,
  type MenuPayload,
  type PermissionItem,
  type PermissionPayload,
} from '../utils/menuApi';

const menuTypeMap: Record<string, { color: string; label: string }> = {
  DIR: { color: 'blue', label: '目录' },
  MENU: { color: 'green', label: '菜单' },
  BUTTON: { color: 'orange', label: '按钮' },
};

const platformTabs = [
  { key: 'PC', label: 'PC端', icon: <AppstoreOutlined /> },
  { key: 'MINI', label: '小程序', icon: <MobileOutlined /> },
  { key: 'SCREEN', label: '大屏', icon: <FundProjectionScreenOutlined /> },
];

/** Recursively build Ant Design tree nodes from MenuTreeNode[] */
function buildTreeData(nodes: MenuTreeNode[]): DataNode[] {
  return nodes.map((node) => {
    const typeInfo = menuTypeMap[node.menuType] || { color: 'default', label: node.menuType };
    return {
      key: node.id,
      title: (
        <span className="inline-flex items-center gap-1">
          <span>{node.menuName}</span>
          <Tag color={typeInfo.color} className="border-none text-xs ml-1 leading-tight" style={{ fontSize: 10, padding: '0 4px', lineHeight: '16px' }}>
            {typeInfo.label}
          </Tag>
        </span>
      ),
      icon: node.menuType === 'DIR' ? <FolderOutlined /> : <MenuOutlined />,
      children: node.children && node.children.length > 0 ? buildTreeData(node.children) : undefined,
    };
  });
}

/** Build TreeSelect data (for parent menu selector) */
function buildTreeSelectData(nodes: MenuTreeNode[]): any[] {
  return nodes
    .filter((n) => n.menuType === 'DIR' || n.menuType === 'MENU')
    .map((node) => ({
      value: node.id,
      title: node.menuName,
      children: node.children && node.children.length > 0 ? buildTreeSelectData(node.children) : undefined,
    }));
}

/** Flatten tree to map for quick lookup */
function flattenTree(nodes: MenuTreeNode[]): Map<string, MenuTreeNode> {
  const map = new Map<string, MenuTreeNode>();
  const walk = (list: MenuTreeNode[]) => {
    for (const n of list) {
      map.set(n.id, n);
      if (n.children) walk(n.children);
    }
  };
  walk(nodes);
  return map;
}

/** Filter tree nodes by keyword (match menuName) */
function filterTree(nodes: MenuTreeNode[], keyword: string): MenuTreeNode[] {
  if (!keyword) return nodes;
  const lower = keyword.toLowerCase();
  const filter = (list: MenuTreeNode[]): MenuTreeNode[] => {
    const result: MenuTreeNode[] = [];
    for (const node of list) {
      const childrenFiltered = node.children ? filter(node.children) : [];
      if (node.menuName.toLowerCase().includes(lower) || childrenFiltered.length > 0) {
        result.push({ ...node, children: childrenFiltered.length > 0 ? childrenFiltered : node.children && node.children.length > 0 ? [] : undefined } as MenuTreeNode);
      }
    }
    return result;
  };
  return filter(nodes);
}

const MenuManagement: React.FC = () => {
  const [platform, setPlatform] = useState<string>('PC');
  const [treeRaw, setTreeRaw] = useState<MenuTreeNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedMenuId, setSelectedMenuId] = useState<string | null>(null);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [menuDetail, setMenuDetail] = useState<MenuDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissions, setPermissions] = useState<PermissionItem[]>([]);
  const [permLoading, setPermLoading] = useState(false);

  // Create menu modal
  const [createMenuOpen, setCreateMenuOpen] = useState(false);
  const [createMenuLoading, setCreateMenuLoading] = useState(false);
  const [menuForm] = Form.useForm<MenuPayload>();

  // Edit menu form
  const [editForm] = Form.useForm();
  const [editSaving, setEditSaving] = useState(false);

  // Create permission modal
  const [createPermOpen, setCreatePermOpen] = useState(false);
  const [createPermLoading, setCreatePermLoading] = useState(false);
  const [permForm] = Form.useForm<PermissionPayload>();

  // ── Data loading ────────────────────────────────────

  const loadTree = useCallback(async (plat: string) => {
    setTreeLoading(true);
    try {
      const data = await fetchMenuTree(plat);
      setTreeRaw(data);
    } catch {
      message.error('加载菜单树失败');
    } finally {
      setTreeLoading(false);
    }
  }, []);

  const loadDetail = useCallback(async (menuId: string) => {
    setDetailLoading(true);
    try {
      const detail = await fetchMenuDetail(menuId);
      setMenuDetail(detail);
      editForm.setFieldsValue({
        menuName: detail.menuName,
        menuCode: detail.menuCode,
        menuType: detail.menuType,
        platform: detail.platform,
        path: detail.path,
        component: detail.component,
        icon: detail.icon,
        sortOrder: detail.sortOrder,
        visible: detail.visible ?? 'Y',
        status: detail.status ?? 'ENABLED',
      });
    } catch {
      message.error('加载菜单详情失败');
    } finally {
      setDetailLoading(false);
    }
  }, [editForm]);

  const loadPermissions = useCallback(async (menuId: string) => {
    setPermLoading(true);
    try {
      const list = await fetchPermissions(menuId);
      setPermissions(list);
    } catch {
      message.error('加载权限列表失败');
    } finally {
      setPermLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTree(platform);
    setSelectedMenuId(null);
    setMenuDetail(null);
    setPermissions([]);
    editForm.resetFields();
  }, [platform, loadTree, editForm]);

  useEffect(() => {
    if (selectedMenuId) {
      void loadDetail(selectedMenuId);
      void loadPermissions(selectedMenuId);
    }
  }, [selectedMenuId, loadDetail, loadPermissions]);

  // ── Tree data ───────────────────────────────────────

  const filteredTree = useMemo(() => filterTree(treeRaw, searchKeyword), [treeRaw, searchKeyword]);
  const treeData = useMemo(() => buildTreeData(filteredTree), [filteredTree]);
  const treeSelectData = useMemo(() => buildTreeSelectData(treeRaw), [treeRaw]);
  const nodeMap = useMemo(() => flattenTree(treeRaw), [treeRaw]);

  // Collect all keys and auto-expand on initial load
  const allKeys = useMemo(() => {
    const keys: string[] = [];
    const walk = (nodes: MenuTreeNode[]) => {
      for (const n of nodes) {
        keys.push(n.id);
        if (n.children) walk(n.children);
      }
    };
    walk(filteredTree);
    return keys;
  }, [filteredTree]);

  // Auto-expand all when tree data changes (platform switch / search)
  useEffect(() => {
    setExpandedKeys(allKeys);
  }, [allKeys]);

  // ── Handlers ────────────────────────────────────────

  const handleTreeSelect = (keys: React.Key[]) => {
    if (keys.length > 0) {
      setSelectedMenuId(keys[0] as string);
    }
  };

  const handleSaveDetail = async () => {
    if (!menuDetail) return;
    try {
      const values = await editForm.validateFields();
      setEditSaving(true);
      await updateMenu(menuDetail.id, {
        menuName: values.menuName,
        menuCode: values.menuCode,
        menuType: values.menuType,
        platform: menuDetail.platform,
        path: values.path,
        component: values.component,
        icon: values.icon,
        sortOrder: values.sortOrder,
        visible: values.visible,
      });
      message.success('菜单已更新');
      void loadTree(platform);
      void loadDetail(menuDetail.id);
    } catch (err: any) {
      if (err?.errorFields) return; // form validation error
      message.error('保存失败');
    } finally {
      setEditSaving(false);
    }
  };

  const handleDeleteMenu = async () => {
    if (!menuDetail) return;
    try {
      await deleteMenu(menuDetail.id);
      message.success('菜单已删除');
      setSelectedMenuId(null);
      setMenuDetail(null);
      setPermissions([]);
      editForm.resetFields();
      void loadTree(platform);
    } catch {
      message.error('删除失败');
    }
  };

  const openCreateMenuModal = () => {
    menuForm.resetFields();
    menuForm.setFieldsValue({
      menuType: 'MENU',
      platform,
      sortOrder: 0,
      visible: 'Y',
      parentId: selectedMenuId || undefined,
    });
    setCreateMenuOpen(true);
  };

  const handleCreateMenu = async () => {
    try {
      const values = await menuForm.validateFields();
      setCreateMenuLoading(true);
      const newId = await createMenu({
        ...values,
        platform,
      });
      message.success('菜单创建成功');
      setCreateMenuOpen(false);
      menuForm.resetFields();
      await loadTree(platform);
      if (newId) {
        setSelectedMenuId(newId);
      }
    } catch (err: any) {
      if (err?.errorFields) return;
      message.error('创建失败');
    } finally {
      setCreateMenuLoading(false);
    }
  };

  const openCreatePermModal = () => {
    permForm.resetFields();
    permForm.setFieldsValue({
      menuId: menuDetail?.id,
      resourceType: 'BUTTON',
    });
    setCreatePermOpen(true);
  };

  const handleCreatePermission = async () => {
    if (!menuDetail) return;
    try {
      const values = await permForm.validateFields();
      setCreatePermLoading(true);
      await createPermission({
        ...values,
        menuId: menuDetail.id,
      });
      message.success('权限创建成功');
      setCreatePermOpen(false);
      permForm.resetFields();
      void loadPermissions(menuDetail.id);
    } catch (err: any) {
      if (err?.errorFields) return;
      message.error('创建失败');
    } finally {
      setCreatePermLoading(false);
    }
  };

  const handleDeletePermission = async (permId: string) => {
    try {
      await deletePermission(permId);
      message.success('权限已删除');
      if (menuDetail) void loadPermissions(menuDetail.id);
    } catch {
      message.error('删除失败');
    }
  };

  // ── Permission columns ──────────────────────────────

  const permColumns: ColumnsType<PermissionItem> = [
    {
      title: '权限编码',
      dataIndex: 'permissionCode',
      key: 'permissionCode',
      width: 200,
      render: (text: string) => <span className="font-mono text-xs">{text}</span>,
    },
    {
      title: '权限名称',
      dataIndex: 'permissionName',
      key: 'permissionName',
      width: 200,
    },
    {
      title: '资源类型',
      dataIndex: 'resourceType',
      key: 'resourceType',
      width: 120,
      render: (val: string) => (
        <Tag color={val === 'API' ? 'purple' : 'cyan'} className="border-none">
          {val === 'API' ? 'API接口' : '按钮操作'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (val: string) => (
        <Tag color={val === 'ENABLED' ? 'success' : 'default'} className="border-none">
          {val === 'ENABLED' ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: any, record: PermissionItem) => (
        <Popconfirm title="确认删除该权限？" onConfirm={() => void handleDeletePermission(record.id)}>
          <Button type="text" danger size="small" icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ];

  // ── Render ──────────────────────────────────────────

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6 h-[calc(100vh-110px)] flex flex-col"
    >
      <div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="g-btn-primary border-none"
          onClick={openCreateMenuModal}
        >
          新增菜单
        </Button>
      </div>
      {/* Platform Tabs */}
      <Tabs
        activeKey={platform}
        onChange={(key) => setPlatform(key)}
        items={platformTabs.map((t) => ({
          key: t.key,
          label: (
            <span className="inline-flex items-center gap-1">
              {t.icon}
              {t.label}
            </span>
          ),
        }))}
        className="g-text-primary"
        style={{ marginBottom: 0 }}
      />

      {/* Main content: left tree + right detail */}
      <div className="flex gap-6 flex-1 min-h-0">
        {/* Left: Menu tree */}
        <Card
          className="glass-panel g-border-panel border w-80 flex flex-col"
          bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}
        >
          <div className="p-3 border-b g-border-panel border g-bg-toolbar">
            <Input
              prefix={<SearchOutlined className="g-text-secondary" />}
              placeholder="搜索菜单名称..."
              allowClear
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="bg-transparent"
            />
          </div>
          <div className="flex-1 overflow-auto p-2">
            {treeLoading ? (
              <div className="flex items-center justify-center h-32">
                <Spin />
              </div>
            ) : treeData.length > 0 ? (
              <Tree
                showIcon
                expandedKeys={expandedKeys}
                onExpand={(keys) => setExpandedKeys(keys)}
                selectedKeys={selectedMenuId ? [selectedMenuId] : []}
                onSelect={handleTreeSelect}
                treeData={treeData}
                className="bg-transparent g-text-secondary custom-tree"
                blockNode
              />
            ) : (
              <Empty description="暂无菜单数据" className="mt-12" />
            )}
          </div>
          <div className="p-3 border-t g-border-panel border g-bg-toolbar text-center">
            <span className="text-xs g-text-secondary">
              共 {nodeMap.size} 个菜单项
            </span>
          </div>
        </Card>

        {/* Right: Detail + Permissions */}
        <div className="flex-1 flex flex-col gap-6 min-h-0">
          {selectedMenuId && menuDetail ? (
            <>
              {/* Menu detail card */}
              <Card
                className="glass-panel g-border-panel border flex-shrink-0"
                bodyStyle={{ padding: '20px 24px' }}
                title={
                  <span className="g-text-primary inline-flex items-center gap-2">
                    <EditOutlined />
                    菜单详情
                    {menuDetail.menuType && (
                      <Tag color={menuTypeMap[menuDetail.menuType]?.color || 'default'} className="border-none ml-1">
                        {menuTypeMap[menuDetail.menuType]?.label || menuDetail.menuType}
                      </Tag>
                    )}
                  </span>
                }
                extra={
                  <Space>
                    <Popconfirm title="确认删除该菜单？删除后子菜单将一并移除。" onConfirm={() => void handleDeleteMenu()}>
                      <Button danger icon={<DeleteOutlined />}>
                        删除
                      </Button>
                    </Popconfirm>
                    <Button
                      type="primary"
                      icon={<SaveOutlined />}
                      loading={editSaving}
                      onClick={() => void handleSaveDetail()}
                      className="bg-green-600 hover:bg-green-500 border-none"
                    >
                      保存
                    </Button>
                  </Space>
                }
                loading={detailLoading}
              >
                <Form form={editForm} layout="vertical" className="grid grid-cols-3 gap-x-6 gap-y-2">
                  <Form.Item
                    name="menuName"
                    label={<span className="g-text-secondary">菜单名称</span>}
                    rules={[{ required: true, message: '请输入菜单名称' }]}
                  >
                    <Input placeholder="请输入菜单名称" />
                  </Form.Item>

                  <Form.Item
                    name="menuCode"
                    label={<span className="g-text-secondary">菜单编码</span>}
                    rules={[{ required: true, message: '请输入菜单编码' }]}
                  >
                    <Input placeholder="请输入菜单编码" className="font-mono" />
                  </Form.Item>

                  <Form.Item
                    name="menuType"
                    label={<span className="g-text-secondary">菜单类型</span>}
                  >
                    <Select
                      options={[
                        { label: '目录', value: 'DIR' },
                        { label: '菜单', value: 'MENU' },
                        { label: '按钮', value: 'BUTTON' },
                      ]}
                    />
                  </Form.Item>

                  <Form.Item
                    name="platform"
                    label={<span className="g-text-secondary">所属平台</span>}
                  >
                    <Select
                      disabled
                      options={[
                        { label: 'PC端', value: 'PC' },
                        { label: '小程序', value: 'MINI' },
                        { label: '大屏', value: 'SCREEN' },
                      ]}
                    />
                  </Form.Item>

                  <Form.Item
                    name="path"
                    label={<span className="g-text-secondary">路由路径</span>}
                  >
                    <Input placeholder="如 /system/menus" className="font-mono" />
                  </Form.Item>

                  <Form.Item
                    name="component"
                    label={<span className="g-text-secondary">组件路径</span>}
                  >
                    <Input placeholder="如 pages/MenuManagement" className="font-mono" />
                  </Form.Item>

                  <Form.Item
                    name="icon"
                    label={<span className="g-text-secondary">图标</span>}
                  >
                    <Input placeholder="如 AppstoreOutlined" />
                  </Form.Item>

                  <Form.Item
                    name="sortOrder"
                    label={<span className="g-text-secondary">排序号</span>}
                  >
                    <InputNumber min={0} max={9999} className="w-full" />
                  </Form.Item>

                  <Form.Item
                    name="visible"
                    label={<span className="g-text-secondary">是否可见</span>}
                  >
                    <Select
                      options={[
                        { label: '可见', value: 'Y' },
                        { label: '隐藏', value: 'N' },
                      ]}
                    />
                  </Form.Item>

                  <Form.Item
                    name="status"
                    label={<span className="g-text-secondary">状态</span>}
                  >
                    <Select
                      options={[
                        { label: '启用', value: 'ENABLED' },
                        { label: '停用', value: 'DISABLED' },
                      ]}
                    />
                  </Form.Item>
                </Form>
              </Card>

              {/* Permissions card */}
              <Card
                className="glass-panel g-border-panel border flex-1 flex flex-col"
                bodyStyle={{ padding: '12px 24px', flex: 1, overflow: 'auto' }}
                title={
                  <span className="g-text-primary inline-flex items-center gap-2">
                    操作权限
                    <Tag className="border-none ml-1">{permissions.length}</Tag>
                  </span>
                }
                extra={
                  <Button
                    type="primary"
                    size="small"
                    icon={<PlusOutlined />}
                    className="g-btn-primary border-none"
                    onClick={openCreatePermModal}
                  >
                    新增权限
                  </Button>
                }
              >
                <Table<PermissionItem>
                  dataSource={permissions}
                  columns={permColumns}
                  rowKey="id"
                  size="small"
                  pagination={false}
                  loading={permLoading}
                  locale={{ emptyText: <Empty description="暂无操作权限" /> }}
                  className="g-text-secondary"
                />
              </Card>
            </>
          ) : (
            <Card className="glass-panel g-border-panel border flex-1 flex items-center justify-center">
              <Empty description="请从左侧菜单树中选择一个菜单节点" />
            </Card>
          )}
        </div>

      {/* Create Menu Modal */}
      <Modal
        title="新增菜单"
        open={createMenuOpen}
        onCancel={() => {
          setCreateMenuOpen(false);
          menuForm.resetFields();
        }}
        onOk={() => void handleCreateMenu()}
        confirmLoading={createMenuLoading}
        width={600}
        destroyOnClose
      >
        <Form form={menuForm} layout="vertical" className="mt-4">
          <div className="grid grid-cols-2 gap-x-4">
            <Form.Item
              name="menuName"
              label="菜单名称"
              rules={[{ required: true, message: '请输入菜单名称' }]}
            >
              <Input placeholder="请输入菜单名称" />
            </Form.Item>

            <Form.Item
              name="menuCode"
              label="菜单编码"
              rules={[{ required: true, message: '请输入菜单编码' }]}
            >
              <Input placeholder="请输入唯一菜单编码" className="font-mono" />
            </Form.Item>

            <Form.Item
              name="menuType"
              label="菜单类型"
              rules={[{ required: true, message: '请选择菜单类型' }]}
            >
              <Select
                options={[
                  { label: '目录', value: 'DIR' },
                  { label: '菜单', value: 'MENU' },
                  { label: '按钮', value: 'BUTTON' },
                ]}
              />
            </Form.Item>

            <Form.Item name="platform" label="所属平台">
              <Select
                disabled
                options={[
                  { label: 'PC端', value: 'PC' },
                  { label: '小程序', value: 'MINI' },
                  { label: '大屏', value: 'SCREEN' },
                ]}
              />
            </Form.Item>

            <Form.Item name="parentId" label="上级菜单" className="col-span-2">
              <TreeSelect
                treeData={treeSelectData}
                placeholder="无（作为顶级菜单）"
                allowClear
                treeDefaultExpandAll
                className="w-full"
              />
            </Form.Item>

            <Form.Item name="path" label="路由路径">
              <Input placeholder="如 /system/menus" className="font-mono" />
            </Form.Item>

            <Form.Item name="component" label="组件路径">
              <Input placeholder="如 pages/MenuManagement" className="font-mono" />
            </Form.Item>

            <Form.Item name="icon" label="图标">
              <Input placeholder="如 AppstoreOutlined" />
            </Form.Item>

            <Form.Item name="sortOrder" label="排序号">
              <InputNumber min={0} max={9999} className="w-full" />
            </Form.Item>

            <Form.Item name="visible" label="是否可见" initialValue="Y">
              <Select
                options={[
                  { label: '可见', value: 'Y' },
                  { label: '隐藏', value: 'N' },
                ]}
              />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      {/* Create Permission Modal */}
      <Modal
        title="新增权限"
        open={createPermOpen}
        onCancel={() => {
          setCreatePermOpen(false);
          permForm.resetFields();
        }}
        onOk={() => void handleCreatePermission()}
        confirmLoading={createPermLoading}
        width={480}
        destroyOnClose
      >
        <Form form={permForm} layout="vertical" className="mt-4">
          <Form.Item
            name="permissionName"
            label="权限名称"
            rules={[{ required: true, message: '请输入权限名称' }]}
          >
            <Input placeholder="如：新增合同" />
          </Form.Item>

          <Form.Item
            name="permissionCode"
            label="权限编码"
            rules={[{ required: true, message: '请输入权限编码' }]}
          >
            <Input placeholder="如：contract:create" className="font-mono" />
          </Form.Item>

          <Form.Item
            name="resourceType"
            label="资源类型"
            rules={[{ required: true, message: '请选择资源类型' }]}
          >
            <Select
              options={[
                { label: '按钮操作', value: 'BUTTON' },
                { label: 'API接口', value: 'API' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </motion.div>
  );    </div>
  );
};

export default MenuManagement;
