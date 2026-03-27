import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Input,
  Button,
  DatePicker,
  Select,
  Space,
  Modal,
  Form,
  InputNumber,
  message,
  Drawer,
  Descriptions,
} from 'antd';
import { SearchOutlined, FilterOutlined, UnlockOutlined, StopOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import {
  disableVehicle,
  fetchVehicleViolationDetail,
  fetchVehicleViolationSummary,
  fetchVehicleViolations,
  fetchVehicles,
  releaseVehicleViolation,
  type VehicleViolationDetailRecord,
  type VehicleRecord,
  type VehicleViolationRecord,
} from '../utils/vehicleApi';

const { RangePicker } = DatePicker;
const { Option } = Select;

const ViolationsList: React.FC = () => {
    const [searchText, setSearchText] = useState('');
    const [violationType, setViolationType] = useState<string>('all');
    const [actionStatus, setActionStatus] = useState<string>('all');
    const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null);
    const [loading, setLoading] = useState(false);
    const [records, setRecords] = useState<VehicleViolationRecord[]>([]);
    const [pageNo, setPageNo] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [disableOpen, setDisableOpen] = useState(false);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [vehicleOptions, setVehicleOptions] = useState<VehicleRecord[]>([]);
    const [summary, setSummary] = useState({
        totalCount: 0,
        pendingCount: 0,
        processedCount: 0,
        disabledCount: 0,
        releasedCount: 0,
        vehicleCount: 0,
    });
    const [detailOpen, setDetailOpen] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailRecord, setDetailRecord] = useState<VehicleViolationDetailRecord | null>(null);
    const [disableForm] = Form.useForm();

    const loadData = async (currentPage = pageNo, currentPageSize = pageSize) => {
        setLoading(true);
        try {
            const queryParams = {
                keyword: searchText || undefined,
                violationType: violationType !== 'all' ? violationType : undefined,
                actionStatus: actionStatus !== 'all' ? actionStatus : undefined,
                startTime: dateRange?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
                endTime: dateRange?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
            };
            const [violationPage, vehiclePage, violationSummary] = await Promise.all([
                fetchVehicleViolations({
                    ...queryParams,
                    pageNo: currentPage,
                    pageSize: currentPageSize,
                }),
                fetchVehicles({ pageNo: 1, pageSize: 200 }),
                fetchVehicleViolationSummary(queryParams),
            ]);
            setRecords(violationPage.records || []);
            setTotal(violationPage.total || 0);
            setVehicleOptions(vehiclePage.records || []);
            setSummary(violationSummary);
            setPageNo(currentPage);
            setPageSize(currentPageSize);
        } catch (error) {
            console.error(error);
            message.error('获取违规车辆清单失败');
            setRecords([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadData(1, pageSize);
    }, [searchText, violationType, actionStatus, dateRange]);

    const openDetail = async (record: VehicleViolationRecord) => {
        setDetailOpen(true);
        setDetailLoading(true);
        try {
            setDetailRecord(await fetchVehicleViolationDetail(record.id));
        } catch (error) {
            console.error(error);
            message.error('获取违法详情失败');
            setDetailRecord(null);
        } finally {
            setDetailLoading(false);
        }
    };

    const handleDisableSubmit = async () => {
        try {
            const values = await disableForm.validateFields();
            setSubmitLoading(true);
            await disableVehicle({
                vehicleId: Number(values.vehicleId),
                violationType: values.violationType,
                triggerLocation: values.triggerLocation,
                penaltyResult: values.penaltyResult,
                banDays: values.banDays,
                remark: values.remark,
                triggerTime: values.triggerTime ? values.triggerTime.format('YYYY-MM-DDTHH:mm:ss') : undefined,
            });
            message.success('车辆已禁用');
            setDisableOpen(false);
            disableForm.resetFields();
            await loadData(1, pageSize);
        } catch (error) {
            if ((error as Error)?.message?.includes('validation')) {
                return;
            }
            console.error(error);
            message.error('车辆禁用失败');
        } finally {
            setSubmitLoading(false);
        }
    };

    const handleRelease = async (record: VehicleViolationRecord) => {
        Modal.confirm({
            title: `确认提前解禁 ${record.plateNo} 吗？`,
            content: '解禁后车辆将恢复为可用状态。',
            okText: '确认解禁',
            cancelText: '取消',
            onOk: async () => {
                await releaseVehicleViolation(record.id, '人工提前解禁');
                message.success('已提前解禁');
                await loadData(pageNo, pageSize);
            },
        });
    };

    const columns = [
        { title: '车牌号', dataIndex: 'plateNo', key: 'plateNo', render: (text: string) => <strong className="g-text-primary-link">{text}</strong> },
        { title: '所属单位', dataIndex: 'orgName', key: 'orgName', render: (text: string) => <span className="g-text-secondary">{text || '--'}</span> },
        { 
            title: '违规类型', 
            dataIndex: 'violationType', 
            key: 'violationType',
            render: (type: string) => {
                let color = 'default';
                if (type === '闯禁区' || type === '证件过期') color = 'red';
                else if (type === '偏航预警' || type === '超速行驶') color = 'orange';
                else if (type === '未打卡入场') color = 'warning';
                return <Tag color={color} className="border-none">{type}</Tag>;
            }
        },
        { title: '违规时间', dataIndex: 'triggerTime', key: 'triggerTime', render: (t: string) => <span className="g-text-secondary font-mono">{t || '-'}</span> },
        { title: '违规地点', dataIndex: 'triggerLocation', key: 'triggerLocation', render: (l: string) => <span className="g-text-secondary">{l || '--'}</span> },
        { 
            title: '处理状态', 
            dataIndex: 'actionStatusLabel', 
            key: 'actionStatusLabel',
            render: (status: string) => {
                const colorMap: Record<string, string> = { '待处理': 'error', '已处理': 'success', '禁用中': 'red', '已解禁': 'default' };
                return <Tag color={colorMap[status] || 'default'} className="border-none">{status}</Tag>;
            }
        },
        { title: '处罚结果', dataIndex: 'penaltyResult', key: 'penaltyResult', render: (p: string) => <span className="g-text-secondary">{p || '--'}</span> },
        { title: '禁用时段', key: 'banRange', render: (_: unknown, record: VehicleViolationRecord) => <span className="g-text-secondary">{record.banStartTime && record.banEndTime ? `${record.banStartTime} ~ ${record.banEndTime}` : '--'}</span> },
        { 
            title: '操作', 
            key: 'action', 
            render: (_: unknown, record: VehicleViolationRecord) => (
                <Space size="middle">
                    <a onClick={() => void openDetail(record)}>查看详情</a>
                    {record.actionStatus === 'DISABLED' && <a className="g-text-success hover:g-text-success" onClick={() => void handleRelease(record)}><UnlockOutlined /> 提前解禁</a>}
                </Space>
            )
        },
    ];

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">违规车辆清单</h1>
                    <p className="g-text-secondary mt-1">记录并处理车辆在运输过程中的各类违规行为及禁用状态</p>
                </div>
                <Button type="primary" icon={<StopOutlined />} onClick={() => setDisableOpen(true)}>禁用车辆</Button>
            </div>

            <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 border-b g-border-panel border g-bg-toolbar">
                    <Card type="inner" className="bg-white g-border-panel border">
                        <div className="g-text-secondary mb-2">违法总数</div>
                        <div className="text-2xl font-bold g-text-primary">{summary.totalCount}</div>
                    </Card>
                    <Card type="inner" className="bg-white g-border-panel border">
                        <div className="g-text-secondary mb-2">待处理 / 禁用中</div>
                        <div className="text-2xl font-bold g-text-warning">{summary.pendingCount} / {summary.disabledCount}</div>
                    </Card>
                    <Card type="inner" className="bg-white g-border-panel border">
                        <div className="g-text-secondary mb-2">已处理 / 已解禁</div>
                        <div className="text-2xl font-bold g-text-success">{summary.processedCount} / {summary.releasedCount}</div>
                    </Card>
                </div>
                <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
                    <Input 
                        placeholder="搜索车牌号" 
                        prefix={<SearchOutlined className="g-text-secondary" />} 
                        className="w-64 bg-white g-border-panel border g-text-primary"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                    />
                    <Select value={violationType} onChange={setViolationType} className="w-40" popupClassName="bg-white">
                        <Option value="all">全部违规类型</Option>
                        <Option value="偏航预警">偏航预警</Option>
                        <Option value="未打卡入场">未打卡入场</Option>
                        <Option value="证件过期">证件过期</Option>
                        <Option value="超速行驶">超速行驶</Option>
                        <Option value="闯禁区">闯禁区</Option>
                    </Select>
                    <Select value={actionStatus} onChange={setActionStatus} className="w-40" popupClassName="bg-white">
                        <Option value="all">全部处理状态</Option>
                        <Option value="PENDING">待处理</Option>
                        <Option value="PROCESSED">已处理</Option>
                        <Option value="DISABLED">禁用中</Option>
                        <Option value="RELEASED">已解禁</Option>
                    </Select>
                    <RangePicker value={dateRange} onChange={(value) => setDateRange(value as [Dayjs, Dayjs] | null)} className="bg-white g-border-panel border g-text-primary" />
                    <div className="flex-1 flex justify-end gap-3">
                        <Button
                            icon={<FilterOutlined />}
                            className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary"
                            onClick={() => {
                                setSearchText('');
                                setViolationType('all');
                                setActionStatus('all');
                                setDateRange(null);
                            }}
                        >
                            重置
                        </Button>
                    </div>
                </div>

                <Table 
                    columns={columns} 
                    loading={loading}
                    dataSource={records} 
                    rowKey="id"
                    pagination={{
                        current: pageNo,
                        pageSize,
                        total,
                        showSizeChanger: true,
                        className: 'pr-4 pb-2',
                        onChange: (current, size) => void loadData(current, size),
                    }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                />
            </Card>

            <Modal
                title="禁用车辆"
                open={disableOpen}
                onCancel={() => {
                    setDisableOpen(false);
                    disableForm.resetFields();
                }}
                onOk={() => void handleDisableSubmit()}
                confirmLoading={submitLoading}
            >
                <Form
                    form={disableForm}
                    layout="vertical"
                    initialValues={{
                        violationType: '偏航预警',
                        banDays: 3,
                        triggerTime: dayjs(),
                    }}
                >
                    <Form.Item name="vehicleId" label="车辆" rules={[{ required: true, message: '请选择车辆' }]}>
                        <Select
                            showSearch
                            optionFilterProp="children"
                            placeholder="请选择车辆"
                            options={vehicleOptions.map(item => ({ value: Number(item.id), label: `${item.plateNo} · ${item.orgName || '未归属单位'}` }))}
                        />
                    </Form.Item>
                    <Form.Item name="violationType" label="违规类型" rules={[{ required: true, message: '请选择违规类型' }]}>
                        <Select
                            options={[
                                { value: '偏航预警', label: '偏航预警' },
                                { value: '未打卡入场', label: '未打卡入场' },
                                { value: '证件过期', label: '证件过期' },
                                { value: '超速行驶', label: '超速行驶' },
                                { value: '闯禁区', label: '闯禁区' },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item name="triggerTime" label="触发时间">
                        <DatePicker showTime className="w-full" />
                    </Form.Item>
                    <Form.Item name="triggerLocation" label="触发地点">
                        <Input placeholder="请输入触发地点" />
                    </Form.Item>
                    <Form.Item name="banDays" label="禁用时长（天）" rules={[{ required: true, message: '请输入禁用时长' }]}>
                        <InputNumber min={1} max={365} className="w-full" />
                    </Form.Item>
                    <Form.Item name="penaltyResult" label="处罚结果">
                        <Input placeholder="例如：停运3天 / 警告整改" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} placeholder="请输入备注" />
                    </Form.Item>
                </Form>
            </Modal>
            <Drawer
                title={detailRecord ? `${detailRecord.plateNo} 违法详情` : '违法详情'}
                width={560}
                open={detailOpen}
                onClose={() => {
                    setDetailOpen(false);
                    setDetailRecord(null);
                }}
                loading={detailLoading}
            >
                {detailRecord ? (
                    <Descriptions column={1} size="small" bordered>
                        <Descriptions.Item label="所属单位">{detailRecord.orgName || '--'}</Descriptions.Item>
                        <Descriptions.Item label="违规类型">{detailRecord.violationType || '--'}</Descriptions.Item>
                        <Descriptions.Item label="处理状态">{detailRecord.actionStatusLabel || '--'}</Descriptions.Item>
                        <Descriptions.Item label="违规时间">{detailRecord.triggerTime || '--'}</Descriptions.Item>
                        <Descriptions.Item label="违规地点">{detailRecord.triggerLocation || '--'}</Descriptions.Item>
                        <Descriptions.Item label="处罚结果">{detailRecord.penaltyResult || '--'}</Descriptions.Item>
                        <Descriptions.Item label="禁用时段">{detailRecord.banStartTime && detailRecord.banEndTime ? `${detailRecord.banStartTime} ~ ${detailRecord.banEndTime}` : '--'}</Descriptions.Item>
                        <Descriptions.Item label="解禁信息">{detailRecord.releaseTime ? `${detailRecord.releaseTime} / ${detailRecord.releaseReason || '未填写原因'}` : '--'}</Descriptions.Item>
                        <Descriptions.Item label="操作人">{detailRecord.operatorName || '--'}</Descriptions.Item>
                        <Descriptions.Item label="车型信息">{[detailRecord.vehicleType, detailRecord.brand, detailRecord.model].filter(Boolean).join(' / ') || '--'}</Descriptions.Item>
                        <Descriptions.Item label="司机信息">{detailRecord.driverName ? `${detailRecord.driverName}${detailRecord.driverPhone ? ` / ${detailRecord.driverPhone}` : ''}` : '--'}</Descriptions.Item>
                        <Descriptions.Item label="车队 / 状态">{`${detailRecord.fleetName || '--'} / ${detailRecord.useStatus || '--'}`}</Descriptions.Item>
                        <Descriptions.Item label="实时速度 / 里程">{`${detailRecord.currentSpeed ?? 0} km/h / ${detailRecord.currentMileage ?? 0} km`}</Descriptions.Item>
                        <Descriptions.Item label="备注">{detailRecord.remark || '--'}</Descriptions.Item>
                    </Descriptions>
                ) : null}
            </Drawer>
        </motion.div>
    );

export default ViolationsList;
