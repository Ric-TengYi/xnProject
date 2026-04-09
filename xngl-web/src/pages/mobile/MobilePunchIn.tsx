import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, Select, Upload, message, DatePicker } from 'antd';
import { CameraOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

const { Option } = Select;

const MobilePunchIn = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<any[]>([]);

  const handleUpload = (info: any) => {
    let newFileList = [...info.fileList];
    newFileList = newFileList.slice(-1);
    setFileList(newFileList);
  };

  const handleSubmit = async () => {
    if (fileList.length === 0) {
      message.warning('请上传出土照片');
      return;
    }
    
    setLoading(true);
    try {
      // simulate API Call
      await new Promise(resolve => setTimeout(resolve, 1000));
      message.success('打卡成功');
      navigate(-1);
    } catch (e) {
      message.error('打卡失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-6">
      <div className="bg-white px-4 py-3 flex items-center border-b border-gray-100 sticky top-0 z-50">
        <ArrowLeftOutlined className="text-xl text-gray-700 p-2 -ml-2" onClick={() => navigate(-1)} />
        <h2 className="text-lg font-medium m-0 ml-2">出土打卡</h2>
      </div>

      <div className="p-4">
        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '16px' }}>
          <Form form={form} layout="vertical" onFinish={handleSubmit} initialValues={{ time: dayjs() }}>
            <Form.Item name="projectId" label={<span className="font-medium text-gray-700">项目名称</span>} rules={[{ required: true, message: '请选择项目' }]}>
              <Select placeholder="请选择出土项目" size="large" className="w-full">
                <Option value={1}>南山区三号地铁线工地</Option>
                <Option value={2}>科技园旧改项目</Option>
              </Select>
            </Form.Item>

            <Form.Item name="plateNo" label={<span className="font-medium text-gray-700">车牌号</span>} rules={[{ required: true, message: '请输入车牌号' }]}>
              <Input placeholder="例如: 粤B12345" size="large" />
            </Form.Item>
            
            <Form.Item name="time" label={<span className="font-medium text-gray-700">打卡时间</span>}>
              <DatePicker showTime size="large" className="w-full" disabled />
            </Form.Item>

            <Form.Item label={<span className="font-medium text-gray-700">现场照片 (必传)</span>}>
              <Upload
                listType="picture-card"
                fileList={fileList}
                onChange={handleUpload}
                beforeUpload={() => false}
                maxCount={1}
                className="mb-0"
              >
                {fileList.length >= 1 ? null : (
                  <div>
                    <CameraOutlined className="text-2xl text-gray-400 mb-2" />
                    <div className="text-sm text-gray-500">拍照</div>
                  </div>
                )}
              </Upload>
              <p className="text-xs text-gray-400 mt-2">请拍摄能够清晰反映出土现场和车辆的照片</p>
            </Form.Item>

            <Form.Item name="remark" label={<span className="font-medium text-gray-700">备注说明</span>}>
              <Input.TextArea rows={3} placeholder="如有特殊情况请说明" />
            </Form.Item>

            <Button type="primary" htmlType="submit" block size="large" loading={loading} className="mt-4 h-12 rounded-lg bg-blue-600">
              确认打卡
            </Button>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default MobilePunchIn;
