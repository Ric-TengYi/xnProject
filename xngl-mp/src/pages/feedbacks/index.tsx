import { View, Text, ScrollView, Button, Input, Textarea } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getFeedbacks, submitFeedback } from '../../utils/request'
import './index.scss'

type Item = { id: number; feedbackType: string; content: string; status: string; createTime: string }

export default function Feedbacks() {
  const [list, setList] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [content, setContent] = useState('')
  const [feedbackType, setFeedbackType] = useState('OTHER')
  const [submitting, setSubmitting] = useState(false)

  const load = () => {
    setLoading(true)
    getFeedbacks({ pageNo: 1, pageSize: 20 })
      .then((res) => {
        if (res.code === 200 && res.data && (res.data as { records?: Item[] }).records)
          setList((res.data as { records: Item[] }).records)
      })
      .catch(() => setList([
        { id: 1, feedbackType: 'OTHER', content: '现场设备显示异常', status: 'OPEN', createTime: '2024-03-15 10:00' },
      ]))
      .finally(() => setLoading(false))
  }

  useEffect(() => load(), [])

  const submit = () => {
    if (!content.trim()) {
      Taro.showToast({ title: '请填写反馈内容', icon: 'none' })
      return
    }
    setSubmitting(true)
    submitFeedback({ feedbackType, content: content.trim() })
      .then((res) => {
        if (res.code === 200) {
          Taro.showToast({ title: '提交成功', icon: 'success' })
          setShowForm(false)
          setContent('')
          load()
        }
      })
      .catch(() => Taro.showToast({ title: '提交失败', icon: 'none' }))
      .finally(() => setSubmitting(false))
  }

  const statusText = (s: string) => ({ OPEN: '未处理', PROCESSING: '处理中', CLOSED: '已关闭' }[s] || s)

  return (
    <View className="page">
      <View className="toolbar">
        <Button className="btn-primary" onClick={() => setShowForm(true)}>新增反馈</Button>
      </View>
      {loading ? (
        <Text className="loading">加载中…</Text>
      ) : (
        <ScrollView scrollY className="list">
          {list.length === 0 ? (
            <Text className="empty">暂无反馈记录</Text>
          ) : (
            list.map((item) => (
              <View key={item.id} className="item">
                <Text className="content">{item.content}</Text>
                <Text className="time">{item.createTime}</Text>
                <Text className="status">{statusText(item.status)}</Text>
              </View>
            ))
          )}
        </ScrollView>
      )}
      {showForm && (
        <View className="mask">
          <View className="panel">
            <Text className="title">问题反馈</Text>
            <Text className="label">反馈内容 *</Text>
            <Textarea className="textarea" placeholder="请描述您遇到的问题" value={content} onInput={(e) => setContent(e.detail.value)} />
            <View className="actions">
              <Button size="mini" onClick={() => setShowForm(false)}>取消</Button>
              <Button size="mini" type="primary" loading={submitting} onClick={submit}>提交</Button>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}
