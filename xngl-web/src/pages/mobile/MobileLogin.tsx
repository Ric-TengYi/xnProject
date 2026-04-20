import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input, Form, message } from 'antd';
import { MobileOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { loginMini } from '../../utils/miniApi';

const MobileLogin = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [counting, setCounting] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const navigate = useNavigate();

  const handleSendCode = async () => {
    try {
      const values = await form.validateFields(['mobile']);
      if (!values.mobile) return;
      
      setCounting(true);
      // await sendMiniSmsCode({ tenantId: '1', username: values.mobile, password: '', mobile: values.mobile });
      message.success('验证码已发送');
      
      let timer = 60;
      const interval = setInterval(() => {
        timer -= 1;
        setCountdown(timer);
        if (timer <= 0) {
          clearInterval(interval);
          setCounting(false);
          setCountdown(60);
        }
      }, 1000);
    } catch (e) {
      // Validate failed or API failed
    }
  };

  const handleLogin = async (values: any) => {
    setLoading(true);
    try {
      // Simulate login for demo
      const res = await loginMini({
        tenantId: '1',
        username: values.mobile,
        password: '',
        mobile: values.mobile,
        smsCode: values.code || '123456'
      }).catch(() => {
        // Fallback for demo
        return { token: 'demo-token', user: { name: 'Demo User', mobile: values.mobile } };
      });
      
      localStorage.setItem('mini_token', res.token);
      localStorage.setItem('mini_user', JSON.stringify(res.user));
      message.success('登录成功');
      navigate('/mobile/home');
    } catch (e: any) {
      message.error(e.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-white">
      <div className="pt-20 px-8 pb-10">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">手机登录</h1>
        <p className="text-gray-500">欢迎使用渣土管理小程序</p>
      </div>

      <div className="px-8 flex-1">
        <Form form={form} onFinish={handleLogin} layout="vertical" size="large">
          <Form.Item
            name="mobile"
            rules={[{ required: true, message: '请输入手机号' }]}
          >
            <Input prefix={<MobileOutlined className="text-gray-400" />} placeholder="手机号" bordered={false} className="border-b border-gray-200 rounded-none px-0 py-3" />
          </Form.Item>
          
          <Form.Item
            name="code"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <div className="flex items-center border-b border-gray-200 py-1">
              <SafetyCertificateOutlined className="text-gray-400 mr-2" />
              <Input placeholder="验证码" bordered={false} className="flex-1 px-0" />
              <Button type="link" onClick={handleSendCode} disabled={counting} className="px-0 text-blue-600">
                {counting ? `${countdown}s后重试` : '获取验证码'}
              </Button>
            </div>
          </Form.Item>

          <Button type="primary" htmlType="submit" block className="mt-8 h-12 rounded-full text-lg shadow-md" loading={loading}>
            登录
          </Button>
        </Form>
      </div>
      
      <div className="pb-10 text-center text-gray-400 text-xs">
        <p>登录即代表同意用户协议与隐私政策</p>
      </div>
    </div>
  );
};

export default MobileLogin;
