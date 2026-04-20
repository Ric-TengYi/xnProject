package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleViolationRecord;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleViolationRecordMapper;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.site.SiteService;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.report.ProjectDailyReportItemDto;
import com.xngl.web.dto.report.ProjectReportSummaryDto;
import com.xngl.web.dto.report.ProjectViolationAnalysisDto;
import com.xngl.web.support.MasterDataAccessScope;
import com.xngl.web.support.MasterDataAccessScopeResolver;
import com.xngl.web.support.UserContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class OperationsReportControllerScopeTest {

  @Mock private ProjectMapper projectMapper;
  @Mock private ContractMapper contractMapper;
  @Mock private ContractTicketMapper contractTicketMapper;
  @Mock private OrgMapper orgMapper;
  @Mock private VehicleMapper vehicleMapper;
  @Mock private VehicleViolationRecordMapper vehicleViolationRecordMapper;
  @Mock private SiteService siteService;
  @Mock private ExportTaskService exportTaskService;
  @Mock private UserContext userContext;
  @Mock private MasterDataAccessScopeResolver accessScopeResolver;

  @Test
  void projectDailyShouldExcludeOutOfScopeProjects() {
    OperationsReportController controller = buildController();
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(1L, 1L);
    LocalDate date = LocalDate.of(2026, 3, 30);
    Contract visibleContract = buildContract(1001L, 1L, 100L, new BigDecimal("100"));
    Contract hiddenContract = buildContract(1002L, 1L, 200L, new BigDecimal("200"));
    ContractTicket visibleTicket =
        buildTicket(2001L, 1001L, date, new BigDecimal("10"), new BigDecimal("1000"));
    ContractTicket hiddenTicket =
        buildTicket(2002L, 1002L, date, new BigDecimal("20"), new BigDecimal("2000"));
    Project visibleProject = buildProject(100L, 10L, "Visible Project");
    Project hiddenProject = buildProject(200L, 20L, "Hidden Project");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(contractMapper.selectList(any())).thenReturn(List.of(visibleContract, hiddenContract));
    when(contractTicketMapper.selectList(any())).thenReturn(List.of(visibleTicket, hiddenTicket));
    when(projectMapper.selectBatchIds(any())).thenReturn(List.of(visibleProject, hiddenProject));
    when(orgMapper.selectBatchIds(any())).thenReturn(List.of(buildOrg(10L, "Visible Org"), buildOrg(20L, "Hidden Org")));

    ApiResult<PageResult<ProjectDailyReportItemDto>> result =
        controller.projectDaily(null, date, 1, 20, request);

    assertThat(result.getData().getTotal()).isEqualTo(1L);
    assertThat(result.getData().getRecords())
        .extracting(ProjectDailyReportItemDto::getProjectId)
        .containsExactly("100");
    assertThat(result.getData().getRecords().get(0).getTodayVolume())
        .isEqualByComparingTo("10");
  }

  @Test
  void projectSummaryShouldExcludeOutOfScopeProjects() {
    OperationsReportController controller = buildController();
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(2L, 1L);
    LocalDate date = LocalDate.of(2026, 3, 30);
    Contract visibleContract = buildContract(1101L, 1L, 100L, new BigDecimal("100"));
    Contract hiddenContract = buildContract(1102L, 1L, 200L, new BigDecimal("200"));
    ContractTicket visibleTicket =
        buildTicket(2101L, 1101L, date, new BigDecimal("12"), new BigDecimal("1200"));
    ContractTicket hiddenTicket =
        buildTicket(2102L, 1102L, date, new BigDecimal("18"), new BigDecimal("1800"));
    Project visibleProject = buildProject(100L, 10L, "Visible Project");
    Project hiddenProject = buildProject(200L, 20L, "Hidden Project");

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(10L), List.of()));
    when(contractMapper.selectList(any())).thenReturn(List.of(visibleContract, hiddenContract));
    when(contractTicketMapper.selectList(any())).thenReturn(List.of(visibleTicket, hiddenTicket));
    when(projectMapper.selectBatchIds(any())).thenReturn(List.of(visibleProject, hiddenProject));
    when(orgMapper.selectBatchIds(any())).thenReturn(List.of(buildOrg(10L, "Visible Org"), buildOrg(20L, "Hidden Org")));

    ApiResult<ProjectReportSummaryDto> result =
        controller.projectSummary("DAY", date, null, null, request);

    assertThat(result.getData().getProjectCount()).isEqualTo(1);
    assertThat(result.getData().getPeriodVolume()).isEqualByComparingTo("12");
    assertThat(result.getData().getPeriodAmount()).isEqualByComparingTo("1200");
    assertThat(result.getData().getProjectTotal()).isEqualByComparingTo("100");
  }

  @Test
  void projectViolationsShouldUseProjectScopeDerivedOrganizations() {
    OperationsReportController controller = buildController();
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(3L, 1L);
    LocalDate date = LocalDate.of(2026, 3, 30);
    Project visibleProject = buildProject(100L, 10L, "Visible Project");
    Vehicle visibleVehicle = buildVehicle(3001L, "Fleet A");
    Vehicle hiddenVehicle = buildVehicle(3002L, "Fleet B");
    VehicleViolationRecord visibleRecord =
        buildViolationRecord(4001L, 1L, 3001L, 10L, "浙A10001", date.atStartOfDay().plusHours(2));
    VehicleViolationRecord hiddenRecord =
        buildViolationRecord(4002L, 1L, 3002L, 20L, "浙A20002", date.atStartOfDay().plusHours(3));

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(accessScopeResolver.resolve(currentUser, MasterDataAccessScopeResolver.BIZ_MODULE_PROJECT))
        .thenReturn(MasterDataAccessScope.scoped(List.of(), List.of(100L)));
    when(projectMapper.selectBatchIds(any())).thenReturn(List.of(visibleProject));
    when(vehicleViolationRecordMapper.selectList(any())).thenReturn(List.of(visibleRecord, hiddenRecord));
    when(vehicleMapper.selectBatchIds(any())).thenReturn(List.of(visibleVehicle, hiddenVehicle));
    when(orgMapper.selectBatchIds(any())).thenReturn(List.of(buildOrg(10L, "Visible Team"), buildOrg(20L, "Hidden Team")));

    ApiResult<ProjectViolationAnalysisDto> result =
        controller.projectViolations("DAY", date, null, null, request);

    assertThat(result.getData().getSummary().getTotalViolations()).isEqualTo(1);
    assertThat(result.getData().getByFleet()).extracting(item -> item.getName()).containsExactly("Fleet A");
    assertThat(result.getData().getByTeam()).extracting(item -> item.getName()).containsExactly("Visible Team");
  }

  private OperationsReportController buildController() {
    return new OperationsReportController(
        projectMapper,
        contractMapper,
        contractTicketMapper,
        orgMapper,
        vehicleMapper,
        vehicleViolationRecordMapper,
        siteService,
        exportTaskService,
        userContext,
        accessScopeResolver,
        new ObjectMapper());
  }

  private User buildUser(Long id, Long tenantId) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    return user;
  }

  private Contract buildContract(Long id, Long tenantId, Long projectId, BigDecimal agreedVolume) {
    Contract contract = new Contract();
    contract.setId(id);
    contract.setTenantId(tenantId);
    contract.setProjectId(projectId);
    contract.setAgreedVolume(agreedVolume);
    return contract;
  }

  private ContractTicket buildTicket(
      Long id, Long contractId, LocalDate ticketDate, BigDecimal volume, BigDecimal amount) {
    ContractTicket ticket = new ContractTicket();
    ticket.setId(id);
    ticket.setContractId(contractId);
    ticket.setTicketDate(ticketDate);
    ticket.setVolume(volume);
    ticket.setAmount(amount);
    return ticket;
  }

  private Project buildProject(Long id, Long orgId, String name) {
    Project project = new Project();
    project.setId(id);
    project.setOrgId(orgId);
    project.setName(name);
    project.setCode("P-" + id);
    project.setStatus(1);
    return project;
  }

  private Org buildOrg(Long id, String name) {
    Org org = new Org();
    org.setId(id);
    org.setOrgName(name);
    return org;
  }

  private Vehicle buildVehicle(Long id, String fleetName) {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setFleetName(fleetName);
    return vehicle;
  }

  private VehicleViolationRecord buildViolationRecord(
      Long id, Long tenantId, Long vehicleId, Long orgId, String plateNo, LocalDateTime triggerTime) {
    VehicleViolationRecord record = new VehicleViolationRecord();
    record.setId(id);
    record.setTenantId(tenantId);
    record.setVehicleId(vehicleId);
    record.setOrgId(orgId);
    record.setPlateNo(plateNo);
    record.setTriggerTime(triggerTime);
    record.setActionStatus("PENDING");
    return record;
  }
}
