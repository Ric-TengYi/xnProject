import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, InputNumber, Select, message, Upload } from 'antd';
import { ArrowLeftOutlined, InboxOutlined, UploadOutlined } from '@ant-design/icons';

const { Option } = Select;

const MobileDisposal = () => {
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
      message.success('消纳确认成功');
      navigate(-1);
    } catch (e) {
      message.error('提交失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-6">
      <div className="bg-white px-4 py-3 flex items-center border-b border-gray-100 sticky top-0 z-50">
        <ArrowLeftOutlined className="text-xl text-gray-700 p-2 -ml-2" onClick={() => navigate(-1)} />
        <h2 className="text-lg font-medium m-0 ml-2">消纳确认</h2>
      </div>

      <div className="p-4">
        <Card className="w-full shadow-sm border-0 rounded-xl mb-4 bg-green-50/50" bodyStyle={{ padding: '16px' }}>
          <div className="flex items-center text-green-700 mb-2">
            <InboxOutlined className="text-xl mr-2" />
            <h3 className="text-base font-medium m-0">一号消纳场</h3>
          </div>
          <p className="text-sm text-green-600/80 mb-0 leading-relaxed">今日已消纳车次：12车，累计容量：360方</p>
        </Card>

        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '16px' }}>
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            <Form.Item name="contractId" label={<span className="font-medium text-gray-700">关联项目/合同</span>} rules={[{ required: true, message: '请选择关联项目' }]}>
              <Select placeholder="请选择" size="large" className="w-full">
                <Option value={1}>南山区三号地铁线外运合同</Option>
                <Option value={2}>科技园清场合同</Option>
              </Select>
            </Form.Item>

            <Form.Item name="plateNo" label={<span className="font-medium text-gray-700">车牌号</span>} rules={[{ required: true, message: '请输入车牌号' }]}>
              <Input placeholder="输入来车车牌" size="large" />
            </Form.Item>

            <Form.Item name="volume" label={<span className="font-medium text-gray-700">消纳方量 (m³)</span>} rules={[{ required: true, message: '请输入消纳方量' }]}>
              <InputNumber placeholder="0.0" size="large" className="w-full" min={0} step={0.1} />
            </Form.Item>

            <Form.Item name="amount" label={<span className="font-medium text-gray-700">实际金额 (元)</span>}>
              <InputNumber placeholder="选填，如有额外费用" size="large" className="w-full" min={0} />
            </Form.Item>

            <Form.Item label={<span className="font-medium text-gray-700">凭证上传</span>}>
              <Upload
                listType="picture-card"
                fileList={fileList}
                onChange={handleUpload}
                beforeUpload={() => false}
                maxCount={3}
              >
                {fileList.length >= 3 ? null : (
                  <div>
                    <UploadOutlined className="text-2xl text-gray-400 mb-2" />
                    <div className="text-sm text-gray-500">票据照片</div>
                  </div>
                )}
              </Upload>
            </Form.Item>

            <Form.Item name="remark" label={<span className="font-medium text-gray-700">备注说明</span>}>
              <Input.TextArea rows={2} placeholder="如有特殊情况请说明" />
            </Form.Item>

            <Button type="primary" htmlType="submit" block size="large" loading={loading} className="mt-4 h-12 rounded-lg bg-green-600 border-green-600 hover:bg-green-700">
              提交确认
            </Button>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default MobileDisposal;
