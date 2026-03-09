/**
 * 登录页 - 风格对齐 uniubi-uan-inspection-web 浅色系
 * 白底卡片、#0F62FF 主色、账号/验证码 Tab
 */
import React, { useState } from 'react';
import { Form, Input, Button, Tabs } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import styles from './Login.module.css';

const Login: React.FC = () => {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState<string>('account');
  const navigate = useNavigate();

  const onFinish = (values: Record<string, string>) => {
    console.log('Login values', values);
    // 模拟登录成功，跳转首页
    navigate('/', { replace: true });
  };

  return (
    <div className={styles.wrap}>
      <div className={styles.background} />
      <div className={styles.content}>
        <div className={styles.left}>
          <div className={styles.header}>
            <div
              className={styles.logo}
              style={{ background: '#0F62FF', color: '#fff' }}
            >
              渣
            </div>
            <div className={styles.appName}>智慧渣土消纳管控</div>
          </div>
          <Tabs
            className={styles.tabs}
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              { key: 'account', label: '账号登录' },
              { key: 'code', label: '验证码登录' },
            ]}
          />
          <Form
            className={styles.form}
            form={form}
            layout="vertical"
            onFinish={onFinish}
            colon={false}
          >
            {activeTab === 'account' && (
              <>
                <Form.Item
                  name="account"
                  rules={[{ required: true, message: '请输入账号' }]}
                >
                  <Input
                    size="large"
                    placeholder="请输入账号"
                    prefix={<UserOutlined className={styles.inputIcon} />}
                  />
                </Form.Item>
                <Form.Item
                  name="password"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password
                    size="large"
                    placeholder="请输入密码"
                    prefix={<LockOutlined className={styles.inputIcon} />}
                  />
                </Form.Item>
              </>
            )}
            {activeTab === 'code' && (
              <Form.Item
                name="mobile"
                rules={[
                  { required: true, message: '请输入手机号' },
                  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号' },
                ]}
              >
                <Input size="large" placeholder="请输入手机号" />
              </Form.Item>
            )}
            <Form.Item noStyle>
              <Button
                type="primary"
                htmlType="submit"
                block
                size="large"
                className={styles.submit}
              >
                登录
              </Button>
            </Form.Item>
            <div className={styles.forget} onClick={() => {}}>
              忘记密码
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
};

export default Login;
