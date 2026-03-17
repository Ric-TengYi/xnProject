import { View, Text, ScrollView, Button, Input, Textarea } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getDelayApplies, submitDelayApply } from '../../utils/request'
import './index.scss'

type Item = { id: number; bizType: string; projectId: number; projectName?: string; requestedEndTime: string; status: string; createTime: string }

export default function DelayApplies() {
  const [list, setList] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [reason, setReason] = useState('')
  const [requestedEndTime, setRequestedEndTime] = useState('')
  const [projectId, setProjectId] = useState(1)
  const [submitting, setSubmitting] = useState(false)

  const load = () => {
    setLoading(true)
    getDelayApplies({ pageNo: 1, pageSize: 20 })
      .then((res) => {
        if (res.code === 200 && res.data && (res.data as { records?: Item[] }).records)
          setList((res.data as { records: Item[] }).records)
      })
      .catch(() => setList([
        { id: 1, bizType: 'PROJECT', projectId: 1, projectName: '滨海新区B标段', requestedEndTime: '2024-06-30', status: 'SUBMITTED', createTime: '2024-03-15 10:00' },
      ]))
      .finally(() => setLoading(false))
  }

  useEffect(() => load(), [])

  const submit = () => {
    if (!requestedEndTime.trim()) {
      Taro.showToast({ title: '请选择延期截止日期', icon: 'none' })
      return
    }
    if (!reason.trim()) {
      Taro.showToast({ title: '请填写原因', icon: 'none' })
      return
    }
    setSubmitting(true)
    submitDelayApply({ bizType: 'PROJECT', projectId, requestedEndTime: requestedEndTime.trim(), reason: reason.trim() })
      .then((res) => {
        if (res.code === 200) {
          Taro.showToast({ title: '提交成功', icon: 'success' })
          setShowForm(false)
          setReason('')
          setRequestedEndTime('')
          load()
        }
      })
      .catch(() => Taro.showToast({ title: '提交失败', icon: 'none' }))
      .finally(() => setSubmitting(false))
  }

  const statusText = (s: string) => ({ SUBMITTED: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)

  return (
    <View className="page">
      <View className="toolbar">
        <Button className="btn-primary" onClick={() => setShowForm(true)}>新增延期申报</Button>
      </View>
      {loading ? (
        <Text className="loading">加载中…</Text>
      ) : (
        <ScrollView scrollY className="list">
          {list.length === 0 ? (
            <Text className="empty">暂无申报记录</Text>
          ) : (
            list.map((item) => (
              <View key={item.id} className="item">
                <Text className="project">{item.projectName || '项目#' + item.projectId}</Text>
                <Text className="time">申请延期至 {item.requestedEndTime}</Text>
                <Text className="create">申报时间 {item.createTime}</Text>
                <Text className="status">{statusText(item.status)}</Text>
              </View>
            ))
          )}
        </ScrollView>
      )}
      {showForm && (
        <View className="mask">
          <View className="panel">
            <Text className="title">延期申报</Text>
            <Text className="label">延期至日期 *</Text>
            <Input className="input" type="text" placeholder="如 2024-06-30" value={requestedEndTime} onInput={(e) => setRequestedEndTime(e.detail.value)} />
            <Text className="label">原因说明 *</Text>
            <Textarea className="textarea" placeholder="请填写申请延期原因" value={reason} onInput={(e) => setReason(e.detail.value)} />
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
