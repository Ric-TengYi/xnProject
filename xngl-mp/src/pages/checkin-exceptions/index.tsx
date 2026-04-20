import { View, Text, ScrollView, Button, Input, Textarea } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getCheckinExceptions, submitCheckinException } from '../../utils/request'
import './index.scss'

type Item = { id: number; checkinRecordId: string; exceptionType: string; reason: string; status: string; createTime: string }

export default function CheckinExceptions() {
  const [list, setList] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [reason, setReason] = useState('')
  const [exceptionType, setExceptionType] = useState('TIME_ERROR')
  const [submitting, setSubmitting] = useState(false)

  const load = () => {
    setLoading(true)
    getCheckinExceptions({ pageNo: 1, pageSize: 20 })
      .then((res) => {
        if (res.code === 200 && res.data && (res.data as { records?: Item[] }).records)
          setList((res.data as { records: Item[] }).records)
      })
      .catch(() => setList([
        { id: 1, checkinRecordId: 'CK-001', exceptionType: 'TIME_ERROR', reason: '设备时间异常', status: 'SUBMITTED', createTime: '2024-03-15 10:00' },
      ]))
      .finally(() => setLoading(false))
  }

  useEffect(() => load(), [])

  const submit = () => {
    if (!reason.trim()) {
      Taro.showToast({ title: '请填写原因', icon: 'none' })
      return
    }
    setSubmitting(true)
    submitCheckinException({ checkinRecordId: 'CK-MOCK', exceptionType, reason: reason.trim() })
      .then((res) => {
        if (res.code === 200) {
          Taro.showToast({ title: '提交成功', icon: 'success' })
          setShowForm(false)
          setReason('')
          load()
        }
      })
      .catch(() => Taro.showToast({ title: '提交失败', icon: 'none' }))
      .finally(() => setSubmitting(false))
  }

  const statusText = (s: string) => ({ SUBMITTED: '待审核', REVIEWING: '审核中', APPROVED: '已通过', REJECTED: '已驳回', CLOSED: '已关闭' }[s] || s)

  return (
    <View className="page">
      <View className="toolbar">
        <Button className="btn-primary" onClick={() => setShowForm(true)}>新增申报</Button>
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
                <Text className="record">打卡记录 {item.checkinRecordId}</Text>
                <Text className="reason">{item.reason}</Text>
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
            <Text className="title">打卡异常申报</Text>
            <Text className="label">异常类型</Text>
            <Text className="value">时间异常</Text>
            <Text className="label">原因说明 *</Text>
            <Textarea className="textarea" placeholder="请填写原因" value={reason} onInput={(e) => setReason(e.detail.value)} />
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
