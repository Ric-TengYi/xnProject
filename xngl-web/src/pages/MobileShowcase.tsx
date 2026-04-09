import React from 'react';
import { Card, Space, Tag, Typography } from 'antd';
import { Link, Navigate, useParams } from 'react-router-dom';
import MobileLogin from './mobile/MobileLogin';
import MobileHome from './mobile/MobileHome';
import MobilePunchIn from './mobile/MobilePunchIn';
import MobileDisposal from './mobile/MobileDisposal';
import MobileEventReport from './mobile/MobileEventReport';
import MobileVehicleTracking from './mobile/MobileVehicleTracking';

const { Paragraph, Title, Text } = Typography;

type ScreenConfig = {
  title: string;
  description: string;
  render: () => React.ReactNode;
};

const screenMap: Record<string, ScreenConfig> = {
  login: {
    title: '小程序登录',
    description: '展示现场账号登录入口，作为移动端作业起点。',
    render: () => <MobileLogin />,
  },
  home: {
    title: '小程序首页',
    description: '展示常用功能入口、通知信息和现场工作台。',
    render: () => <MobileHome />,
  },
  'punch-in': {
    title: '出土打卡',
    description: '展示项目选择、车牌登记、拍照上传和备注留痕。',
    render: () => <MobilePunchIn />,
  },
  disposal: {
    title: '消纳确认',
    description: '展示合同关联、消纳方量、票据上传和确认提交流程。',
    render: () => <MobileDisposal />,
  },
  'event-report': {
    title: '事件上报',
    description: '展示异常上报、紧急程度、现场说明和图片上传。',
    render: () => <MobileEventReport />,
  },
  'vehicle-tracking': {
    title: '车辆跟踪',
    description: '展示现场车辆状态、位置和更新时间。',
    render: () => <MobileVehicleTracking />,
  },
};

const orderedScreens = ['login', 'home', 'punch-in', 'disposal', 'event-report', 'vehicle-tracking'];

const MobileShowcase: React.FC = () => {
  const { screen = 'home' } = useParams();
  const current = screenMap[screen];

  if (!current) {
    return <Navigate to="/mobile-showcase/home" replace />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Title level={2} style={{ marginBottom: 4 }}>
            移动作业端正式演示
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0, maxWidth: 860 }}>
            本页用于正式录屏，逐页展示小程序真实功能界面，覆盖登录、首页、出土打卡、消纳确认、事件上报和车辆跟踪。
          </Paragraph>
        </div>
        <Space wrap>
          <Tag color="blue">移动作业</Tag>
          <Tag color="green">现场留痕</Tag>
          <Tag color="orange">车辆监管</Tag>
        </Space>
      </div>

      <Card
        className="glass-panel g-border-panel border"
        bodyStyle={{ padding: 24, background: 'linear-gradient(160deg, rgba(15,98,255,0.06), rgba(255,255,255,0.96))' }}
      >
        <div className="flex flex-wrap gap-3">
          {orderedScreens.map((key) => (
            <Link key={key} to={`/mobile-showcase/${key}`}>
              <Tag color={screen === key ? 'processing' : 'default'} style={{ padding: '6px 12px', cursor: 'pointer' }}>
                {screenMap[key].title}
              </Tag>
            </Link>
          ))}
        </div>

        <div className="mt-5 grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
          <Card className="border" bodyStyle={{ padding: 20 }}>
            <div className="space-y-3">
              <div>
                <Text type="secondary">当前演示页面</Text>
                <div className="mt-1 text-xl font-semibold text-slate-800">{current.title}</div>
              </div>
              <Paragraph style={{ marginBottom: 0, lineHeight: 1.75 }}>{current.description}</Paragraph>
              <div className="rounded-2xl bg-slate-50 p-4 text-sm leading-7 text-slate-600">
                本页画面用于甲方演示视频录制，重点体现移动端与后台平台共用同一套业务场景与作业链路。
              </div>
            </div>
          </Card>

          <div
            className="rounded-[40px] border border-slate-200 bg-slate-900 p-3 shadow-2xl"
            style={{ width: 430, margin: '0 auto' }}
          >
            <div className="mx-auto mb-3 h-2.5 w-28 rounded-full bg-slate-700" />
            <div
              className="overflow-hidden rounded-[32px] bg-white"
              style={{ height: 820 }}
            >
              {current.render()}
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default MobileShowcase;
