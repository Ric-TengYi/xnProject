import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, DatePicker, Input, Select, Table, Tabs, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ExportOutlined, SearchOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import type { Dayjs } from 'dayjs';
import request from '../utils/request';

const { RangePicker } = DatePicker;

type RangeValue = [Dayjs | null, Dayjs | null] | null;

type LogPagination = { current: number; pageSize: number; total: number };

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);

const SystemLogs: React.FC = () => {
  const [loginLogs, setLoginLogs] = useState<any[]>([]);
  const [operateLogs, setOperateLogs] = useState<any[]>([]);
  const [errorLogs, setErrorLogs] = useState<any[]>([]);
  const [loginLoading, setLoginLoading] = useState(false);
  const [operateLoading, setOperateLoading] = useState(false);
  const [errorLoading, setErrorLoading] = useState(false);
  const [exporting, setExporting] = useState<'login' | 'operate' | 'error' | null>(null);
  const [loginPagination, setLoginPagination] = useState<LogPagination>({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [operatePagination, setOperatePagination] = useState<LogPagination>({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [errorPagination, setErrorPagination] = useState<LogPagination>({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const [loginKeyword, setLoginKeyword] = useState('');
  const [loginStatus, setLoginStatus] = useState('all');
  const [loginRange, setLoginRange] = useState<RangeValue>(null);

  const [operateKeyword, setOperateKeyword] = useState('');
  const [operateModule, setOperateModule] = useState('');
  const [operateRange, setOperateRange] = useState<RangeValue>(null);

  const [errorKeyword, setErrorKeyword] = useState('');
  const [errorLevel, setErrorLevel] = useState('all');
  const [errorRange, setErrorRange] = useState<RangeValue>(null);

  const tenantId = useMemo(() => {
    try {
      const raw = localStorage.getItem('userInfo');
      const parsed = raw ? JSON.parse(raw) : {};
      return parsed.tenantId || '1';
    } catch {
      return '1';
    }
  }, []);

  const buildRangeParams = (range: RangeValue) => ({
    startTime: range?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endTime: range?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
  });

  const buildLoginParams = (pageNo = loginPagination.current, pageSize = loginPagination.pageSize) => ({
    tenantId,
    keyword: loginKeyword.trim() || undefined,
    status: loginStatus === 'all' ? undefined : loginStatus,
    ...buildRangeParams(loginRange),
    pageNo,
    pageSize,
  });

  const buildOperateParams = (
    pageNo = operatePagination.current,
    pageSize = operatePagination.pageSize,
  ) => ({
    tenantId,
    keyword: operateKeyword.trim() || undefined,
    module: operateModule.trim() || undefined,
    ...buildRangeParams(operateRange),
    pageNo,
    pageSize,
  });

  const buildErrorParams = (pageNo = errorPagination.current, pageSize = errorPagination.pageSize) => ({
    tenantId,
    keyword: errorKeyword.trim() || undefined,
    level: errorLevel === 'all' ? undefined : errorLevel,
    ...buildRangeParams(errorRange),
    pageNo,
    pageSize,
  });

  const fetchLoginLogs = async (pageNo = loginPagination.current, pageSize = loginPagination.pageSize) => {
    setLoginLoading(true);
    try {
      const res = await request.get('/login-logs', { params: buildLoginParams(pageNo, pageSize) });
      if (res.code === 200 && res.data) {
        const records = (res.data.records || []).map((r: any) => ({
          id: r.id,
          account: r.username,
          time: r.loginTime,
          ip: r.ip,
          browser: r.userAgent || '-',
          os: '-',
          status: r.success ? '成功' : '失败',
          failReason: r.failReason || '-',
        }));
        setLoginLogs(records);
        setLoginPagination((prev) => ({ ...prev, total: res.data.total || 0 }));
      }
    } catch (error) {
      console.error(error);
      setLoginLogs([]);
      setLoginPagination((prev) => ({ ...prev, total: 0 }));
    } finally {
      setLoginLoading(false);
    }
  };

  const fetchOperateLogs = async (
    pageNo = operatePagination.current,
    pageSize = operatePagination.pageSize,
  ) => {
    setOperateLoading(true);
    try {
      const res = await request.get('/operation-logs', {
        params: buildOperateParams(pageNo, pageSize),
      });
      if (res.code === 200 && res.data) {
        const records = (res.data.records || []).map((r: any) => ({
          id: r.id,
          operator: r.username,
          module: r.module,
          action: r.operation,
          content: r.content || `${r.method || ''} ${r.requestUri || ''}`.trim() || '-',
          time: r.createTime,
          ip: r.ip,
          requestUri: r.requestUri || '-',
          method: r.method || '-',
          durationMs: r.durationMs || 0,
        }));
        setOperateLogs(records);
        setOperatePagination((prev) => ({ ...prev, total: res.data.total || 0 }));
      }
    } catch (error) {
      console.error(error);
      setOperateLogs([]);
      setOperatePagination((prev) => ({ ...prev, total: 0 }));
    } finally {
      setOperateLoading(false);
    }
  };

  const fetchErrorLogs = async (pageNo = errorPagination.current, pageSize = errorPagination.pageSize) => {
    setErrorLoading(true);
    try {
      const res = await request.get('/error-logs', {
        params: buildErrorParams(pageNo, pageSize),
      });
      if (res.code === 200 && res.data) {
        const records = (res.data.records || []).map((r: any) => ({
          id: r.id,
          operator: r.username || '-',
          level: r.level || 'ERROR',
          exceptionType: r.exceptionType || '-',
          message: r.errorMessage || '-',
          requestUri: r.requestUri || '-',
          method: r.httpMethod || '-',
          ip: r.ip || '-',
          time: r.createTime || '-',
        }));
        setErrorLogs(records);
        setErrorPagination((prev) => ({ ...prev, total: res.data.total || 0 }));
      }
    } catch (error) {
      console.error(error);
      setErrorLogs([]);
      setErrorPagination((prev) => ({ ...prev, total: 0 }));
    } finally {
      setErrorLoading(false);
    }
  };

  useEffect(() => {
    void fetchLoginLogs(loginPagination.current, loginPagination.pageSize);
  }, [loginPagination.current, loginPagination.pageSize, loginKeyword, loginStatus, loginRange]);

  useEffect(() => {
    void fetchOperateLogs(operatePagination.current, operatePagination.pageSize);
  }, [operatePagination.current, operatePagination.pageSize, operateKeyword, operateModule, operateRange]);

  useEffect(() => {
    void fetchErrorLogs(errorPagination.current, errorPagination.pageSize);
  }, [errorPagination.current, errorPagination.pageSize, errorKeyword, errorLevel, errorRange]);

  const handleExport = async (type: 'login' | 'operate' | 'error') => {
    setExporting(type);
    try {
      const config =
        type === 'login'
          ? { url: '/login-logs/export', params: buildLoginParams(1, 2000), fileName: 'login_logs.csv' }
          : type === 'operate'
            ? {
                url: '/operation-logs/export',
                params: buildOperateParams(1, 2000),
                fileName: 'operation_logs.csv',
              }
            : { url: '/error-logs/export', params: buildErrorParams(1, 2000), fileName: 'error_logs.csv' };
      const blob = (await request.get(config.url, {
        params: config.params,
        responseType: 'blob',
      })) as unknown as Blob;
      downloadBlob(blob, config.fileName);
      message.success('日志导出成功');
    } catch (error) {
      console.error(error);
      message.error('日志导出失败');
    } finally {
      setExporting(null);
    }
  };

  const loginColumns: ColumnsType<any> = [
    {
      title: '登录账号',
      dataIndex: 'account',
      key: 'account',
      render: (value: string) => <strong className="g-text-primary">{value}</strong>,
    },
    {
      title: '登录时间',
      dataIndex: 'time',
      key: 'time',
      render: (value: string) => <span className="g-text-secondary font-mono">{value}</span>,
    },
    {
      title: 'IP 地址',
      dataIndex: 'ip',
      key: 'ip',
      render: (value: string) => <span className="g-text-secondary">{value}</span>,
    },
    {
      title: '终端信息',
      key: 'device',
      render: (_, record) => <span className="g-text-secondary text-sm">{record.browser || '-'}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '成功' ? 'success' : 'error'} className="border-none">
          {status}
        </Tag>
      ),
    },
    {
      title: '失败原因',
      dataIndex: 'failReason',
      key: 'failReason',
      render: (value: string) => <span className="g-text-secondary">{value}</span>,
    },
  ];

  const operateColumns: ColumnsType<any> = [
    {
      title: '操作人',
      dataIndex: 'operator',
      key: 'operator',
      render: (value: string) => <strong className="g-text-primary">{value}</strong>,
    },
    {
      title: '操作模块',
      dataIndex: 'module',
      key: 'module',
      render: (value: string) => (
        <Tag color="blue" className="border-none">
          {value || '-'}
        </Tag>
      ),
    },
    {
      title: '操作类型',
      dataIndex: 'action',
      key: 'action',
      render: (value: string) => <span className="g-text-secondary">{value}</span>,
    },
    {
      title: '操作内容',
      dataIndex: 'content',
      key: 'content',
      render: (value: string) => <span className="g-text-secondary">{value}</span>,
    },
    {
      title: '操作时间',
      dataIndex: 'time',
      key: 'time',
      render: (value: string) => <span className="g-text-secondary font-mono">{value}</span>,
    },
    {
      title: 'IP 地址',
      dataIndex: 'ip',
      key: 'ip',
      render: (value: string) => <span className="g-text-secondary text-sm">{value}</span>,
    },
  ];

  const errorColumns: ColumnsType<any> = [
    {
      title: '记录时间',
      dataIndex: 'time',
      key: 'time',
      render: (value: string) => <span className="g-text-secondary font-mono">{value}</span>,
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      render: (value: string) => (
        <Tag color={value === 'ERROR' ? 'error' : 'warning'} className="border-none">
          {value}
        </Tag>
      ),
    },
    {
      title: '异常类型',
      dataIndex: 'exceptionType',
      key: 'exceptionType',
      render: (value: string) => <span className="g-text-primary">{value}</span>,
    },
    {
      title: '异常信息',
      dataIndex: 'message',
      key: 'message',
      render: (value: string) => <span className="g-text-secondary">{value}</span>,
    },
    {
      title: '请求',
      key: 'request',
      render: (_, record) => (
        <span className="g-text-secondary text-sm">
          {record.method} {record.requestUri}
        </span>
      ),
    },
    {
      title: '操作人/IP',
      key: 'operatorIp',
      render: (_, record) => (
        <span className="g-text-secondary text-sm">
          {record.operator} / {record.ip}
        </span>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">系统日志</h1>
          <p className="g-text-secondary mt-1">查询系统登录日志、操作行为审计及系统错误日志</p>
        </div>
      </div>
      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <Tabs
          defaultActiveKey="operate"
          className="custom-tabs px-6 pt-4"
          items={[
            {
              key: 'operate',
              label: '操作日志',
              children: (
                <>
                  <div className="pb-4 flex flex-wrap gap-4">
                    <Input
                      placeholder="搜索操作人/模块/内容"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      className="w-64 bg-white g-border-panel border g-text-primary"
                      value={operateKeyword}
                      onChange={(e) => {
                        setOperateKeyword(e.target.value);
                        setOperatePagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <Input
                      placeholder="模块精确筛选"
                      className="w-48 bg-white g-border-panel border g-text-primary"
                      value={operateModule}
                      onChange={(e) => {
                        setOperateModule(e.target.value);
                        setOperatePagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <RangePicker
                      value={operateRange}
                      className="bg-white g-border-panel border g-text-primary"
                      onChange={(value) => {
                        setOperateRange(value as RangeValue);
                        setOperatePagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <div className="flex-1 flex justify-end">
                      <Button
                        icon={<ExportOutlined />}
                        loading={exporting === 'operate'}
                        onClick={() => void handleExport('operate')}
                        className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary"
                      >
                        导出
                      </Button>
                    </div>
                  </div>
                  <Table
                    columns={operateColumns}
                    dataSource={operateLogs}
                    rowKey="id"
                    loading={operateLoading}
                    pagination={{
                      current: operatePagination.current,
                      pageSize: operatePagination.pageSize,
                      total: operatePagination.total,
                      className: 'pb-4',
                      showSizeChanger: true,
                      onChange: (page, size) =>
                        setOperatePagination((prev) => ({
                          ...prev,
                          current: page,
                          pageSize: size || 10,
                        })),
                    }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                  />
                </>
              ),
            },
            {
              key: 'login',
              label: '登录日志',
              children: (
                <>
                  <div className="pb-4 flex flex-wrap gap-4">
                    <Input
                      placeholder="搜索账号/IP/失败原因"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      className="w-64 bg-white g-border-panel border g-text-primary"
                      value={loginKeyword}
                      onChange={(e) => {
                        setLoginKeyword(e.target.value);
                        setLoginPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <Select
                      value={loginStatus}
                      className="w-36"
                      options={[
                        { label: '全部状态', value: 'all' },
                        { label: '成功', value: 'SUCCESS' },
                        { label: '失败', value: 'FAIL' },
                      ]}
                      onChange={(value) => {
                        setLoginStatus(value);
                        setLoginPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <RangePicker
                      value={loginRange}
                      className="bg-white g-border-panel border g-text-primary"
                      onChange={(value) => {
                        setLoginRange(value as RangeValue);
                        setLoginPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <div className="flex-1 flex justify-end">
                      <Button
                        icon={<ExportOutlined />}
                        loading={exporting === 'login'}
                        onClick={() => void handleExport('login')}
                        className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary"
                      >
                        导出
                      </Button>
                    </div>
                  </div>
                  <Table
                    columns={loginColumns}
                    dataSource={loginLogs}
                    rowKey="id"
                    loading={loginLoading}
                    pagination={{
                      current: loginPagination.current,
                      pageSize: loginPagination.pageSize,
                      total: loginPagination.total,
                      className: 'pb-4',
                      showSizeChanger: true,
                      onChange: (page, size) =>
                        setLoginPagination((prev) => ({
                          ...prev,
                          current: page,
                          pageSize: size || 10,
                        })),
                    }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                  />
                </>
              ),
            },
            {
              key: 'error',
              label: '错误日志',
              children: (
                <>
                  <div className="pb-4 flex flex-wrap gap-4">
                    <Input
                      placeholder="搜索异常类型/信息/请求/IP"
                      prefix={<SearchOutlined className="g-text-secondary" />}
                      className="w-72 bg-white g-border-panel border g-text-primary"
                      value={errorKeyword}
                      onChange={(e) => {
                        setErrorKeyword(e.target.value);
                        setErrorPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <Select
                      value={errorLevel}
                      className="w-36"
                      onChange={(value) => {
                        setErrorLevel(value);
                        setErrorPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                      options={[
                        { label: '全部级别', value: 'all' },
                        { label: 'ERROR', value: 'ERROR' },
                        { label: 'WARN', value: 'WARN' },
                      ]}
                    />
                    <RangePicker
                      value={errorRange}
                      className="bg-white g-border-panel border g-text-primary"
                      onChange={(value) => {
                        setErrorRange(value as RangeValue);
                        setErrorPagination((prev) => ({ ...prev, current: 1 }));
                      }}
                    />
                    <div className="flex-1 flex justify-end">
                      <Button
                        icon={<ExportOutlined />}
                        loading={exporting === 'error'}
                        onClick={() => void handleExport('error')}
                        className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary"
                      >
                        导出
                      </Button>
                    </div>
                  </div>
                  <Table
                    columns={errorColumns}
                    dataSource={errorLogs}
                    rowKey="id"
                    loading={errorLoading}
                    pagination={{
                      current: errorPagination.current,
                      pageSize: errorPagination.pageSize,
                      total: errorPagination.total,
                      className: 'pb-4',
                      showSizeChanger: true,
                      onChange: (page, size) =>
                        setErrorPagination((prev) => ({
                          ...prev,
                          current: page,
                          pageSize: size || 10,
                        })),
                    }}
                    locale={{ emptyText: '暂无系统异常错误记录' }}
                    className="bg-transparent"
                    rowClassName="hover:bg-white transition-colors"
                  />
                </>
              ),
            },
          ]}
        />
      </Card>
    </motion.div>
  );

export default SystemLogs;
