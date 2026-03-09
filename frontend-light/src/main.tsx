import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { ThemeProvider } from './contexts/ThemeContext'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#0F62FF',
          colorBgBase: '#F0F3F6',
          colorBgContainer: '#ffffff',
          colorBgElevated: '#ffffff',
          colorTextBase: '#4a4a4a',
          colorTextSecondary: '#7c7c7c',
          colorBorder: '#e0e0e0',
          borderRadius: 8,
          wireframe: false,
        },
        components: {
          Layout: {
            headerBg: '#ffffff',
            siderBg: '#ffffff',
          },
          Card: {
            colorBgContainer: '#ffffff',
          },
        },
      }}
    >
      <ThemeProvider>
        <App />
      </ThemeProvider>
    </ConfigProvider>
  </StrictMode>,
)
