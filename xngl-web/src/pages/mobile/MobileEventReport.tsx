import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, Select, message, Upload, List, Tag } from 'antd';
import { AlertOutlined, ArrowLeftOutlined, UploadOutlined, FileTextOutlined } from '@ant-design/icons';

const { Option } = Select;

type EventRecord = {
  id: string;
  title: string;
  eventType: string;
  priority: '一般' | '较急' | '紧急';
  status: '待审核' | '已上报';
};

const initialRecords: EventRecord[] = [
  {
    id: 'EV-20260412-001',
    title: '运输路线存在洒落风险',
    eventType: '违章违规',
    priority: '较急',
    status: '待审核',
  },
  {
    id: 'EV-20260411-002',
    title: '夜间工地出入口拥堵',
    eventType: '工地冲突',
    priority: '一般',
    status: '已上报',
  },
];

const MobileEventReport = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<any[]>([]);
  const [records, setRecords] = useState<EventRecord[]>(initialRecords);

  const nextCode = useMemo(() => `EV-${Date.now()}`, [loading]);

  const handleUpload = (info: any) => {
    let newFileList = [...info.fileList];
    setFileList(newFileList);
  };

  const handleSubmit = async (values: { eventType: string; priority: string; title: string; content: string }) => {
    setLoading(true);
    try {
      // Simulate API Call
      await new Promise(resolve => setTimeout(resolve, 1000));
      const eventTypeMap: Record<string, string> = {
        accident: '交通事故',
        violation: '违章违规',
        environment: '环保投诉',
        site: '工地冲突',
        other: '其他异常',
      };
      const priorityMap: Record<string, EventRecord['priority']> = {
        low: '一般',
        medium: '较急',
        high: '紧急',
      };
      setRecords((current) => [
        {
          id: nextCode,
          title: values.title,
          eventType: eventTypeMap[values.eventType] || '其他异常',
          priority: priorityMap[values.priority] || '一般',
          status: '待审核',
        },
        ...current,
      ]);
      message.success('事件已上报');
      form.resetFields();
      setFileList([]);
      form.setFieldsValue({
        eventType: 'violation',
        priority: 'high',
        title: '运输路线存在洒落风险',
        content: '现场发现车辆篷布覆盖不完全，存在运输途中洒落风险，请尽快调度处置。',
      });
    } catch (e) {
      message.error('上报失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-6">
      <div className="bg-white px-4 py-3 flex items-center border-b border-gray-100 sticky top-0 z-50">
        <ArrowLeftOutlined className="text-xl text-gray-700 p-2 -ml-2" onClick={() => navigate(-1)} />
        <h2 className="text-lg font-medium m-0 ml-2">事件上报</h2>
      </div>

      <div className="p-4">
        <Card className="w-full shadow-sm border-0 rounded-xl bg-red-50/50 mb-4" bodyStyle={{ padding: '16px' }}>
           <div className="flex items-start text-red-600">
             <AlertOutlined className="text-xl mr-2 mt-0.5" />
             <div>
               <h3 className="text-base font-medium m-0">注意安全，如遇紧急情况请先报警</h3>
               <p className="text-xs mt-1 mb-0 opacity-80">提交的事件将推送至调度中心并记录至平台。</p>
             </div>
           </div>
        </Card>

        <Card className="w-full shadow-sm border-0 rounded-xl mb-4" bodyStyle={{ padding: '16px' }}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={{
              eventType: 'violation',
              priority: 'high',
              title: '运输路线存在洒落风险',
              content: '现场发现车辆篷布覆盖不完全，存在运输途中洒落风险，请尽快调度处置。',
            }}
          >
            <Form.Item name="eventType" label={<span className="font-medium text-gray-700">事件类型</span>} rules={[{ required: true, message: '请选择事件类型' }]}>
              <Select placeholder="请选择" size="large" className="w-full">
                <Option value="accident">交通事故</Option>
                <Option value="violation">违章违规</Option>
                <Option value="environment">环保投诉</Option>
                <Option value="site">工地冲突</Option>
                <Option value="other">其他异常</Option>
              </Select>
            </Form.Item>

            <Form.Item name="priority" label={<span className="font-medium text-gray-700">紧急程度</span>} rules={[{ required: true, message: '请选择紧急程度' }]}>
              <Select placeholder="请选择" size="large" className="w-full">
                <Option value="low">一般</Option>
                <Option value="medium">较急</Option>
                <Option value="high" className="text-red-500 font-medium">紧急 (将触发短信告警)</Option>
              </Select>
            </Form.Item>

            <Form.Item name="title" label={<span className="font-medium text-gray-700">事件简述</span>} rules={[{ required: true, message: '请输入事件简述' }]}>
              <Input placeholder="一句话概括事件" size="large" />
            </Form.Item>

            <Form.Item name="content" label={<span className="font-medium text-gray-700">详细描述</span>} rules={[{ required: true, message: '请输入详细描述' }]}>
              <Input.TextArea rows={4} placeholder="描述事件发生时间、地点、涉及人员车辆及当前情况" />
            </Form.Item>

            <Form.Item label={<span className="font-medium text-gray-700">现场照片</span>}>
              <Upload
                listType="picture-card"
                fileList={fileList}
                onChange={handleUpload}
                beforeUpload={() => false}
                maxCount={4}
              >
                {fileList.length >= 4 ? null : (
                  <div>
                    <UploadOutlined className="text-2xl text-gray-400 mb-2" />
                    <div className="text-sm text-gray-500">上传图片</div>
                  </div>
                )}
              </Upload>
            </Form.Item>

            <Button type="primary" htmlType="submit" block size="large" loading={loading} className="mt-4 h-12 rounded-lg bg-red-600 border-red-600 hover:bg-red-700">
              立即上报
            </Button>
          </Form>
        </Card>

        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '12px 16px' }}>
          <div className="flex items-center mb-3">
            <FileTextOutlined className="text-red-500 mr-2" />
            <h3 className="text-base font-medium m-0 text-gray-800">最近上报记录</h3>
          </div>
          <List
            dataSource={records}
            renderItem={(item) => (
              <List.Item className="px-0">
                <List.Item.Meta
                  title={
                    <div className="flex items-center justify-between gap-3">
                      <span className="font-medium text-gray-800">{item.title}</span>
                      <Tag color={item.priority === '紧急' ? 'red' : item.priority === '较急' ? 'orange' : 'blue'} className="m-0 border-0">
                        {item.priority}
                      </Tag>
                    </div>
                  }
                  description={
                    <div className="text-xs text-gray-500 leading-5">
                      <div>事件编号：{item.id}</div>
                      <div>类型：{item.eventType}</div>
                      <div>状态：{item.status}</div>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      </div>
    </div>
  );
};

export default MobileEventReport;
