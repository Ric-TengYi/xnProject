/**
 * 将 frontend 深色系页面复制到 frontend-light 并替换为浅色系 class/style
 * 在项目根目录执行: node frontend-light/scripts/convert-to-light.js
 */
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../..');
const srcDir = path.join(root, 'frontend/src/pages');
const destDir = path.join(root, 'frontend-light/src/pages');

const skipFiles = new Set(['Dashboard.tsx', 'ProjectsManagement.tsx']);
const files = fs.readdirSync(srcDir).filter(f => f.endsWith('.tsx') && !skipFiles.has(f));

const replacements = [
  [/trailColor="rgba\(255,255,255,0\.1\)"/g, 'trailColor="rgba(0,0,0,0.06)"'],
  [/trailColor="rgba\(255,255,255,0\.08\)"/g, 'trailColor="rgba(0,0,0,0.06)"'],
  [/text-slate-900 dark:text-white/g, 'g-text-primary'],
  [/text-slate-700 dark:text-slate-200/g, 'g-text-primary'],
  [/text-slate-600 dark:text-slate-400/g, 'g-text-secondary'],
  [/text-slate-600 dark:text-slate-300/g, 'g-text-secondary'],
  [/text-blue-600 dark:text-blue-500/g, 'g-text-primary-link'],
  [/text-blue-600 dark:text-blue-400/g, 'g-text-primary-link'],
  [/text-green-600 dark:text-green-500/g, 'g-text-success'],
  [/text-green-600 dark:text-green-400/g, 'g-text-success'],
  [/text-red-600 dark:text-red-500/g, 'g-text-error'],
  [/text-red-600 dark:text-red-400/g, 'g-text-error'],
  [/text-orange-600 dark:text-orange-500/g, 'g-text-warning'],
  [/text-orange-600 dark:text-orange-400/g, 'g-text-warning'],
  [/border-slate-200 dark:border-slate-700\/50/g, 'g-border-panel border'],
  [/border-slate-200 dark:border-slate-700/g, 'g-border-panel border'],
  [/bg-slate-50 dark:bg-slate-900\/30/g, 'g-bg-toolbar'],
  [/bg-white dark:bg-slate-800\/80/g, 'bg-white'],
  [/bg-white dark:bg-slate-800\/50/g, 'bg-white'],
  [/bg-white dark:bg-slate-800\/40/g, 'bg-white'],
  [/bg-white dark:bg-slate-800/g, 'bg-white'],
  [/hover:bg-white dark:bg-slate-800\/40/g, 'hover:bg-black/5'],
  [/hover:text-slate-900 dark:text-white/g, 'hover:opacity-90 g-text-primary'],
  [/hover:text-blue-600 dark:text-blue-400/g, 'hover:opacity-90 g-text-primary-link'],
  [/text-slate-600 dark:text-slate-400 bg-white dark:bg-slate-800/g, 'g-text-secondary bg-white'],
  [/bg-slate-800 hover:bg-slate-700 border-none/g, 'g-btn-primary border-none'],
  [/bg-blue-600 hover:bg-blue-500 border-none/g, 'g-btn-primary border-none'],
  [/popupClassName="dark-picker"/g, ''],
];

files.forEach(file => {
  const srcPath = path.join(srcDir, file);
  let content = fs.readFileSync(srcPath, 'utf8');
  replacements.forEach(([from, to]) => {
    content = content.replace(from, to);
  });
  const destPath = path.join(destDir, file);
  fs.writeFileSync(destPath, content);
  console.log('OK', file);
});

console.log('Done. Total:', files.length);
