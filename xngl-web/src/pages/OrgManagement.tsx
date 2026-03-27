import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Tree,
  TreeSelect,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ApartmentOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { fetchDataDicts } from '../utils/dataDictApi';
import {
  createOrg,
  deleteOrg,
  fetchOrgContractGroups,
  fetchOrgDetail,
  fetchOrgProjects,
  fetchOrgTree,
  fetchOrgUsers,
  updateOrg,
  updateOrgStatus,
  type OrgContractItem,
  type OrgCreatePayload,
  type OrgCreateResponse,
  type OrgDetail,
  type OrgProjectStat,
  type OrgSiteContractGroup,
  type OrgTreeNode,
  type OrgUserRecord,
} from '../utils/orgApi';

type DictItem = { dictCode: string; dictLabel: string };

type AntTreeNode = {
  title: React.ReactNode;
  key: string;
  icon?: React.ReactNode;
  children?: AntTreeNode[];
};

type TreeSelectNode = {
  title: string;
  value: string;
  children?: TreeSelectNode[];
};

const statusColorMap: Record<string, string> = {
  ENABLED: 'success',
  DISABLED: 'error',
};

const formatMoney = (value?: number | null) => '¥ ' + Number(value || 0).toLocaleString();

const OrgManagement: React.FC = () => {
  // ── Tree node full-width style ──
  const treeStyle = `
    .custom-tree .ant-tree-node-content-wrapper {
      flex: 1;
      min-width: 0;
      overflow: hidden;
    }
    .custom-tree .ant-tree-title {
      display: block !important;
      width: 100%;
    }
  `;
  // ── State ──────────────────────────────
  const [treeData, setTreeData] = useState<OrgTreeNode[]>([]);
  const [antTreeData, setAntTreeData] = useState<AntTreeNode[]>([]);
  const [treeSelectData, setTreeSelectData] = useState<TreeSelectNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
  const [detail, setDetail] = useState<OrgDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [projects, setProjects] = useState<OrgProjectStat[]>([]);
  const [contractGroups, setContractGroups] = useState<OrgSiteContractGroup[]>([]);
  const [contractGroupLoading, setContractGroupLoading] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [users, setUsers] = useState<OrgUserRecord[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [orgTypes, setOrgTypes] = useState<DictItem[]>([]);
  const [searchText, setSearchText] = useState('');

  // Create / Edit Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOrgId, setEditingOrgId] = useState<string | null>(null);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [form] = Form.useForm<OrgCreatePayload>();

  // Admin credentials modal (shown after creation)
  const [adminResult, setAdminResult] = useState<OrgCreateResponse | null>(null);

  // ── Derived ────────────────────────────
  const tenantId = useMemo(() => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    return userInfo.tenantId || '1';
  }, []);

  // ── Data loading ───────────────────────
  const loadOrgTypes = async () => {
    try {
      const items = await fetchDataDicts({ dictType: 'ORG_TYPE' });
      setOrgTypes(
        items
          .filter((d) => d.status === 'ENABLED')
          .map((d) => ({ dictCode: d.dictCode, dictLabel: d.dictLabel })),
      );
    } catch {
      // ignore
    }
  };

  const typeColorMap: Record<string, string> = useMemo(() => {
    const palette = ['blue', 'gold', 'green', 'purple', 'cyan', 'magenta', 'orange'];
    const map: Record<string, string> = {};
    orgTypes.forEach((t, i) => {
      map[t.dictCode] = palette[i % palette.length];
    });
    return map;
  }, [orgTypes]);

  const buildAntTree = (nodes: OrgTreeNode[], colorMap: Record<string, string>): AntTreeNode[] =>
    nodes.map((n) => ({
      title: (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', minWidth: 0 }}>
          <Tooltip title={n.orgName}>
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1, minWidth: 0 }}>
              {n.orgName}
            </span>
          </Tooltip>
          {n.orgTypeLabel && (
            <Tag color={colorMap[n.orgType || ''] || 'default'} style={{ fontSize: 11, marginLeft: 4, flexShrink: 0, border: 'none' }}>
              {n.orgTypeLabel}
            </Tag>
          )}
        </div>
      ),
      key: String(n.id),
      children: n.children ? buildAntTree(n.children, colorMap) : [],
    }));

  const buildTreeSelect = (nodes: OrgTreeNode[]): TreeSelectNode[] =>
    nodes.map((n) => ({
      title: n.orgName,
      value: String(n.id),
      children: n.children ? buildTreeSelect(n.children) : [],
    }));

  const loadTree = async () => {
    setTreeLoading(true);
    try {
      const nodes = await fetchOrgTree();
      setTreeData(nodes);
      setTreeSelectData(buildTreeSelect(nodes));
    } catch {
      message.error('获取组织树失败');
    } finally {
      setTreeLoading(false);
    }
  };

  // Rebuild antTreeData whenever treeData or typeColorMap changes
  useEffect(() => {
    setAntTreeData(buildAntTree(treeData, typeColorMap));
  }, [treeData, typeColorMap]);

  const loadDetail = async (orgId: string) => {
    setDetailLoading(true);
    try {
      const [d, p, u] = await Promise.all([
        fetchOrgDetail(orgId),
        fetchOrgProjects(orgId),
        fetchOrgUsers(orgId),
      ]);
      setDetail(d);
      setProjects(p);
      setUsers(u);
      if (p.length > 0) {
        const pid = p[0].projectId;
        setSelectedProjectId(pid);
        setContractGroupLoading(true);
        try {
          setContractGroups(await fetchOrgContractGroups(orgId, pid));
        } finally {
          setContractGroupLoading(false);
        }
      } else {
        setSelectedProjectId('');
        setContractGroups([]);
      }
    } catch {
      message.error('获取组织详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleSelectProject = async (projectId: string) => {
    if (!selectedOrgId) return;
    setSelectedProjectId(projectId);
    setContractGroupLoading(true);
    try {
      setContractGroups(await fetchOrgContractGroups(selectedOrgId, projectId));
    } catch {
      message.error('获取合同明细失败');
      setContractGroups([]);
    } finally {
      setContractGroupLoading(false);
    }
  };

  useEffect(() => {
    void loadTree();
    void loadOrgTypes();
  }, []);

  useEffect(() => {
    if (selectedOrgId) {
      void loadDetail(selectedOrgId);
    } else {
      setDetail(null);
      setProjects([]);
      setContractGroups([]);
      setUsers([]);
    }
  }, [selectedOrgId]);

  // ── Create / Edit ──────────────────────
  const openCreate = () => {
    setEditingOrgId(null);
    form.resetFields();
    form.setFieldsValue({
      parentId: selectedOrgId || undefined,
      status: 'ENABLED',
    });
    setModalOpen(true);
  };

  const openEdit = async () => {
    if (!detail) return;
    setEditingOrgId(detail.id);
    form.setFieldsValue({
      orgName: detail.orgName,
      orgCode: detail.orgCode || undefined,
      orgType: detail.orgType || undefined,
      parentId: detail.parentId || undefined,
      contactPerson: detail.contactPerson || undefined,
      contactPhone: detail.contactPhone || undefined,
      address: detail.address || undefined,
      unifiedSocialCode: detail.unifiedSocialCode || undefined,
      remark: detail.remark || undefined,
      sortOrder: detail.sortOrder ?? undefined,
      status: detail.status || 'ENABLED',
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);
      if (editingOrgId) {
        await updateOrg(editingOrgId, values);
        message.success('组织已更新');
        setModalOpen(false);
        await loadTree();
        if (selectedOrgId === editingOrgId) {
          await loadDetail(editingOrgId);
        }
      } else {
        const payload: OrgCreatePayload = { ...values, tenantId };
        const result = await createOrg(payload);
        message.success('组织已创建');
        setModalOpen(false);
        setAdminResult(result);
        await loadTree();
      }
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) return;
      console.error(error);
      message.error('保存组织失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedOrgId) return;
    try {
      await deleteOrg(selectedOrgId);
      message.success('组织已删除');
      setSelectedOrgId(null);
      await loadTree();
    } catch {
      message.error('删除组织失败');
    }
  };

  const handleToggleStatus = async () => {
    if (!detail) return;
    const newStatus = detail.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    try {
      await updateOrgStatus(detail.id, newStatus);
      message.success('状态已更新');
      await loadDetail(detail.id);
      await loadTree();
    } catch {
      message.error('更新状态失败');
    }
  };

  // ── Filtered tree ──────────────────────
  const filteredTreeData = useMemo(() => {
    if (!searchText.trim()) return antTreeData;
    const lower = searchText.toLowerCase();
    const filterNodes = (nodes: AntTreeNode[]): AntTreeNode[] =>
      nodes
        .map((node) => {
          const raw = treeData.find((n) => String(n.id) === node.key) ||
            (function findInTree(list: OrgTreeNode[]): OrgTreeNode | undefined {
              for (const n of list) {
                if (String(n.id) === node.key) return n;
                if (n.children) {
                  const found = findInTree(n.children);
                  if (found) return found;
                }
              }
              return undefined;
            })(treeData);
          const nameMatch = raw?.orgName?.toLowerCase().includes(lower);
          const filteredChildren = node.children ? filterNodes(node.children) : [];
          if (nameMatch || filteredChildren.length > 0) {
            return { ...node, children: filteredChildren };
          }
          return null;
        })
        .filter(Boolean) as AntTreeNode[];
    return filterNodes(antTreeData);
  }, [antTreeData, searchText, treeData]);

  // ── Table columns ──────────────────────
  const projectColumns: ColumnsType<OrgProjectStat> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (value: string, record: OrgProjectStat) => (
        <a style={{ color: 'var(--primary)' }} onClick={() => void handleSelectProject(record.projectId)}>
          {value}
        </a>
      ),
    },
    { title: '项目编码', dataIndex: 'projectCode', key: 'projectCode', render: (v: string) => v || '-' },
    { title: '合同数', dataIndex: 'contractCount', key: 'contractCount' },
    { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', render: (v: number) => formatMoney(v) },
    { title: '约定方量', dataIndex: 'agreedVolume', key: 'agreedVolume', render: (v: number) => Number(v || 0).toLocaleString() },
  ];

  const contractColumns: ColumnsType<OrgContractItem> = [
    { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', render: (v: string) => v || '-' },
    { title: '合同名称', dataIndex: 'name', key: 'name', render: (v: string) => v || '-' },
    { title: '合同类型', dataIndex: 'contractType', key: 'contractType', render: (v: string) => v || '-' },
    { title: '状态', dataIndex: 'contractStatus', key: 'contractStatus', render: (v: string) => v || '-' },
    { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', render: (v: number) => formatMoney(v) },
    { title: '已入账', dataIndex: 'receivedAmount', key: 'receivedAmount', render: (v: number) => formatMoney(v) },
    { title: '约定方量', dataIndex: 'agreedVolume', key: 'agreedVolume', render: (v: number) => Number(v || 0).toLocaleString() },
  ];

  const userColumns: ColumnsType<OrgUserRecord> = [
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <span className="g-text-primary font-bold">{text || '-'}</span>,
    },
    {
      title: '账号',
      dataIndex: 'username',
      key: 'username',
      render: (text: string) => <span className="g-text-secondary font-mono">{text}</span>,
    },
    {
      title: '角色',
      dataIndex: 'roleNames',
      key: 'roleNames',
      render: (roleNames: string[]) => (
        <Space size={[0, 4]} wrap>
          {roleNames?.map((name: string, i: number) => (
            <Tag color="blue" key={i} className="border-none">{name}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColorMap[status] || 'default'} className="border-none">
          {status === 'ENABLED' ? '正常' : '停用'}
        </Tag>
      ),
    },
  ];

  // ── Render ─────────────────────────────
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <style>{treeStyle}</style>

      <div className="flex gap-6" style={{ minHeight: 700 }}>
        {/* ── Left: Org Tree ── */}
        <Card
          className="glass-panel g-border-panel border flex flex-col"
          style={{ width: 320, flexShrink: 0 }}
          bodyStyle={{ padding: '16px', flex: 1, overflow: 'auto' }}
        >
          <div className="flex justify-between items-center mb-3">
            <span className="g-text-primary font-bold">
              <ApartmentOutlined className="mr-1" /> 组织架构
            </span>
            <Button type="link" icon={<PlusOutlined />} size="small" onClick={openCreate}>
              新增
            </Button>
          </div>
          <Input
            placeholder="搜索组织..."
            prefix={<SearchOutlined className="g-text-secondary" />}
            className="bg-white g-border-panel border g-text-primary mb-3"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
          />
          <Tree
            defaultExpandAll
            treeData={filteredTreeData}
            selectedKeys={selectedOrgId ? [selectedOrgId] : []}
            onSelect={(keys) => setSelectedOrgId((keys[0] as string) || null)}
            className="bg-transparent g-text-secondary custom-tree"
            showLine
          />
        </Card>

        {/* ── Right: Detail Panel ── */}
        <div className="flex-1 space-y-4 overflow-auto">
          {!selectedOrgId ? (
            <Card className="glass-panel g-border-panel border" style={{ minHeight: 400 }}>
              <Empty description="请在左侧选择一个组织查看详情" />
            </Card>
          ) : detailLoading && !detail ? (
            <Card className="glass-panel g-border-panel border" loading />
          ) : detail ? (
            <>
              {/* Basic info */}
              <Card
                className="glass-panel g-border-panel border"
                title="基本信息"
                extra={
                  <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => void openEdit()}>编辑</Button>
                    <Button size="small" onClick={handleToggleStatus}>
                      {detail.status === 'ENABLED' ? '停用' : '启用'}
                    </Button>
                    <Popconfirm title="确认删除该组织？删除后不可恢复" onConfirm={() => void handleDelete()}>
                      <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
                    </Popconfirm>
                  </Space>
                }
              >
                <Descriptions bordered column={2} size="small">
                  <Descriptions.Item label="组织名称">{detail.orgName}</Descriptions.Item>
                  <Descriptions.Item label="组织编码">{detail.orgCode || '-'}</Descriptions.Item>
                  <Descriptions.Item label="组织类型">
                    <Tag color={typeColorMap[detail.orgType || ''] || 'default'} className="border-none">
                      {detail.orgTypeLabel || detail.orgType || '-'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color={statusColorMap[detail.status || ''] || 'default'} className="border-none">
                      {detail.status === 'ENABLED' ? '正常' : '停用'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="联系人">{detail.contactPerson || '-'}</Descriptions.Item>
                  <Descriptions.Item label="联系电话">{detail.contactPhone || '-'}</Descriptions.Item>
                  <Descriptions.Item label="统一社会信用代码" span={2}>{detail.unifiedSocialCode || '-'}</Descriptions.Item>
                  <Descriptions.Item label="联系地址" span={2}>{detail.address || '-'}</Descriptions.Item>
                  <Descriptions.Item label="负责人">{detail.leaderName || '-'}</Descriptions.Item>
                  <Descriptions.Item label="备注">{detail.remark || '-'}</Descriptions.Item>
                </Descriptions>
                <Row gutter={16} className="mt-4">
                  <Col span={6}>
                    <Statistic title="关联项目" value={detail.projectCount || 0} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="关联合同" value={detail.contractCount || 0} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="车辆总数" value={detail.vehicleCount || 0} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="在用车辆" value={detail.activeVehicleCount || 0} />
                  </Col>
                </Row>
              </Card>

              {/* Users */}
              <Card
                className="glass-panel g-border-panel border"
                title={<span><UserOutlined className="mr-1" />组织用户 ({users.length})</span>}
                size="small"
              >
                <Table
                  size="small"
                  rowKey="id"
                  dataSource={users}
                  columns={userColumns}
                  loading={usersLoading}
                  pagination={users.length > 10 ? { pageSize: 10 } : false}
                  locale={{ emptyText: <Empty description="暂无用户" /> }}
                />
              </Card>

              {/* Projects */}
              <Card className="glass-panel g-border-panel border" title="关联项目" size="small">
                <Table
                  size="small"
                  rowKey="projectId"
                  dataSource={projects}
                  columns={projectColumns}
                  pagination={false}
                  scroll={{ x: 'max-content', y: 300 }}
                  locale={{ emptyText: <Empty description="暂无关联项目" /> }}
                />
              </Card>

              {/* Contract groups */}
              <Card
                className="glass-panel g-border-panel border"
                title="合同明细（按消纳场地汇总）"
                size="small"
                extra={selectedProjectId ? `项目ID ${selectedProjectId}` : '未选择项目'}
                loading={contractGroupLoading}
              >
                {contractGroups.length ? (
                  <div className="space-y-4">
                    {contractGroups.map((group) => (
                      <Card
                        key={group.siteId || group.siteName}
                        type="inner"
                        title={group.siteName}
                        extra={`合同 ${group.contractCount} 份`}
                      >
                        <div className="grid grid-cols-3 gap-4 mb-4 text-sm">
                          <div className="g-text-secondary">合同金额：<span className="g-text-primary">{formatMoney(group.contractAmount)}</span></div>
                          <div className="g-text-secondary">已入账：<span className="g-text-primary">{formatMoney(group.receivedAmount)}</span></div>
                          <div className="g-text-secondary">约定方量：<span className="g-text-primary">{Number(group.agreedVolume || 0).toLocaleString()}</span></div>
                        </div>
                        <Table
                          size="small"
                          rowKey="id"
                          dataSource={group.contracts}
                          columns={contractColumns}
                          pagination={false}
                          scroll={{ x: 'max-content', y: 300 }}
                        />
                      </Card>
                    ))}
                  </div>
                ) : (
                  <Empty description="暂无合同明细" />
                )}
              </Card>
            </>
          ) : null}
        </div>

      {/* ── Create / Edit Modal ── */}
      <Modal
        title={editingOrgId ? '编辑组织' : '新增组织'}
        open={modalOpen}
        width={720}
        onCancel={() => {
          setModalOpen(false);
          setEditingOrgId(null);
          form.resetFields();
        }}
        onOk={() => void handleSubmit()}
        confirmLoading={submitLoading}
      >
        <Form form={form} layout="vertical" initialValues={{ status: 'ENABLED' }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="orgName" label="组织名称" rules={[{ required: true, message: '请输入组织名称' }]}>
                <Input placeholder="请输入组织名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgType" label="组织类型" rules={[{ required: true, message: '请选择组织类型' }]}>
                <Select
                  placeholder="请选择组织类型"
                  options={orgTypes.map((t) => ({ label: t.dictLabel, value: t.dictCode }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="parentId" label="上级组织">
                <TreeSelect
                  treeData={treeSelectData}
                  placeholder="不选则为顶级组织"
                  allowClear
                  showSearch
                  treeNodeFilterProp="title"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orgCode" label="组织编码">
                <Input placeholder="不填则自动生成" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPerson" label="联系人">
                <Input placeholder="请输入联系人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPhone" label="联系电话">
                <Input placeholder="请输入联系电话" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unifiedSocialCode" label="统一社会信用代码">
                <Input placeholder="请输入" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select
                  options={[
                    { label: '正常', value: 'ENABLED' },
                    { label: '停用', value: 'DISABLED' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="address" label="联系地址">
                <Input placeholder="请输入联系地址" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={2} placeholder="请输入备注" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* ── Admin credentials result modal ── */}
      <Modal
        title="组织创建成功"
        open={!!adminResult}
        onCancel={() => setAdminResult(null)}
        footer={[
          <Button key="ok" type="primary" onClick={() => setAdminResult(null)}>
            我已记录，关闭
          </Button>,
        ]}
      >
        {adminResult && (
          <div className="space-y-4">
            <p className="g-text-secondary">已自动创建该组织的管理员账号，请妥善保存以下信息（仅展示一次）：</p>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="管理员账号">
                <span className="font-mono font-bold">{adminResult.adminUsername}</span>
              </Descriptions.Item>
              <Descriptions.Item label="初始密码">
                <span className="font-mono font-bold text-red-600">{adminResult.adminPassword}</span>
              </Descriptions.Item>
            </Descriptions>
            <p className="text-xs g-text-secondary">该管理员首次登录后需修改密码。</p>
          </div>
        )}
      </Modal>
    </motion.div>
    </div>
  );
};

export default OrgManagement;
