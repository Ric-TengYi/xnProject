import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tree,
  message,
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

type OrgTreeNode = {
  title: React.ReactNode;
  key: string;
  children?: OrgTreeNode[];
};

type UserRecord = {
  id: string;
  name?: string;
  username?: string;
  mainOrgName?: string;
  roleNames?: string[];
  status?: string;
};

type OrgTypeRecord = {
  id: string;
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  sort?: number;
  status?: string;
  remark?: string;
};

type OrgTypeFormValues = {
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  sort?: number;
  status?: string;
  remark?: string;
};

const ORG_TYPE_DICT = 'ORG_CATEGORY';

const Organization: React.FC = () => {
  const [searchText, setSearchText] = useState('');
  const [treeData, setTreeData] = useState<OrgTreeNode[]>([]);
  const [usersData, setUsersData] = useState<UserRecord[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [orgTypesLoading, setOrgTypesLoading] = useState(false);
  const [orgTypes, setOrgTypes] = useState<OrgTypeRecord[]>([]);
  const [typeKeyword, setTypeKeyword] = useState('');
  const [typeModalOpen, setTypeModalOpen] = useState(false);
  const [editingType, setEditingType] = useState<OrgTypeRecord | null>(null);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [typeForm] = Form.useForm<OrgTypeFormValues>();

  useEffect(() => {
    void fetchOrgs();
    void fetchOrgTypes();
  }, []);

  useEffect(() => {
    void fetchUsers();
  }, [selectedOrgId, searchText]);

  const tenantId = useMemo(() => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    return userInfo.tenantId || '1';
  }, []);

  const fetchOrgs = async () => {
    try {
      const res = await request.get('/orgs/tree', { params: { tenantId } });
      if (res.code === 200) {
        const formatTree = (nodes: any[]): OrgTreeNode[] =>
          nodes.map((node) => ({
            title: node.orgName,
            key: String(node.id),
            children: node.children ? formatTree(node.children) : [],
          }));
        setTreeData(formatTree(res.data || []));
      }
    } catch (error) {
      console.error(error);
      message.error('获取组织架构失败');
    }
  };

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = {
        pageNo: 1,
        pageSize: 100,
        tenantId,
      };
      if (selectedOrgId) params.orgId = selectedOrgId;
      if (searchText.trim()) params.keyword = searchText.trim();

      const res = await request.get('/users', { params });
      if (res.code === 200) {
        setUsersData(res.data.records || []);
      }
    } catch (error) {
      console.error(error);
      message.error('获取人员列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchOrgTypes = async () => {
    setOrgTypesLoading(true);
    try {
      const res = await request.get('/data-dicts', {
        params: {
          dictType: ORG_TYPE_DICT,
          keyword: typeKeyword.trim() || undefined,
        },
      });
      if (res.code === 200) {
        setOrgTypes(
          (res.data || []).map((item: any) => ({
            id: String(item.id || ''),
            dictCode: item.dictCode || '',
            dictLabel: item.dictLabel || '',
            dictValue: item.dictValue || '',
            sort: item.sort ?? 0,
            status: item.status || 'ENABLED',
            remark: item.remark || '',
          }))
        );
      }
    } catch (error) {
      console.error(error);
      message.error('获取组织类型失败');
    } finally {
      setOrgTypesLoading(false);
    }
  };

  const openCreateType = () => {
    setEditingType(null);
    typeForm.resetFields();
    typeForm.setFieldsValue({ status: 'ENABLED', sort: orgTypes.length + 1 });
    setTypeModalOpen(true);
  };

  const openEditType = (record: OrgTypeRecord) => {
    setEditingType(record);
    typeForm.setFieldsValue({
      dictCode: record.dictCode,
      dictLabel: record.dictLabel,
      dictValue: record.dictValue,
      sort: record.sort,
      status: record.status || 'ENABLED',
      remark: record.remark,
    });
    setTypeModalOpen(true);
  };

  const handleSubmitType = async () => {
    try {
      const values = await typeForm.validateFields();
      setSubmitLoading(true);
      const payload = {
        dictType: ORG_TYPE_DICT,
        dictCode: values.dictCode,
        dictLabel: values.dictLabel,
        dictValue: values.dictValue,
        sort: values.sort || 0,
        status: values.status || 'ENABLED',
        remark: values.remark,
      };
      if (editingType) {
        await request.put(`/data-dicts/${editingType.id}`, payload);
        message.success('组织类型已更新');
      } else {
        await request.post('/data-dicts', payload);
        message.success('组织类型已新增');
      }
      setTypeModalOpen(false);
      await fetchOrgTypes();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error(editingType ? '更新组织类型失败' : '新增组织类型失败');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleToggleTypeStatus = async (record: OrgTypeRecord) => {
    try {
      await request.put(`/data-dicts/${record.id}/status`, {
        status: record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
      });
      message.success('组织类型状态已更新');
      await fetchOrgTypes();
    } catch (error) {
      console.error(error);
      message.error('更新组织类型状态失败');
    }
  };

  const handleDeleteType = async (id: string) => {
    try {
      await request.delete(`/data-dicts/${id}`);
      message.success('组织类型已删除');
      await fetchOrgTypes();
    } catch (error) {
      console.error(error);
      message.error('删除组织类型失败');
    }
  };

  const orgTypeSummary = useMemo(() => ({
    total: orgTypes.length,
    enabled: orgTypes.filter((item) => item.status === 'ENABLED').length,
    lawEnforcement: orgTypes.filter((item) => item.dictCode.includes('LAW')).length,
    company: orgTypes.filter((item) => item.dictCode.includes('COMPANY')).length,
  }), [orgTypes]);

  const userColumns = [
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <strong className="g-text-primary">{text}</strong>,
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
            <Tag color="blue" key={index} className="border-none">
              {name}
            </Tag>
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
      render: () => (
        <Space size="middle">
          <a className="g-text-primary-link hover:g-text-primary-link">
            <EditOutlined /> 编辑
          </a>
          <a className="g-text-error hover:g-text-error">
            <DeleteOutlined /> 停用
          </a>
        </Space>
      ),
    },
  ];

  const orgTypeColumns = [
    {
      title: '类型名称',
      dataIndex: 'dictLabel',
      key: 'dictLabel',
      render: (text: string) => <strong className="g-text-primary">{text}</strong>,
    },
    {
      title: '类型编码',
      dataIndex: 'dictCode',
      key: 'dictCode',
      render: (text: string) => <span className="g-text-secondary font-mono">{text}</span>,
    },
    {
      title: '归类值',
      dataIndex: 'dictValue',
      key: 'dictValue',
      render: (text: string) => <span className="g-text-secondary">{text}</span>,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ENABLED' ? 'success' : 'default'} className="border-none">
          {status === 'ENABLED' ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      render: (text: string) => <span className="g-text-secondary">{text || '-'}</span>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: OrgTypeRecord) => (
        <Space size="middle">
          <a onClick={() => openEditType(record)}>
            <EditOutlined /> 编辑
          </a>
          <a onClick={() => void handleToggleTypeStatus(record)}>
            {record.status === 'ENABLED' ? '停用' : '启用'}
          </a>
          <Popconfirm title="确认删除当前组织类型？" onConfirm={() => void handleDeleteType(record.id)}>
            <a className="g-text-error">
              <DeleteOutlined /> 删除
            </a>
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
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">组织与人员管理</h1>
          <p className="g-text-secondary mt-1">管理系统组织架构树、组织类型及各组织下属人员账号</p>
        </div>
      </div>

      <Tabs
        items={[
          {
            key: 'org-users',
            label: '组织人员',
            children: (
              <div className="flex gap-6 min-h-[640px]">
                <Card
                  className="glass-panel g-border-panel border w-80 flex flex-col"
                  bodyStyle={{ padding: '16px', flex: 1, overflow: 'auto' }}
                >
                  <div className="flex justify-between items-center mb-4">
                    <span className="g-text-primary font-bold">组织架构</span>
                    <Button type="link" icon={<PlusOutlined />} size="small">
                      新增
                    </Button>
                  </div>
                  <Input
                    placeholder="搜索组织..."
                    prefix={<SearchOutlined className="g-text-secondary" />}
                    className="bg-white g-border-panel border g-text-primary mb-4"
                  />
                  <Tree
                    defaultExpandAll
                    treeData={treeData}
                    onSelect={(selectedKeys) => setSelectedOrgId((selectedKeys[0] as string) || null)}
                    className="bg-transparent g-text-secondary custom-tree"
                  />
                </Card>

                <Card
                  className="glass-panel g-border-panel border flex-1 flex flex-col"
                  bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}
                >
                  <div className="p-4 border-b g-border-panel border flex justify-between g-bg-toolbar">
                    <Input
                      placeholder="搜索姓名/账号"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      className="w-64 bg-white g-border-panel border g-text-primary"
                      value={searchText}
                      onChange={(e) => setSearchText(e.target.value)}
                      onPressEnter={() => void fetchUsers()}
                    />
                    <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none">
                      新增人员
                    </Button>
                  </div>
                  <div className="flex-1 overflow-auto">
                    <Table
                      columns={userColumns}
                      dataSource={usersData}
                      rowKey="id"
                      loading={loading}
                      pagination={{ defaultPageSize: 10, className: 'pr-4 pb-2' }}
                      className="bg-transparent"
                      rowClassName="hover:bg-white transition-colors"
                    />
                  </div>
                </Card>
              </div>
            ),
          },
          {
            key: 'org-types',
            label: '组织类型',
            children: (
              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                  <Card className="glass-panel g-border-panel border">
                    <Statistic title="类型总数" value={orgTypeSummary.total} />
                  </Card>
                  <Card className="glass-panel g-border-panel border">
                    <Statistic title="启用类型" value={orgTypeSummary.enabled} />
                  </Card>
                  <Card className="glass-panel g-border-panel border">
                    <Statistic title="执法组织类" value={orgTypeSummary.lawEnforcement} />
                  </Card>
                  <Card className="glass-panel g-border-panel border">
                    <Statistic title="公司单位类" value={orgTypeSummary.company} />
                  </Card>
                </div>

                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                  <div className="p-4 border-b g-border-panel border flex justify-between g-bg-toolbar">
                    <Input
                      placeholder="搜索类型名称/编码/归类值"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      className="w-80 bg-white g-border-panel border g-text-primary"
                      value={typeKeyword}
                      onChange={(e) => setTypeKeyword(e.target.value)}
                      onPressEnter={() => void fetchOrgTypes()}
                    />
                    <Space>
                      <Button onClick={() => void fetchOrgTypes()}>查询</Button>
                      <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none" onClick={openCreateType}>
                        新增组织类型
                      </Button>
                    </Space>
                  </div>
                  <Table
                    columns={orgTypeColumns}
                    dataSource={orgTypes}
                    rowKey="id"
                    loading={orgTypesLoading}
                    pagination={false}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                  />
                </Card>
              </div>
            ),
          },
        ]}
      />

      <Modal
        title={editingType ? '编辑组织类型' : '新增组织类型'}
        open={typeModalOpen}
        onCancel={() => {
          setTypeModalOpen(false);
          setEditingType(null);
          typeForm.resetFields();
        }}
        onOk={() => void handleSubmitType()}
        confirmLoading={submitLoading}
        width={640}
      >
        <Form form={typeForm} layout="vertical">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="dictLabel" label="类型名称" rules={[{ required: true, message: '请输入类型名称' }]}>
              <Input placeholder="如：执法组织" />
            </Form.Item>
            <Form.Item name="dictCode" label="类型编码" rules={[{ required: true, message: '请输入类型编码' }]}>
              <Input placeholder="如：LAW_ENFORCEMENT" />
            </Form.Item>
            <Form.Item name="dictValue" label="归类值" rules={[{ required: true, message: '请输入归类值' }]}>
              <Input placeholder="如：LAW_ENFORCEMENT" />
            </Form.Item>
            <Form.Item name="sort" label="排序">
              <InputNumber min={0} precision={0} className="w-full" />
            </Form.Item>
            <Form.Item name="status" label="状态" initialValue="ENABLED">
              <Input placeholder="ENABLED / DISABLED" />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input placeholder="补充说明" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </motion.div>
  );
};

export default Organization;
