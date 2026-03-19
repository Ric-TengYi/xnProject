import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  List,
  Space,
  Spin,
  Tabs,
  Tag,
  message,
} from 'antd';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { fetchContractList, type ContractRecord } from '../utils/contractApi';
import {
  fetchProjectDetail,
  fetchProjectPaymentSummary,
  type ProjectRecord,
} from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';

const statusColorMap: Record<string, string> = {
  立项: 'warning',
  在建: 'processing',
  预警: 'error',
  完工: 'success',
};

const paymentStatusColorMap: Record<string, string> = {
  已结清: 'success',
  欠款中: 'error',
};

const formatMoney = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const ProjectDetail: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const defaultTab = searchParams.get('tab') || 'info';
  const [loading, setLoading] = useState(false);
  const [project, setProject] = useState<ProjectRecord | null>(null);
  const [paymentSummary, setPaymentSummary] = useState<ProjectRecord | null>(null);
  const [contracts, setContracts] = useState<ContractRecord[]>([]);
  const [sites, setSites] = useState<SiteRecord[]>([]);

  useEffect(() => {
    if (!id) {
      return;
    }

    const loadData = async () => {
      setLoading(true);
      try {
        const [projectDetail, summary, contractPage, siteList] = await Promise.all([
          fetchProjectDetail(id),
          fetchProjectPaymentSummary(id),
          fetchContractList({ projectId: id, pageNo: 1, pageSize: 20 }),
          fetchSites(),
        ]);

        setProject(projectDetail);
        setPaymentSummary({
          ...projectDetail,
          totalAmount: summary.totalAmount,
          paidAmount: summary.paidAmount,
          debtAmount: summary.debtAmount,
          lastPaymentDate: summary.lastPaymentDate,
          paymentStatus: summary.status,
          paymentStatusLabel: summary.status === 'SETTLED' ? '已结清' : '欠款中',
        });
        setContracts(contractPage.records || []);
        setSites(
          (siteList || []).filter(
            (item) => String(item.projectId || '') === String(id)
          )
        );
      } catch (error) {
        console.error(error);
        message.error('获取项目详情失败');
        setProject(null);
        setPaymentSummary(null);
        setContracts([]);
        setSites([]);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [id]);

  const permitMock = useMemo(
    () =>
      contracts.slice(0, 3).map((item, index) => ({
        id: 'CZ-' + String(item.id).padStart(4, '0'),
        car: '浙A' + String(index + 1) + '23' + String(index + 4) + '5',
        site: item.siteName || '-',
        status: index === 0 ? '已绑定' : '待补齐',
        expire: item.expireDate || '-',
      })),
    [contracts]
  );

  const items = [
    {
      key: 'info',
      label: '基础信息与交款',
      children: (
        <div className="space-y-6">
          <Card
            title="基础信息"
            className="glass-panel g-border-panel border"
            extra={<Button type="link" icon={<EditOutlined />}>编辑</Button>}
          >
            <Descriptions column={3} className="g-text-secondary">
              <Descriptions.Item label="项目编号">
                {project?.code || 'PRJ-' + String(project?.id || '')}
              </Descriptions.Item>
              <Descriptions.Item label="项目名称">{project?.name || '-'}</Descriptions.Item>
              <Descriptions.Item label="项目状态">
                <Tag color={statusColorMap[project?.statusLabel || ''] || 'default'}>
                  {project?.statusLabel || '未知'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="所属组织">{project?.orgName || '-'}</Descriptions.Item>
              <Descriptions.Item label="关联合同">
                {(project?.contractCount || 0) + ' 份'}
              </Descriptions.Item>
              <Descriptions.Item label="关联场地">
                {(project?.siteCount || 0) + ' 个'}
              </Descriptions.Item>
              <Descriptions.Item label="项目地址" span={2}>
                {project?.address || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{project?.createTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{project?.updateTime || '-'}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="交款数据" className="glass-panel g-border-panel border">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">应收总额</div>
                <div className="text-2xl font-bold g-text-primary">
                  {formatMoney(paymentSummary?.totalAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">累计交款</div>
                <div className="text-2xl font-bold g-text-success">
                  {formatMoney(paymentSummary?.paidAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">欠款金额</div>
                <div className="text-2xl font-bold g-text-error">
                  {formatMoney(paymentSummary?.debtAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">结算状态</div>
                <div className="text-2xl font-bold">
                  <Tag
                    color={
                      paymentStatusColorMap[paymentSummary?.paymentStatusLabel || ''] ||
                      'processing'
                    }
                  >
                    {paymentSummary?.paymentStatusLabel || '-'}
                  </Tag>
                </div>
                <div className="mt-2 text-xs g-text-secondary">
                  最近交款: {paymentSummary?.lastPaymentDate || '-'}
                </div>
              </Card>
            </div>
          </Card>
        </div>
      ),
    },
    {
      key: 'contracts',
      label: '合同与场地清单',
      children: (
        <div className="space-y-6">
          <Card title="关联合同" className="glass-panel g-border-panel border">
            <List
              locale={{ emptyText: <Empty description="暂无关联合同" /> }}
              dataSource={contracts}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <a key="detail" onClick={() => navigate('/contracts/' + item.id)}>
                      查看合同
                    </a>,
                  ]}
                >
                  <List.Item.Meta
                    title={<span className="g-text-primary">{item.contractNo || 'HT-' + item.id}</span>}
                    description={
                      <Space className="g-text-secondary" size="large">
                        <span>{item.name}</span>
                        <span>场地: {item.siteName || '-'}</span>
                        <span>金额: {formatMoney(item.contractAmount)}</span>
                      </Space>
                    }
                  />
                  <Tag>{item.contractStatus || '未知'}</Tag>
                </List.Item>
              )}
            />
          </Card>

          <Card title="关联场地" className="glass-panel g-border-panel border">
            <List
              locale={{ emptyText: <Empty description="暂无关联场地" /> }}
              dataSource={sites}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <a key="site" onClick={() => navigate('/sites/' + item.id)}>
                      查看场地
                    </a>,
                  ]}
                >
                  <List.Item.Meta
                    title={<span className="g-text-primary">{item.name}</span>}
                    description={
                      <span className="g-text-secondary">
                        地址: {item.address || '-'} | 编码: {item.code || '-'}
                      </span>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </div>
      ),
    },
    {
      key: 'permits',
      label: '处置证清单',
      children: (
        <Card className="glass-panel g-border-panel border">
          <List
            locale={{
              emptyText: <Empty description="当前未沉淀真实处置证数据，先保留合同映射入口" />,
            }}
            dataSource={permitMock}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  title={
                    <Space>
                      <span className="g-text-primary">{item.id}</span>
                      <Tag color={item.status === '已绑定' ? 'success' : 'warning'}>
                        {item.status}
                      </Tag>
                    </Space>
                  }
                  description={
                    <Space className="g-text-secondary mt-2" size="large">
                      <span>关联车辆: {item.car}</span>
                      <span>消纳场地: {item.site}</span>
                      <span>有效期至: {item.expire}</span>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      ),
    },
    {
      key: 'config',
      label: '项目配置',
      children: (
        <Card className="glass-panel g-border-panel border">
          <Descriptions column={2} className="g-text-secondary">
            <Descriptions.Item label="打卡配置">待接真实配置表</Descriptions.Item>
            <Descriptions.Item label="位置判断">待接真实规则表</Descriptions.Item>
            <Descriptions.Item label="出土预扣值">待接真实阈值配置</Descriptions.Item>
            <Descriptions.Item label="线路配置">待接地图线路数据</Descriptions.Item>
          </Descriptions>
        </Card>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6 pb-10"
    >
      <div className="flex items-center gap-4 mb-6">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/projects')}
          className="g-text-secondary hover:g-text-primary"
        />
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">
            {project?.name || '项目详情'}
          </h1>
          <p className="g-text-secondary mt-1">
            项目编号: {project?.code || (project?.id ? 'PRJ-' + project.id : '-')}
          </p>
        </div>
      </div>

      <Spin spinning={loading}>
        {project ? (
          <Tabs defaultActiveKey={defaultTab} items={items} className="custom-tabs" />
        ) : (
          <Card className="glass-panel g-border-panel border">
            <Empty description="项目不存在或暂无数据" />
          </Card>
        )}
      </Spin>
    </motion.div>
  );
};

export default ProjectDetail;
