import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getToken, isDriverOnly } from '../../utils/request'
import { getCurrentOrg, getProjects } from '../../utils/request'
import './index.scss'

export default function Index() {
  const [orgName, setOrgName] = useState<string>('')
  const [projects, setProjects] = useState<Array<{ id: number; name: string; status: string }>>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!getToken()) {
      Taro.redirectTo({ url: '/pages/login/index' })
      return
    }
    if (isDriverOnly()) {
      Taro.redirectTo({ url: '/pages/driver/index' })
      return
    }
    Promise.all([
      getCurrentOrg().then((res) => {
        if (res.code === 200 && res.data) setOrgName((res.data as { orgName: string }).orgName)
      }).catch(() => setOrgName('当前单位（模拟）')),
      getProjects().then((res) => {
        if (res.code === 200 && res.data) setProjects((res.data as { records: Array<{ id: number; name: string; status: string }> }).records || [])
      }).catch(() => setProjects([
        { id: 1, name: '滨海新区基础建设B标段', status: '在建' },
        { id: 2, name: '老旧小区改造工程综合包', status: '在建' },
      ])),
    ]).finally(() => setLoading(false))
  }, [])

  const shortcuts = [
    { title: '出土拍照', url: '/pages/photos/index', icon: '📷' },
    { title: '打卡异常申报', url: '/pages/checkin-exceptions/index', icon: '📋' },
    { title: '延期申报', url: '/pages/delay-applies/index', icon: '📅' },
    { title: '问题反馈', url: '/pages/feedbacks/index', icon: '💬' },
    { title: '司机端', url: '/pages/driver/index', icon: '🚛' },
  ]

  if (loading) {
    return (
      <View className="index">
        <Text className="loading">加载中…</Text>
      </View>
    )
  }

  return (
    <ScrollView className="index" scrollY>
      <View className="section">
        <Text className="label">当前单位</Text>
        <Text className="org-name">{orgName || '—'}</Text>
      </View>
      <View className="section">
        <Text className="label">可用项目</Text>
        {projects.length === 0 ? (
          <Text className="empty">暂无项目</Text>
        ) : (
          projects.map((p) => (
            <View
              key={p.id}
              className="project-item"
              onClick={() => Taro.showToast({ title: p.name, icon: 'none' })}
            >
              <Text className="project-name">{p.name}</Text>
              <Text className="project-status">{p.status}</Text>
            </View>
          ))
        )}
      </View>
      <View className="section">
        <Text className="label">快捷入口</Text>
        <View className="shortcuts">
          {shortcuts.map((s) => (
            <View
              key={s.url}
              className="shortcut-item"
              onClick={() => Taro.navigateTo({ url: s.url })}
            >
              <Text className="shortcut-icon">{s.icon}</Text>
              <Text className="shortcut-title">{s.title}</Text>
            </View>
          ))}
        </View>
      </View>
    </ScrollView>
  )
}
