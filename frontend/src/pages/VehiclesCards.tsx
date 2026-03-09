import React, { useState } from 'react';
import { Card, Table, Button, Input, Select, Tag, Space, Modal, Form, InputNumber, Row, Col, Statistic } from 'antd';
import { SearchOutlined, PlusOutlined, CreditCardOutlined, WalletOutlined } from '@ant-design/icons';

const { Option } = Select;

const cardsData = [
    { id: 'CARD-889922', type: '油卡', company: '顺达运输公司', plate: '京A·12345', balance: 5200.50, totalRecharge: 20000, totalConsume: 14799.50, status: '正常' },
    { id: 'CARD-889923', type: '电卡', company: '绿能物流', plate: '京B·67890', balance: 150.00, totalRecharge: 5000, totalConsume: 4850.00, status: '余额不足' },
    { id: 'CARD-889924', type: '油卡', company: '顺达运输公司', plate: '-', balance: 0, totalRecharge: 0, totalConsume: 0, status: '未绑定' },
    { id: 'CARD-889925', type: '电卡', company: '宏远车队', plate: '京C·11223', balance: 3400.00, totalRecharge: 10000, totalConsume: 6600.00, status: '正常' },
];

const VehiclesCards: React.FC = () => {
    const [isRechargeModalVisible, setIsRechargeModalVisible] = useState(false);
    const [isBindModalVisible, setIsBindModalVisible] = useState(false);
    const [currentCard, setCurrentCard] = useState<any>(null);
    const [form] = Form.useForm();

    const columns = [
        { title: '卡号', dataIndex: 'id', key: 'id', render: (text: string) => <span className="font-mono text-slate-600 dark:text-slate-300">{text}</span> },
        { title: '卡类型', dataIndex: 'type', key: 'type', render: (text: string) => <Tag color={text === '油卡' ? 'orange' : 'blue'}>{text}</Tag> },
        { title: '所属单位', dataIndex: 'company', key: 'company' },
        { title: '绑定车牌', dataIndex: 'plate', key: 'plate', render: (text: string) => text === '-' ? <span className="text-slate-600 dark:text-slate-400">未绑定</span> : <Tag color="default">{text}</Tag> },
        { title: '当前余额 (元)', dataIndex: 'balance', key: 'balance', render: (val: number) => <span className={`font-bold ${val < 500 ? 'text-red-600 dark:text-red-500' : 'text-green-600 dark:text-green-500'}`}>{val.toFixed(2)}</span> },
        { title: '累计充值 (元)', dataIndex: 'totalRecharge', key: 'totalRecharge', render: (val: number) => val.toFixed(2) },
        { title: '累计消费 (元)', dataIndex: 'totalConsume', key: 'totalConsume', render: (val: number) => val.toFixed(2) },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => (
                <Tag color={status === '正常' ? 'green' : status === '余额不足' ? 'red' : 'default'}>{status}</Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: any) => (
                <Space size="small">
                    <Button type="link" size="small" onClick={() => { setCurrentCard(record); setIsRechargeModalVisible(true); }}>充值</Button>
                    {record.plate === '-' ? (
                        <Button type="link" size="small" onClick={() => { setCurrentCard(record); setIsBindModalVisible(true); }}>绑定</Button>
                    ) : (
                        <Button type="link" size="small" danger>解绑</Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold text-slate-900 dark:text-white m-0">油电卡管理</h1>
            </div>

            <Row gutter={[24, 24]}>
                <Col span={8}>
                    <Card className="glass-panel border-l-4 border-l-blue-500">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-400">发卡总数 (张)</span>} value={125} prefix={<CreditCardOutlined className="text-blue-600 dark:text-blue-500" />} valueStyle={{ color: 'var(--text-primary)' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-l-4 border-l-orange-500">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-400">油卡总余额 (元)</span>} value={145800.50} precision={2} prefix={<WalletOutlined className="text-orange-600 dark:text-orange-500" />} valueStyle={{ color: 'var(--text-primary)' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card className="glass-panel border-l-4 border-l-green-500">
                        <Statistic title={<span className="text-slate-600 dark:text-slate-400">电卡总余额 (元)</span>} value={89200.00} precision={2} prefix={<WalletOutlined className="text-green-600 dark:text-green-500" />} valueStyle={{ color: 'var(--text-primary)' }} />
                    </Card>
                </Col>
            </Row>

            <Card className="glass-panel border-slate-200 dark:border-slate-700/50">
                <div className="flex justify-between mb-4">
                    <Space>
                        <Input placeholder="搜索卡号/车牌" prefix={<SearchOutlined />} className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white w-64" />
                        <Select defaultValue="all" className="w-32 bg-white dark:bg-slate-800">
                            <Option value="all">全部类型</Option>
                            <Option value="油卡">油卡</Option>
                            <Option value="电卡">电卡</Option>
                        </Select>
                        <Select defaultValue="all" className="w-40 bg-white dark:bg-slate-800">
                            <Option value="all">全部运输单位</Option>
                            <Option value="顺达">顺达运输公司</Option>
                            <Option value="绿能">绿能物流</Option>
                        </Select>
                        <Button type="primary">查询</Button>
                    </Space>
                    <Button type="primary" icon={<PlusOutlined />}>新增卡片</Button>
                </div>

                <Table 
                    columns={columns} 
                    dataSource={cardsData} 
                    className="bg-transparent"
                    rowClassName="hover:bg-white dark:bg-slate-800/50 transition-colors"
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            {/* 充值弹窗 */}
            <Modal
                title="卡片充值"
                open={isRechargeModalVisible}
                onOk={() => { setIsRechargeModalVisible(false); form.resetFields(); }}
                onCancel={() => { setIsRechargeModalVisible(false); form.resetFields(); }}
                className="dark-modal"
            >
                <Form form={form} layout="vertical">
                    <Form.Item label="当前卡号">
                        <Input disabled value={currentCard?.id} className="bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300" />
                    </Form.Item>
                    <Form.Item label="当前余额">
                        <Input disabled value={`¥ ${currentCard?.balance?.toFixed(2)}`} className="bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300" />
                    </Form.Item>
                    <Form.Item name="amount" label="充值金额 (元)" rules={[{ required: true }]}>
                        <InputNumber className="w-full" placeholder="请输入充值金额" min={0.01} />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 绑定弹窗 */}
            <Modal
                title="绑定车辆"
                open={isBindModalVisible}
                onOk={() => { setIsBindModalVisible(false); form.resetFields(); }}
                onCancel={() => { setIsBindModalVisible(false); form.resetFields(); }}
                className="dark-modal"
            >
                <Form form={form} layout="vertical">
                    <Form.Item label="当前卡号">
                        <Input disabled value={currentCard?.id} className="bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300" />
                    </Form.Item>
                    <Form.Item name="plate" label="选择车辆" rules={[{ required: true }]}>
                        <Select placeholder="请选择要绑定的车牌号" className="w-full">
                            <Option value="京A·99999">京A·99999</Option>
                            <Option value="京B·88888">京B·88888</Option>
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default VehiclesCards;
