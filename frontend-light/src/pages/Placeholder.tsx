import React from 'react';

interface PlaceholderProps {
  title: string;
}

const Placeholder: React.FC<PlaceholderProps> = ({ title }) => (
  <div className="flex items-center justify-center h-full min-h-[400px]">
    <h2
      className="text-2xl font-light"
      style={{ color: 'var(--text-secondary)' }}
    >
      {title} 页面开发中...
    </h2>
  </div>
);

export default Placeholder;
