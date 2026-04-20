import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Input, List, Tag, Spin, message } from 'antd';
import { SearchOutlined, ArrowLeftOutlined, CarOutlined, EnvironmentOutlined } from '@ant-design/icons';

const MobileVehicleTracking = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchVehicles();
  }, []);

  const fetchVehicles = async () => {
    setLoading(true);
    try {
      // Simulate API call for list of vehicles
      await new Promise(resolve => setTimeout(resolve, 800));
      setVehicles([
        { id: 1, plateNo: '粤B12345', status: 'moving', speed: 45, location: '南山区沙河西路', time: '10分钟前' },
        { id: 2, plateNo: '粤B54321', status: 'stopped', speed: 0, location: '一号消纳场', time: '2分钟前' },
        { id: 3, plateNo: '粤B99999', status: 'offline', speed: 0, location: '科技园工地', time: '2小时前' },
      ]);
    } catch (e) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const getStatusTag = (status: string) => {
    switch (status) {
      case 'moving': return <Tag color="green" className="m-0 border-0">行驶中</Tag>;
      case 'stopped': return <Tag color="orange" className="m-0 border-0">静止</Tag>;
      case 'offline': return <Tag color="default" className="m-0 border-0">离线</Tag>;
      default: return <Tag color="default" className="m-0 border-0">未知</Tag>;
    }
  };

  const filteredVehicles = vehicles.filter(v => v.plateNo.includes(search));

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 pb-6">
      <div className="bg-white px-4 py-3 flex flex-col border-b border-gray-100 sticky top-0 z-50">
        <div className="flex items-center mb-3">
          <ArrowLeftOutlined className="text-xl text-gray-700 p-2 -ml-2" onClick={() => navigate(-1)} />
          <h2 className="text-lg font-medium m-0 ml-2">车辆跟踪</h2>
        </div>
        <Input 
          prefix={<SearchOutlined className="text-gray-400" />} 
          placeholder="搜索车牌号..." 
          size="large"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-lg bg-gray-50 border-0"
        />
      </div>

      <div className="flex-1 p-4">
        {loading ? (
          <div className="flex justify-center items-center h-32">
            <Spin size="large" />
          </div>
        ) : (
          <Card className="w-full shadow-sm border-0 rounded-xl" bodyStyle={{ padding: '0' }}>
            <List
              dataSource={filteredVehicles}
              renderItem={item => (
                <List.Item 
                  className="px-4 py-4 cursor-pointer hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0 block"
                  onClick={() => message.info('功能开发中：点击查看地图轨迹')}
                >
                  <div className="flex justify-between items-center mb-2">
                    <div className="flex items-center">
                      <div className="w-8 h-8 rounded bg-blue-50 text-blue-500 flex items-center justify-center mr-3 font-bold text-sm">
                        <CarOutlined />
                      </div>
                      <span className="font-bold text-gray-800 text-base">{item.plateNo}</span>
                    </div>
                    {getStatusTag(item.status)}
                  </div>
                  
                  <div className="pl-11 pr-2">
                    <div className="flex items-start text-sm text-gray-500 mb-1">
                      <EnvironmentOutlined className="mr-1.5 mt-1 opacity-70" />
                      <span className="leading-tight">{item.location}</span>
                    </div>
                    <div className="flex justify-between text-xs text-gray-400 mt-1.5">
                      <span>更新于：{item.time}</span>
                      {item.speed > 0 && <span>速度：{item.speed} km/h</span>}
                    </div>
                  </div>
                </List.Item>
              )}
            />
            {filteredVehicles.length === 0 && (
              <div className="py-12 text-center text-gray-400">
                暂无匹配的车辆数据
              </div>
            )}
          </Card>
        )}
      </div>
    </div>
  );
};

export default MobileVehicleTracking;
