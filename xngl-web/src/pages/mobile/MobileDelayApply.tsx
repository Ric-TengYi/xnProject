import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, DatePicker, Form, Input, List, Tag, message } from 'antd';
import { ArrowLeftOutlined, ClockCircleOutlined, FileTextOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

type DelayRecord = {
  id: string;
  projectName: string;
  requestedEnd: string;
  reason: string;
  status: '待审核' | '已提交';
};

const initialRecords: DelayRecord[] = [
  {
    id: 'DL-20260412-001',
    projectName: '南山区三号地铁线工地',
    requestedEnd: '2026-04-14 18:00',
    reason: '受天气影响，申请顺延现场外运时间。',
    status: '待审核',
  },
  {
    id: 'DL-20260410-002',
    projectName: '科技园旧改项目',
    requestedEnd: '2026-04-11 20:00',
    reason: '场地调度冲突，申请延期作业。',
    status: '已提交',
  },
];

const MobileDelayApply = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<DelayRecord[]>(initialRecords);

  const nextCode = useMemo(() => `DL-${dayjs().format('YYYYMMDD-HHmmss')}`, [loading]);

  const handleSubmit = async (values: { projectName: string; requestedEnd: dayjs.Dayjs; reason: string; attachment?: string }) => {
    setLoading(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      setRecords((current) => [
        {
          id: nextCode,
          projectName: values.projectName,
          requestedEnd: values.requestedEnd.format('YYYY-MM-DD HH:mm'),
          reason: values.reason,
          status: '待审核',
        },
        ...current,
      ]);
      message.success('延期申报已提交');
      form.resetFields();
      form.setFieldsValue({
        projectName: '南山区三号地铁线工地',
        requestedEnd: dayjs().add(2, 'day').hour(18).minute(0),
      });
    } catch (error) {
      message.error('提交失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-6">
      <div className="bg-white px-4 py-3 flex items-center border-b border-gray-100 sticky top-0 z-50">
        <ArrowLeftOutlined className="text-xl text-gray-700 p-2 -ml-2" onClick={() => navigate(-1)} />
        <h2 className="text-lg font-medium m-0 ml-2">延期申报</h2>
      </div>

      <div className="p-4 space-y-4">
        <Card className="w-full shadow-sm border-0 rounded-xl bg-amber-50/70" bodyStyle={{ padding: '16px' }}>
          <div className="flex items-center text-amber-700 mb-2">
            <ClockCircleOutlined className="text-xl mr-2" />
            <h3 className="text-base font-medium m-0">作业顺延申请</h3>
          </div>
          <p className="text-sm text-amber-700/80 mb-0 leading-relaxed">
            适用于受天气、场地调度、运输组织影响导致无法按计划完成作业的场景。
          </p>
        </Card>

        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '16px' }}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={{
              projectName: '南山区三号地铁线工地',
              requestedEnd: dayjs().add(2, 'day').hour(18).minute(0),
            }}
          >
            <Form.Item name="projectName" label={<span className="font-medium text-gray-700">项目名称</span>} rules={[{ required: true, message: '请输入项目名称' }]}>
              <Input size="large" />
            </Form.Item>

            <Form.Item name="requestedEnd" label={<span className="font-medium text-gray-700">申请延期到</span>} rules={[{ required: true, message: '请选择延期时间' }]}>
              <DatePicker showTime size="large" className="w-full" />
            </Form.Item>

            <Form.Item name="reason" label={<span className="font-medium text-gray-700">延期原因</span>} rules={[{ required: true, message: '请输入延期原因' }]}>
              <Input.TextArea rows={3} placeholder="请输入延期说明" />
            </Form.Item>

            <Form.Item name="attachment" label={<span className="font-medium text-gray-700">附件地址</span>}>
              <Input placeholder="可选，填写佐证材料地址" size="large" />
            </Form.Item>

            <Button type="primary" htmlType="submit" block size="large" loading={loading} className="mt-2 h-12 rounded-lg bg-amber-500 border-amber-500 hover:bg-amber-600">
              提交延期申报
            </Button>
          </Form>
        </Card>

        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '12px 16px' }}>
          <div className="flex items-center mb-3">
            <FileTextOutlined className="text-amber-500 mr-2" />
            <h3 className="text-base font-medium m-0 text-gray-800">最近延期记录</h3>
          </div>
          <List
            dataSource={records}
            renderItem={(item) => (
              <List.Item className="px-0">
                <List.Item.Meta
                  title={
                    <div className="flex items-center justify-between gap-3">
                      <span className="font-medium text-gray-800">{item.projectName}</span>
                      <Tag color={item.status === '待审核' ? 'orange' : 'blue'} className="m-0 border-0">
                        {item.status}
                      </Tag>
                    </div>
                  }
                  description={
                    <div className="text-xs text-gray-500 leading-5">
                      <div>申请单号：{item.id}</div>
                      <div>延期到：{item.requestedEnd}</div>
                      <div>{item.reason}</div>
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

export default MobileDelayApply;
