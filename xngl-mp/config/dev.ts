export default {
  env: {
    NODE_ENV: '"development"',
  },
  defineConstants: {
    'process.env.TARO_APP_API_BASE': JSON.stringify('http://localhost:8090'),
  },
  mini: {},
  h5: {
    // 开发时用固定 bundle 名，便于 index.template.html 引用 /js/app.js，避免根路径显示目录列表（~/）
    output: {
      filename: 'js/[name].js',
      chunkFilename: 'js/[name].js',
    },
  },
}
