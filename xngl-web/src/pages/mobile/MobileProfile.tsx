import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Avatar, List, message, Modal, Input } from 'antd';
import { UserOutlined, SettingOutlined, QuestionCircleOutlined, LogoutOutlined } from '@ant-design/icons';

const MobileProfile = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<any>(null);
  const [feedbackVisible, setFeedbackVisible] = useState(false);
  const [feedbackContent, setFeedbackContent] = useState('');

  useEffect(() => {
    const userData = localStorage.getItem('mini_user');
    if (userData) {
      try {
        setUser(JSON.parse(userData));
      } catch (e) {}
    } else {
      navigate('/mobile/login');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('mini_token');
    localStorage.removeItem('mini_user');
    navigate('/mobile/login');
  };

  const handleFeedbackSubmit = () => {
    if (!feedbackContent.trim()) {
      message.warning('请输入反馈内容');
      return;
    }
    // API Call
    message.success('反馈已提交，感谢您的建议');
    setFeedbackVisible(false);
    setFeedbackContent('');
  };

  const menuData = [
    { title: '账号设置', icon: <SettingOutlined className="text-gray-500" />, onClick: () => message.info('功能开发中') },
    { title: '意见反馈', icon: <QuestionCircleOutlined className="text-gray-500" />, onClick: () => setFeedbackVisible(true) },
    { title: '退出登录', icon: <LogoutOutlined className="text-red-500" />, onClick: handleLogout, className: 'text-red-500' }
  ];

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-20">
      <div className="bg-blue-600 text-white pt-16 pb-12 px-6 shadow-sm flex flex-col items-center">
        <Avatar size={80} icon={<UserOutlined />} className="bg-blue-400 border-2 border-white shadow-md mb-4" />
        <h2 className="text-xl font-bold mb-1">{user?.name || user?.mobile || '演示用户'}</h2>
        <p className="text-blue-100 text-sm">{user?.mobile || '138****0000'}</p>
      </div>

      <div className="flex-1 px-4 -mt-6 z-10">
        <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '0' }}>
          <List
            itemLayout="horizontal"
            dataSource={menuData}
            renderItem={item => (
              <List.Item 
                className="px-4 py-4 cursor-pointer hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
                onClick={item.onClick}
              >
                <List.Item.Meta
                  avatar={<div className="mt-0.5">{item.icon}</div>}
                  title={<span className={`text-base ${item.className || 'text-gray-700'}`}>{item.title}</span>}
                />
                <div className="text-gray-300">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </List.Item>
            )}
          />
        </Card>
      </div>

      <Modal
        title="意见反馈"
        open={feedbackVisible}
        onOk={handleFeedbackSubmit}
        onCancel={() => setFeedbackVisible(false)}
        okText="提交"
        cancelText="取消"
        okButtonProps={{ className: 'bg-blue-600' }}
      >
        <Input.TextArea
          rows={4}
          placeholder="请输入您遇到的问题或建议..."
          value={feedbackContent}
          onChange={(e) => setFeedbackContent(e.target.value)}
          className="mt-4"
        />
      </Modal>
    </div>
  );
};

export default MobileProfile;
