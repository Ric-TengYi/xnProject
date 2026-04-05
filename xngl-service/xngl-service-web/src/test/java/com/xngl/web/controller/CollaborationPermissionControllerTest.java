package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xngl.infrastructure.persistence.entity.alert.AlertEvent;
import com.xngl.infrastructure.persistence.entity.event.ManualEvent;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.security.SecurityInspection;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.mapper.AlertEventMapper;
import com.xngl.infrastructure.persistence.mapper.AlertFenceMapper;
import com.xngl.infrastructure.persistence.mapper.AlertPushRuleMapper;
import com.xngl.infrastructure.persistence.mapper.AlertRuleMapper;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventAuditLogMapper;
import com.xngl.infrastructure.persistence.mapper.ManualEventMapper;
import com.xngl.infrastructure.persistence.mapper.MiniSafetyLearningRecordMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionActionMapper;
import com.xngl.infrastructure.persistence.mapper.SecurityInspectionMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDocumentMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.UserMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleInsuranceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMaintenanceRecordMapper;
import com.xngl.infrastructure.persistence.mapper.VehiclePersonnelCertificateMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleViolationRecordMapper;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.manager.message.MessageRecordService;
import com.xngl.manager.sysparam.mapper.SysParamMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.CollaborationAccessScope;
import com.xngl.web.support.CollaborationAccessScopeResolver;
import com.xngl.web.support.UserContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CollaborationPermissionControllerTest {

  @Mock
  private AlertEventMapper alertEventMapper;

  @Mock
  private AlertFenceMapper alertFenceMapper;

  @Mock
  private AlertRuleMapper alertRuleMapper;

  @Mock
  private AlertPushRuleMapper alertPushRuleMapper;

  @Mock
  private ProjectMapper projectMapper;

  @Mock
  private SiteMapper siteMapper;

  @Mock
  private VehicleMapper vehicleMapper;

  @Mock
  private VehicleViolationRecordMapper vehicleViolationRecordMapper;

  @Mock
  private ContractMapper contractMapper;

  @Mock
  private ContractTicketMapper contractTicketMapper;

  @Mock
  private SecurityInspectionMapper securityInspectionMapper;

  @Mock
  private DisposalPermitMapper disposalPermitMapper;

  @Mock
  private UserMapper userMapper;

  @Mock
  private SysParamMapper sysParamMapper;

  @Mock
  private ManualEventMapper manualEventMapper;

  @Mock
  private ManualEventAuditLogMapper manualEventAuditLogMapper;

  @Mock
  private MessageRecordService messageRecordService;

  @Mock
  private SecurityInspectionActionMapper securityInspectionActionMapper;

  @Mock
  private VehiclePersonnelCertificateMapper vehiclePersonnelCertificateMapper;

  @Mock
  private MiniSafetyLearningRecordMapper miniSafetyLearningRecordMapper;

  @Mock
  private VehicleInsuranceRecordMapper vehicleInsuranceRecordMapper;

  @Mock
  private VehicleMaintenanceRecordMapper vehicleMaintenanceRecordMapper;

  @Mock
  private SiteDocumentMapper siteDocumentMapper;

  @Mock
  private SiteDeviceMapper siteDeviceMapper;

  @Mock
  private UserContext userContext;

  @Mock
  private CollaborationAccessScopeResolver collaborationAccessScopeResolver;

  @Test
  void alertsListShouldHideOutOfScopeRows() {
    AlertsController controller =
        new AlertsController(
            alertEventMapper,
            alertFenceMapper,
            alertRuleMapper,
            alertPushRuleMapper,
            projectMapper,
            siteMapper,
            vehicleMapper,
            vehicleViolationRecordMapper,
            contractMapper,
            contractTicketMapper,
            securityInspectionMapper,
            disposalPermitMapper,
            userMapper,
            sysParamMapper,
            userContext,
            collaborationAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(11L, 1L);
    AlertEvent visible = buildAlert(1L, 1L, 100L);
    AlertEvent hidden = buildAlert(2L, 1L, 200L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(collaborationAccessScopeResolver.resolve(currentUser))
        .thenReturn(CollaborationAccessScope.scoped(11L, List.of(), List.of(100L), List.of(), List.of(), List.of(), List.of()));
    when(alertEventMapper.selectList(any())).thenReturn(List.of(visible, hidden));
    when(projectMapper.selectBatchIds(any())).thenReturn(List.of(buildProject(100L, 10L, "Visible"), buildProject(200L, 20L, "Hidden")));
    ApiResult<List<java.util.Map<String, Object>>> result =
        controller.list(null, null, null, null, null, null, null, null, null, null, null, request);

    assertThat(result.getData()).extracting(item -> item.get("id")).containsExactly(1L);
  }

  @Test
  void manualEventCreateShouldRejectOutOfScopeProjectReference() {
    ManualEventsController controller =
        new ManualEventsController(
            manualEventMapper,
            manualEventAuditLogMapper,
            projectMapper,
            siteMapper,
            vehicleMapper,
            messageRecordService,
            userContext,
            collaborationAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(12L, 1L);
    ManualEventsController.EventUpsertRequest body = new ManualEventsController.EventUpsertRequest();
    body.setEventType("SITE_EXCEPTION");
    body.setTitle("Out Of Scope");
    body.setProjectId(200L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(collaborationAccessScopeResolver.resolve(currentUser))
        .thenReturn(CollaborationAccessScope.scoped(12L, List.of(), List.of(100L), List.of(), List.of(), List.of(), List.of()));
    when(projectMapper.selectById(200L)).thenReturn(buildProject(200L, 20L, "Hidden Project"));

    assertThatThrownBy(() -> controller.create(body, request))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("项目");
  }

  @Test
  void manualEventApproveShouldRequireApprovalPermission() {
    ManualEventsController controller =
        new ManualEventsController(
            manualEventMapper,
            manualEventAuditLogMapper,
            projectMapper,
            siteMapper,
            vehicleMapper,
            messageRecordService,
            userContext,
            collaborationAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(13L, 1L);
    ManualEvent event = buildEvent(31L, 1L, 100L, "PENDING_AUDIT");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    org.mockito.Mockito.doThrow(new BizException(403, "无审批权限"))
        .when(userContext)
        .requireApprovalPermission(currentUser);

    assertThatThrownBy(() -> controller.approve(31L, new ManualEventsController.AuditActionRequest(), request))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("审批");
  }

  @Test
  void securityListShouldHideOutOfScopeRows() {
    SecurityInspectionsController controller =
        new SecurityInspectionsController(
            securityInspectionMapper,
            securityInspectionActionMapper,
            projectMapper,
            siteMapper,
            vehicleMapper,
            userMapper,
            vehiclePersonnelCertificateMapper,
            miniSafetyLearningRecordMapper,
            vehicleInsuranceRecordMapper,
            vehicleMaintenanceRecordMapper,
            alertEventMapper,
            siteDocumentMapper,
            siteDeviceMapper,
            messageRecordService,
            userContext,
            collaborationAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(14L, 1L);
    SecurityInspection visible = buildInspection(41L, 1L, 300L);
    SecurityInspection hidden = buildInspection(42L, 1L, 400L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(collaborationAccessScopeResolver.resolve(currentUser))
        .thenReturn(CollaborationAccessScope.scoped(14L, List.of(), List.of(), List.of(300L), List.of(), List.of(), List.of()));
    when(securityInspectionMapper.selectList(any())).thenReturn(List.of(visible, hidden));
    when(siteMapper.selectBatchIds(any())).thenReturn(List.of(buildSite(300L, 100L, 10L, "Visible Site"), buildSite(400L, 200L, 20L, "Hidden Site")));

    ApiResult<List<java.util.Map<String, Object>>> result =
        controller.list(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, request);

    assertThat(result.getData()).extracting(item -> item.get("id")).containsExactly(41L);
  }

  private User buildUser(Long id, Long tenantId) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    user.setMainOrgId(10L);
    user.setUserType("ORG_ADMIN");
    user.setUsername("tester");
    user.setName("Tester");
    return user;
  }

  private AlertEvent buildAlert(Long id, Long tenantId, Long projectId) {
    AlertEvent event = new AlertEvent();
    event.setId(id);
    event.setTenantId(tenantId);
    event.setProjectId(projectId);
    event.setTargetType("PROJECT");
    event.setTargetId(projectId);
    event.setTitle("alert-" + id);
    return event;
  }

  private ManualEvent buildEvent(Long id, Long tenantId, Long projectId, String status) {
    ManualEvent event = new ManualEvent();
    event.setId(id);
    event.setTenantId(tenantId);
    event.setProjectId(projectId);
    event.setStatus(status);
    event.setTitle("event-" + id);
    event.setCurrentAuditNode("MANUAL_EVENT_AUDIT");
    return event;
  }

  private SecurityInspection buildInspection(Long id, Long tenantId, Long siteId) {
    SecurityInspection inspection = new SecurityInspection();
    inspection.setId(id);
    inspection.setTenantId(tenantId);
    inspection.setSiteId(siteId);
    inspection.setTitle("inspection-" + id);
    inspection.setStatus("OPEN");
    inspection.setObjectType("SITE");
    inspection.setObjectId(siteId);
    return inspection;
  }

  private Project buildProject(Long id, Long orgId, String name) {
    Project project = new Project();
    project.setId(id);
    project.setOrgId(orgId);
    project.setName(name);
    return project;
  }

  private Site buildSite(Long id, Long projectId, Long orgId, String name) {
    Site site = new Site();
    site.setId(id);
    site.setProjectId(projectId);
    site.setOrgId(orgId);
    site.setName(name);
    return site;
  }
}
