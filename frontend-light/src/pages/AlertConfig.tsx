import React from 'react';
import { Button, Tag, Select, Collapse, Switch, InputNumber, Space, Form } from 'antd';
import { SaveOutlined, BellOutlined, EnvironmentOutlined, CarOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Panel } = Collapse;
const { Option } = Select;

const AlertConfig: React.FC = () => {

    return (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">预警配置</h1>
                    <p className="g-text-secondary mt-1">配置各类预警的触发阈值、电子围栏及推送规则</p>
                </div>
                <Button type="primary" icon={<SaveOutlined />} className="g-btn-primary border-none">
                    保存全部配置
                </Button>
            </div>

            <Collapse defaultActiveKey={['1', '2', '3']} className="bg-transparent border-none" expandIconPosition="end">
                <Panel 
                    header={<span className="text-lg g-text-primary font-bold flex items-center gap-2"><EnvironmentOutlined className="g-text-primary-link" /> 场地预警规则</span>} 
                    key="1" 
                    className="glass-panel g-border-panel border mb-4 rounded-lg overflow-hidden"
                >
                    <Form layout="vertical" className="grid grid-cols-3 gap-6">
                        <Form.Item label={<span className="g-text-secondary">容量超限预警阈值 (%)</span>}>
                            <InputNumber min={50} max={100} defaultValue={80} className="w-full" />
                        </Form.Item>
                        <Form.Item label={<span className="g-text-secondary">凭证到期提前预警 (天)</span>}>
                            <InputNumber min={1} max={90} defaultValue={30} className="w-full" />
                        </Form.Item>
                        <Form.Item label={<span className="g-text-secondary">林地占用超限开关</span>}>
                            <Switch defaultChecked />
                        </Form.Item>
                    </Form>
                </Panel>

                <Panel 
                    header={<span className="text-lg g-text-primary font-bold flex items-center gap-2"><CarOutlined className="g-text-warning" /> 车辆与人员预警规则</span>} 
                    key="2" 
                    className="glass-panel g-border-panel border mb-4 rounded-lg overflow-hidden"
                >
                    <Form layout="vertical" className="grid grid-cols-3 gap-6">
                        <Form.Item label={<span className="g-text-secondary">偏航距离阈值 (米)</span>}>
                            <InputNumber min={50} max={1000} defaultValue={200} className="w-full" />
                        </Form.Item>
                        <Form.Item label={<span className="g-text-secondary">未打卡容忍次数 (次/月)</span>}>
                            <InputNumber min={0} max={10} defaultValue={3} className="w-full" />
                        </Form.Item>
                        <Form.Item label={<span className="g-text-secondary">司机准驾年龄上限 (岁)</span>}>
                            <InputNumber min={50} max={65} defaultValue={55} className="w-full" />
                        </Form.Item>
                        <Form.Item label={<span className="g-text-secondary">证件到期提前预警 (天)</span>}>
                            <InputNumber min={1} max={90} defaultValue={30} className="w-full" />
                        </Form.Item>
                    </Form>
                </Panel>

                <Panel 
                    header={<span className="text-lg g-text-primary font-bold flex items-center gap-2"><BellOutlined className="g-text-success" /> 推送规则配置</span>} 
                    key="3" 
                    className="glass-panel g-border-panel border mb-4 rounded-lg overflow-hidden"
                >
                    <Form layout="vertical">
                        <div className="grid grid-cols-4 gap-4 mb-4 pb-4 border-b g-border-panel border">
                            <div className="g-text-secondary">预警等级</div>
                            <div className="g-text-secondary">推送方式</div>
                            <div className="g-text-secondary col-span-2">推送对象 (角色)</div>
                        </div>
                        
                        <div className="grid grid-cols-4 gap-4 mb-4 items-center">
                            <div><Tag color="red" className="border-none">L3 高风险</Tag></div>
                            <div>
                                <Space>
                                    <Tag color="blue">站内信</Tag>
                                    <Tag color="blue">短信</Tag>
                                    <Tag color="blue">钉钉</Tag>
                                </Space>
                            </div>
                            <div className="col-span-2">
                                <Select mode="multiple" defaultValue={['admin', 'leader']} className="w-full" popupClassName="bg-white">
                                    <Option value="admin">系统管理员</Option>
                                    <Option value="leader">执法领导</Option>
                                    <Option value="manager">车队管理员</Option>
                                </Select>
                            </div>
                        </div>

                        <div className="grid grid-cols-4 gap-4 mb-4 items-center">
                            <div><Tag color="orange" className="border-none">L2 中风险</Tag></div>
                            <div>
                                <Space>
                                    <Tag color="blue">站内信</Tag>
                                    <Tag color="blue">短信</Tag>
                                </Space>
                            </div>
                            <div className="col-span-2">
                                <Select mode="multiple" defaultValue={['manager']} className="w-full" popupClassName="bg-white">
                                    <Option value="admin">系统管理员</Option>
                                    <Option value="leader">执法领导</Option>
                                    <Option value="manager">车队管理员</Option>
                                </Select>
                            </div>
                        </div>
                    </Form>
                </Panel>
            </Collapse>
        </motion.div>
    );
};

export default AlertConfig;
