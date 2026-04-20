import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { HomeOutlined, UserOutlined } from '@ant-design/icons';

const MobileLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleTabClick = (path: string) => {
    navigate(path);
  };

  const isTabActive = (path: string) => {
    return location.pathname === path;
  };

  const hideTabBarPaths = ['/mobile/login'];
  const showTabBar = !hideTabBarPaths.includes(location.pathname);

  return (
    <div className="flex flex-col h-screen w-full bg-gray-50 overflow-hidden font-sans">
      <div className="flex-1 overflow-y-auto pb-16">
        <Outlet />
      </div>
      
      {showTabBar && (
        <div className="fixed bottom-0 left-0 right-0 h-16 bg-white border-t border-gray-200 flex justify-around items-center px-4 z-50">
          <div 
            className={`flex flex-col items-center justify-center w-full h-full cursor-pointer ${isTabActive('/mobile/home') ? 'text-blue-600' : 'text-gray-500'}`}
            onClick={() => handleTabClick('/mobile/home')}
          >
            <HomeOutlined className="text-xl mb-1" />
            <span className="text-xs">首页</span>
          </div>
          <div 
            className={`flex flex-col items-center justify-center w-full h-full cursor-pointer ${isTabActive('/mobile/profile') ? 'text-blue-600' : 'text-gray-500'}`}
            onClick={() => handleTabClick('/mobile/profile')}
          >
            <UserOutlined className="text-xl mb-1" />
            <span className="text-xs">我的</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default MobileLayout;
