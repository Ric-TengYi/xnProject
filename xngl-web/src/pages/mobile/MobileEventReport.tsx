import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, Select, message, Upload } from 'antd';
import { AlertOutlined, ArrowLeftOutlined, UploadOutlined } from '@ant-design/icons';

const { Option } = Select;

const MobileEventReport = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<any[]>([]);

  const handleUpload = (info: any) => {
    let newFileList = [...info.fileList];
    setFileList(newFileList);
  };

  const handleSubmit = async () => {
    setLoading(true);
    try {
      // Simulate API Call
      await new Promise(resolve => setTimeout(resolve, 1000));
      message.success('事件已上报');
      navigate(-1);
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

        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '16px' }}>
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
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
      </div>
    </div>
  );
};

export default MobileEventReport;
