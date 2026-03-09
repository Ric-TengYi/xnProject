import React from 'react';
import { Card, Tabs, Descriptions, Tag, Button, Space, Timeline, List, Switch, InputNumber, Row, Col } from 'antd';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const ProjectDetail: React.FC = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const defaultTab = searchParams.get('tab') || 'info';

    // 模拟数据
    const projectInfo = {
        id: id || 'PRJ-24001',
        name: '滨海新区基础建设B标段',
        builder: '中建八局',
        transporter: '顺达土方工程队',
        status: '在建',
        startDate: '2024-01-15',
        totalAmount: 1500000,
        usedAmount: 1250000,
        address: '滨海新区科技路与创新大道交汇处',
        manager: '张建国',
        phone: '13800138000',
    };

    const items = [
        {
            key: 'info',
            label: '基础信息与交款',
            children: (
                <div className="space-y-6">
                    <Card title="基础信息" className="glass-panel border-slate-200 dark:border-slate-700/50" extra={<Button type="link" icon={<EditOutlined />}>编辑</Button>}>
                        <Descriptions column={3} className="text-slate-600 dark:text-slate-300">
                            <Descriptions.Item label="项目编号">{projectInfo.id}</Descriptions.Item>
                            <Descriptions.Item label="项目名称">{projectInfo.name}</Descriptions.Item>
                            <Descriptions.Item label="项目状态"><Tag color="processing">{projectInfo.status}</Tag></Descriptions.Item>
                            <Descriptions.Item label="建设单位">{projectInfo.builder}</Descriptions.Item>
                            <Descriptions.Item label="运输单位">{projectInfo.transporter}</Descriptions.Item>
                            <Descriptions.Item label="开工日期">{projectInfo.startDate}</Descriptions.Item>
                            <Descriptions.Item label="项目地址" span={2}>{projectInfo.address}</Descriptions.Item>
                            <Descriptions.Item label="项目负责人">{projectInfo.manager} ({projectInfo.phone})</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    <Card title="交款数据" className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Row gutter={16}>
                            <Col span={8}>
                                <Card type="inner" className="bg-white dark:bg-slate-800/50 border-slate-200 dark:border-slate-700">
                                    <div className="text-slate-600 dark:text-slate-400 mb-2">应消纳金额 (元)</div>
                                    <div className="text-2xl font-bold text-slate-900 dark:text-white">¥ 4,500,000</div>
                                </Card>
                            </Col>
                            <Col span={8}>
                                <Card type="inner" className="bg-white dark:bg-slate-800/50 border-slate-200 dark:border-slate-700">
                                    <div className="text-slate-600 dark:text-slate-400 mb-2">累计交款金额 (元)</div>
                                    <div className="text-2xl font-bold text-green-600 dark:text-green-400">¥ 3,000,000</div>
                                </Card>
                            </Col>
                            <Col span={8}>
                                <Card type="inner" className="bg-white dark:bg-slate-800/50 border-slate-200 dark:border-slate-700">
                                    <div className="text-slate-600 dark:text-slate-400 mb-2">欠款金额 (元)</div>
                                    <div className="text-2xl font-bold text-red-600 dark:text-red-400">¥ 1,500,000</div>
                                </Card>
                            </Col>
                        </Row>
                    </Card>
                </div>
            ),
        },
        {
            key: 'contracts',
            label: '合同与场地清单',
            children: (
                <div className="space-y-6">
                    <Card title="场地消纳进度" className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <List
                            itemLayout="horizontal"
                            dataSource={[
                                { site: '城东一号消纳场', contractId: 'HT-2024-001', total: 800000, used: 650000 },
                                { site: '高新区临时堆场', contractId: 'HT-2024-005', total: 700000, used: 600000 },
                            ]}
                            renderItem={item => {
                                const percent = Math.round((item.used / item.total) * 100);
                                return (
                                    <List.Item>
                                        <List.Item.Meta
                                            title={<span className="text-slate-900 dark:text-white">{item.site}</span>}
                                            description={<span className="text-slate-600 dark:text-slate-400">关联合同: {item.contractId}</span>}
                                        />
                                        <div className="w-1/2 flex items-center gap-4">
                                            <div className="flex-1 bg-white dark:bg-slate-800 rounded-full h-2 overflow-hidden">
                                                <div className={`h-full ${percent > 80 ? 'bg-green-500' : 'bg-blue-500'}`} style={{ width: `${percent}%` }}></div>
                                            </div>
                                            <span className="text-slate-600 dark:text-slate-300 w-32 text-right">{item.used / 10000} / {item.total / 10000} 万方</span>
                                        </div>
                                    </List.Item>
                                );
                            }}
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'permits',
            label: '处置证清单',
            children: (
                <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                    <List
                        dataSource={[
                            { id: 'CZ-2024-0012', type: '处置证', car: '鲁A·12345', site: '城东一号消纳场', status: '已绑定', expire: '2024-12-31' },
                            { id: 'CZ-2024-0013', type: '处置证', car: '鲁A·54321', site: '高新区临时堆场', status: '已过期', expire: '2024-01-31' },
                        ]}
                        renderItem={item => (
                            <List.Item extra={<Button type="link">查看详情</Button>}>
                                <List.Item.Meta
                                    title={
                                        <Space>
                                            <span className="text-slate-900 dark:text-white">{item.id}</span>
                                            <Tag color={item.status === '已绑定' ? 'success' : 'error'}>{item.status}</Tag>
                                        </Space>
                                    }
                                    description={
                                        <Space className="text-slate-600 dark:text-slate-400 mt-2" size="large">
                                            <span>关联车辆: {item.car}</span>
                                            <span>消纳场地: {item.site}</span>
                                            <span className={item.status === '已过期' ? 'text-red-600 dark:text-red-400' : ''}>有效期至: {item.expire}</span>
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
            key: 'daily',
            label: '项目日报与违规',
            children: (
                <div className="space-y-6">
                    <Card title="违规记录" className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Timeline
                            items={[
                                {
                                    color: 'red',
                                    children: (
                                        <>
                                            <p className="text-slate-900 dark:text-white mb-1">车辆偏航预警 - 鲁A·12345</p>
                                            <p className="text-slate-600 dark:text-slate-400 text-sm">2024-03-05 14:30:00</p>
                                        </>
                                    ),
                                },
                                {
                                    color: 'orange',
                                    children: (
                                        <>
                                            <p className="text-slate-900 dark:text-white mb-1">未打卡入场 - 鲁A·54321</p>
                                            <p className="text-slate-600 dark:text-slate-400 text-sm">2024-03-04 09:15:00</p>
                                        </>
                                    ),
                                }
                            ]}
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'config',
            label: '项目配置',
            children: (
                <div className="space-y-6">
                    <Card title="打卡与位置判断配置" className="glass-panel border-slate-200 dark:border-slate-700/50">
                        <Descriptions column={1} labelStyle={{ width: '200px' }}>
                            <Descriptions.Item label="车辆位置校验">
                                <Switch defaultChecked /> <span className="ml-2 text-slate-600 dark:text-slate-300">开启后打卡将校验车辆GPS与场地距离</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="人员位置校验">
                                <Switch defaultChecked /> <span className="ml-2 text-slate-600 dark:text-slate-300">开启后打卡将校验手机定位与场地距离</span>
                            </Descriptions.Item>
                            <Descriptions.Item label="位置偏差阈值 (米)">
                                <InputNumber defaultValue={100} min={10} max={500} />
                            </Descriptions.Item>
                            <Descriptions.Item label="出土预扣值 (m³)">
                                <InputNumber defaultValue={500} min={0} /> <span className="ml-2 text-slate-600 dark:text-slate-300">剩余量低于此值时拦截打卡</span>
                            </Descriptions.Item>
                        </Descriptions>
                        <div className="mt-4">
                            <Button type="primary">保存配置</Button>
                        </div>
                    </Card>
                    
                    <Card title="违规围栏与线路配置" className="glass-panel border-slate-200 dark:border-slate-700/50" extra={<Button type="primary" size="small">新增围栏/线路</Button>}>
                        <List
                            dataSource={[
                                { name: 'A区入场围栏', type: '入场围栏', status: '启用' },
                                { name: '主干道运输线路', type: '运输线路', status: '启用', rule: '偏航>100米预警' },
                            ]}
                            renderItem={item => (
                                <List.Item actions={[<a key="edit">编辑</a>, <a key="delete" className="text-red-600 dark:text-red-500">删除</a>]}>
                                    <List.Item.Meta
                                        title={<span className="text-slate-900 dark:text-white">{item.name}</span>}
                                        description={
                                            <Space className="text-slate-600 dark:text-slate-400 mt-1">
                                                <Tag>{item.type}</Tag>
                                                {item.rule && <span>{item.rule}</span>}
                                            </Space>
                                        }
                                    />
                                    <Switch checked={item.status === '启用'} size="small" />
                                </List.Item>
                            )}
                        />
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
                    onClick={() => navigate('/projects')}
                    className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:text-white"
                />
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">{projectInfo.name}</h1>
                    <p className="text-slate-600 dark:text-slate-400 mt-1">项目编号: {projectInfo.id}</p>
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

export default ProjectDetail;
