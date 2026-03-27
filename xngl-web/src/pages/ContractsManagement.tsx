import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  BlockOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  DollarOutlined,
  DownloadOutlined,
  FileTextOutlined,
  FilterOutlined,
  PlusOutlined,
  SearchOutlined,
  SendOutlined,
  SwapOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  commitContractImport,
  createContractChangeApplication,
  createContractExtension,
  createContract,
  createOnlineContract,
  downloadContractExport,
  exportContracts,
  fetchChangeApplications,
  previewContractImport,
  fetchExtensions,
  fetchContractExportTask,
  fetchContractList,
  fetchContractStats,
  approveContractChange,
  approveContractExtension,
  rejectContractChange,
  rejectContractExtension,
  submitContractChange,
  submitContractExtension,
  submitContract,
  type ContractCreateDto,
  type ContractChangeRecord,
  type ContractExtensionRecord,
  type ContractRecord,
  type ImportErrorRecord,
  type ImportPreviewResult,
  type ContractStats,
} from '../utils/contractApi';
import { fetchProjects, type ProjectRecord } from '../utils/projectApi';
import { fetchSites, type SiteRecord } from '../utils/siteApi';
import { fetchOrgTree, type OrgTreeNode } from '../utils/orgApi';

type UnitRecord = {
  id: string;
  orgName: string;
  orgType?: string | null;

const { RangePicker } = DatePicker;

const statusLabelMap: Record<string, { color: string; text: string }> = {
  EFFECTIVE: { color: 'green', text: '生效' },
  EXECUTING: { color: 'green', text: '生效' },
  APPROVING: { color: 'processing', text: '审批中' },
  PENDING: { color: 'processing', text: '审批中' },
  TERMINATED: { color: 'default', text: '终止' },
  CANCELLED: { color: 'error', text: '作废' },
  VOID: { color: 'error', text: '作废' },

const approvalStatusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  APPROVING: { color: 'processing', text: '审批中' },
  APPROVED: { color: 'success', text: '已通过' },
  REJECTED: { color: 'error', text: '已驳回' },

const changeTypeOptions = [
  { label: '场地 + 方量变更', value: 'SITE_VOLUME_CHANGE' },
  { label: '仅场地变更', value: 'SITE_CHANGE_ONLY' },
  { label: '仅方量变更', value: 'VOLUME_CHANGE_ONLY' },
];

const entryTypeOptions = [
  { label: '普通消纳合同', value: 'DISPOSAL' },
  { label: '三方消纳合同', value: 'DISPOSAL_THREE_PARTY' },
];

const formatMoney = (value?: number | null) =>
  '¥ ' + Number(value || 0).toLocaleString();

const importTemplate = `合同编号,合同名称,合同类型,项目ID,场地ID,建设单位ID,运输单位ID,签订日期,生效日期,到期日期,约定方量,合同单价,合同金额,三方合同,备注,区内单价,区外单价
HT-IMPORT-001,历史合同A,DISPOSAL,1,1,1,1,2026-03-01,2026-03-01,2026-12-31,1000,12.5,12500,否,历史补录合同,12.5,13.0`;

const contractTypeOptions = [
  { label: '普通消纳合同', value: 'DISPOSAL' },
  { label: '三方消纳合同', value: 'DISPOSAL_THREE_PARTY' },
  { label: '车辆租赁合同', value: 'VEHICLE_LEASE' },
  { label: '人员用工合同', value: 'LABOR' },
  { label: '其他合同', value: 'OTHER' },
];

const importHeaderAliases: Record<string, string> = {
  contractno: 'contractNo',
  '合同编号': 'contractNo',
  name: 'name',
  '合同名称': 'name',
  contracttype: 'contractType',
  '合同类型': 'contractType',
  projectid: 'projectId',
  '项目id': 'projectId',
  '项目ID': 'projectId',
  siteid: 'siteId',
  '场地id': 'siteId',
  '场地ID': 'siteId',
  constructionorgid: 'constructionOrgId',
  '建设单位id': 'constructionOrgId',
  '建设单位ID': 'constructionOrgId',
  transportorgid: 'transportOrgId',
  '运输单位id': 'transportOrgId',
  '运输单位ID': 'transportOrgId',
  partyid: 'partyId',
  '三方单位id': 'partyId',
  '三方单位ID': 'partyId',
  signdate: 'signDate',
  '签订日期': 'signDate',
  effectivedate: 'effectiveDate',
  '生效日期': 'effectiveDate',
  expiredate: 'expireDate',
  '到期日期': 'expireDate',
  agreedvolume: 'agreedVolume',
  '约定方量': 'agreedVolume',
  unitprice: 'unitPrice',
  '合同单价': 'unitPrice',
  contractamount: 'contractAmount',
  '合同金额': 'contractAmount',
  isthreeparty: 'isThreeParty',
  '三方合同': 'isThreeParty',
  remark: 'remark',
  '备注': 'remark',
  unitpriceinside: 'unitPriceInside',
  '区内单价': 'unitPriceInside',
  unitpriceoutside: 'unitPriceOutside',
  '区外单价': 'unitPriceOutside',
  approvalstatus: 'approvalStatus',
  '审批状态': 'approvalStatus',
  contractstatus: 'contractStatus',
  '合同状态': 'contractStatus',
  sourcetype: 'sourceType',
  '来源类型': 'sourceType',

function normalizeImportHeader(header: string) {
  const raw = header.trim();
  return importHeaderAliases[raw] || importHeaderAliases[raw.toLowerCase()] || raw;
}

function parseCsvLine(line: string) {
  const values: string[] = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }
    if (char === ',' && !inQuotes) {
      values.push(current);
      current = '';
      continue;
    }
    current += char;
  }
  values.push(current);
  return values;
}

function parseCsvText(text: string) {
  const lines = text
    .replace(/^\uFEFF/, '')
    .replace(/\r\n/g, '\n')
    .split('\n')
    .filter((line) => line.trim().length > 0);
  if (lines.length < 2) {
    return [];
  }
  const headers = parseCsvLine(lines[0]).map(normalizeImportHeader);
  return lines.slice(1).map((line) => {
    const cells = parseCsvLine(line);
    const row: Record<string, string> = {};
    headers.forEach((header, index) => {
      row[header] = (cells[index] || '').trim();
    });
    return row;
  });
}

function downloadImportTemplate() {
  const blob = new Blob([importTemplate], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'contract_import_template.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

const ContractsManagement: React.FC = () => {
  const navigate = useNavigate();
  const [offlineForm] = Form.useForm<ContractCreateDto & { pricingZone?: 'INSIDE' | 'OUTSIDE' }>();
  const [changeForm] = Form.useForm<{ changeType: string; newSiteId?: number; newAgreedVolume?: number; reason?: string }>();
  const [extensionForm] = Form.useForm<{ requestedExpireDate?: any; requestedVolumeDelta?: number; reason?: string }>();
  const [loading, setLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [offlineOpen, setOfflineOpen] = useState(false);
  const [entryMode, setEntryMode] = useState<'OFFLINE' | 'ONLINE'>('OFFLINE');
  const [offlineLoading, setOfflineLoading] = useState(false);
  const [referenceLoading, setReferenceLoading] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [changeOpen, setChangeOpen] = useState(false);
  const [extensionOpen, setExtensionOpen] = useState(false);
  const [applicationLoading, setApplicationLoading] = useState(false);
  const [changeRecords, setChangeRecords] = useState<ContractChangeRecord[]>([]);
  const [extensionRecords, setExtensionRecords] = useState<ContractExtensionRecord[]>([]);
  const [selectedContract, setSelectedContract] = useState<ContractRecord | null>(null);
  const [importFileName, setImportFileName] = useState('');
  const [importRows, setImportRows] = useState<Record<string, string>[]>([]);
  const [importPreview, setImportPreview] = useState<ImportPreviewResult | null>(null);
  const [importParseError, setImportParseError] = useState<string>('');
  const [previewLoading, setPreviewLoading] = useState(false);
  const [commitLoading, setCommitLoading] = useState(false);
  const [projectOptions, setProjectOptions] = useState<ProjectRecord[]>([]);
  const [siteOptions, setSiteOptions] = useState<SiteRecord[]>([]);
  const [unitOptions, setUnitOptions] = useState<UnitRecord[]>([]);
  const [stats, setStats] = useState<ContractStats | null>(null);
  const [records, setRecords] = useState<ContractRecord[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string>('all');
  const [range, setRange] = useState<[string | undefined, string | undefined]>([
    undefined,
    undefined,
  ]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [statsData, listData, changePage, extensionPage] = await Promise.all([
          fetchContractStats(),
          fetchContractList({
            keyword: keyword.trim() || undefined,
            contractStatus: status === 'all' ? undefined : status,
            startDate: range[0],
            endDate: range[1],
            pageNo,
            pageSize,
          }),
          fetchChangeApplications({ pageNo: 1, pageSize: 8 }),
          fetchExtensions({ pageNo: 1, pageSize: 8 }),
        ]);
        setStats(statsData);
        setRecords(listData.records || []);
        setTotal(listData.total || 0);
        setChangeRecords(changePage.records || []);
        setExtensionRecords(extensionPage.records || []);
      } catch (error) {
        console.error(error);
        message.error('获取合同数据失败');
        setStats(null);
        setRecords([]);
        setTotal(0);
        setChangeRecords([]);
        setExtensionRecords([]);
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [keyword, status, range, pageNo, pageSize, reloadVersion]);

  const summaryAmount = useMemo(
    () => records.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0),
    [records]
  );

  const selectedProjectId = Form.useWatch('projectId', offlineForm);

  const availableSites = useMemo(() => {
    if (!selectedProjectId) {
      return siteOptions;
    }
    return siteOptions.filter((item) => String(item.projectId || '') === String(selectedProjectId));
  }, [selectedProjectId, siteOptions]);

  const transportUnitOptions = useMemo(
    () => unitOptions.filter((item) => item.orgType === 'TRANSPORT_COMPANY'),
    [unitOptions]
  );

  const syncOfflineAmounts = (values?: Partial<ContractCreateDto & { pricingZone?: 'INSIDE' | 'OUTSIDE' }>) => {
    const current = {
      ...offlineForm.getFieldsValue(),
      ...values,
    };
    const zone = current.pricingZone || 'INSIDE';
    const volume = Number(current.agreedVolume || 0);
    const insidePrice = Number(current.unitPriceInside || 0);
    const outsidePrice = Number(current.unitPriceOutside || 0);
    const unitPrice = zone === 'OUTSIDE' ? outsidePrice : insidePrice;
    const contractAmount = Number((volume * unitPrice).toFixed(2));
    offlineForm.setFieldsValue({
      unitPrice,
      contractAmount,
    });
  };

  const loadContractReferences = async () => {
    setReferenceLoading(true);
    try {
      const [projectsPage, sites, orgTree] = await Promise.all([
        fetchProjects({ pageNo: 1, pageSize: 200 }),
        fetchSites(),
        fetchOrgTree(),
      ]);
      const flattenOrgs = (nodes: OrgTreeNode[]): UnitRecord[] =>
        nodes.flatMap((n) => [
          { id: String(n.id), orgName: n.orgName, orgType: n.orgType },
          ...(n.children ? flattenOrgs(n.children) : []),
        ]);
      setProjectOptions(projectsPage.records || []);
      setSiteOptions(sites || []);
      setUnitOptions(flattenOrgs(orgTree));
    } catch (error) {
      console.error(error);
      message.error('加载合同基础资料失败');
    } finally {
      setReferenceLoading(false);
    }
  };

  const openOfflineModal = async () => {
    setEntryMode('OFFLINE');
    setOfflineOpen(true);
    offlineForm.resetFields();
    offlineForm.setFieldsValue({
      contractType: 'DISPOSAL',
      pricingZone: 'INSIDE',
      sourceType: 'OFFLINE',
      isThreeParty: false,
      unitPrice: 0,
      contractAmount: 0,
    });
    await loadContractReferences();
  };

  const openOnlineModal = async () => {
    setEntryMode('ONLINE');
    setOfflineOpen(true);
    offlineForm.resetFields();
    offlineForm.setFieldsValue({
      contractType: 'DISPOSAL',
      pricingZone: 'INSIDE',
      sourceType: 'ONLINE',
      isThreeParty: false,
      unitPrice: 0,
      contractAmount: 0,
    });
    await loadContractReferences();
  };

  const handleExport = async () => {
    setExportLoading(true);
    try {
      const { taskId } = await exportContracts({
        keyword: keyword.trim() || undefined,
        contractStatus: status === 'all' ? undefined : status,
        startDate: range[0],
        endDate: range[1],
        exportType: 'CSV',
      });

      let task = await fetchContractExportTask(taskId);
      for (
        let i = 0;
        i < 5 && (task.status === 'PENDING' || task.status === 'PROCESSING');
        i += 1
      ) {
        await new Promise((resolve) => window.setTimeout(resolve, 400));
        task = await fetchContractExportTask(taskId);
      }

      if (task.status !== 'COMPLETED') {
        throw new Error(task.failReason || '导出任务未完成');
      }

      const blob = await downloadContractExport(taskId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = task.fileName || 'contracts.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success(`已导出 ${records.length || total || 0} 条合同数据`);
    } catch (error) {
      console.error(error);
      message.error(error instanceof Error ? error.message : '合同导出失败');
    } finally {
      setExportLoading(false);
    }
  };

  const resetImportState = () => {
    setImportFileName('');
    setImportRows([]);
    setImportPreview(null);
    setImportParseError('');
    setPreviewLoading(false);
    setCommitLoading(false);
  };

  const handleSelectImportFile = async (file: File) => {
    try {
      const text = await file.text();
      const rows = parseCsvText(text);
      if (!rows.length) {
        throw new Error('导入文件至少需要表头和一条数据');
      }
      setImportFileName(file.name);
      setImportRows(rows);
      setImportPreview(null);
      setImportParseError('');
      message.success(`已解析 ${rows.length} 条导入数据`);
    } catch (error) {
      console.error(error);
      setImportRows([]);
      setImportPreview(null);
      setImportParseError(error instanceof Error ? error.message : '导入文件解析失败');
    }
    return false;
  };

  const handlePreviewImport = async () => {
    if (!importRows.length) {
      message.warning('请先选择导入文件');
      return;
    }
    setPreviewLoading(true);
    try {
      const preview = await previewContractImport(importFileName || 'contract_import.csv', importRows);
      setImportPreview(preview);
      message.success(`预览完成：有效 ${preview.validCount} 条，异常 ${preview.errorCount} 条`);
    } catch (error) {
      console.error(error);
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleCommitImport = async () => {
    if (!importPreview?.batchId) {
      message.warning('请先执行导入预览');
      return;
    }
    setCommitLoading(true);
    try {
      const result = await commitContractImport(importPreview.batchId);
      message.success(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`);
      setImportOpen(false);
      resetImportState();
      setReloadVersion((value) => value + 1);
    } catch (error) {
      console.error(error);
    } finally {
      setCommitLoading(false);
    }
  };

  const handleContractEntrySubmit = async () => {
    try {
      const values = await offlineForm.validateFields();
      const payload: ContractCreateDto = {
        contractNo: values.contractNo?.trim() || undefined,
        name: values.name.trim(),
        contractType: values.contractType,
        projectId: String(values.projectId),
        siteId: String(values.siteId),
        constructionOrgId: String(values.constructionOrgId),
        transportOrgId: String(values.transportOrgId),
        partyId: values.partyId ? String(values.partyId) : undefined,
        signDate: dayjs(values.signDate).format('YYYY-MM-DD'),
        effectiveDate: dayjs(values.effectiveDate).format('YYYY-MM-DD'),
        expireDate: dayjs(values.expireDate).format('YYYY-MM-DD'),
        agreedVolume: Number(values.agreedVolume || 0),
        unitPrice: Number(values.unitPrice || 0),
        contractAmount: Number(values.contractAmount || 0),
        isThreeParty: !!values.isThreeParty,
        unitPriceInside: Number(values.unitPriceInside || 0),
        unitPriceOutside: Number(values.unitPriceOutside || 0),
        sourceType: entryMode,
        remark: values.remark?.trim(),
      };
      setOfflineLoading(true);
      const createdContractId =
        entryMode === 'ONLINE'
          ? await createOnlineContract(payload)
          : await createContract(payload);
      const contractId = String(createdContractId);
      if (entryMode === 'ONLINE') {
        await submitContract(contractId);
      }
      message.success(entryMode === 'ONLINE' ? '在线合同申请已提交' : '线下合同录入成功');
      setOfflineOpen(false);
      offlineForm.resetFields();
      setReloadVersion((value) => value + 1);
      navigate('/contracts/' + contractId);
    } catch (error) {
      if (error instanceof Error) {
        console.error(error);
      }
    } finally {
      setOfflineLoading(false);
    }
  };

  const openChangeModal = async (record: ContractRecord) => {
    setSelectedContract(record);
    setChangeOpen(true);
    changeForm.resetFields();
    changeForm.setFieldsValue({
      changeType: 'SITE_VOLUME_CHANGE',
      newSiteId: record.siteId ? Number(record.siteId) : undefined,
      newAgreedVolume: Number(record.agreedVolume || 0),
    });
    await loadContractReferences();
  };

  const openExtensionModal = async (record: ContractRecord) => {
    setSelectedContract(record);
    setExtensionOpen(true);
    extensionForm.resetFields();
    await loadContractReferences();
  };

  const reloadApplications = async () => {
    setApplicationLoading(true);
    try {
      const [changePage, extensionPage] = await Promise.all([
        fetchChangeApplications({ pageNo: 1, pageSize: 8 }),
        fetchExtensions({ pageNo: 1, pageSize: 8 }),
      ]);
      setChangeRecords(changePage.records || []);
      setExtensionRecords(extensionPage.records || []);
    } catch (error) {
      console.error(error);
      message.error('刷新申请列表失败');
    } finally {
      setApplicationLoading(false);
    }
  };

  const handleCreateChange = async () => {
    if (!selectedContract) {
      return;
    }
    try {
      const values = await changeForm.validateFields();
      setApplicationLoading(true);
      const snapshot: Record<string, unknown> = {
        siteId: values.newSiteId,
        agreedVolume: values.newAgreedVolume,
        remark: values.reason?.trim() || undefined,
      };
      const applyId = await createContractChangeApplication(selectedContract.id, {
        contractId: Number(selectedContract.id),
        changeType: values.changeType,
        afterSnapshotJson: JSON.stringify(snapshot),
        reason: values.reason?.trim() || undefined,
        newSiteId: values.newSiteId,
        newAgreedVolume: values.newAgreedVolume,
        volumeDelta:
          values.newAgreedVolume != null
            ? Number(values.newAgreedVolume) - Number(selectedContract.agreedVolume || 0)
            : undefined,
      });
      await submitContractChange(applyId);
      message.success('变更申请已提交审批');
      setChangeOpen(false);
      changeForm.resetFields();
      await reloadApplications();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('提交变更申请失败');
    } finally {
      setApplicationLoading(false);
    }
  };

  const handleCreateExtension = async () => {
    if (!selectedContract) {
      return;
    }
    try {
      const values = await extensionForm.validateFields();
      setApplicationLoading(true);
      const applyId = await createContractExtension(selectedContract.id, {
        contractId: Number(selectedContract.id),
        requestedExpireDate: dayjs(values.requestedExpireDate).format('YYYY-MM-DD'),
        requestedVolumeDelta:
          values.requestedVolumeDelta != null ? Number(values.requestedVolumeDelta) : undefined,
        reason: values.reason?.trim() || undefined,
      });
      await submitContractExtension(applyId);
      message.success('延期申请已提交审批');
      setExtensionOpen(false);
      extensionForm.resetFields();
      await reloadApplications();
    } catch (error) {
      if ((error as { errorFields?: unknown[] })?.errorFields) {
        return;
      }
      console.error(error);
      message.error('提交延期申请失败');
    } finally {
      setApplicationLoading(false);
    }
  };

  const importErrorColumns: ColumnsType<ImportErrorRecord> = [
    { title: '行号', dataIndex: 'rowNo', key: 'rowNo', width: 80 },
    { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 180 },
    { title: '错误码', dataIndex: 'errorCode', key: 'errorCode', width: 220 },
    { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage' },
  ];

  const columns: ColumnsType<ContractRecord> = [
    {
      title: '合同编号',
      dataIndex: 'contractNo',
      key: 'contractNo',
      render: (value, record) => (
        <a
          className="g-text-primary-link font-mono tracking-wide"
          onClick={() => navigate('/contracts/' + record.id)}
        >
          {value || 'HT-' + record.id}
        </a>
      ),
    },
    {
      title: '合同类型',
      dataIndex: 'contractType',
      key: 'contractType',
      render: (value?: string) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '关联项目',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (value, record) => (
        <strong className="g-text-primary">{value || '项目#' + String(record.projectId || '-')}</strong>
      ),
    },
    {
      title: '约定消纳场',
      dataIndex: 'siteName',
      key: 'siteName',
      render: (value, record) => (
        <span className="g-text-secondary">{value || '场地#' + String(record.siteId || '-')}</span>
      ),
    },
    {
      title: '总金额/已入账',
      key: 'money',
      render: (_, record) => {
        const totalAmount = Number(record.contractAmount || 0);
        const receivedAmount = Number(record.receivedAmount || 0);
        const percent =
          totalAmount > 0 ? Math.min(100, Math.round((receivedAmount / totalAmount) * 100)) : 0;
        return (
          <div className="flex flex-col gap-1 w-40">
            <span className="g-text-success font-bold">{formatMoney(totalAmount)}</span>
            <Progress
              percent={percent}
              size="small"
              showInfo={false}
              strokeColor="var(--success)"
              trailColor="rgba(0,0,0,0.06)"
            />
            <span className="text-xs g-text-secondary">
              已入账 {formatMoney(receivedAmount)}
            </span>
          </div>
        );
      },
    },
    {
      title: '约定方量(m³)',
      dataIndex: 'agreedVolume',
      key: 'agreedVolume',
      render: (value?: number) => (
        <span className="g-text-secondary">{Number(value || 0).toLocaleString()}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'contractStatus',
      key: 'contractStatus',
      render: (value: string | undefined, record: ContractRecord) => {
        const normalized = String(value || '').toUpperCase();
        const matched = statusLabelMap[normalized];
        return (
          <div className="flex flex-col gap-1">
            <Tag color={matched?.color || 'default'}>{matched?.text || value || '未知'}</Tag>
            {record.rejectReason ? (
              <span className="text-xs" style={{ color: 'var(--warning)' }}>
                驳回原因：{record.rejectReason}
              </span>
            ) : null}
          </div>
        );
      },
    },
    {
      title: '签订日期',
      dataIndex: 'signDate',
      key: 'signDate',
      render: (value?: string) => <span className="g-text-secondary">{value || '-'}</span>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <a onClick={() => navigate('/contracts/' + record.id)}>详情</a>
          <a onClick={() => navigate('/contracts/payments?contractId=' + record.id)}>入账</a>
          {['EFFECTIVE', 'EXECUTING'].includes(String(record.contractStatus || '').toUpperCase()) ? (
            <>
              <a onClick={() => void openChangeModal(record)}>变更申请</a>
              <a onClick={() => void openExtensionModal(record)}>延期申请</a>
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  const changeColumns: ColumnsType<ContractChangeRecord> = [
    { title: '变更单号', dataIndex: 'changeNo', key: 'changeNo' },
    { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo' },
    {
      title: '变更类型',
      dataIndex: 'changeType',
      key: 'changeType',
      render: (value) => {
        if (value === 'SITE_VOLUME_CHANGE') return '场地+方量变更';
        if (value === 'SITE_CHANGE_ONLY') return '场地变更';
        if (value === 'VOLUME_CHANGE_ONLY') return '方量变更';
        return value || '-';
      },
    },
    {
      title: '审批状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (value) => {
        const statusInfo = approvalStatusMap[String(value || '').toUpperCase()];
        return <Tag color={statusInfo?.color || 'default'}>{statusInfo?.text || value || '-'}</Tag>;
      },
    },
    { title: '申请说明', dataIndex: 'reason', key: 'reason', ellipsis: true },
    { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          {record.approvalStatus === 'DRAFT' ? (
            <Button type="link" size="small" icon={<SendOutlined />} onClick={() => void (async () => {
              setApplicationLoading(true);
              try {
                await submitContractChange(record.id);
                message.success('变更申请已提交');
                await reloadApplications();
              } finally {
                setApplicationLoading(false);
              }
            })()}>
              提交
            </Button>
          ) : null}
          {record.approvalStatus === 'APPROVING' ? (
            <>
              <Button type="link" size="small" icon={<CheckOutlined />} onClick={() => void (async () => {
                setApplicationLoading(true);
                try {
                  await approveContractChange(record.id);
                  message.success('变更申请已通过');
                  setReloadVersion((value) => value + 1);
                  await reloadApplications();
                } finally {
                  setApplicationLoading(false);
                }
              })()}>
                通过
              </Button>
              <Button type="link" size="small" danger icon={<CloseOutlined />} onClick={() => void (async () => {
                setApplicationLoading(true);
                try {
                  await rejectContractChange(record.id, '页面联调驳回');
                  message.success('变更申请已驳回');
                  await reloadApplications();
                } finally {
                  setApplicationLoading(false);
                }
              })()}>
                驳回
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  const extensionColumns: ColumnsType<ContractExtensionRecord> = [
    { title: '延期单号', dataIndex: 'applyNo', key: 'applyNo' },
    { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo' },
    {
      title: '原到期 / 申请到期',
      key: 'dates',
      render: (_, record) => `${record.originalExpireDate || '-'} -> ${record.requestedExpireDate || '-'}`,
    },
    {
      title: '增补方量',
      dataIndex: 'requestedVolumeDelta',
      key: 'requestedVolumeDelta',
      render: (value) => Number(value || 0).toLocaleString(),
    },
    {
      title: '审批状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (value) => {
        const statusInfo = approvalStatusMap[String(value || '').toUpperCase()];
        return <Tag color={statusInfo?.color || 'default'}>{statusInfo?.text || value || '-'}</Tag>;
      },
    },
    { title: '延期原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          {record.approvalStatus === 'DRAFT' ? (
            <Button type="link" size="small" icon={<SendOutlined />} onClick={() => void (async () => {
              setApplicationLoading(true);
              try {
                await submitContractExtension(record.id);
                message.success('延期申请已提交');
                await reloadApplications();
              } finally {
                setApplicationLoading(false);
              }
            })()}>
              提交
            </Button>
          ) : null}
          {record.approvalStatus === 'APPROVING' ? (
            <>
              <Button type="link" size="small" icon={<CheckOutlined />} onClick={() => void (async () => {
                setApplicationLoading(true);
                try {
                  await approveContractExtension(record.id);
                  message.success('延期申请已通过');
                  setReloadVersion((value) => value + 1);
                  await reloadApplications();
                } finally {
                  setApplicationLoading(false);
                }
              })()}>
                通过
              </Button>
              <Button type="link" size="small" danger icon={<CloseOutlined />} onClick={() => void (async () => {
                setApplicationLoading(true);
                try {
                  await rejectContractExtension(record.id, '页面联调驳回');
                  message.success('延期申请已驳回');
                  await reloadApplications();
                } finally {
                  setApplicationLoading(false);
                }
              })()}>
                驳回
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold g-text-primary m-0">合同与财务结算</h1>
          <p className="g-text-secondary mt-1">
            管理合同主档、审批状态、分次入账及财务结算进度
          </p>
        </div>
        <div className="text-right">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            className="g-btn-primary border-none mr-3 text-white"
            onClick={() => void openOnlineModal()}
          </Button>
        </div>
      </div>
          >
            在线合同发起
          </Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            className="g-btn-primary border-none mr-3 text-white"
            loading={exportLoading}
            onClick={() => void handleExport()}
          >
            导出筛选结果
          </Button>
          <Button
            icon={<UploadOutlined />}
            className="mr-3"
            onClick={() => {
              resetImportState();
              setImportOpen(true);
            }}
          >
            批量导入合同
          </Button>
          <Button
            type="primary"
            className="g-btn-primary border-none"
            onClick={() => void openOfflineModal()}
          >
            线下合同录入
          </Button>
        </div>

      <Row gutter={[24, 24]}>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">本月累计入账</span>}
              value={stats?.monthlyReceiptAmount || 0}
              valueStyle={{ color: 'var(--success)', fontWeight: 'bold' }}
              prefix={<DollarOutlined />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              已入账 {stats?.monthlyReceiptCount || 0} 笔
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">待入账金额</span>}
              value={stats?.pendingReceiptAmount || 0}
              valueStyle={{ color: 'var(--warning)', fontWeight: 'bold' }}
              prefix={<FileTextOutlined />}
            />
            <div className="mt-2 text-xs g-text-secondary">含审批中合同</div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">生效合同数</span>}
              value={stats?.effectiveContracts || 0}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={<BlockOutlined className="g-text-primary-link" />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              总合同 {stats?.totalContracts || 0} 份
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="glass-panel g-border-panel border">
            <Statistic
              title={<span className="g-text-secondary">当前页合同额</span>}
              value={summaryAmount}
              valueStyle={{ color: 'var(--text-primary)', fontWeight: 'bold' }}
              prefix={<FileTextOutlined className="g-text-primary-link" />}
            />
            <div className="mt-2 text-xs g-text-secondary">
              结算单 {stats?.totalSettlementOrders || 0} 张
            </div>
          </Card>
        </Col>
      </Row>

      <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
        <div className="p-4 border-b g-border-panel border flex flex-wrap gap-4 g-bg-toolbar">
          <Input
            placeholder="搜索合同编号或项目名称"
            prefix={<SearchOutlined className="g-text-secondary" />}
            className="w-64 bg-white g-border-panel border g-text-primary"
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPageNo(1);
            }}
          />
          <RangePicker
            className="bg-white g-border-panel border"
            onChange={(values) => {
              setRange([
                values?.[0] ? dayjs(values[0]).format('YYYY-MM-DD') : undefined,
                values?.[1] ? dayjs(values[1]).format('YYYY-MM-DD') : undefined,
              ]);
              setPageNo(1);
            }}
          />
          <Select
            value={status}
            style={{ width: 160 }}
            options={[
              { label: '全部状态', value: 'all' },
              { label: '生效', value: 'EFFECTIVE' },
              { label: '审批中', value: 'APPROVING' },
              { label: '终止', value: 'TERMINATED' },
              { label: '作废', value: 'CANCELLED' },
            ]}
            onChange={(value) => {
              setStatus(value);
              setPageNo(1);
            }}
          />
          <Button icon={<FilterOutlined />} className="bg-transparent g-text-secondary g-border-panel border hover:g-text-primary">
            状态筛选
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPageNo(nextPage);
              setPageSize(nextPageSize);
            },
          }}
          className="bg-transparent"
          rowClassName="hover:bg-white transition-colors"
        />
      </Card>

      <Row gutter={[24, 24]}>
        <Col span={12}>
          <Card
            className="glass-panel g-border-panel border"
            title={
              <Space>
                <SwapOutlined />
                <span>变更申请</span>
              </Space>
            }
            extra={<span className="g-text-secondary text-sm">场地变更 / 部分方量变更</span>}
          >
            <Table
              size="small"
              rowKey="id"
              loading={applicationLoading}
              columns={changeColumns}
              dataSource={changeRecords}
              pagination={false}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card
            className="glass-panel g-border-panel border"
            title={
              <Space>
                <ClockCircleOutlined />
                <span>延期申请</span>
              </Space>
            }
            extra={<span className="g-text-secondary text-sm">延期到期日 / 增补方量</span>}
          >
            <Table
              size="small"
              rowKey="id"
              loading={applicationLoading}
              columns={extensionColumns}
              dataSource={extensionRecords}
              pagination={false}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title={entryMode === 'ONLINE' ? '在线合同发起' : '线下合同录入'}
        open={offlineOpen}
        width={960}
        confirmLoading={offlineLoading}
        onCancel={() => {
          setOfflineOpen(false);
          offlineForm.resetFields();
        }}
        onOk={() => void handleContractEntrySubmit()}
        okText={entryMode === 'ONLINE' ? '创建并提交审批' : '确认录入'}
      >
        <Form
          form={offlineForm}
          layout="vertical"
          onValuesChange={(changedValues) => {
            if ('isThreeParty' in changedValues) {
              offlineForm.setFieldsValue({
                contractType: changedValues.isThreeParty ? 'DISPOSAL_THREE_PARTY' : 'DISPOSAL',
              });
            }
            if (
              'agreedVolume' in changedValues
              || 'unitPriceInside' in changedValues
              || 'unitPriceOutside' in changedValues
              || 'pricingZone' in changedValues
            ) {
              syncOfflineAmounts(changedValues);
            }
          }}
        >
          <Alert
            type="info"
            showIcon
            className="mb-4"
            message={
              entryMode === 'ONLINE'
                ? '用于在线发起普通消纳合同或三方合同。创建成功后将自动提交审批，审批退回原因可在合同详情中查看。'
                : '用于补录非线上申请的普通合同或三方合同。区内/区外单价会根据价格区域自动计算合同单价和金额。'
            }
          />
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="contractNo" label="合同编号">
                <Input placeholder="可不填，系统自动生成" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="name"
                label="合同名称"
                rules={[{ required: true, message: '请输入合同名称' }]}
              >
                <Input placeholder="请输入合同名称" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="contractType"
                label="合同类型"
                rules={[{ required: true, message: '请选择合同类型' }]}
              >
                <Select
                  options={entryMode === 'ONLINE' ? entryTypeOptions : contractTypeOptions}
                  loading={referenceLoading}
                  placeholder="请选择合同类型"
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="isThreeParty" label="三方合同" valuePropName="checked">
                <Switch checkedChildren="是" unCheckedChildren="否" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="projectId"
                label="关联项目"
                rules={[{ required: true, message: '请选择项目' }]}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择项目"
                  loading={referenceLoading}
                  options={projectOptions.map((item) => ({
                    label: `${item.name}${item.code ? ` (${item.code})` : ''}`,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="siteId"
                label="消纳场地"
                rules={[{ required: true, message: '请选择场地' }]}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择场地"
                  loading={referenceLoading}
                  options={availableSites.map((item) => ({
                    label: item.name,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="constructionOrgId"
                label="建设/施工单位"
                rules={[{ required: true, message: '请选择建设/施工单位' }]}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择建设/施工单位"
                  loading={referenceLoading}
                  options={unitOptions.map((item) => ({
                    label: `${item.orgName}${item.orgTypeLabel ? ` (${item.orgTypeLabel})` : ''}`,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="transportOrgId"
                label="运输单位"
                rules={[{ required: true, message: '请选择运输单位' }]}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择运输单位"
                  loading={referenceLoading}
                  options={transportUnitOptions.map((item) => ({
                    label: item.orgName,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item shouldUpdate noStyle>
                {() =>
                  offlineForm.getFieldValue('isThreeParty') ? (
                    <Form.Item
                      name="partyId"
                      label="三方单位"
                      rules={[{ required: true, message: '请选择三方单位' }]}
                    >
                      <Select
                        showSearch
                        optionFilterProp="label"
                        placeholder="请选择三方单位"
                        loading={referenceLoading}
                        options={unitOptions.map((item) => ({
                          label: `${item.orgName}${item.orgTypeLabel ? ` (${item.orgTypeLabel})` : ''}`,
                          value: item.id,
                        }))}
                      />
                    </Form.Item>
                  ) : (
                    <div />
                  )
                }
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="signDate"
                label="签订日期"
                rules={[{ required: true, message: '请选择签订日期' }]}
              >
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="effectiveDate"
                label="生效日期"
                rules={[{ required: true, message: '请选择生效日期' }]}
              >
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="expireDate"
                label="到期日期"
                rules={[{ required: true, message: '请选择到期日期' }]}
              >
                <DatePicker className="w-full" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="agreedVolume"
                label="约定方量(m³)"
                rules={[{ required: true, message: '请输入约定方量' }]}
              >
                <InputNumber min={0} precision={2} className="w-full" placeholder="请输入约定方量" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="pricingZone" label="价格区域" rules={[{ required: true, message: '请选择价格区域' }]}>
                <Radio.Group
                  options={[
                    { label: '区内价格', value: 'INSIDE' },
                    { label: '区外价格', value: 'OUTSIDE' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="unitPriceInside"
                label="区内单价"
                rules={[{ required: true, message: '请输入区内单价' }]}
              >
                <InputNumber min={0} precision={2} className="w-full" placeholder="请输入区内单价" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="unitPriceOutside"
                label="区外单价"
                rules={[{ required: true, message: '请输入区外单价' }]}
              >
                <InputNumber min={0} precision={2} className="w-full" placeholder="请输入区外单价" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="unitPrice" label="生效单价">
                <InputNumber disabled precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="contractAmount" label="合同金额">
                <InputNumber disabled precision={2} className="w-full" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={3} placeholder="可填写线下补录说明、三方关联说明等" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title={selectedContract ? `发起变更申请 · ${selectedContract.contractNo}` : '发起变更申请'}
        open={changeOpen}
        onCancel={() => setChangeOpen(false)}
        onOk={() => void handleCreateChange()}
        confirmLoading={applicationLoading}
        okText="提交变更申请"
      >
        <Form form={changeForm} layout="vertical">
          <Form.Item
            name="changeType"
            label="变更类型"
            rules={[{ required: true, message: '请选择变更类型' }]}
          >
            <Radio.Group options={changeTypeOptions} />
          </Form.Item>
          <Form.Item
            name="newSiteId"
            label="变更后场地"
            rules={[{ required: true, message: '请选择变更后场地' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={siteOptions.map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item
            name="newAgreedVolume"
            label="变更后合同方量(m³)"
            rules={[{ required: true, message: '请输入变更后方量' }]}
          >
            <InputNumber min={0} precision={2} className="w-full" />
          </Form.Item>
          <Form.Item name="reason" label="变更原因" rules={[{ required: true, message: '请输入变更原因' }]}>
            <Input.TextArea rows={3} placeholder="填写场地变更原因、部分方量调整说明等" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={selectedContract ? `发起延期申请 · ${selectedContract.contractNo}` : '发起延期申请'}
        open={extensionOpen}
        onCancel={() => setExtensionOpen(false)}
        onOk={() => void handleCreateExtension()}
        confirmLoading={applicationLoading}
        okText="提交延期申请"
      >
        <Form form={extensionForm} layout="vertical">
          <Form.Item
            name="requestedExpireDate"
            label="申请延期到"
            rules={[{ required: true, message: '请选择延期到期日' }]}
          >
            <DatePicker className="w-full" />
          </Form.Item>
          <Form.Item name="requestedVolumeDelta" label="增补方量(m³)">
            <InputNumber min={0} precision={2} className="w-full" />
          </Form.Item>
          <Form.Item name="reason" label="延期原因" rules={[{ required: true, message: '请输入延期原因' }]}>
            <Input.TextArea rows={3} placeholder="填写延期运输、天气影响或场地调度原因" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量导入历史合同"
        open={importOpen}
        width={880}
        onCancel={() => {
          setImportOpen(false);
          resetImportState();
        }}
        footer={[
          <Button
            key="template"
            onClick={() => downloadImportTemplate()}
          >
            下载模板
          </Button>,
          <Button
            key="preview"
            type="default"
            loading={previewLoading}
            disabled={!importRows.length || !!importParseError}
            onClick={() => void handlePreviewImport()}
          >
            预览校验
          </Button>,
          <Button
            key="commit"
            type="primary"
            loading={commitLoading}
            disabled={!importPreview || importPreview.validCount <= 0}
            onClick={() => void handleCommitImport()}
          >
            提交导入
          </Button>,
        ]}
      >
        <div className="space-y-4">
          <Alert
            type="info"
            showIcon
            message="支持导入 CSV 文件，表头可使用中文模板或英文字段名。建议先下载模板后填写。"
          />
          <Space>
            <Upload
              accept=".csv,.txt"
              showUploadList={false}
              beforeUpload={(file) => {
                void handleSelectImportFile(file as unknown as File);
                return false;
              }}
            >
              <Button icon={<UploadOutlined />}>选择导入文件</Button>
            </Upload>
            <span className="g-text-secondary text-sm">
              {importFileName ? `当前文件：${importFileName}` : '尚未选择文件'}
            </span>
          </Space>
          {importParseError ? (
            <Alert type="error" showIcon message={importParseError} />
          ) : null}
          {importRows.length ? (
            <Alert
              type="success"
              showIcon
              message={`已解析 ${importRows.length} 条数据，可执行预览校验`}
            />
          ) : null}
          {importPreview ? (
            <Alert
              type={importPreview.errorCount > 0 ? 'warning' : 'success'}
              showIcon
              message={`预览完成：总计 ${importPreview.totalCount} 条，有效 ${importPreview.validCount} 条，异常 ${importPreview.errorCount} 条`}
            />
          ) : null}
          <Table
            size="small"
            rowKey={(record, index) => String(record.id || `${record.rowNo}-${index}`)}
            dataSource={importPreview?.errors || []}
            columns={importErrorColumns}
            pagination={false}
            locale={{ emptyText: '当前无导入错误，预览后将在此展示异常明细' }}
            scroll={{ y: 260 }}
          />
        </div>
      </Modal>
    </motion.div>
  );    </div>
  );
};
export default ContractsManagement;
