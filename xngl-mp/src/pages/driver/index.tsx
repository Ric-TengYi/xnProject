import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getDriverHome } from '../../utils/request'
import './index.scss'

export default function Driver() {
  const [tasks, setTasks] = useState<unknown[]>([])
  const [alerts, setAlerts] = useState<unknown[]>([])
  const [permits, setPermits] = useState<unknown[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDriverHome()
      .then((res) => {
        if (res.code === 200 && res.data) {
          const d = res.data as { tasks?: unknown[]; alerts?: unknown[]; permits?: unknown[] }
          setTasks(d.tasks || [])
          setAlerts(d.alerts || [])
          setPermits(d.permits || [])
        }
      })
      .catch(() => {
        setTasks([{ id: 1, projectName: '滨海新区B标段', siteName: '东区消纳场', status: 'IN_PROGRESS' }])
        setAlerts([{ id: 1, type: 'OVERSPEED', message: '超速预警', time: '2024-03-15 09:00' }])
        setPermits([{ id: 1, permitNo: 'PZ-2024-001', validUntil: '2024-12-31', status: 'VALID' }])
      })
      .finally(() => setLoading(false))
  }, [])

  const cards = [
    { title: '今日任务', count: tasks.length, url: '/pages/driver/index', icon: '📋' },
    { title: '预警', count: alerts.length, url: '/pages/driver/index', icon: '⚠️' },
    { title: '处置证', count: permits.length, url: '/pages/driver/index', icon: '📄' },
    { title: '打卡异常申报', url: '/pages/checkin-exceptions/index', icon: '📋' },
    { title: '问题反馈', url: '/pages/feedbacks/index', icon: '💬' },
  ]

  if (loading) {
    return (
      <View className="driver-page">
        <Text className="loading">加载中…</Text>
      </View>
    )
  }

  return (
    <ScrollView className="driver-page" scrollY>
      <View className="section">
        <Text className="section-title">司机工作台</Text>
        <View className="cards">
          {cards.map((c) => (
            <View
              key={c.title}
              className="card"
              onClick={() => (c.url === '/pages/driver/index' ? null : Taro.navigateTo({ url: c.url }))}
            >
              <Text className="card-icon">{c.icon}</Text>
              <Text className="card-title">{c.title}</Text>
              {'count' in c && c.count !== undefined && <Text className="card-count">{c.count}</Text>}
            </View>
          ))}
        </View>
      </View>
      <View className="section">
        <Text className="section-title">今日任务</Text>
        {tasks.length === 0 ? (
          <Text className="empty">暂无任务</Text>
        ) : (
          (tasks as Array<{ id: number; projectName?: string; siteName?: string; status?: string }>).map((t) => (
            <View key={t.id} className="task-item">
              <Text className="task-name">{t.projectName || '项目'} - {t.siteName || '场地'}</Text>
              <Text className="task-status">{t.status === 'IN_PROGRESS' ? '进行中' : t.status}</Text>
            </View>
          ))
        )}
      </View>
      <View className="section">
        <Text className="section-title">待处理预警</Text>
        {alerts.length === 0 ? (
          <Text className="empty">暂无预警</Text>
        ) : (
          (alerts as Array<{ id: number; type?: string; message?: string; time?: string }>).map((a) => (
            <View key={a.id} className="alert-item">
              <Text className="alert-msg">{a.message || a.type}</Text>
              <Text className="alert-time">{a.time}</Text>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  )
}
