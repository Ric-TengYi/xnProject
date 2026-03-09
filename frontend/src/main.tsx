import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import './index.css'
import App from './App.tsx'
import { ThemeProvider, useTheme } from './contexts/ThemeContext'

const ThemedApp = () => {
  const { isDarkMode } = useTheme();

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: isDarkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          colorBgBase: isDarkMode ? '#020617' : '#f1f5f9',
          colorBgContainer: isDarkMode ? 'rgba(15, 23, 42, 0.4)' : '#ffffff',
          colorBgElevated: isDarkMode ? 'rgba(30, 41, 59, 0.8)' : '#ffffff',
          colorTextBase: isDarkMode ? '#f8fafc' : '#0f172a',
          colorTextSecondary: isDarkMode ? '#94a3b8' : '#475569',
          colorBorder: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : '#e2e8f0',
          borderRadius: 8,
          wireframe: false,
        },
        components: {
          Layout: {
            headerBg: isDarkMode ? 'rgba(2, 6, 23, 0.9)' : '#ffffff',
            siderBg: isDarkMode ? 'rgba(2, 6, 23, 0.9)' : '#ffffff',
          },
          Card: {
            colorBgContainer: isDarkMode ? 'rgba(15, 23, 42, 0.6)' : '#ffffff',
          }
        }
      }}
    >
      <App />
    </ConfigProvider>
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <ThemedApp />
    </ThemeProvider>
  </StrictMode>,
)
