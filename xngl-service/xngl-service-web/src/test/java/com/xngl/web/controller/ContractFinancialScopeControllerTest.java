package com.xngl.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractAccessScope;
import com.xngl.manager.contract.ContractExportFileService;
import com.xngl.manager.contract.ContractQueryParams;
import com.xngl.manager.contract.ContractReportService;
import com.xngl.manager.contract.ContractService;
import com.xngl.manager.contract.ContractStatsResult;
import com.xngl.manager.contract.ExportTaskService;
import com.xngl.manager.contract.SettlementService;
import com.xngl.manager.contract.SettlementStatsResult;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.contract.ContractItemDto;
import com.xngl.web.dto.contract.ContractReceiptItemDto;
import com.xngl.web.dto.contract.ContractStatsDto;
import com.xngl.web.dto.contract.MonthlySummaryDto;
import com.xngl.web.dto.contract.SettlementItemDto;
import com.xngl.web.dto.contract.SettlementStatsDto;
import com.xngl.web.support.ContractAccessScopeResolver;
import com.xngl.web.support.UserContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ContractFinancialScopeControllerTest {

  @Mock private ContractService contractService;
  @Mock private SettlementService settlementService;
  @Mock private ContractReportService contractReportService;
  @Mock private ExportTaskService exportTaskService;
  @Mock private ContractExportFileService contractExportFileService;
  @Mock private UserContext userContext;
  @Mock private ContractAccessScopeResolver contractAccessScopeResolver;

  @Test
  void contractsListShouldPassResolvedScopeToService() {
    ContractsController controller =
        new ContractsController(contractService, userContext, contractAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(1L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));
    Contract contract = new Contract();
    contract.setId(101L);
    IPage<Contract> page = pageOf(contract);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(contractService.pageContracts(
            1L, null, null, null, null, null, null, null, 1, 20, scope))
        .thenReturn(page);

    ApiResult<PageResult<ContractItemDto>> result =
        controller.list(null, null, null, null, null, null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).hasSize(1);
    verify(contractService)
        .pageContracts(1L, null, null, null, null, null, null, null, 1, 20, scope);
  }

  @Test
  void contractsStatsShouldPassResolvedScopeToService() {
    ContractsController controller =
        new ContractsController(contractService, userContext, contractAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(2L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(contractService.getContractStats(1L, scope))
        .thenReturn(
            contractStatsResult(
                3L, 2L, new BigDecimal("120.00"), 2L, new BigDecimal("80.00"), 4L, 1L));

    ApiResult<ContractStatsDto> result = controller.stats(request);

    assertThat(result.getData().getTotalContracts()).isEqualTo(3L);
    verify(contractService).getContractStats(1L, scope);
  }

  @Test
  void receiptsListShouldPassResolvedScopeToService() {
    ContractReceiptController controller =
        new ContractReceiptController(contractService, null, userContext, contractAccessScopeResolver);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(3L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));
    ContractReceipt receipt = new ContractReceipt();
    receipt.setId(201L);
    receipt.setContractId(301L);
    IPage<ContractReceipt> page = pageOf(receipt);
    Contract contract = new Contract();
    contract.setId(301L);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(contractService.pageReceipts(1L, null, null, null, null, null, 1, 20, scope))
        .thenReturn(page);
    doReturn(List.of(contract))
        .when(contractService)
        .listContractsByIds(org.mockito.ArgumentMatchers.anyCollection(), eq(1L));

    ApiResult<PageResult<ContractReceiptItemDto>> result =
        controller.list(null, null, null, null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).hasSize(1);
    verify(contractService).pageReceipts(1L, null, null, null, null, null, 1, 20, scope);
  }

  @Test
  void settlementListShouldPassResolvedScopeToService() {
    SettlementController controller =
        new SettlementController(
            settlementService,
            contractService,
            userContext,
            contractAccessScopeResolver,
            null,
            null,
            null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(4L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));
    SettlementOrder order = new SettlementOrder();
    order.setId(401L);
    IPage<SettlementOrder> page = pageOf(order);

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(settlementService.pageSettlements(1L, null, null, null, null, 1, 20, scope))
        .thenReturn(page);

    ApiResult<PageResult<SettlementItemDto>> result =
        controller.list(null, null, null, null, 1, 20, request);

    assertThat(result.getData().getRecords()).hasSize(1);
    verify(settlementService).pageSettlements(1L, null, null, null, null, 1, 20, scope);
  }

  @Test
  void settlementStatsShouldPassResolvedScopeToService() {
    SettlementController controller =
        new SettlementController(
            settlementService,
            contractService,
            userContext,
            contractAccessScopeResolver,
            null,
            null,
            null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(5L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(settlementService.getSettlementStats(1L, scope))
        .thenReturn(new SettlementStatsResult(new BigDecimal("30.00"), new BigDecimal("70.00"), 5L, 2L, 1L, 2L));

    ApiResult<SettlementStatsDto> result = controller.stats(request);

    assertThat(result.getData().getTotalOrders()).isEqualTo(5L);
    verify(settlementService).getSettlementStats(1L, scope);
  }

  @Test
  void monthlySummaryShouldPassResolvedScopeToService() {
    ContractReportController controller =
        new ContractReportController(
            contractReportService,
            exportTaskService,
            null,
            userContext,
            contractAccessScopeResolver,
            new ObjectMapper());
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(6L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(contractReportService.getMonthlySummary(1L, "2026-03", scope))
        .thenReturn(Map.of("month", "2026-03", "contractCount", 1));

    ApiResult<MonthlySummaryDto> result = controller.monthlySummary("2026-03", request);

    assertThat(result.getData().getMonth()).isEqualTo("2026-03");
    verify(contractReportService).getMonthlySummary(1L, "2026-03", scope);
  }

  @Test
  void contractExportShouldPassResolvedScopeToExporter() {
    ContractExportController controller =
        new ContractExportController(
            exportTaskService,
            contractExportFileService,
            null,
            userContext,
            contractAccessScopeResolver,
            new ObjectMapper());
    MockHttpServletRequest request = new MockHttpServletRequest();
    User currentUser = buildUser(7L, 1L);
    ContractAccessScope scope = ContractAccessScope.scoped(List.of(10L), List.of(20L), List.of(30L));

    when(userContext.requireCurrentUser(request)).thenReturn(currentUser);
    when(contractAccessScopeResolver.resolve(currentUser)).thenReturn(scope);
    when(exportTaskService.createExportTask(
            eq(1L), eq(7L), eq("CONTRACT"), eq("CSV"), anyString()))
        .thenReturn(900L);

    ApiResult<Map<String, String>> result =
        controller.export(new com.xngl.web.dto.contract.ContractExportRequestDto(), request);

    assertThat(result.getData()).containsEntry("taskId", "900");
    verify(contractExportFileService)
        .generateContractCsv(eq(900L), eq(1L), org.mockito.ArgumentMatchers.any(ContractQueryParams.class), eq(scope));
  }

  private User buildUser(Long id, Long tenantId) {
    User user = new User();
    user.setId(id);
    user.setTenantId(tenantId);
    return user;
  }

  private ContractStatsResult contractStatsResult(
      long totalContracts,
      long effectiveContracts,
      BigDecimal monthlyReceiptAmount,
      long monthlyReceiptCount,
      BigDecimal pendingReceiptAmount,
      long totalSettlementOrders,
      long pendingSettlementOrders) {
    ContractStatsResult result = new ContractStatsResult();
    result.setTotalContracts(totalContracts);
    result.setEffectiveContracts(effectiveContracts);
    result.setMonthlyReceiptAmount(monthlyReceiptAmount);
    result.setMonthlyReceiptCount(monthlyReceiptCount);
    result.setPendingReceiptAmount(pendingReceiptAmount);
    result.setTotalSettlementOrders(totalSettlementOrders);
    result.setPendingSettlementOrders(pendingSettlementOrders);
    return result;
  }

  private <T> IPage<T> pageOf(T record) {
    Page<T> page = new Page<>(1, 20);
    page.setRecords(List.of(record));
    page.setTotal(1L);
    return page;
  }
}
