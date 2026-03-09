import React from 'react';
import { Card, Tabs, Descriptions, Tag, Button, Space, Timeline, List, Row, Col, Table } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined, DownloadOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const ContractDetail: React.FC = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    // 模拟数据
    const contractInfo = {
        id: id || 'HT-2401-0012',
        type: '正常合同',
        project: '滨海新区基础建设B标段',
        builder: '中建八局',
        transporter: '顺达土方工程队',
        site: '东区临时消纳场',
        amount: 3500000,
        unearned: 2100000,
        volume: 150000,
        priceIn: 25,
        priceOut: 30,
        status: '生效',
        date: '2024-01-10',
        expire: '2024-12-31'
    };

    const items = [
        {
            key: 'info',
            label: '基础信息',
            children: (
                <div className="space-y-6">
                    <Card title="合同基础信息" className="glass-panel g-border-panel border" extra={<Button type="link" icon={<EditOutlined />}>编辑草稿</Button>}>
                        <Descriptions column={3} className="g-text-secondary">
                            <Descriptions.Item label="合同编号">{contractInfo.id}</Descriptions.Item>
                            <Descriptions.Item label="合同类型"><Tag color="blue">{contractInfo.type}</Tag></Descriptions.Item>
                            <Descriptions.Item label="合同状态"><Tag color="green">{contractInfo.status}</Tag></Descriptions.Item>
                            
                            <Descriptions.Item label="关联项目" span={2}>{contractInfo.project}</Descriptions.Item>
                            <Descriptions.Item label="约定消纳场">{contractInfo.site}</Descriptions.Item>
                            
                            <Descriptions.Item label="建设单位">{contractInfo.builder}</Descriptions.Item>
                            <Descriptions.Item label="运输单位" span={2}>{contractInfo.transporter}</Descriptions.Item>
                            
                            <Descriptions.Item label="合同总金额">¥ {(contractInfo.amount / 10000).toFixed(2)} 万</Descriptions.Item>
                            <Descriptions.Item label="约定方量">{contractInfo.volume.toLocaleString()} m³</Descriptions.Item>
                            <Descriptions.Item label="单价配置">区内: ¥{contractInfo.priceIn} / 区外: ¥{contractInfo.priceOut}</Descriptions.Item>
                            
                            <Descriptions.Item label="签订日期">{contractInfo.date}</Descriptions.Item>
                            <Descriptions.Item label="有效期限">{contractInfo.expire}</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    
                    <Card title="资金流向" className="glass-panel g-border-panel border">
                        <Row gutter={16}>
                            <Col span={8}>
                                <Card type="inner" className="bg-white g-border-panel border">
                                    <div className="g-text-secondary mb-2">合同总金额 (元)</div>
                                    <div className="text-2xl font-bold g-text-primary">¥ {(contractInfo.amount).toLocaleString()}</div>
                                </Card>
                            </Col>
                            <Col span={8}>
                                <Card type="inner" className="bg-white g-border-panel border">
                                    <div className="g-text-secondary mb-2">累计已入账 (元)</div>
                                    <div className="text-2xl font-bold g-text-success">¥ {(contractInfo.amount - contractInfo.unearned).toLocaleString()}</div>
                                </Card>
                            </Col>
                            <Col span={8}>
                                <Card type="inner" className="bg-white g-border-panel border">
                                    <div className="g-text-secondary mb-2">剩余待入账 (元)</div>
                                    <div className="text-2xl font-bold g-text-warning">¥ {(contractInfo.unearned).toLocaleString()}</div>
                                </Card>
                            </Col>
                        </Row>
                    </Card>
                </div>
            ),
        },
        {
            key: 'approval',
            label: '审批流程',
            children: (
                <Card className="glass-panel g-border-panel border">
                    <Timeline
                        items={[
                            {
                                color: 'green',
                                dot: <CheckCircleOutlined className="text-lg" />,
                                children: (
                                    <div className="mb-4">
                                        <p className="g-text-primary font-bold mb-1">合同发起申请</p>
                                        <p className="g-text-secondary text-sm">发起人: 王经办 <span className="ml-4">2024-01-08 10:00:00</span></p>
                                    </div>
                                ),
                            },
                            {
                                color: 'green',
                                dot: <CheckCircleOutlined className="text-lg" />,
                                children: (
                                    <div className="mb-4">
                                        <p className="g-text-primary font-bold mb-1">项目部初审</p>
                                        <p className="g-text-secondary text-sm">审批人: 李经理 <span className="ml-4">2024-01-08 14:30:00</span></p>
                                        <div className="bg-white p-2 rounded mt-2 g-text-secondary text-sm border g-border-panel border">审批意见: 同意，单价符合标准。</div>
                                    </div>
                                ),
                            },
                            {
                                color: 'green',
                                dot: <CheckCircleOutlined className="text-lg" />,
                                children: (
                                    <div className="mb-4">
                                        <p className="g-text-primary font-bold mb-1">财务复核</p>
                                        <p className="g-text-secondary text-sm">审批人: 张财务 <span className="ml-4">2024-01-09 09:15:00</span></p>
                                        <div className="bg-white p-2 rounded mt-2 g-text-secondary text-sm border g-border-panel border">审批意见: 资金预算已确认。</div>
                                    </div>
                                ),
                            },
                            {
                                color: 'blue',
                                dot: <ClockCircleOutlined className="text-lg" />,
                                children: (
                                    <div className="mb-4">
                                        <p className="g-text-primary font-bold mb-1">合同生效</p>
                                        <p className="g-text-secondary text-sm">系统自动流转 <span className="ml-4">2024-01-10 00:00:00</span></p>
                                    </div>
                                ),
                            }
                        ]}
                    />
                </Card>
            ),
        },
        {
            key: 'payments',
            label: '入账与发票',
            children: (
                <div className="space-y-6">
                    <Card title="入账记录" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">新增入账</Button>}>
                        <Table
                            dataSource={[
                                { id: 'RZ-202401-001', time: '2024-01-15 10:00:00', amount: 500000, voucher: 'PZ-8892101', status: '已入账', operator: '张财务' },
                                { id: 'RZ-202402-015', time: '2024-02-20 14:30:00', amount: 900000, voucher: 'PZ-8892155', status: '已入账', operator: '张财务' },
                            ]}
                            columns={[
                                { title: '入账单号', dataIndex: 'id', key: 'id' },
                                { title: '入账时间', dataIndex: 'time', key: 'time' },
                                { title: '入账金额(元)', dataIndex: 'amount', key: 'amount', render: (val) => <span className="g-text-success font-bold">¥ {val.toLocaleString()}</span> },
                                { title: '凭证号', dataIndex: 'voucher', key: 'voucher' },
                                { title: '操作人', dataIndex: 'operator', key: 'operator' },
                                { title: '状态', dataIndex: 'status', key: 'status', render: (s) => <Tag color="success">{s}</Tag> },
                            ]}
                            pagination={false}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </Card>
                    
                    <Card title="发票记录" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small">开具发票</Button>}>
                        <Table
                            dataSource={[
                                { id: 'FP-202401-001', code: '031002200111', number: '12345678', amount: 500000, date: '2024-01-16', type: '增值税专用发票' },
                            ]}
                            columns={[
                                { title: '发票编号', dataIndex: 'id', key: 'id' },
                                { title: '发票代码', dataIndex: 'code', key: 'code' },
                                { title: '发票号码', dataIndex: 'number', key: 'number' },
                                { title: '开票金额(元)', dataIndex: 'amount', key: 'amount', render: (val) => <span>¥ {val.toLocaleString()}</span> },
                                { title: '发票类型', dataIndex: 'type', key: 'type' },
                                { title: '开票日期', dataIndex: 'date', key: 'date' },
                                { title: '操作', key: 'action', render: () => <a className="g-text-primary-link">查看附件</a> },
                            ]}
                            pagination={false}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'docs',
            label: '办事材料',
            children: (
                <Card className="glass-panel g-border-panel border">
                    <List
                        dataSource={[
                            { name: '渣土消纳三方协议扫描件.pdf', status: '已审核', required: true, time: '2024-01-08' },
                            { name: '施工单位资质证明.jpg', status: '已审核', required: true, time: '2024-01-08' },
                            { name: '场地评估报告.docx', status: '已审核', required: false, time: '2024-01-08' },
                        ]}
                        renderItem={item => (
                            <List.Item actions={[<a key="preview">预览</a>, <a key="download"><DownloadOutlined /> 下载</a>]}>
                                <List.Item.Meta
                                    title={
                                        <Space>
                                            <span className="g-text-primary">{item.name}</span>
                                            {item.required && <Tag color="red" className="border-none">必填</Tag>}
                                            <Tag color="success" className="border-none">{item.status}</Tag>
                                        </Space>
                                    }
                                    description={<span className="g-text-secondary">上传时间: {item.time}</span>}
                                />
                            </List.Item>
                        )}
                    />
                </Card>
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
                    onClick={() => navigate('/contracts')}
                    className="g-text-secondary hover:g-text-primary"
                />
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">合同详情: {contractInfo.id}</h1>
                    <p className="g-text-secondary mt-1">{contractInfo.project} - {contractInfo.site}</p>
                </div>
            </div>

            <Tabs 
                defaultActiveKey="info" 
                items={items} 
                className="custom-tabs"
            />
        </motion.div>
    );
};

export default ContractDetail;
