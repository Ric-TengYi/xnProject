import React, { useEffect } from 'react';
import { Modal, Form, Input, Switch, InputNumber, message } from 'antd';
import type { ProjectConfigRecord } from '../utils/projectApi';
import { updateProjectConfig } from '../utils/projectApi';

interface ProjectConfigModalProps {
  projectId: string;
  visible: boolean;
  initialValues: ProjectConfigRecord | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const ProjectConfigModal: React.FC<ProjectConfigModalProps> = ({
  projectId,
  visible,
  initialValues,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm<ProjectConfigRecord>();
  const [loading, setLoading] = React.useState(false);

  useEffect(() => {
    if (visible) {
      form.setFieldsValue(initialValues || {});
    } else {
      form.resetFields();
    }
  }, [visible, initialValues, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await updateProjectConfig(projectId, values);
      message.success('项目配置更新成功');
      onSuccess();
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="编辑项目配置"
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
    >
      <Form form={form} layout="vertical">
        <div className="grid grid-cols-2 gap-4">
          <Form.Item name="checkinEnabled" label="打卡配置" valuePropName="checked">
            <Switch checkedChildren="已启用" unCheckedChildren="未启用" />
          </Form.Item>
          <Form.Item name="checkinAccount" label="打卡账号">
            <Input placeholder="请输入打卡账号" />
          </Form.Item>
          <Form.Item name="checkinAuthScope" label="打卡授权范围" className="col-span-2">
            <Input placeholder="请输入打卡授权范围" />
          </Form.Item>
          <Form.Item name="locationCheckRequired" label="位置判断配置" valuePropName="checked">
            <Switch checkedChildren="启用位置判断" unCheckedChildren="未启用" />
          </Form.Item>
          <Form.Item name="locationRadiusMeters" label="位置判断半径(米)">
            <InputNumber min={0} className="w-full" placeholder="请输入半径" />
          </Form.Item>
          <Form.Item name="preloadVolume" label="出土预扣值(方)">
            <InputNumber min={0} className="w-full" placeholder="请输入预扣值" />
          </Form.Item>
          <Form.Item name="violationRuleEnabled" label="违规围栏规则" valuePropName="checked">
            <Switch checkedChildren="围栏规则启用" unCheckedChildren="围栏规则关闭" />
          </Form.Item>
          <Form.Item name="violationFenceName" label="违规围栏名称">
            <Input placeholder="请输入违规围栏名称" />
          </Form.Item>
          <Form.Item name="violationFenceCode" label="违规围栏编码">
            <Input placeholder="请输入违规围栏编码" />
          </Form.Item>
          <Form.Item name="routeGeoJson" label="线路配置 (GeoJSON)" className="col-span-2">
            <Input.TextArea rows={4} placeholder="请输入线路 GeoJSON" />
          </Form.Item>
          <Form.Item name="violationFenceGeoJson" label="违规围栏 (GeoJSON)" className="col-span-2">
            <Input.TextArea rows={4} placeholder="请输入违规围栏 GeoJSON" />
          </Form.Item>
          <Form.Item name="remark" label="备注" className="col-span-2">
            <Input.TextArea rows={2} placeholder="请输入备注" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  );
};

export default ProjectConfigModal;
