package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.alert.AlertFence;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.ProjectConfig;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectConfigMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SiteOperationConfigMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleTrackPointMapper;
import com.xngl.manager.disposal.entity.DisposalPermit;
import com.xngl.manager.disposal.mapper.DisposalPermitMapper;
import com.xngl.manager.project.ProjectPaymentService;
import com.xngl.manager.project.ProjectPaymentSummaryVo;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.project.ProjectListItemDto;
import com.xngl.web.dto.project.ProjectConfigUpdateRequestDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.dto.site.SiteListItemDto;
import com.xngl.web.dto.site.SiteCreateDto;
import com.xngl.web.dto.unit.UnitListItemDto;
import com.xngl.web.dto.vehicle.VehicleListItemDto;
import com.xngl.web.support.MasterDataAccessScope;
import com.xngl.web.support.MasterDataAccessScopeResolver;
import com.xngl.web.support.UserContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class MasterDataPermissionControllerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private OrgMapper orgMapper;

  @Mock
  private ProjectMapper projectMapper;

  @Mock
  private SiteMapper siteMapper;

  @Mock
  private ContractMapper contractMapper;

  @Mock
  private VehicleMapper vehicleMapper;

  @Mock
  private UserContext userContext;

  @Mock
  private MasterDataAccessScopeResolver accessScopeResolver;

  @Mock
  private ProjectPaymentService projectPaymentService;

  @Mock
  private ContractTicketMapper contractTicketMapper;

  @Mock
  private ProjectConfigMapper projectConfigMapper;

  @Mock
  private com.xngl.infrastructure.persistence.mapper.AlertFenceMapper alertFenceMapper;

  @Mock
  private SiteService siteService;

  @Mock
  private SiteDeviceMapper siteDeviceMapper;

  @Mock
  private SiteOperationConfigMapper siteOperationConfigMapper;

  @Mock
  private VehicleTrackPointMapper vehicleTrackPointMapper;

  @Mock
  private DisposalPermitMapper disposalPermitMapper;

  @Test
  void unitsListShouldHideOutOfScopeOrganizations() {
    UnitsController controller =
        new UnitsController(
            orgMapper,
            projectMapper,
            siteMapper,
            contractMapper,
            vehicleMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(1L, 1L);
    Org visible = buildOrg(10L, 1L, "Visible Org");
    visible.setOrgType("CONSTRUCTION_UNIT");
    Org hidden = buildOrg(20L, 1L, "Hidden Org");
    hidden.setOrgType("CONSTRUCTION_UNIT");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_UNIT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(orgMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(pageOf(visible, hidden));
    when(projectMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    when(contractMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    when(vehicleMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    ApiResult<PageResult<UnitListItemDto>> result = controller.list(null, null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).extracting(UnitListItemDto::getId).containsExactly("10");
  }

  @Test
  void projectsListShouldHideOutOfScopeProjects() {
    ProjectsController controller =
        new ProjectsController(
            projectMapper,
            orgMapper,
            contractMapper,
            contractTicketMapper,
            siteMapper,
            projectConfigMapper,
            alertFenceMapper,
            projectPaymentService,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(2L, 1L);
    Project visible = buildProject(100L, 10L, "Visible Project");
    Project hidden = buildProject(200L, 20L, "Hidden Project");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(projectMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(pageOf(visible, hidden));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Visible Org"), buildOrg(20L, 1L, "Hidden Org")));
    when(projectPaymentService.getSummary(1L, 100L))
        .thenReturn(
            new ProjectPaymentSummaryVo(
                100L,
                "Visible Project",
                "P-100",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "NONE"));
    when(contractMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
    when(siteMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

    ApiResult<PageResult<ProjectListItemDto>> result = controller.list(null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).extracting(ProjectListItemDto::getId).containsExactly("100");
  }

  @Test
  void projectsListShouldPreserveDatabaseTotalAfterScopeFiltering() {
    ProjectsController controller =
        new ProjectsController(
            projectMapper,
            orgMapper,
            contractMapper,
            contractTicketMapper,
            siteMapper,
            projectConfigMapper,
            alertFenceMapper,
            projectPaymentService,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(21L, 1L);
    Project visible = buildProject(101L, 10L, "Visible Project");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(projectMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(pageWithTotal(5L, visible));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Visible Org")));
    when(projectPaymentService.getSummary(1L, 101L))
        .thenReturn(
            new ProjectPaymentSummaryVo(
                101L,
                "Visible Project",
                "P-101",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "NONE"));
    when(contractMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
    when(siteMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

    ApiResult<PageResult<ProjectListItemDto>> result = controller.list(null, null, 1, 1, request);

    assertThat(result.getData().getTotal()).isEqualTo(5L);
  }

  @Test
  void updateProjectConfigShouldUpsertScopedProjectAndReturnPersistedFields() {
    ProjectsController controller =
        new ProjectsController(
            projectMapper,
            orgMapper,
            contractMapper,
            contractTicketMapper,
            siteMapper,
            projectConfigMapper,
            alertFenceMapper,
            projectPaymentService,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(22L, 1L);
    Project visible = buildProject(101L, 10L, "Visible Project");
    AlertFence fence = new AlertFence();
    fence.setId(901L);
    fence.setTenantId(1L);
    fence.setFenceCode("PROJECT-FENCE-001");
    fence.setFenceName("项目-001 出土围栏");
    fence.setGeoJson("{\"type\":\"Polygon\",\"coordinates\":[[[120.1,30.1],[120.2,30.1],[120.2,30.2],[120.1,30.1]]]}");
    ProjectConfigUpdateRequestDto body = new ProjectConfigUpdateRequestDto();
    body.setCheckinEnabled(true);
    body.setCheckinAccount("  proj-punch-009  ");
    body.setCheckinAuthScope("  建设单位/司机  ");
    body.setLocationCheckRequired(true);
    body.setLocationRadiusMeters(new BigDecimal("260"));
    body.setPreloadVolume(new BigDecimal("1880"));
    body.setRouteGeoJson("{\"type\":\"LineString\",\"coordinates\":[[120.1,30.1],[120.2,30.2]]}");
    body.setViolationRuleEnabled(true);
    body.setViolationFenceCode("  PROJECT-FENCE-001  ");
    body.setRemark("  项目配置已更新  ");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(projectMapper.selectById(101L)).thenReturn(visible);
    AtomicReference<ProjectConfig> stored = new AtomicReference<>();
    when(projectConfigMapper.selectOne(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> stored.get());
    org.mockito.Mockito.doAnswer(
            invocation -> {
              ProjectConfig entity = invocation.getArgument(0);
              entity.setId(601L);
              stored.set(entity);
              return 1;
            })
        .when(projectConfigMapper)
        .insert(org.mockito.ArgumentMatchers.any(ProjectConfig.class));
    when(alertFenceMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(fence);

    ApiResult<?> result = controller.updateConfig(101L, body, request);

    ArgumentCaptor<ProjectConfig> captor = ArgumentCaptor.forClass(ProjectConfig.class);
    org.mockito.Mockito.verify(projectConfigMapper).insert(captor.capture());
    ProjectConfig saved = captor.getValue();

    assertThat(saved.getTenantId()).isEqualTo(1L);
    assertThat(saved.getProjectId()).isEqualTo(101L);
    assertThat(saved.getCheckinEnabled()).isEqualTo(1);
    assertThat(saved.getCheckinAccount()).isEqualTo("proj-punch-009");
    assertThat(saved.getCheckinAuthScope()).isEqualTo("建设单位/司机");
    assertThat(saved.getLocationCheckRequired()).isEqualTo(1);
    assertThat(saved.getLocationRadiusMeters()).isEqualByComparingTo("260");
    assertThat(saved.getPreloadVolume()).isEqualByComparingTo("1880");
    assertThat(saved.getRouteGeoJson()).isEqualTo(body.getRouteGeoJson());
    assertThat(saved.getViolationFenceCode()).isEqualTo("PROJECT-FENCE-001");
    assertThat(saved.getViolationRuleEnabled()).isEqualTo(1);
    assertThat(saved.getRemark()).isEqualTo("项目配置已更新");
    assertThat(OBJECT_MAPPER.valueToTree(result.getData()).path("violationFenceName").asText())
        .isEqualTo("项目-001 出土围栏");
  }

  @Test
  void sitesListShouldHideOutOfScopeSites() {
    SitesController controller =
        new SitesController(
            siteService,
            siteMapper,
            contractMapper,
            contractTicketMapper,
            projectMapper,
            siteDeviceMapper,
            siteOperationConfigMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(3L, 1L);
    Site visible = buildSite(1000L, 10L, "Visible Site");
    Site hidden = buildSite(2000L, 20L, "Hidden Site");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_SITE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(siteService.list()).thenReturn(List.of(visible, hidden));

    ApiResult<List<SiteListItemDto>> result = controller.list(request);

    assertThat(result.getData()).extracting(SiteListItemDto::getId).containsExactly("1000");
  }

  @Test
  void sitesListShouldAllowProjectScopedSitesWhenOrgIsOutOfScope() {
    SitesController controller =
        new SitesController(
            siteService,
            siteMapper,
            contractMapper,
            contractTicketMapper,
            projectMapper,
            siteDeviceMapper,
            siteOperationConfigMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(32L, 1L);
    Site visible = buildSite(1200L, 99L, "Project Scoped Site");
    visible.setProjectId(188L);
    Site hidden = buildSite(2200L, 88L, "Hidden Site");
    hidden.setProjectId(288L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_SITE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(), List.of(188L)));
    when(siteService.list()).thenReturn(List.of(visible, hidden));

    ApiResult<List<SiteListItemDto>> result = controller.list(request);

    assertThat(result.getData()).extracting(SiteListItemDto::getId).containsExactly("1200");
  }

  @Test
  void sitesListShouldExposeProjectAndOrgIdentifiers() {
    SitesController controller =
        new SitesController(
            siteService,
            siteMapper,
            contractMapper,
            contractTicketMapper,
            projectMapper,
            siteDeviceMapper,
            siteOperationConfigMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(31L, 1L);
    Site visible = buildSite(1100L, 10L, "Project Linked Site");
    visible.setProjectId(88L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_SITE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(siteService.list()).thenReturn(List.of(visible));

    ApiResult<List<SiteListItemDto>> result = controller.list(request);

    JsonNode row = OBJECT_MAPPER.valueToTree(result.getData().get(0));
    assertThat(row.path("projectId").asLong()).isEqualTo(88L);
    assertThat(row.path("orgId").asLong()).isEqualTo(10L);
  }

  @Test
  void siteCreateShouldRejectOutOfScopeProjectReference() {
    SitesController controller =
        new SitesController(
            siteService,
            siteMapper,
            contractMapper,
            contractTicketMapper,
            projectMapper,
            siteDeviceMapper,
            siteOperationConfigMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(33L, 1L);
    SiteCreateDto body = new SiteCreateDto();
    body.setName("Scoped Site");
    body.setOrgId(10L);
    body.setProjectId(188L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_SITE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));

    assertThatThrownBy(() -> controller.create(body, request))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("项目不存在");
  }

  @Test
  void vehiclesListShouldHideOutOfScopeVehicles() {
    VehiclesController controller =
        new VehiclesController(
            vehicleMapper,
            vehicleTrackPointMapper,
            orgMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(4L, 1L);
    Vehicle visible = buildVehicle(3000L, 1L, 10L, "A-100");
    Vehicle hidden = buildVehicle(4000L, 1L, 20L, "B-200");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_VEHICLE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(vehicleMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(pageOf(visible, hidden));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Visible Org"), buildOrg(20L, 1L, "Hidden Org")));

    ApiResult<PageResult<VehicleListItemDto>> result =
        controller.list(null, null, null, null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).extracting(VehicleListItemDto::getId).containsExactly("3000");
  }

  @Test
  void vehiclesListShouldPreserveDatabaseTotalAfterScopeFiltering() {
    VehiclesController controller =
        new VehiclesController(
            vehicleMapper,
            vehicleTrackPointMapper,
            orgMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(41L, 1L);
    Vehicle visible = buildVehicle(3100L, 1L, 10L, "A-101");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_VEHICLE))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(vehicleMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(pageWithTotal(7L, visible));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Visible Org")));

    ApiResult<PageResult<VehicleListItemDto>> result =
        controller.list(null, null, null, null, null, 1, 1, request);

    assertThat(result.getData().getTotal()).isEqualTo(7L);
  }

  @Test
  void disposalPermitsListShouldHideOutOfScopePermits() {
    DisposalPermitsController controller =
        new DisposalPermitsController(
            disposalPermitMapper,
            contractMapper,
            projectMapper,
            siteMapper,
            vehicleMapper,
            userContext,
            accessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(5L, 1L);
    DisposalPermit visible = buildPermit(5000L, 1L, 100L, 1000L, "A-100");
    DisposalPermit hidden = buildPermit(6000L, 1L, 200L, 2000L, "B-200");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_DISPOSAL_PERMIT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of(100L)));
    when(disposalPermitMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(visible, hidden));
    when(projectMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildProject(100L, 10L, "Visible Project"), buildProject(200L, 20L, "Hidden Project")));
    when(siteMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildSite(1000L, 10L, "Visible Site"), buildSite(2000L, 20L, "Hidden Site")));

    ApiResult<List<DisposalPermit>> result =
        controller.list(null, null, null, null, null, null, null, null, null, request);

    assertThat(result.getData()).extracting(DisposalPermit::getId).containsExactly(5000L);
  }

  @Test
  void disposalsListShouldNotSynthesizeVehicleAssociationWithoutTicketVehicleLink() {
    DisposalsController controller =
        new DisposalsController(
            contractTicketMapper,
            contractMapper,
            projectMapper,
            orgMapper,
            siteService,
            userContext);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(41L, 1L);
    Contract contract = buildContract(7000L, 1L, 100L, 1000L, 10L);
    ContractTicket ticket = buildTicket(7100L, 1L, 7000L, "TK-7100");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(contract));
    when(contractTicketMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(ticket));
    when(projectMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildProject(100L, 10L, "Visible Project")));
    when(siteService.list()).thenReturn(List.of(buildSite(1000L, 10L, "Visible Site")));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Transport Org")));

    ApiResult<PageResult<com.xngl.web.dto.query.DisposalRecordDto>> result =
        controller.list(null, null, null, null, null, null, 1, 20, request);

    com.xngl.web.dto.query.DisposalRecordDto row = result.getData().getRecords().get(0);
    assertThat(row.getPlateNo()).isNull();
    assertThat(row.getDriverName()).isNull();
  }

  @Test
  void checkinsListShouldNotSynthesizeVehicleAssociationWithoutTicketVehicleLink() {
    CheckinsController controller =
        new CheckinsController(
            contractTicketMapper,
            contractMapper,
            projectMapper,
            orgMapper,
            siteService,
            userContext);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(42L, 1L);
    Contract contract = buildContract(8000L, 1L, 100L, 1000L, 10L);
    ContractTicket ticket = buildTicket(8100L, 1L, 8000L, "TK-8100");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(contract));
    when(contractTicketMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(ticket));
    when(projectMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildProject(100L, 10L, "Visible Project")));
    when(siteService.list()).thenReturn(List.of(buildSite(1000L, 10L, "Visible Site")));
    when(orgMapper.selectBatchIds(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(buildOrg(10L, 1L, "Transport Org")));

    ApiResult<PageResult<com.xngl.web.dto.query.CheckinListItemDto>> result =
        controller.list(null, null, null, null, null, null, 1, 20, request);

    com.xngl.web.dto.query.CheckinListItemDto row = result.getData().getRecords().get(0);
    assertThat(row.getPlateNo()).isNull();
    assertThat(row.getDriverName()).isNull();
  }

  private User buildUser(Long id, Long tenantId) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    user.setMainOrgId(10L);
    return user;
  }

  private Org buildOrg(Long id, Long tenantId, String name) {
    Org org = new Org();
    org.setId(id);
    org.setTenantId(tenantId);
    org.setOrgName(name);
    return org;
  }

  private Project buildProject(Long id, Long orgId, String name) {
    Project project = new Project();
    project.setId(id);
    project.setOrgId(orgId);
    project.setName(name);
    project.setCode("P-" + id);
    return project;
  }

  private Site buildSite(Long id, Long orgId, String name) {
    Site site = new Site();
    site.setId(id);
    site.setOrgId(orgId);
    site.setName(name);
    site.setCode("S-" + id);
    return site;
  }

  private Vehicle buildVehicle(Long id, Long tenantId, Long orgId, String plateNo) {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setTenantId(tenantId);
    vehicle.setOrgId(orgId);
    vehicle.setPlateNo(plateNo);
    return vehicle;
  }

  private Contract buildContract(
      Long id, Long tenantId, Long projectId, Long siteId, Long transportOrgId) {
    Contract contract = new Contract();
    contract.setId(id);
    contract.setTenantId(tenantId);
    contract.setProjectId(projectId);
    contract.setSiteId(siteId);
    contract.setTransportOrgId(transportOrgId);
    contract.setContractNo("CT-" + id);
    contract.setName("Contract " + id);
    return contract;
  }

  private ContractTicket buildTicket(Long id, Long tenantId, Long contractId, String ticketNo) {
    ContractTicket ticket = new ContractTicket();
    ticket.setId(id);
    ticket.setTenantId(tenantId);
    ticket.setContractId(contractId);
    ticket.setTicketNo(ticketNo);
    ticket.setStatus("NORMAL");
    ticket.setVolume(BigDecimal.TEN);
    return ticket;
  }

  private DisposalPermit buildPermit(
      Long id, Long tenantId, Long projectId, Long siteId, String vehicleNo) {
    DisposalPermit permit = new DisposalPermit();
    permit.setId(id);
    permit.setTenantId(tenantId);
    permit.setProjectId(projectId);
    permit.setSiteId(siteId);
    permit.setVehicleNo(vehicleNo);
    return permit;
  }

  @SafeVarargs
  private final <T> IPage<T> pageOf(T... records) {
    Page<T> page = new Page<>(1, records.length == 0 ? 1 : records.length);
    page.setRecords(List.of(records));
    page.setTotal(records.length);
    return page;
  }

  @SafeVarargs
  private final <T> IPage<T> pageWithTotal(long total, T... records) {
    Page<T> page = new Page<>(1, records.length == 0 ? 1 : records.length);
    page.setRecords(List.of(records));
    page.setTotal(total);
    return page;
  }
}
