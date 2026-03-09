import React, { useState } from 'react';
import { Card, Input, Button, Tag, Row, Col, Avatar, Tooltip, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { SearchOutlined, PlusOutlined, MoreOutlined, UserOutlined, CarOutlined, PhoneOutlined, ScheduleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const fleetData = [
    {
        id: 'F001',
        name: '宏基第一先锋车队',
        company: '宏基渣土运输公司',
        captain: '张建国',
        phone: '13800138001',
        driverCount: 45,
        vehicleCount: 40,
        status: '出车',
        activeVehicles: 38,
    },
    {
        id: 'F002',
        name: '宏基夜间突击队',
        company: '宏基渣土运输公司',
        captain: '李志强',
        phone: '13900139002',
        driverCount: 30,
        vehicleCount: 30,
        status: '待命',
        activeVehicles: 0,
    },
    {
        id: 'F003',
        name: '顺达一队',
        company: '顺达土方工程队',
        captain: '王海波',
        phone: '13700137003',
        driverCount: 25,
        vehicleCount: 22,
        status: '出车',
        activeVehicles: 20,
    },
    {
        id: 'F004',
        name: '捷安特种运输队',
        company: '捷安运输',
        captain: '赵铁柱',
        phone: '13600136004',
        driverCount: 15,
        vehicleCount: 15,
        status: '休整',
        activeVehicles: 0,
    },
    {
        id: 'F005',
        name: '新思路快运一队',
        company: '新思路运输',
        captain: '孙大伟',
        phone: '13500135005',
        driverCount: 35,
        vehicleCount: 32,
        status: '出车',
        activeVehicles: 30,
    },
];

const FleetManagement: React.FC = () => {
    const [searchTerm, setSearchTerm] = useState('');

    const filteredFleets = fleetData.filter(fleet =>
        fleet.name.includes(searchTerm) || fleet.company.includes(searchTerm) || fleet.captain.includes(searchTerm)
    );

    const getStatusColor = (status: string) => {
        switch (status) {
            case '出车': return 'green';
            case '待命': return 'blue';
            case '休整': return 'default';
            default: return 'default';
        }
    };

    const actionItems: MenuProps['items'] = [
        { key: 'detail', label: '车队详情' },
        { key: 'edit', label: '编辑信息' },
        { type: 'divider' },
        { key: 'delete', label: '解散车队', danger: true },
    ];

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">车队管理</h1>
                    <p className="g-text-secondary mt-1">管理各运输单位下属的车队组织、人员及排班出勤情况</p>
                </div>
                <div className="flex gap-3">
                    <Input
                        placeholder="搜索车队名称/队长/所属单位"
                        prefix={<SearchOutlined className="g-text-secondary" />}
                        className="w-72 glass-panel g-border-panel border g-text-primary"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                    <Button type="primary" icon={<PlusOutlined />} className="g-btn-primary border-none shadow-[0_0_15px_rgba(37,99,235,0.4)]">
                        新增车队
                    </Button>
                </div>
            </div>

            <Row gutter={[24, 24]}>
                {filteredFleets.map((fleet, index) => (
                    <Col xs={24} sm={12} lg={8} xl={6} key={fleet.id}>
                        <motion.div
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.3, delay: index * 0.1 }}
                        >
                            <Card
                                className="glass-panel hover:-translate-y-1 transition-transform duration-300 g-border-panel border hover:border-blue-500/50"
                                bodyStyle={{ padding: '20px' }}
                                actions={[
                                    <Tooltip title="排班管理"><ScheduleOutlined key="schedule" className="g-text-secondary hover:g-text-primary-link" /></Tooltip>,
                                    <Tooltip title="快捷出勤"><CarOutlined key="dispatch" className="g-text-secondary hover:g-text-success" /></Tooltip>,
                                    <Dropdown menu={{ items: actionItems }} trigger={['click']}>
                                        <MoreOutlined key="more" className="g-text-secondary hover:g-text-primary" />
                                    </Dropdown>
                                ]}
                            >
                                <div className="flex justify-between items-start mb-4">
                                    <div className="flex flex-col gap-1">
                                        <h3 className="text-lg font-bold g-text-primary m-0 truncate w-40" title={fleet.name}>{fleet.name}</h3>
                                        <span className="text-xs g-text-secondary">{fleet.company}</span>
                                    </div>
                                    <Tag color={getStatusColor(fleet.status)} className="border-none m-0">{fleet.status}</Tag>
                                </div>

                                <div className="flex items-center gap-3 mb-4 p-3 bg-white rounded-lg border g-border-panel border">
                                    <Avatar icon={<UserOutlined />} className="bg-blue-500/20 g-text-primary-link" />
                                    <div className="flex flex-col">
                                        <span className="g-text-primary text-sm font-bold">队长: {fleet.captain}</span>
                                        <span className="g-text-secondary text-xs flex items-center gap-1"><PhoneOutlined /> {fleet.phone}</span>
                                    </div>
                                </div>

                                <div className="flex justify-between mt-4">
                                    <div className="text-center w-1/2 border-r g-border-panel border">
                                        <div className="text-2xl font-bold g-text-primary">{fleet.driverCount}</div>
                                        <div className="text-xs g-text-secondary">司机人数</div>
                                    </div>
                                    <div className="text-center w-1/2">
                                        <div className="text-2xl font-bold g-text-primary-link">{fleet.activeVehicles}<span className="text-sm g-text-secondary font-normal">/{fleet.vehicleCount}</span></div>
                                        <div className="text-xs g-text-secondary">出车/总车辆</div>
                                    </div>
                                </div>
                            </Card>
                        </motion.div>
                    </Col>
                ))}
            </Row>
        </div>
    );
};

export default FleetManagement;
