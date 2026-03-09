import React, { useState } from 'react';
import { Card, List, Table, Button, Input, Space, Switch } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const dictTypes = [
    { id: 'contract_type', name: '合同类型', code: 'DICT_CONTRACT_TYPE' },
    { id: 'site_type', name: '场地类型', code: 'DICT_SITE_TYPE' },
    { id: 'vehicle_status', name: '车辆状态', code: 'DICT_VEHICLE_STATUS' },
    { id: 'alert_level', name: '预警等级', code: 'DICT_ALERT_LEVEL' },
];

const dictDataMap: Record<string, any[]> = {
    'contract_type': [
        { id: 1, label: '正常合同', value: 'normal', sort: 1, status: true },
        { id: 2, label: '三方合同', value: 'tripartite', sort: 2, status: true },
        { id: 3, label: '租赁合同', value: 'lease', sort: 3, status: true },
        { id: 4, label: '用工合同', value: 'labor', sort: 4, status: false },
    ]
};

const Dictionary: React.FC = () => {
    const [selectedType, setSelectedType] = useState(dictTypes[0]);

    const columns = [
        { title: '字典标签', dataIndex: 'label', key: 'label', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '字典键值', dataIndex: 'value', key: 'value', render: (t: string) => <span className="g-text-primary-link font-mono">{t}</span> },
        { title: '排序', dataIndex: 'sort', key: 'sort', render: (t: number) => <span className="g-text-secondary">{t}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: boolean) => <Switch checked={status} size="small" />
        },
        { 
            title: '操作', 
            key: 'action', 
            render: () => (
                <Space size="middle">
                    <a className="g-text-primary-link hover:g-text-primary-link"><EditOutlined /></a>
                    <a className="g-text-error hover:g-text-error"><DeleteOutlined /></a>
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6 h-[calc(100vh-110px)] flex flex-col">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">数据字典</h1>
                    <p className="g-text-secondary mt-1">维护系统各业务模块的下拉选项及通用配置数据</p>
                </div>
            </div>

            <div className="flex gap-6 flex-1 min-h-0">
                {/* 左侧字典类型 */}
                <Card className="glass-panel g-border-panel border w-80 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between items-center g-bg-toolbar">
                        <Input placeholder="搜索字典类型" prefix={<SearchOutlined className="g-text-secondary" />} className="bg-white g-border-panel border g-text-primary" />
                    </div>
                    <div className="flex-1 overflow-auto p-2">
                        <List
                            dataSource={dictTypes}
                            renderItem={item => (
                                <List.Item 
                                    className={`px-4 py-3 cursor-pointer rounded mb-1 transition-colors border-none ${selectedType.id === item.id ? 'bg-blue-600/20 border border-blue-500/50' : 'hover:bg-white'}`}
                                    onClick={() => setSelectedType(item)}
                                >
                                    <div className="w-full">
                                        <div className="font-bold g-text-primary mb-1">{item.name}</div>
                                        <div className="text-xs g-text-secondary font-mono">{item.code}</div>
                                    </div>
                                </List.Item>
                            )}
                        />
                    </div>
                </Card>

                {/* 右侧字典数据 */}
                <Card className="glass-panel g-border-panel border flex-1 flex flex-col" bodyStyle={{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between items-center g-bg-toolbar">
                        <span className="g-text-primary font-bold">【{selectedType.name}】数据列表</span>
                        <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none">
                            新增字典项
                        </Button>
                    </div>
                    <div className="flex-1 overflow-auto p-4">
                        <Table 
                            columns={columns} 
                            dataSource={dictDataMap[selectedType.id] || []} 
                            rowKey="id"
                            pagination={false}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </div>
                </Card>
            </div>
        </motion.div>
    );
};

export default Dictionary;
