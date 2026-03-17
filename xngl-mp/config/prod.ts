export default {
  env: {
    NODE_ENV: '"production"',
  },
  defineConstants: {},
  mini: {},
  h5: {
    /**
     * WebpackChain 插件配置
     * 可对接现有后端 API 域名
     */
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    webpackChain(chain) {
      // 示例：chain.plugin('define').use(require('webpack').DefinePlugin, [{ 'process.env.API_BASE': JSON.stringify('https://api.example.com') }])
    },
  },
}
