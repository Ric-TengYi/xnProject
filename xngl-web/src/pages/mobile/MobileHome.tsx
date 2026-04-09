import { useNavigate } from 'react-router-dom';
import { Card } from 'antd';
import { CameraOutlined, CheckCircleOutlined, AlertOutlined, CarOutlined, AppstoreOutlined } from '@ant-design/icons';

const MobileHome = () => {
  const navigate = useNavigate();

  const menuItems = [
    { title: '出土打卡', path: '/mobile/punch-in', icon: <CameraOutlined className="text-2xl text-blue-500" />, color: 'bg-blue-50' },
    { title: '消纳确认', path: '/mobile/disposal', icon: <CheckCircleOutlined className="text-2xl text-green-500" />, color: 'bg-green-50' },
    { title: '事件上报', path: '/mobile/event-report', icon: <AlertOutlined className="text-2xl text-red-500" />, color: 'bg-red-50' },
    { title: '车辆跟踪', path: '/mobile/vehicle-tracking', icon: <CarOutlined className="text-2xl text-orange-500" />, color: 'bg-orange-50' }
  ];

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <div className="bg-blue-600 text-white pt-12 pb-6 px-6 rounded-b-3xl shadow-sm">
        <h1 className="text-2xl font-bold mb-1">小牛管土</h1>
        <p className="text-blue-100 text-sm">渣土运输数字化管理平台</p>
      </div>

      <div className="flex-1 px-4 -mt-4 z-10">
        <Card className="w-full shadow-md border-0 rounded-xl mb-4" bodyStyle={{ padding: '16px' }}>
          <div className="flex items-center mb-4 pb-2 border-b border-gray-100">
            <AppstoreOutlined className="text-blue-500 mr-2 text-lg" />
            <h2 className="text-base font-medium m-0">常用功能</h2>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {menuItems.map((item, idx) => (
              <div 
                key={idx} 
                className={`${item.color} rounded-xl p-4 flex flex-col items-center justify-center cursor-pointer shadow-sm border border-white`}
                onClick={() => navigate(item.path)}
              >
                <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center mb-2 shadow-sm">
                  {item.icon}
                </div>
                <span className="text-sm font-medium text-gray-700">{item.title}</span>
              </div>
            ))}
          </div>
        </Card>
        
        <Card className="w-full shadow-sm border-0 rounded-xl mb-6" bodyStyle={{ padding: '16px' }}>
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-medium text-base m-0 text-gray-800">最新通知</h3>
            <span className="text-xs text-gray-400">更多</span>
          </div>
          <div className="bg-blue-50 text-blue-800 p-3 rounded-lg text-sm mb-2 border border-blue-100 flex items-start">
            <div className="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1.5 mr-2 flex-shrink-0" />
            <p className="m-0 leading-relaxed">关于近期夜间运输管理要求的补充通知，请各车队务必遵守...</p>
          </div>
          <div className="bg-gray-50 text-gray-600 p-3 rounded-lg text-sm border border-gray-100 flex items-start">
            <div className="w-1.5 h-1.5 rounded-full bg-gray-300 mt-1.5 mr-2 flex-shrink-0" />
            <p className="m-0 leading-relaxed">三号工地因天气原因暂停外运作业，预计明日恢复...</p>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default MobileHome;
