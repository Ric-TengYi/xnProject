import { View, Text, Input, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useState } from 'react'
import { login, setToken, setUserInfo, getToken, isDriverOnly } from '../../utils/request'
import './index.scss'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  if (getToken()) {
    Taro.redirectTo({ url: isDriverOnly() ? '/pages/driver/index' : '/pages/index/index' })
    return null
  }

  const handleLogin = async () => {
    if (!username.trim()) {
      Taro.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!password.trim()) {
      Taro.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    setLoading(true)
    try {
      const res = await login({ username: username.trim(), password })
      if (res.code === 200 && res.data && (res.data as { token?: string }).token) {
        const d = res.data as { token: string; user?: { id: number; name: string; role?: string; userType?: string } }
        setToken(d.token)
        if (d.user) setUserInfo({ id: d.user.id, name: d.user.name, role: d.user.role || 'USER', userType: d.user.userType })
        Taro.showToast({ title: '登录成功', icon: 'success' })
        const isDriver = d.user && (d.user.role === 'DRIVER' || d.user.userType === 'DRIVER')
        setTimeout(() => Taro.redirectTo({ url: isDriver ? '/pages/driver/index' : '/pages/index/index' }), 500)
      } else {
        setToken('mock_token_' + Date.now())
        setUserInfo(null)
        Taro.showToast({ title: '已使用模拟登录', icon: 'none' })
        setTimeout(() => Taro.redirectTo({ url: '/pages/index/index' }), 500)
      }
    } catch {
      setToken('mock_token_' + Date.now())
      setUserInfo(null)
      Taro.showToast({ title: '接口未就绪，已模拟登录', icon: 'none' })
      setTimeout(() => Taro.redirectTo({ url: '/pages/index/index' }), 500)
    } finally {
      setLoading(false)
    }
  }

  return (
    <View className="login">
      <Text className="title">消纳移动端</Text>
      <Text className="subtitle">登录 / 绑定账号</Text>
      <View className="form">
        <Input
          className="input"
          placeholder="请输入账号"
          value={username}
          onInput={(e) => setUsername(e.detail.value)}
        />
        <Input
          className="input"
          type="password"
          placeholder="请输入密码"
          value={password}
          onInput={(e) => setPassword(e.detail.value)}
        />
        <Button className="btn" onClick={handleLogin} loading={loading}>
          登录
        </Button>
      </View>
      <Text className="tip">未绑定账号请联系管理员在 PC 端创建账号后使用账号密码登录</Text>
    </View>
  )
}
