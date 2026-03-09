import React from 'react';
import { Card, Tabs, Descriptions, Tag, Button, Space, Table, List, Switch, InputNumber, Select } from 'antd';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const SiteDetail: React.FC = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const defaultTab = searchParams.get('tab') || 'info';

    // 模拟数据
    const siteInfo = {
        id: id || '1',
        name: '东区临时消纳场',
        type: '国有场地',
        capacity: 500000,
        used: 350000,
        status: '正常',
        address: '经济开发区科技路 128 号',
        operator: '市城投集团',
        manager: '李四',
        phone: '13900139000',
    };

    const items = [
        {
            key: 'info',
            label: '基础信息',
            children: (
                <div className="space-y-6">
                    <Card title="场地基础信息" className="glass-panel g-border-panel border" extra={<Button type="link" icon={<EditOutlined />}>编辑</Button>}>
                        <Descriptions column={3} className="g-text-secondary">
                            <Descriptions.Item label="场地名称">{siteInfo.name}</Descriptions.Item>
                            <Descriptions.Item label="场地类型"><Tag color="blue">{siteInfo.type}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地状态"><Tag color="green">{siteInfo.status}</Tag></Descriptions.Item>
                            <Descriptions.Item label="总容量">{siteInfo.capacity / 10000} 万方</Descriptions.Item>
                            <Descriptions.Item label="已用容量">{siteInfo.used / 10000} 万方</Descriptions.Item>
                            <Descriptions.Item label="剩余容量">{(siteInfo.capacity - siteInfo.used) / 10000} 万方</Descriptions.Item>
                            <Descriptions.Item label="详细地址" span={2}>{siteInfo.address}</Descriptions.Item>
                            <Descriptions.Item label="运营单位">{siteInfo.operator}</Descriptions.Item>
                            <Descriptions.Item label="联系人">{siteInfo.manager} ({siteInfo.phone})</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    <Card title="结算规则配置" className="glass-panel g-border-panel border">
                        <Descriptions column={2} className="g-text-secondary">
                            <Descriptions.Item label="结算方式">按月结算申请</Descriptions.Item>
                            <Descriptions.Item label="计费规则">金额 = 消纳量 × 单价(政府定价或合同单价)</Descriptions.Item>
                        </Descriptions>
                    </Card>
                </div>
            ),
        },
        {
            key: 'disposals',
            label: '消纳清单',
            children: (
                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between">
                        <Space>
                            <Select defaultValue="all" style={{ width: 120 }} options={[{ value: 'all', label: '全部来源' }, { value: 'scale', label: '地磅称重' }, { value: 'manual', label: '人工录入' }]} />
                            <Select defaultValue="valid" style={{ width: 120 }} options={[{ value: 'valid', label: '有效记录' }, { value: 'invalid', label: '已作废' }]} />
                        </Space>
                        <Button>导出 Excel</Button>
                    </div>
                    <Table
                        dataSource={[
                            { id: 'XN-20240305-001', time: '2024-03-05 10:20:00', car: '鲁A·12345', project: '滨海新区基础建设B标段', source: '地磅称重', amount: 15.5, status: '有效' },
                            { id: 'XN-20240305-002', time: '2024-03-05 11:15:00', car: '鲁A·54321', project: '老旧小区改造工程综合包', source: '人工录入', amount: 12.0, status: '有效' },
                        ]}
                        columns={[
                            { title: '记录编号', dataIndex: 'id', key: 'id' },
                            { title: '消纳时间', dataIndex: 'time', key: 'time' },
                            { title: '车牌号', dataIndex: 'car', key: 'car' },
                            { title: '来源', dataIndex: 'source', key: 'source' },
                            { title: '消纳量(方)', dataIndex: 'amount', key: 'amount' },
                            { title: '状态', dataIndex: 'status', key: 'status', render: (s) => <Tag color={s === '有效' ? 'success' : 'default'}>{s}</Tag> },
                            { title: '操作', key: 'action', render: () => <a className="g-text-error">作废</a> }
                        ]}
                        pagination={false}
                        className="bg-transparent"
                        rowClassName="hover:bg-white transition-colors"
                    />
                </Card>
            ),
        },
        {
            key: 'docs',
            label: '场地资料',
            children: (
                <div className="space-y-6">
                    <Card title="审批阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">上传资料</Button>}>
                        <List
                            dataSource={[
                                { name: '立项批复文件.pdf', time: '2023-10-01', uploader: '系统管理员' },
                                { name: '环评批复报告.pdf', time: '2023-10-15', uploader: '系统管理员' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>预览</a>, <a>下载</a>]}>
                                    <List.Item.Meta title={<span className="g-text-primary">{item.name}</span>} description={<span className="g-text-secondary">{item.uploader} 上传于 {item.time}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="运营阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">上传资料</Button>}>
                        <List
                            dataSource={[
                                { name: '2024年1月安全检查记录.docx', time: '2024-02-01', uploader: '李四' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>预览</a>, <a>下载</a>]}>
                                    <List.Item.Meta title={<span className="g-text-primary">{item.name}</span>} description={<span className="g-text-secondary">{item.uploader} 上传于 {item.time}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'config',
            label: '场地配置',
            children: (
                <div className="space-y-6">
                    <Card title="设备配置" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">新增设备</Button>}>
                        <List
                            dataSource={[
                                { name: '入口抓拍机', type: '抓拍机', ip: '192.168.1.101', status: '在线' },
                                { name: '1号地磅', type: '地磅', ip: '192.168.1.102', status: '在线' },
                                { name: '全景监控', type: '视频', ip: '192.168.1.103', status: '离线' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a>配置</a>]}>
                                    <List.Item.Meta
                                        title={<Space><span className="g-text-primary">{item.name}</span><Tag color={item.status === '在线' ? 'success' : 'error'}>{item.status}</Tag></Space>}
                                        description={<span className="g-text-secondary">类型: {item.type} | IP: {item.ip}</span>}
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                    
                    <Card title="运营配置" className="glass-panel g-border-panel border">
                        <Descriptions column={1} labelStyle={{ width: '200px' }}>
                            <Descriptions.Item label="排号规则">
                                <Switch defaultChecked /> <span className="ml-2 g-text-secondary">开启后车辆需排队入场</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="最大等待数">
                                <InputNumber defaultValue={50} min={1} />
                            </Descriptions.Item>
                            <Descriptions.Item label="人工消纳开关">
                                <Switch /> <span className="ml-2 g-text-secondary">开启后可在系统手动录入消纳记录(用于异常情况)</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="范围检测半径 (米)">
                                <InputNumber defaultValue={200} min={50} />
                            </Descriptions.Item>
                        </Descriptions>
                        <div className="mt-4">
                            <Button type="primary">保存配置</Button>
                        </div>
                    </Card>
                </div>
            ),
        }
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
                    onClick={() => navigate('/sites')}
                    className="g-text-secondary hover:g-text-primary"
                />
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">{siteInfo.name}</h1>
                    <p className="g-text-secondary mt-1">场地类型: {siteInfo.type}</p>
                </div>
            </div>

            <Tabs 
                defaultActiveKey={defaultTab} 
                items={items} 
                className="custom-tabs"
            />
        </motion.div>
    );
};

export default SiteDetail;
