import React, { useState, useEffect } from 'react';
import { Card, Tabs, Table, Input, DatePicker, Button, Tag } from 'antd';
import { SearchOutlined, ExportOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import request from '../utils/request';

const { TabPane } = Tabs;
const { RangePicker } = DatePicker;

const SystemLogs: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [loginLogs, setLoginLogs] = useState<any[]>([]);
    const [operateLogs, setOperateLogs] = useState<any[]>([]);
    const [loginLoading, setLoginLoading] = useState(false);
    const [operateLoading, setOperateLoading] = useState(false);
    const [loginPagination, setLoginPagination] = useState({ current: 1, pageSize: 10, total: 0 });
    const [operatePagination, setOperatePagination] = useState({ current: 1, pageSize: 10, total: 0 });

    const fetchLoginLogs = async (pageNo = 1, pageSize = 10) => {
        setLoginLoading(true);
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const res = await request.get('/login-logs', { params: { tenantId: userInfo.tenantId || '1', pageNo, pageSize } });
            if (res.code === 200 && res.data) {
                const records = (res.data.records || []).map((r: any) => ({
                    id: r.id,
                    account: r.username,
                    time: r.loginTime,
                    ip: r.ip,
                    browser: '-',
                    os: '-',
                    status: r.success ? '成功' : (r.failReason ? `失败 (${r.failReason})` : '失败'),
                }));
                setLoginLogs(records);
                setLoginPagination(prev => ({ ...prev, total: res.data.total || 0 }));
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoginLoading(false);
        }
    };

    const fetchOperateLogs = async (pageNo = 1, pageSize = 10) => {
        setOperateLoading(true);
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const res = await request.get('/operation-logs', { params: { tenantId: userInfo.tenantId || '1', pageNo, pageSize } });
            if (res.code === 200 && res.data) {
                const records = (res.data.records || []).map((r: any) => ({
                    id: r.id,
                    operator: r.username,
                    module: r.module,
                    action: r.operation,
                    content: r.requestUri || `${r.method || ''} ${r.requestUri || ''}`.trim() || '-',
                    time: r.createTime,
                    ip: r.ip,
                }));
                setOperateLogs(records);
                setOperatePagination(prev => ({ ...prev, total: res.data.total || 0 }));
            }
        } catch (e) {
            console.error(e);
        } finally {
            setOperateLoading(false);
        }
    };

    useEffect(() => { fetchLoginLogs(loginPagination.current, loginPagination.pageSize); }, [loginPagination.current, loginPagination.pageSize]);
    useEffect(() => { fetchOperateLogs(operatePagination.current, operatePagination.pageSize); }, [operatePagination.current, operatePagination.pageSize]);

    const loginColumns = [
        { title: '登录账号', dataIndex: 'account', key: 'account', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '登录时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: 'IP 地址', dataIndex: 'ip', key: 'ip', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '终端信息', key: 'device', render: (_: any, r: any) => <span className="g-text-secondary text-sm">{r.os} / {r.browser}</span> },
        { 
            title: '状态', 
            dataIndex: 'status', 
            key: 'status',
            render: (status: string) => <Tag color={status === '成功' ? 'success' : 'error'} className="border-none">{status}</Tag>
        },
    ];

    const operateColumns = [
        { title: '操作人', dataIndex: 'operator', key: 'operator', render: (t: string) => <strong className="g-text-primary">{t}</strong> },
        { title: '操作模块', dataIndex: 'module', key: 'module', render: (t: string) => <Tag color="blue" className="border-none">{t}</Tag> },
        { title: '操作类型', dataIndex: 'action', key: 'action', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '操作内容', dataIndex: 'content', key: 'content', render: (t: string) => <span className="g-text-secondary">{t}</span> },
        { title: '操作时间', dataIndex: 'time', key: 'time', render: (t: string) => <span className="g-text-secondary font-mono">{t}</span> },
        { title: 'IP 地址', dataIndex: 'ip', key: 'ip', render: (t: string) => <span className="g-text-secondary text-sm">{t}</span> },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">系统日志</h1>
                    <p className="g-text-secondary mt-1">查询系统登录日志、操作行为审计及系统错误日志</p>
                </div>
            </div>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <Tabs defaultActiveKey="1" className="custom-tabs px-6 pt-4">
                    <TabPane tab="操作日志" key="1">
                        <div className="pb-4 flex flex-wrap gap-4">
                            <Input placeholder="搜索操作人/模块/内容" prefix={<SearchOutlined className="g-text-secondary" />} className="w-64 bg-white g-border-panel border g-text-primary" value={searchText} onChange={e => setSearchText(e.target.value)} />
                            <RangePicker className="bg-white g-border-panel border g-text-primary" />
                            <div className="flex-1 flex justify-end">
                                <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出</Button>
                            </div>
                        </div>
                        <Table 
                            columns={operateColumns} 
                            dataSource={operateLogs.filter((l: any) => !searchText || (l.operator && l.operator.includes(searchText)) || (l.content && l.content.includes(searchText)))} 
                            rowKey="id"
                            loading={operateLoading}
                            pagination={{
                                current: operatePagination.current,
                                pageSize: operatePagination.pageSize,
                                total: operatePagination.total,
                                className: 'pb-4',
                                showSizeChanger: true,
                                onChange: (page, size) => setOperatePagination(prev => ({ ...prev, current: page, pageSize: size || 10 })),
                            }}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </TabPane>
                    <TabPane tab="登录日志" key="2">
                        <div className="pb-4 flex flex-wrap gap-4">
                            <Input placeholder="搜索账号/IP" prefix={<SearchOutlined className="g-text-secondary" />} className="w-64 bg-white g-border-panel border g-text-primary" />
                            <RangePicker className="bg-white g-border-panel border g-text-primary" />
                            <div className="flex-1 flex justify-end">
                                <Button icon={<ExportOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">导出</Button>
                            </div>
                        </div>
                        <Table 
                            columns={loginColumns} 
                            dataSource={loginLogs} 
                            rowKey="id"
                            loading={loginLoading}
                            pagination={{
                                current: loginPagination.current,
                                pageSize: loginPagination.pageSize,
                                total: loginPagination.total,
                                className: 'pb-4',
                                showSizeChanger: true,
                                onChange: (page, size) => setLoginPagination(prev => ({ ...prev, current: page, pageSize: size || 10 })),
                            }}
                            className="bg-transparent"
                            rowClassName="hover:bg-white transition-colors"
                        />
                    </TabPane>
                    <TabPane tab="错误日志" key="3">
                        <div className="p-10 text-center g-text-secondary">
                            暂无系统异常错误记录
                        </div>
                    </TabPane>
                </Tabs>
            </Card>
        </motion.div>
    );
};

export default SystemLogs;
