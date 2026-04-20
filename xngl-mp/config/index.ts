const config = {
  projectName: 'xngl-mp',
  date: '2024-1-1',
  designWidth: 375,
  deviceRatio: {
    640: 2.34 / 2,
    750: 1,
    375: 2,
    828: 1.81 / 2,
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: [
    '@tarojs/plugin-framework-react',
    '@tarojs/plugin-platform-h5',
    '@tarojs/plugin-platform-alipay-dd', // 钉钉小程序（build:dd 时生效）
  ],
  defineConstants: {},
  copy: {
    patterns: [],
    options: {},
  },
  framework: 'react',
  compiler: 'webpack5',
  cache: {
    enable: false,
  },
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {},
      },
      url: {
        enable: true,
        config: {
          limit: 1024,
        },
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]',
        },
      },
    },
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    output: {
      filename: 'js/[name].[hash:8].js',
      chunkFilename: 'js/[name].[chunkhash:8].js',
    },
    miniCssExtractPluginOption: {
      ignoreOrder: true,
    },
    postcss: {
      autoprefixer: {
        enable: true,
        config: {},
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]',
        },
      },
    },
    devServer: {
      port: 10086,
      host: '0.0.0.0',
      // 根路径 / 直接返回 index 模板，避免 dist 为空时显示目录列表（~/）
      setupMiddlewares: (middlewares, devServer) => {
        const path = require('path')
        const fs = require('fs')
        const indexPath = path.join(__dirname, '..', 'index.template.html')
        middlewares.unshift({
          name: 'serve-root-index',
          path: '/',
          middleware: (req, res, next) => {
            if (req.url === '/' || req.url === '/index.html') {
              fs.readFile(indexPath, 'utf8', (err, data) => {
                if (err) return next()
                res.setHeader('Content-Type', 'text/html')
                res.end(data)
              })
            } else {
              next()
            }
          },
        })
        return middlewares
      },
    },
  },
}

export default function (merge: (a: object, b: object) => object) {
  if (process.env.NODE_ENV === 'development') {
    return merge({}, config, require('./dev').default)
  }
  return merge({}, config, require('./prod').default)
}
