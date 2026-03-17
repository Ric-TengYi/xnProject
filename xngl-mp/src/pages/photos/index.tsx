import { View, Text, ScrollView, Button, Input } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { getPhotos, uploadPhoto } from '../../utils/request'
import './index.scss'

type PhotoItem = { id: number; projectId: number; projectName?: string; shootTime: string; remark?: string; auditStatus: string }

export default function Photos() {
  const [list, setList] = useState<PhotoItem[]>([])
  const [loading, setLoading] = useState(true)
  const [showUpload, setShowUpload] = useState(false)
  const [remark, setRemark] = useState('')
  const [projectId, setProjectId] = useState(1)
  const [submitting, setSubmitting] = useState(false)

  const load = () => {
    setLoading(true)
    getPhotos({ pageNo: 1, pageSize: 20 })
      .then((res) => {
        if (res.code === 200 && res.data && (res.data as { records?: PhotoItem[] }).records)
          setList((res.data as { records: PhotoItem[] }).records)
      })
      .catch(() => setList([
        { id: 1, projectId: 1, projectName: '滨海新区B标段', shootTime: '2024-03-15 10:30', remark: '出土现场', auditStatus: 'PENDING' },
        { id: 2, projectId: 2, projectName: '老旧小区改造', shootTime: '2024-03-14 14:00', auditStatus: 'APPROVED' },
      ]))
      .finally(() => setLoading(false))
  }

  useEffect(() => load(), [])

  const chooseImage = () => {
    Taro.chooseImage({ count: 1, sourceType: ['camera', 'album'] })
      .then((res) => {
        if (res.tempFilePaths && res.tempFilePaths[0]) {
          setShowUpload(true)
          // 实际应先上传文件拿到 fileId，再调 uploadPhoto
        }
      })
      .catch(() => Taro.showToast({ title: '选择图片失败', icon: 'none' }))
  }

  const submitPhoto = () => {
    setSubmitting(true)
    uploadPhoto({ projectId, remark: remark || undefined })
      .then((res) => {
        if (res.code === 200) {
          Taro.showToast({ title: '上传成功', icon: 'success' })
          setShowUpload(false)
          setRemark('')
          load()
        }
      })
      .catch(() => Taro.showToast({ title: '上传失败，请重试', icon: 'none' }))
      .finally(() => setSubmitting(false))
  }

  return (
    <View className="photos-page">
      <View className="toolbar">
        <Button className="btn-primary" onClick={chooseImage}>拍照 / 选图</Button>
      </View>
      {loading ? (
        <Text className="loading">加载中…</Text>
      ) : (
        <ScrollView scrollY className="list">
          {list.length === 0 ? (
            <Text className="empty">暂无拍照记录</Text>
          ) : (
            list.map((item) => (
              <View key={item.id} className="item">
                <Text className="project">{item.projectName || '项目#' + item.projectId}</Text>
                <Text className="time">{item.shootTime}</Text>
                {item.remark && <Text className="remark">{item.remark}</Text>}
                <Text className="status">{item.auditStatus === 'APPROVED' ? '已通过' : item.auditStatus === 'REJECTED' ? '已驳回' : '待审核'}</Text>
              </View>
            ))
          )}
        </ScrollView>
      )}

      {showUpload && (
        <View className="upload-mask">
          <View className="upload-panel">
            <Text className="panel-title">补充信息</Text>
            <Text className="label">项目（可选）</Text>
            <Text className="value">项目 #{projectId}</Text>
            <Text className="label">备注</Text>
            <Input
              className="input"
              placeholder="选填备注"
              value={remark}
              onInput={(e) => setRemark(e.detail.value)}
            />
            <View className="actions">
              <Button size="mini" onClick={() => setShowUpload(false)}>取消</Button>
              <Button size="mini" type="primary" loading={submitting} onClick={submitPhoto}>提交</Button>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}
