import React, { useEffect, useMemo, useState } from 'react';
import { Card, Tabs, Descriptions, Tag, Button, Space, Table, List, Switch, InputNumber, Select, Empty, Spin, message, Form, Input, Modal, Popconfirm } from 'antd';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import TiandituMap from '../components/TiandituMap';
import type { MapMarker, MapPoint, MapPolygon } from '../components/TiandituMap';
import { parseGeoJsonPolygon } from '../utils/mapGeometry';
import {
    createSiteSurvey,
    createSitePersonnel,
    createSiteDocument,
    createSiteDevice,
    deleteSiteDocument,
    deleteSitePersonnel,
    deleteSiteSurvey,
    fetchSiteDetail,
    fetchSiteDocuments,
    fetchSiteDisposals,
    fetchSitePersonnel,
    fetchSitePersonnelCandidates,
    fetchSiteSurveys,
    updateSiteDocument,
    updateSitePersonnel,
    updateSiteSurvey,
    updateSiteDevice,
    updateSiteOperationConfig,
    type DisposalRecord,
    type SiteDocumentRecord,
    type SiteDeviceRecord,
    type SitePersonnelCandidateRecord,
    type SitePersonnelRecord,
    type SiteRecord,
    type SiteSurveyRecord,
} from '../utils/siteApi';

const resolveSiteType = (site?: SiteRecord | null) => {
    if (!site) return '-';
    if (site.siteType === 'STATE_OWNED') return '国有场地';
    if (site.siteType === 'COLLECTIVE') return '集体场地';
    if (site.siteType === 'ENGINEERING') return '工程场地';
    if (site.siteType === 'SHORT_BARGE') return '短驳场地';
    const code = site.code ?? '';
    const suffix = Number(site.id || 0) % 4;
    if (code.startsWith('GY') || suffix === 1) return '国有场地';
    if (code.startsWith('JT') || suffix === 2) return '集体场地';
    if (code.startsWith('GC') || suffix === 3) return '工程场地';
    return '短驳场地';

const resolveStatus = (status?: number | string | null) => {
    if (status === 1 || status === '1' || status === 'ACTIVE' || status === 'ENABLED') return '正常';
    if (status === 2 || status === '2' || status === 'WARNING') return '预警';
    if (status === 0 || status === '0' || status === 'INACTIVE' || status === 'DISABLED') return '停用';
    return '正常';

const buildCapacity = (site?: SiteRecord | null) => {
    if (!site) return 0;
    if (Number(site.capacity || 0) > 0) {
        return Number(site.capacity);
    }
    return ((Number(site.id || 1) % 7) + 3) * 100000;

const resolveSettlementMode = (site?: SiteRecord | null) => {
    switch (site?.settlementMode) {
        case 'MONTHLY_APPLY':
            return '按月结算申请';
        case 'RATIO_SERVICE_FEE':
            return '按消纳费比例 + 平台服务费';
        case 'UNIT_PRICE':
            return '按单价结算';
        case 'SERVICE_FEE':
            return '按平台服务费结算';
        default:
            return '按配置规则结算';
    }

const DEFAULT_MAP_CENTER: MapPoint = [120.1551, 30.2741];

const resolvePersonnelRole = (roleType?: string | null) => {
    switch (roleType) {
        case 'SITE_MANAGER':
            return '场地管理员';
        case 'SCALE_OPERATOR':
            return '地磅员';
        case 'SAFETY_OFFICER':
            return '安全员';
        case 'DISPATCHER':
            return '调度员';
        case 'INSPECTOR':
            return '巡检员';
        case 'FINANCE':
            return '财务';
        default:
            return roleType || '未分类';
    }

const resolveDocumentType = (documentType?: string | null) => {
    switch (documentType) {
        case 'PROJECT_APPROVAL':
            return '立项批复';
        case 'EIA_APPROVAL':
            return '环评批复';
        case 'LAND_LEASE':
            return '土地租赁合同';
        case 'BUSINESS_LICENSE':
            return '营业执照';
        case 'CONSTRUCTION_PLAN':
            return '建设方案';
        case 'BOUNDARY_SURVEY':
            return '红线测绘资料';
        case 'COMPLETION_ACCEPTANCE':
            return '竣工验收资料';
        case 'SAFETY_INSPECTION':
            return '安全检查记录';
        case 'OPERATION_LEDGER':
            return '运营台账';
        case 'WEIGHBRIDGE_RECORD':
            return '地磅记录';
        case 'TRANSFER_ACCEPTANCE':
            return '移交验收资料';
        case 'SITE_ARCHIVE':
            return '场地归档包';
        default:
            return documentType || '-';
    }

const resolveSiteLevel = (level?: string | null) => level === 'SECONDARY' ? '二级场地' : '一级场地';

const resolveSurveyStatus = (status?: string | null) => {
    switch (status) {
        case 'CONFIRMED':
            return '已确认';
        case 'ARCHIVED':
            return '已归档';
        default:
            return '草稿';
    }

const SiteDetail: React.FC = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const defaultTab = searchParams.get('tab') || 'info';
    const [loading, setLoading] = useState(false);
    const [siteInfo, setSiteInfo] = useState<SiteRecord | null>(null);
    const [disposalsLoading, setDisposalsLoading] = useState(false);
    const [disposals, setDisposals] = useState<DisposalRecord[]>([]);
    const [documents, setDocuments] = useState<SiteDocumentRecord[]>([]);
    const [surveys, setSurveys] = useState<SiteSurveyRecord[]>([]);
    const [documentModalOpen, setDocumentModalOpen] = useState(false);
    const [documentSubmitting, setDocumentSubmitting] = useState(false);
    const [editingDocument, setEditingDocument] = useState<SiteDocumentRecord | null>(null);
    const [surveyModalOpen, setSurveyModalOpen] = useState(false);
    const [surveySubmitting, setSurveySubmitting] = useState(false);
    const [editingSurvey, setEditingSurvey] = useState<SiteSurveyRecord | null>(null);
    const [personnel, setPersonnel] = useState<SitePersonnelRecord[]>([]);
    const [personnelCandidates, setPersonnelCandidates] = useState<SitePersonnelCandidateRecord[]>([]);
    const [personnelModalOpen, setPersonnelModalOpen] = useState(false);
    const [personnelSubmitting, setPersonnelSubmitting] = useState(false);
    const [editingPersonnel, setEditingPersonnel] = useState<SitePersonnelRecord | null>(null);
    const [deviceModalOpen, setDeviceModalOpen] = useState(false);
    const [deviceSubmitting, setDeviceSubmitting] = useState(false);
    const [editingDevice, setEditingDevice] = useState<SiteDeviceRecord | null>(null);
    const [operationSubmitting, setOperationSubmitting] = useState(false);
    const [personnelForm] = Form.useForm();
    const [documentForm] = Form.useForm();
    const [surveyForm] = Form.useForm();
    const [deviceForm] = Form.useForm();
    const [operationForm] = Form.useForm();

    useEffect(() => {
        if (!id) {
            return;
        }
        const loadData = async () => {
            setLoading(true);
            setDisposalsLoading(true);
            try {
                const [site, disposalPage, personnelRecords, candidateRecords, documentRecords, surveyRecords] = await Promise.all([
                    fetchSiteDetail(id),
                    fetchSiteDisposals({ siteId: id, pageNo: 1, pageSize: 10 }),
                    fetchSitePersonnel(id),
                    fetchSitePersonnelCandidates(id),
                    fetchSiteDocuments(id),
                    fetchSiteSurveys(id),
                ]);
                setSiteInfo(site);
                setDisposals(disposalPage.records || []);
                setPersonnel(personnelRecords);
                setPersonnelCandidates(candidateRecords);
                setDocuments(documentRecords);
                setSurveys(surveyRecords);
            } catch (error) {
                console.error(error);
                message.error('获取场地详情失败');
                setSiteInfo(null);
                setDisposals([]);
                setPersonnel([]);
                setPersonnelCandidates([]);
                setDocuments([]);
                setSurveys([]);
            } finally {
                setDisposalsLoading(false);
                setLoading(false);
            }
        };

        void loadData();
    }, [id]);

    useEffect(() => {
        operationForm.setFieldsValue({
            queueEnabled: siteInfo?.operationConfig?.queueEnabled ?? false,
            maxQueueCount: siteInfo?.operationConfig?.maxQueueCount ?? 0,
            manualDisposalEnabled: siteInfo?.operationConfig?.manualDisposalEnabled ?? false,
            rangeCheckRadius: siteInfo?.operationConfig?.rangeCheckRadius ?? 0,
            durationLimitMinutes: siteInfo?.operationConfig?.durationLimitMinutes ?? 0,
            remark: siteInfo?.operationConfig?.remark ?? '',
        });
    }, [operationForm, siteInfo?.operationConfig]);

    const reloadSiteData = async () => {
        if (!id) {
            return;
        }
        const [site, disposalPage, personnelRecords, candidateRecords, documentRecords, surveyRecords] = await Promise.all([
            fetchSiteDetail(id),
            fetchSiteDisposals({ siteId: id, pageNo: 1, pageSize: 10 }),
            fetchSitePersonnel(id),
            fetchSitePersonnelCandidates(id),
            fetchSiteDocuments(id),
            fetchSiteSurveys(id),
        ]);
        setSiteInfo(site);
        setDisposals(disposalPage.records || []);
        setPersonnel(personnelRecords);
        setPersonnelCandidates(candidateRecords);
        setDocuments(documentRecords);
        setSurveys(surveyRecords);
    };

    const capacity = useMemo(() => buildCapacity(siteInfo), [siteInfo]);
    const used = useMemo(() => Math.round(capacity * (0.35 + (Number(siteInfo?.id || 1) % 5) * 0.12)), [capacity, siteInfo]);
    const remaining = Math.max(capacity - used, 0);
    const typeText = useMemo(() => resolveSiteType(siteInfo), [siteInfo]);
    const statusText = useMemo(() => resolveStatus(siteInfo?.status), [siteInfo]);
    const siteCenter = useMemo<MapPoint>(() => {
        if (siteInfo?.lng != null && siteInfo?.lat != null) {
            return [siteInfo.lng, siteInfo.lat];
        }
        return DEFAULT_MAP_CENTER;
    }, [siteInfo?.lat, siteInfo?.lng]);
    const siteBoundary = useMemo<MapPolygon[]>(() => {
        const path = parseGeoJsonPolygon(siteInfo?.boundaryGeoJson);
        if (path.length < 3) {
            return [];
        }
        return [{
            id: `site-boundary-${siteInfo?.id || 'default'}`,
            path,
            color: '#1677ff',
            fillColor: '#91caff',
            fillOpacity: 0.22,
            weight: 3,
            opacity: 0.9,
        }];
    }, [siteInfo?.boundaryGeoJson, siteInfo?.id]);
    const siteDeviceMarkers = useMemo<MapMarker[]>(() => (
        siteInfo?.devices?.filter((item) => item.lng != null && item.lat != null).map((item) => ({
            id: `site-device-${item.id}`,
            position: [Number(item.lng), Number(item.lat)] as MapPoint,
            title: `${item.deviceName || item.deviceCode || '场地设备'}`
        })) || []
    ), [siteInfo?.devices]);

    const openDeviceModal = (device?: SiteDeviceRecord) => {
        setEditingDevice(device || null);
        deviceForm.setFieldsValue({
            deviceCode: device?.deviceCode || '',
            deviceName: device?.deviceName || '',
            deviceType: device?.deviceType || 'VIDEO_CAMERA',
            provider: device?.provider || '',
            ipAddress: device?.ipAddress || '',
            status: device?.status || 'ONLINE',
            lng: device?.lng ?? undefined,
            lat: device?.lat ?? undefined,
            remark: device?.remark || '',
        });
        setDeviceModalOpen(true);
    };

    const openPersonnelModal = (record?: SitePersonnelRecord) => {
        setEditingPersonnel(record || null);
        personnelForm.setFieldsValue({
            userId: record?.userId || undefined,
            roleType: record?.roleType || 'SITE_MANAGER',
            shiftGroup: record?.shiftGroup || '',
            dutyScope: record?.dutyScope || '',
            accountEnabled: record?.accountEnabled ?? true,
            remark: record?.remark || '',
        });
        setPersonnelModalOpen(true);
    };

    const openDocumentModal = (record?: SiteDocumentRecord, defaultStageCode?: string) => {
        setEditingDocument(record || null);
        documentForm.setFieldsValue({
            stageCode: record?.stageCode || defaultStageCode || 'APPROVAL',
            approvalType: record?.approvalType || '',
            documentType: record?.documentType || 'PROJECT_APPROVAL',
            fileName: record?.fileName || '',
            fileUrl: record?.fileUrl || '',
            fileSize: record?.fileSize ?? undefined,
            mimeType: record?.mimeType || '',
            remark: record?.remark || '',
        });
        setDocumentModalOpen(true);
    };

    const openSurveyModal = (record?: SiteSurveyRecord) => {
        setEditingSurvey(record || null);
        surveyForm.setFieldsValue({
            surveyNo: record?.surveyNo || '',
            surveyDate: record?.surveyDate || '',
            measuredVolume: record?.measuredVolume ?? 0,
            deductionVolume: record?.deductionVolume ?? 0,
            surveyCompany: record?.surveyCompany || '',
            surveyorName: record?.surveyorName || '',
            status: record?.status || 'DRAFT',
            reportUrl: record?.reportUrl || '',
            remark: record?.remark || '',
        });
        setSurveyModalOpen(true);
    };

    const handleSaveDevice = async () => {
        if (!id) {
            return;
        }
        try {
            const values = await deviceForm.validateFields();
            setDeviceSubmitting(true);
            const payload = {
                deviceCode: values.deviceCode,
                deviceName: values.deviceName,
                deviceType: values.deviceType,
                provider: values.provider,
                ipAddress: values.ipAddress,
                status: values.status,
                lng: values.lng,
                lat: values.lat,
                remark: values.remark,
            };
            if (editingDevice?.id) {
                await updateSiteDevice(id, editingDevice.id, payload);
                message.success('设备配置已更新');
            } else {
                await createSiteDevice(id, payload);
                message.success('设备已新增');
            }
            setDeviceModalOpen(false);
            setEditingDevice(null);
            deviceForm.resetFields();
            await reloadSiteData();
        } catch (error) {
            if ((error as Error)?.message?.includes('validation')) {
                return;
            }
            console.error(error);
            message.error('保存设备失败');
        } finally {
            setDeviceSubmitting(false);
        }
    };

    const handleSaveOperationConfig = async () => {
        if (!id) {
            return;
        }
        try {
            const values = await operationForm.validateFields();
            setOperationSubmitting(true);
            await updateSiteOperationConfig(id, {
                queueEnabled: values.queueEnabled,
                maxQueueCount: values.maxQueueCount,
                manualDisposalEnabled: values.manualDisposalEnabled,
                rangeCheckRadius: values.rangeCheckRadius,
                durationLimitMinutes: values.durationLimitMinutes,
                remark: values.remark,
            });
            message.success('运营配置已保存');
            await reloadSiteData();
        } catch (error) {
            console.error(error);
            message.error('保存运营配置失败');
        } finally {
            setOperationSubmitting(false);
        }
    };

    const handleSavePersonnel = async () => {
        if (!id) {
            return;
        }
        try {
            const values = await personnelForm.validateFields();
            setPersonnelSubmitting(true);
            const payload = {
                userId: values.userId,
                roleType: values.roleType,
                shiftGroup: values.shiftGroup,
                dutyScope: values.dutyScope,
                accountEnabled: values.accountEnabled,
                remark: values.remark,
            };
            if (editingPersonnel?.id) {
                await updateSitePersonnel(id, editingPersonnel.id, payload);
                message.success('人员配置已更新');
            } else {
                await createSitePersonnel(id, payload);
                message.success('场地人员已新增');
            }
            setPersonnelModalOpen(false);
            setEditingPersonnel(null);
            personnelForm.resetFields();
            await reloadSiteData();
        } catch (error) {
            if ((error as Error)?.message?.includes('validation')) {
                return;
            }
            console.error(error);
            message.error('保存人员配置失败');
        } finally {
            setPersonnelSubmitting(false);
        }
    };

    const handleDeletePersonnel = async (record: SitePersonnelRecord) => {
        if (!id || !record.id) {
            return;
        }
        try {
            await deleteSitePersonnel(id, record.id);
            message.success('人员配置已删除');
            await reloadSiteData();
        } catch (error) {
            console.error(error);
            message.error('删除人员配置失败');
        }
    };

    const handleSaveDocument = async () => {
        if (!id) {
            return;
        }
        try {
            const values = await documentForm.validateFields();
            setDocumentSubmitting(true);
            const payload = {
                stageCode: values.stageCode,
                approvalType: values.approvalType,
                documentType: values.documentType,
                fileName: values.fileName,
                fileUrl: values.fileUrl,
                fileSize: values.fileSize,
                mimeType: values.mimeType,
                remark: values.remark,
            };
            if (editingDocument?.id) {
                await updateSiteDocument(id, editingDocument.id, payload);
                message.success('场地资料已更新');
            } else {
                await createSiteDocument(id, payload);
                message.success('场地资料已新增');
            }
            setDocumentModalOpen(false);
            setEditingDocument(null);
            documentForm.resetFields();
            await reloadSiteData();
        } catch (error) {
            if ((error as Error)?.message?.includes('validation')) {
                return;
            }
            console.error(error);
            message.error('保存场地资料失败');
        } finally {
            setDocumentSubmitting(false);
        }
    };

    const handleDeleteDocument = async (record: SiteDocumentRecord) => {
        if (!id || !record.id) {
            return;
        }
        try {
            await deleteSiteDocument(id, record.id);
            message.success('场地资料已删除');
            await reloadSiteData();
        } catch (error) {
            console.error(error);
            message.error('删除场地资料失败');
        }
    };

    const handleSaveSurvey = async () => {
        if (!id) {
            return;
        }
        try {
            const values = await surveyForm.validateFields();
            setSurveySubmitting(true);
            const payload = {
                surveyNo: values.surveyNo,
                surveyDate: values.surveyDate,
                measuredVolume: values.measuredVolume,
                deductionVolume: values.deductionVolume,
                surveyCompany: values.surveyCompany,
                surveyorName: values.surveyorName,
                status: values.status,
                reportUrl: values.reportUrl,
                remark: values.remark,
            };
            if (editingSurvey?.id) {
                await updateSiteSurvey(id, editingSurvey.id, payload);
                message.success('测绘记录已更新');
            } else {
                await createSiteSurvey(id, payload);
                message.success('测绘记录已新增');
            }
            setSurveyModalOpen(false);
            setEditingSurvey(null);
            surveyForm.resetFields();
            await reloadSiteData();
        } catch (error) {
            if ((error as Error)?.message?.includes('validation')) {
                return;
            }
            console.error(error);
            message.error('保存测绘记录失败');
        } finally {
            setSurveySubmitting(false);
        }
    };

    const handleDeleteSurvey = async (record: SiteSurveyRecord) => {
        if (!id || !record.id) {
            return;
        }
        try {
            await deleteSiteSurvey(id, record.id);
            message.success('测绘记录已删除');
            await reloadSiteData();
        } catch (error) {
            console.error(error);
            message.error('删除测绘记录失败');
        }
    };

    const documentGroups = useMemo(() => ({
        APPROVAL: documents.filter((item) => item.stageCode === 'APPROVAL'),
        CONSTRUCTION: documents.filter((item) => item.stageCode === 'CONSTRUCTION'),
        OPERATION: documents.filter((item) => item.stageCode === 'OPERATION'),
        TRANSFER: documents.filter((item) => item.stageCode === 'TRANSFER'),
    }), [documents]);

    const items = [
        {
            key: 'info',
            label: '基础信息',
            children: (
                <div className="space-y-6">
                    <Card title="场地红线与设备点位" className="glass-panel g-border-panel border">
                        <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_280px] gap-4">
                            <div className="relative h-80 rounded overflow-hidden border g-border-panel">
                                <TiandituMap
                                    className="absolute inset-0"
                                    center={siteCenter}
                                    zoom={14}
                                    markers={siteDeviceMarkers}
                                    polygons={siteBoundary}
                                    loadingText="场地地图加载中..."
                                />
                            </div>
                            <div className="space-y-3">
                                <div className="p-3 rounded border g-border-panel bg-white">
                                    <div className="text-sm g-text-primary">中心坐标</div>
                                    <div className="text-xs g-text-secondary mt-1">{siteCenter[0]}, {siteCenter[1]}</div>
                                </div>
                                <div className="p-3 rounded border g-border-panel bg-white">
                                    <div className="text-sm g-text-primary">红线状态</div>
                                    <div className="text-xs g-text-secondary mt-1">{siteBoundary.length ? '已配置真实红线' : '暂无红线数据'}</div>
                                </div>
                                <div className="p-3 rounded border g-border-panel bg-white">
                                    <div className="text-sm g-text-primary">设备点位数</div>
                                    <div className="text-xs g-text-secondary mt-1">{siteDeviceMarkers.length} 个设备已定位</div>
                                </div>
                            </div>
                        </div>
                    </Card>
                    <Card title="场地基础信息" className="glass-panel g-border-panel border" extra={<Button type="link" icon={<EditOutlined />}>编辑</Button>}>
                        <Descriptions column={3} className="g-text-secondary">
                            <Descriptions.Item label="场地名称">{siteInfo?.name || '-'}</Descriptions.Item>
                            <Descriptions.Item label="场地类型"><Tag color="blue">{typeText}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地状态"><Tag color={statusText === '正常' ? 'green' : statusText === '预警' ? 'orange' : 'default'}>{statusText}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地层级"><Tag color={siteInfo?.siteLevel === 'SECONDARY' ? 'geekblue' : 'default'}>{resolveSiteLevel(siteInfo?.siteLevel)}</Tag></Descriptions.Item>
                            <Descriptions.Item label="场地编码">{siteInfo?.code || '-'}</Descriptions.Item>
                            <Descriptions.Item label="总容量">{(capacity / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="已用容量">{(used / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="剩余容量">{(remaining / 10000).toFixed(1)} 万方</Descriptions.Item>
                            <Descriptions.Item label="详细地址" span={2}>{siteInfo?.address || '-'}</Descriptions.Item>
                            <Descriptions.Item label="所属区域">{siteInfo?.managementArea || '-'}</Descriptions.Item>
                            <Descriptions.Item label="上级场地">{siteInfo?.parentSiteName || '-'}</Descriptions.Item>
                            <Descriptions.Item label="借用地磅场地">{siteInfo?.weighbridgeSiteName || '使用本场地设备'}</Descriptions.Item>
                            <Descriptions.Item label="关联项目ID">{siteInfo?.projectId || '-'}</Descriptions.Item>
                            <Descriptions.Item label="所属组织ID">{siteInfo?.orgId || '-'}</Descriptions.Item>
                            <Descriptions.Item label="中心经纬度">{siteInfo?.lng != null && siteInfo?.lat != null ? `${siteInfo.lng}, ${siteInfo.lat}` : '-'}</Descriptions.Item>
                            <Descriptions.Item label="创建时间">{siteInfo?.createTime || '-'}</Descriptions.Item>
                            <Descriptions.Item label="更新时间">{siteInfo?.updateTime || '-'}</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    <Card title="地磅联动说明" className="glass-panel g-border-panel border">
                        <Descriptions column={2} className="g-text-secondary">
                            <Descriptions.Item label="过磅场地选择">{siteInfo?.weighbridgeSiteName ? `当前可按 ${siteInfo.weighbridgeSiteName} 地磅场地过磅` : '默认按本场地设备过磅'}</Descriptions.Item>
                            <Descriptions.Item label="自有地磅数量">{siteInfo?.devices?.filter((item) => item.deviceType === 'WEIGHBRIDGE').length || 0} 台</Descriptions.Item>
                            <Descriptions.Item label="二级场地模式">{siteInfo?.siteLevel === 'SECONDARY' ? '已启用，可挂靠上级场地统一管理' : '未启用'}</Descriptions.Item>
                            <Descriptions.Item label="测绘结算">{(siteInfo?.devices?.filter((item) => item.deviceType === 'WEIGHBRIDGE').length || 0) > 0 || siteInfo?.weighbridgeSiteName ? '可结合地磅数据核对' : '无地磅场地可直接走测绘结算'}</Descriptions.Item>
                        </Descriptions>
                    </Card>
                    <Card title="结算规则配置" className="glass-panel g-border-panel border">
                        <Descriptions column={2} className="g-text-secondary">
                            <Descriptions.Item label="结算方式">{resolveSettlementMode(siteInfo)}</Descriptions.Item>
                            <Descriptions.Item label="消纳费单价">{Number(siteInfo?.disposalUnitPrice || 0).toLocaleString()} 元/方</Descriptions.Item>
                            <Descriptions.Item label="消纳费比例">{Number(siteInfo?.disposalFeeRate || 0) > 0 ? `${Math.round(Number(siteInfo?.disposalFeeRate || 0) * 100)}%` : '-'}</Descriptions.Item>
                            <Descriptions.Item label="平台服务费单价">{Number(siteInfo?.serviceFeeUnitPrice || 0).toLocaleString()} 元/方</Descriptions.Item>
                        </Descriptions>
                    </Card>
                </div>
            ),
        },
        {
            key: 'disposals',
            label: '消纳清单',
            children: (
                <Card className="glass-panel g-border-panel border" bodyStyle={{ padding: 0 }}>
                    <div className="p-4 border-b g-border-panel border flex justify-between">
                        <Space>
                            <Select defaultValue="all" style={{ width: 120 }} options={[{ value: 'all', label: '全部来源' }, { value: 'scale', label: '地磅称重' }, { value: 'manual', label: '人工录入' }]} />
                            <Select defaultValue="valid" style={{ width: 120 }} options={[{ value: 'valid', label: '有效记录' }, { value: 'invalid', label: '已作废' }]} />
                        </Space>
                        <Button>导出 Excel</Button>
                    </div>
                    <Table
                        locale={{ emptyText: disposalsLoading ? '加载中...' : '当前无消纳记录' }}
                        dataSource={disposals.map((item) => ({
                            id: item.id,
                            time: item.time || '-',
                            car: item.plate || '-',
                            project: item.project || '-',
                            source: item.source || '-',
                            amount: item.volume ?? 0,
                            status: item.status || '空',
                        }))}
                        loading={disposalsLoading}
                        rowKey="id"
                        columns={[
                            { title: '记录编号', dataIndex: 'id', key: 'id' },
                            { title: '消纳时间', dataIndex: 'time', key: 'time' },
                            { title: '车牌号', dataIndex: 'car', key: 'car' },
                            { title: '来源', dataIndex: 'source', key: 'source' },
                            { title: '消纳量(方)', dataIndex: 'amount', key: 'amount' },
                            { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === '正常' || s === '有效' ? 'success' : 'default'}>{s}</Tag> },
                            { title: '操作', key: 'action', render: () => <a className="g-text-error">作废</a> }
                        ]}
                        pagination={false}
                        className="bg-transparent"
                        rowClassName="hover:bg-white transition-colors"
                    />
                </Card>
            ),
        },
        {
            key: 'survey',
            label: '场地测绘',
            children: (
                <Card className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openSurveyModal()}>新增测绘</Button>}>
                    <div className="mb-4 text-sm g-text-secondary">
                        无地磅场地可通过测绘记录直接形成结算方量；系统会自动按 `测得方量 - 扣减方量 = 结算方量` 计算。
                    </div>
                    <Table
                        rowKey="id"
                        dataSource={surveys}
                        pagination={false}
                        locale={{ emptyText: '当前暂无测绘结算记录' }}
                        columns={[
                            { title: '测绘编号', dataIndex: 'surveyNo', key: 'surveyNo' },
                            { title: '测绘日期', dataIndex: 'surveyDate', key: 'surveyDate' },
                            { title: '测得方量', dataIndex: 'measuredVolume', key: 'measuredVolume', render: (value: number) => Number(value || 0).toLocaleString() },
                            { title: '扣减方量', dataIndex: 'deductionVolume', key: 'deductionVolume', render: (value: number) => Number(value || 0).toLocaleString() },
                            { title: '结算方量', dataIndex: 'settlementVolume', key: 'settlementVolume', render: (value: number) => <span className="g-text-success font-semibold">{Number(value || 0).toLocaleString()}</span> },
                            { title: '测绘单位', dataIndex: 'surveyCompany', key: 'surveyCompany' },
                            { title: '状态', dataIndex: 'status', key: 'status', render: (value: string) => <Tag color={value === 'CONFIRMED' ? 'success' : value === 'ARCHIVED' ? 'processing' : 'default'}>{resolveSurveyStatus(value)}</Tag> },
                            {
                                title: '操作',
                                key: 'action',
                                render: (_: unknown, record: SiteSurveyRecord) => (
                                    <Space>
                                        <a onClick={() => openSurveyModal(record)}>编辑</a>
                                        <Popconfirm title="确认删除该测绘记录？" onConfirm={() => void handleDeleteSurvey(record)}>
                                            <a className="g-text-error">删除</a>
                                        </Popconfirm>
                                    </Space>
                                ),
                            },
                        ]}
                    />
                </Card>
            ),
        },
        {
            key: 'docs',
            label: '场地资料',
            children: (
                <div className="space-y-6">
                    <Card title="审批阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openDocumentModal(undefined, 'APPROVAL')}>上传资料</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无审批阶段资料' }}
                            dataSource={documentGroups.APPROVAL}
                            renderItem={item => (
                                <List.Item actions={[
                                    <a key="preview" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>预览</a>,
                                    <a key="download" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>下载</a>,
                                    <a key="edit" onClick={() => openDocumentModal(item)}>编辑</a>,
                                    <Popconfirm key="delete" title="确认删除该资料？" onConfirm={() => void handleDeleteDocument(item)}>
                                        <a className="g-text-error">删除</a>
                                    </Popconfirm>,
                                ]}>
                                    <List.Item.Meta title={<Space wrap><span className="g-text-primary">{item.fileName}</span><Tag color="blue">{resolveDocumentType(item.documentType)}</Tag><Tag>{item.formatRequirement || '-'}</Tag></Space>} description={<span className="g-text-secondary">{item.uploaderName || '-'} 上传于 {item.updateTime || '-'} {item.remark ? `| ${item.remark}` : ''}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="建设阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openDocumentModal(undefined, 'CONSTRUCTION')}>上传资料</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无建设阶段资料' }}
                            dataSource={documentGroups.CONSTRUCTION}
                            renderItem={item => (
                                <List.Item actions={[
                                    <a key="preview" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>预览</a>,
                                    <a key="download" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>下载</a>,
                                    <a key="edit" onClick={() => openDocumentModal(item)}>编辑</a>,
                                    <Popconfirm key="delete" title="确认删除该资料？" onConfirm={() => void handleDeleteDocument(item)}>
                                        <a className="g-text-error">删除</a>
                                    </Popconfirm>,
                                ]}>
                                    <List.Item.Meta title={<Space wrap><span className="g-text-primary">{item.fileName}</span><Tag color="blue">{resolveDocumentType(item.documentType)}</Tag><Tag>{item.formatRequirement || '-'}</Tag></Space>} description={<span className="g-text-secondary">{item.uploaderName || '-'} 上传于 {item.updateTime || '-'} {item.remark ? `| ${item.remark}` : ''}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="运营阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openDocumentModal(undefined, 'OPERATION')}>上传资料</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无运营阶段资料' }}
                            dataSource={documentGroups.OPERATION}
                            renderItem={item => (
                                <List.Item actions={[
                                    <a key="preview" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>预览</a>,
                                    <a key="download" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>下载</a>,
                                    <a key="edit" onClick={() => openDocumentModal(item)}>编辑</a>,
                                    <Popconfirm key="delete" title="确认删除该资料？" onConfirm={() => void handleDeleteDocument(item)}>
                                        <a className="g-text-error">删除</a>
                                    </Popconfirm>,
                                ]}>
                                    <List.Item.Meta title={<Space wrap><span className="g-text-primary">{item.fileName}</span><Tag color="blue">{resolveDocumentType(item.documentType)}</Tag><Tag>{item.formatRequirement || '-'}</Tag></Space>} description={<span className="g-text-secondary">{item.uploaderName || '-'} 上传于 {item.updateTime || '-'} {item.remark ? `| ${item.remark}` : ''}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="移交阶段资料" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openDocumentModal(undefined, 'TRANSFER')}>上传资料</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无移交阶段资料' }}
                            dataSource={documentGroups.TRANSFER}
                            renderItem={item => (
                                <List.Item actions={[
                                    <a key="preview" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>预览</a>,
                                    <a key="download" onClick={() => item.fileUrl && window.open(item.fileUrl, '_blank')}>下载</a>,
                                    <a key="edit" onClick={() => openDocumentModal(item)}>编辑</a>,
                                    <Popconfirm key="delete" title="确认删除该资料？" onConfirm={() => void handleDeleteDocument(item)}>
                                        <a className="g-text-error">删除</a>
                                    </Popconfirm>,
                                ]}>
                                    <List.Item.Meta title={<Space wrap><span className="g-text-primary">{item.fileName}</span><Tag color="blue">{resolveDocumentType(item.documentType)}</Tag><Tag>{item.formatRequirement || '-'}</Tag></Space>} description={<span className="g-text-secondary">{item.uploaderName || '-'} 上传于 {item.updateTime || '-'} {item.remark ? `| ${item.remark}` : ''}</span>} />
                                </List.Item>
                            )}
                        />
                    </Card>
                </div>
            ),
        },
        {
            key: 'config',
            label: '场地配置',
            children: (
                <div className="space-y-6">
                    <Card title="人员配置" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openPersonnelModal()}>新增人员</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无人员配置' }}
                            dataSource={personnel}
                            renderItem={item => (
                                <List.Item
                                    actions={[
                                        <a key="edit" onClick={() => openPersonnelModal(item)}>编辑</a>,
                                        <Popconfirm
                                            key="delete"
                                            title="确认删除该场地人员配置？"
                                            onConfirm={() => void handleDeletePersonnel(item)}
                                        >
                                            <a className="g-text-error">删除</a>
                                        </Popconfirm>,
                                    ]}
                                >
                                    <List.Item.Meta
                                        title={(
                                            <Space wrap>
                                                <span className="g-text-primary">{item.userName || item.username || '未命名人员'}</span>
                                                <Tag color="blue">{resolvePersonnelRole(item.roleType)}</Tag>
                                                <Tag color={item.accountEnabled ? 'success' : 'default'}>
                                                    {item.accountEnabled ? '已启用' : '已停用'}
                                                </Tag>
                                            </Space>
                                        )}
                                        description={(
                                            <span className="g-text-secondary">
                                                账号: {item.username || '-'} | 组织: {item.orgName || '-'} | 手机: {item.mobile || '-'} | 班次: {item.shiftGroup || '-'} | 职责: {item.dutyScope || '-'}
                                            </span>
                                        )}
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                    <Card title="设备配置" className="glass-panel g-border-panel border" extra={<Button type="primary" size="small" onClick={() => openDeviceModal()}>新增设备</Button>}>
                        <List
                            locale={{ emptyText: '当前暂无设备配置' }}
                            dataSource={siteInfo?.devices || []}
                            renderItem={item => (
                                <List.Item actions={[<a onClick={() => openDeviceModal(item)}>配置</a>]}>
                                    <List.Item.Meta
                                        title={<Space><span className="g-text-primary">{item.deviceName || item.deviceCode}</span><Tag color={item.status === 'ONLINE' ? 'success' : item.status === 'OFFLINE' ? 'error' : 'default'}>{item.status === 'ONLINE' ? '在线' : item.status === 'OFFLINE' ? '离线' : (item.status || '-')}</Tag></Space>}
                                        description={<span className="g-text-secondary">类型: {item.deviceType || '-'} | IP: {item.ipAddress || '-'} | 坐标: {item.lng != null && item.lat != null ? `${item.lng}, ${item.lat}` : '-'}</span>}
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                    
                    <Card title="运营配置" className="glass-panel g-border-panel border">
                        <Form form={operationForm} layout="vertical">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <Form.Item name="queueEnabled" label="排号规则" valuePropName="checked">
                                    <Switch />
                                </Form.Item>
                                <Form.Item name="maxQueueCount" label="最大等待数">
                                    <InputNumber min={0} className="w-full" />
                                </Form.Item>
                                <Form.Item name="manualDisposalEnabled" label="人工消纳开关" valuePropName="checked">
                                    <Switch />
                                </Form.Item>
                                <Form.Item name="rangeCheckRadius" label="范围检测半径（米）">
                                    <InputNumber min={0} className="w-full" />
                                </Form.Item>
                                <Form.Item name="durationLimitMinutes" label="消纳时长限制（分钟）">
                                    <InputNumber min={0} className="w-full" />
                                </Form.Item>
                                <Form.Item name="remark" label="备注">
                                    <Input.TextArea rows={3} />
                                </Form.Item>
                            </div>
                            <div className="mt-4">
                                <Button type="primary" loading={operationSubmitting} onClick={() => void handleSaveOperationConfig()}>保存配置</Button>
                            </div>
                        </Form>
                    </Card>
                </div>
            ),
        }
    ];

    return (
        <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3 }}
            className="space-y-6 pb-10"
        >
            <div className="flex items-center gap-4 mb-6">
                <Button 
                    type="text" 
                    icon={<ArrowLeftOutlined />} 
                    onClick={() => navigate('/sites')}
                    className="g-text-secondary hover:g-text-primary"
                />
                <div>
                    <h1 className="text-2xl font-bold g-text-primary m-0">{siteInfo?.name || '场地详情'}</h1>
                    <p className="g-text-secondary mt-1">场地类型: {typeText}</p>
                </div>
            </div>
            <Spin spinning={loading}>
                {siteInfo ? (
                    <Tabs
                        defaultActiveKey={defaultTab}
                        items={items}
                        className="custom-tabs"
                    />
                ) : (
                    <Card className="glass-panel g-border-panel border">
                        <Empty description="场地不存在或暂无数据" />
                    </Card>
                )}
            </Spin>
            <Modal
                title={editingDocument ? '编辑场地资料' : '新增场地资料'}
                open={documentModalOpen}
                onCancel={() => {
                    setDocumentModalOpen(false);
                    setEditingDocument(null);
                    documentForm.resetFields();
                }}
                onOk={() => void handleSaveDocument()}
                confirmLoading={documentSubmitting}
            >
                <Form form={documentForm} layout="vertical">
                    <Form.Item name="stageCode" label="资料阶段" rules={[{ required: true, message: '请选择资料阶段' }]}>
                        <Select options={[
                            { value: 'APPROVAL', label: '审批阶段资料' },
                            { value: 'CONSTRUCTION', label: '建设阶段资料' },
                            { value: 'OPERATION', label: '运营阶段资料' },
                            { value: 'TRANSFER', label: '移交阶段资料' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="approvalType" label="审批类型">
                        <Select allowClear options={[
                            { value: 'PROJECT', label: '立项' },
                            { value: 'EIA', label: '环评' },
                            { value: 'LAND', label: '用地/租赁' },
                            { value: 'LICENSE', label: '证照' },
                            { value: 'CONSTRUCTION', label: '建设资料' },
                            { value: 'SAFETY', label: '安全运营' },
                            { value: 'TRANSFER', label: '移交验收' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="documentType" label="资料类型" rules={[{ required: true, message: '请选择资料类型' }]}>
                        <Select options={[
                            { value: 'PROJECT_APPROVAL', label: '立项批复（PDF）' },
                            { value: 'EIA_APPROVAL', label: '环评批复（PDF）' },
                            { value: 'LAND_LEASE', label: '土地租赁合同（PDF）' },
                            { value: 'BUSINESS_LICENSE', label: '营业执照（PDF）' },
                            { value: 'CONSTRUCTION_PLAN', label: '建设方案（PDF）' },
                            { value: 'BOUNDARY_SURVEY', label: '红线测绘资料（PDF/JPG/PNG）' },
                            { value: 'COMPLETION_ACCEPTANCE', label: '竣工验收资料（PDF）' },
                            { value: 'SAFETY_INSPECTION', label: '安全检查记录（DOCX/PDF）' },
                            { value: 'OPERATION_LEDGER', label: '运营台账（XLSX/PDF）' },
                            { value: 'WEIGHBRIDGE_RECORD', label: '地磅记录（XLSX/CSV）' },
                            { value: 'TRANSFER_ACCEPTANCE', label: '移交验收资料（PDF）' },
                            { value: 'SITE_ARCHIVE', label: '场地归档包（ZIP/PDF）' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="fileName" label="文件名" rules={[{ required: true, message: '请输入文件名' }]}>
                        <Input placeholder="需符合资料类型格式要求，如 xxx.pdf / xxx.docx" />
                    </Form.Item>
                    <Form.Item name="fileUrl" label="文件地址" rules={[{ required: true, message: '请输入文件地址' }]}>
                        <Input placeholder="例如：https://files.local/site/approval.pdf" />
                    </Form.Item>
                    <Form.Item name="fileSize" label="文件大小（字节）">
                        <InputNumber min={0} className="w-full" />
                    </Form.Item>
                    <Form.Item name="mimeType" label="MIME 类型">
                        <Input placeholder="例如：application/pdf" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} />
                    </Form.Item>
                </Form>
            </Modal>
            <Modal
                title={editingPersonnel ? '编辑场地人员' : '新增场地人员'}
                open={personnelModalOpen}
                onCancel={() => {
                    setPersonnelModalOpen(false);
                    setEditingPersonnel(null);
                    personnelForm.resetFields();
                }}
                onOk={() => void handleSavePersonnel()}
                confirmLoading={personnelSubmitting}
            >
                <Form form={personnelForm} layout="vertical">
                    <Form.Item name="userId" label="人员账号" rules={[{ required: true, message: '请选择人员账号' }]}>
                        <Select
                            showSearch
                            optionFilterProp="label"
                            options={personnelCandidates.map((item) => ({
                                value: item.userId,
                                label: `${item.userName || item.username || item.userId} / ${item.orgName || '未分配组织'} / ${item.mobile || '无手机号'}`,
                            }))}
                        />
                    </Form.Item>
                    <Form.Item name="roleType" label="岗位类型" rules={[{ required: true, message: '请选择岗位类型' }]}>
                        <Select
                            options={[
                                { value: 'SITE_MANAGER', label: '场地管理员' },
                                { value: 'SCALE_OPERATOR', label: '地磅员' },
                                { value: 'SAFETY_OFFICER', label: '安全员' },
                                { value: 'DISPATCHER', label: '调度员' },
                                { value: 'INSPECTOR', label: '巡检员' },
                                { value: 'FINANCE', label: '财务' },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item name="shiftGroup" label="班次分组">
                        <Input placeholder="如：白班 / 夜班 / 全天值守" />
                    </Form.Item>
                    <Form.Item name="dutyScope" label="职责范围">
                        <Input.TextArea rows={3} placeholder="如：地磅复核、人工消纳、现场巡检" />
                    </Form.Item>
                    <Form.Item name="accountEnabled" label="账号启用" valuePropName="checked">
                        <Switch />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} />
                    </Form.Item>
                </Form>
            </Modal>
            <Modal
                title={editingSurvey ? '编辑测绘记录' : '新增测绘记录'}
                open={surveyModalOpen}
                onCancel={() => {
                    setSurveyModalOpen(false);
                    setEditingSurvey(null);
                    surveyForm.resetFields();
                }}
                onOk={() => void handleSaveSurvey()}
                confirmLoading={surveySubmitting}
            >
                <Form form={surveyForm} layout="vertical">
                    <Form.Item name="surveyNo" label="测绘编号" rules={[{ required: true, message: '请输入测绘编号' }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="surveyDate" label="测绘日期" rules={[{ required: true, message: '请输入测绘日期' }]}>
                        <Input placeholder="YYYY-MM-DD" />
                    </Form.Item>
                    <Form.Item name="measuredVolume" label="测得方量">
                        <InputNumber min={0} className="w-full" />
                    </Form.Item>
                    <Form.Item name="deductionVolume" label="扣减方量">
                        <InputNumber min={0} className="w-full" />
                    </Form.Item>
                    <Form.Item name="surveyCompany" label="测绘单位">
                        <Input />
                    </Form.Item>
                    <Form.Item name="surveyorName" label="测绘员">
                        <Input />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select options={[
                            { value: 'DRAFT', label: '草稿' },
                            { value: 'CONFIRMED', label: '已确认' },
                            { value: 'ARCHIVED', label: '已归档' },
                        ]} />
                    </Form.Item>
                    <Form.Item name="reportUrl" label="测绘报告地址">
                        <Input placeholder="https://files.local/site-survey/report.pdf" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} />
                    </Form.Item>
                </Form>
            </Modal>
            <Modal
                title={editingDevice ? '编辑设备' : '新增设备'}
                open={deviceModalOpen}
                onCancel={() => {
                    setDeviceModalOpen(false);
                    setEditingDevice(null);
                    deviceForm.resetFields();
                }}
                onOk={() => void handleSaveDevice()}
                confirmLoading={deviceSubmitting}
            >
                <Form form={deviceForm} layout="vertical">
                    <Form.Item name="deviceCode" label="设备编码" rules={[{ required: true, message: '请输入设备编码' }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="deviceName" label="设备名称" rules={[{ required: true, message: '请输入设备名称' }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="deviceType" label="设备类型" rules={[{ required: true, message: '请选择设备类型' }]}>
                        <Select
                            options={[
                                { value: 'VIDEO_CAMERA', label: '视频监控' },
                                { value: 'CAPTURE_CAMERA', label: '抓拍机' },
                                { value: 'WEIGHBRIDGE', label: '地磅' },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item name="provider" label="设备厂家">
                        <Input />
                    </Form.Item>
                    <Form.Item name="ipAddress" label="设备IP">
                        <Input />
                    </Form.Item>
                    <Form.Item name="status" label="设备状态">
                        <Select options={[{ value: 'ONLINE', label: '在线' }, { value: 'OFFLINE', label: '离线' }]} />
                    </Form.Item>
                    <Form.Item name="lng" label="经度">
                        <InputNumber className="w-full" />
                    </Form.Item>
                    <Form.Item name="lat" label="纬度">
                        <InputNumber className="w-full" />
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={3} />
                    </Form.Item>
                </Form>
            </Modal>
        </motion.div>
    );

export default SiteDetail;
