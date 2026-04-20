import React, { useState } from 'react';
import { Dropdown, Modal, Form, Input, message, Descriptions, Tag } from 'antd';
import { UserOutlined, LogoutOutlined, LockOutlined } from '@ant-design/icons';
import request from '../utils/request';

interface UserMenuProps {
  userInfo: any;
  onLogout: () => void;
}

const UserMenu: React.FC<UserMenuProps> = ({ userInfo, onLogout }) => {
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleChangePassword = async (values: any) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }

    setLoading(true);
    try {
      const res = await request.put('/me/password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      if (res.code === 200) {
        message.success('密码修改成功');
        setPasswordModalOpen(false);
        form.resetFields();
      } else {
        message.error(res.message || '密码修改失败');
      }
    } catch (error) {
      message.error('密码修改失败');
    } finally {
      setLoading(false);
    }
  };

  const items: any[] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
      onClick: () => setProfileModalOpen(true),
    },
    {
      key: 'password',
      icon: <LockOutlined />,
      label: '修改密码',
      onClick: () => setPasswordModalOpen(true),
    },
  ];

  if (userInfo?.userType === 'TENANT_ADMIN') {
    items.push({
      key: 'users',
      icon: <UserOutlined />,
      label: '用户管理',
      onClick: () => {
        window.location.href = '/users';
      },
    });
  }

  items.push(
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: onLogout,
    }
  );

  return (
    <>
      <Dropdown menu={{ items }} placement="topRight">
        <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
          <UserOutlined style={{ fontSize: 18 }} />
          <span>{userInfo?.name || userInfo?.username}</span>
        </div>
      </Dropdown>

      <Modal
        title="个人信息"
        open={profileModalOpen}
        onCancel={() => setProfileModalOpen(false)}
        footer={null}
        width={500}
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="用户名">{userInfo?.username}</Descriptions.Item>
          <Descriptions.Item label="姓名">{userInfo?.name}</Descriptions.Item>
          <Descriptions.Item label="用户类型">
            <Tag color={userInfo?.userType === 'TENANT_ADMIN' ? 'blue' : 'default'}>
              {userInfo?.userType === 'TENANT_ADMIN' ? '租户管理员' : userInfo?.userType}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Modal>

      <Modal
        title="修改密码"
        open={passwordModalOpen}
        onOk={() => form.submit()}
        onCancel={() => {
          setPasswordModalOpen(false);
          form.resetFields();
        }}
        loading={loading}
        width={400}
      >
        <Form form={form} layout="vertical" onFinish={handleChangePassword}>
          <Form.Item
            name="oldPassword"
            label="原密码"
            rules={[{ required: true, message: '请输入原密码' }]}
          >
            <Input.Password placeholder="请输入原密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[{ required: true, message: '请输入新密码' }]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            rules={[{ required: true, message: '请确认新密码' }]}
          >
            <Input.Password placeholder="请确认新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default UserMenu;
