import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Divider,
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
import TiandituMap from '../components/TiandituMap';
import type { MapPoint, MapPolygon, MapPolyline } from '../components/TiandituMap';
import { parseGeoJsonLine, parseGeoJsonPolygon } from '../utils/mapGeometry';
import {
  fetchProjectDetail,
  type ProjectRecord,
} from '../utils/projectApi';

const statusColorMap: Record<string, string> = {
  立项: 'warning',
  在建: 'processing',
  预警: 'error',
  完工: 'success',

const paymentStatusColorMap: Record<string, string> = {
  已结清: 'success',
  欠款中: 'error',

const formatMoney = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const formatVolume = (value?: number | null) =>
  Number(value || 0).toLocaleString() + ' 方';

const DEFAULT_MAP_CENTER: MapPoint = [120.1551, 30.2741];

const ProjectDetail: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const defaultTab = searchParams.get('tab') || 'info';
  const [loading, setLoading] = useState(false);
  const [project, setProject] = useState<ProjectRecord | null>(null);

  useEffect(() => {
    if (!id) {
      return;
    }

    const loadData = async () => {
      setLoading(true);
      try {
        const projectDetail = await fetchProjectDetail(id);
        setProject(projectDetail);
      } catch (error) {
        console.error(error);
        message.error('获取项目详情失败');
        setProject(null);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [id]);

  const contracts = project?.contractDetails || [];
  const sites = project?.siteDetails || [];
  const projectConfig = project?.config || null;
  const permitMock = useMemo(
    () =>
      contracts.slice(0, 3).map((item, index) => ({
        id: 'CZ-' + String(item.contractId || '').padStart(4, '0'),
        car: '浙A' + String(index + 1) + '23' + String(index + 4) + '5',
        site: item.siteName || '-',
        status: index === 0 ? '已绑定' : '待补齐',
        expire: item.expireDate || '-',
      })),
    [contracts]
  );
};  const routePath = useMemo<MapPoint[]>(
    () => parseGeoJsonLine(projectConfig?.routeGeoJson),
    [projectConfig?.routeGeoJson]
  );
};  const violationFencePath = useMemo<MapPolygon[]>(
    () => {
      const path = parseGeoJsonPolygon(projectConfig?.violationFenceGeoJson);
      if (path.length < 3) {
        return [];
      }
      return [{
        id: 'project-violation-fence',
        path,
        color: '#fa8c16',
        fillColor: '#ffd591',
        fillOpacity: 0.2,
        weight: 3,
        opacity: 0.85,
      }];
    },
    [projectConfig?.violationFenceGeoJson]
  );
};  const routeLines = useMemo<MapPolyline[]>(
    () => (routePath.length >= 2 ? [{ id: 'project-route', path: routePath, color: '#1677ff', weight: 5, opacity: 0.9 }] : []),
    [routePath]
  );
};  const configMapCenter = useMemo<MapPoint>(() => {
    if (routePath.length > 0) {
      return routePath[0];
    }
    const site = sites.find((item) => item.lng != null && item.lat != null);
    if (site?.lng != null && site?.lat != null) {
      return [Number(site.lng), Number(site.lat)];
    }
    return DEFAULT_MAP_CENTER;
  }, [routePath, sites]);

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
                  {formatMoney(project?.totalAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">累计交款</div>
                <div className="text-2xl font-bold g-text-success">
                  {formatMoney(project?.paidAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">欠款金额</div>
                <div className="text-2xl font-bold g-text-error">
                  {formatMoney(project?.debtAmount)}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">结算状态</div>
                <div className="text-2xl font-bold">
                  <Tag
                    color={
                      paymentStatusColorMap[project?.paymentStatusLabel || ''] ||
                      'processing'
                    }
                  >
                    {project?.paymentStatusLabel || '-'}
                  </Tag>
                </div>
                <div className="mt-2 text-xs g-text-secondary">
                  最近交款: {project?.lastPaymentDate || '-'}
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
          <Card title="项目合同清单" className="glass-panel g-border-panel border">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">合同总量</div>
                <div className="text-2xl font-bold g-text-primary">{contracts.length}</div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">合同方量</div>
                <div className="text-2xl font-bold g-text-primary-link">
                  {formatVolume(contracts.reduce((sum, item) => sum + Number(item.agreedVolume || 0), 0))}
                </div>
              </Card>
              <Card type="inner" className="bg-white g-border-panel border">
                <div className="g-text-secondary mb-2">剩余方量</div>
                <div className="text-2xl font-bold g-text-warning">
                  {formatVolume(contracts.reduce((sum, item) => sum + Number(item.remainingVolume || 0), 0))}
                </div>
              </Card>
            </div>
            <List
              locale={{ emptyText: <Empty description="暂无关联合同" /> }}
              dataSource={contracts}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <a key="detail" onClick={() => navigate('/contracts/' + item.contractId)}>
                      查看合同
                    </a>,
                  ]}
                >
                  <List.Item.Meta
                    title={<span className="g-text-primary">{item.contractNo || 'HT-' + item.contractId}</span>}
                    description={
                      <div className="g-text-secondary">
                        <Space size="large" wrap>
                          <span>{item.contractName}</span>
                          <span>场地: {item.siteName || '-'}</span>
                          <span>方量: {formatVolume(item.agreedVolume)}</span>
                          <span>已消纳: {formatVolume(item.disposedVolume)}</span>
                          <span>剩余: {formatVolume(item.remainingVolume)}</span>
                          <span>金额: {formatMoney(item.contractAmount)}</span>
                        </Space>
                        <div className="mt-2 text-xs">
                          审批状态: {item.approvalStatus || '-'} · 到期: {item.expireDate || '-'}
                        </div>
                      </div>
                    }
                  />
                  <Tag color={item.contractStatus === 'EFFECTIVE' ? 'success' : item.contractStatus === 'REJECTED' ? 'error' : 'processing'}>
                    {item.contractStatus || '未知'}
                  </Tag>
                </List.Item>
              )}
            />
          </Card>

          <Card title="项目场地清单" className="glass-panel g-border-panel border">
            <List
              locale={{ emptyText: <Empty description="暂无关联场地" /> }}
              dataSource={sites}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <a key="site" onClick={() => item.siteId && navigate('/sites/' + item.siteId)}>
                      查看场地
                    </a>,
                  ]}
                >
                  <List.Item.Meta
                    title={<span className="g-text-primary">{item.siteName}</span>}
                    description={
                      <div className="g-text-secondary">
                        <Space size="large" wrap>
                          <span>场地类型: {item.siteType || '-'}</span>
                          <span>容量: {formatVolume(item.capacity)}</span>
                          <span>关联合同: {item.contractCount || 0}</span>
                          <span>合同方量: {formatVolume(item.contractVolume)}</span>
                          <span>已消纳: {formatVolume(item.disposedVolume)}</span>
                          <span>剩余: {formatVolume(item.remainingVolume)}</span>
                        </Space>
                      </div>
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
        <div className="space-y-6">
          <Card className="glass-panel g-border-panel border">
            <Descriptions column={2} className="g-text-secondary">
              <Descriptions.Item label="打卡配置">
                <Tag color={projectConfig?.checkinEnabled ? 'success' : 'default'}>
                  {projectConfig?.checkinEnabled ? '已启用' : '未启用'}
                </Tag>
                <span className="ml-2">{projectConfig?.checkinAccount || '-'}</span>
              </Descriptions.Item>
              <Descriptions.Item label="打卡授权范围">{projectConfig?.checkinAuthScope || '-'}</Descriptions.Item>
              <Descriptions.Item label="位置判断配置">
                <Tag color={projectConfig?.locationCheckRequired ? 'blue' : 'default'}>
                  {projectConfig?.locationCheckRequired ? '启用位置判断' : '未启用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="位置判断半径">
                {Number(projectConfig?.locationRadiusMeters || 0).toLocaleString()} 米
              </Descriptions.Item>
              <Descriptions.Item label="出土预扣值">
                {formatVolume(projectConfig?.preloadVolume)}
              </Descriptions.Item>
              <Descriptions.Item label="违规配置">
                <Tag color={projectConfig?.violationRuleEnabled ? 'error' : 'default'}>
                  {projectConfig?.violationRuleEnabled ? '围栏规则启用' : '围栏规则关闭'}
                </Tag>
                <span className="ml-2">{projectConfig?.violationFenceName || projectConfig?.violationFenceCode || '-'}</span>
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {projectConfig?.remark || '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="线路与违规围栏预览" className="glass-panel g-border-panel border">
            <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_280px] gap-4">
              <div className="relative h-80 rounded overflow-hidden border g-border-panel">
                <TiandituMap
                  className="absolute inset-0"
                  center={configMapCenter}
                  zoom={13}
                  polylines={routeLines}
                  polygons={violationFencePath}
                  loadingText="项目配置地图加载中..."
                />
              </div>
              <div className="space-y-3">
                <div className="p-3 rounded border g-border-panel bg-white">
                  <div className="text-sm g-text-primary">线路状态</div>
                  <div className="text-xs g-text-secondary mt-1">{routePath.length >= 2 ? '已配置真实线路' : '暂无线路数据'}</div>
                </div>
                <div className="p-3 rounded border g-border-panel bg-white">
                  <div className="text-sm g-text-primary">违规围栏</div>
                  <div className="text-xs g-text-secondary mt-1">{projectConfig?.violationFenceName || '未配置'}</div>
                </div>
                <div className="p-3 rounded border g-border-panel bg-white">
                  <div className="text-sm g-text-primary">中心坐标</div>
                  <div className="text-xs g-text-secondary mt-1">{configMapCenter[0]}, {configMapCenter[1]}</div>
                </div>
              </div>
            </div>
            <Divider />
            <div className="text-xs g-text-secondary">
              当前项目配置已收口打卡配置、位置判断、出土预扣值、线路配置与违规围栏 5 类数据。
            </div>
          </Card>
        </div>
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
