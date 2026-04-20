package com.xngl.manager.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractStatSnapshot;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractReceiptMapper;
import com.xngl.infrastructure.persistence.mapper.ContractStatSnapshotMapper;
import com.xngl.infrastructure.persistence.mapper.SettlementOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractReportServiceImplTest {

  @Mock private ContractMapper contractMapper;
  @Mock private ContractReceiptMapper receiptMapper;
  @Mock private SettlementOrderMapper settlementOrderMapper;
  @Mock private ContractStatSnapshotMapper snapshotMapper;

  private ContractReportServiceImpl reportService;

  @BeforeEach
  void setUp() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""),
        Contract.class);
    reportService =
        new ContractReportServiceImpl(
            contractMapper, receiptMapper, settlementOrderMapper, snapshotMapper);
  }

  @Test
  void getMonthlySummaryShouldNotBuildEmptyAndClauseForTenantWideScope() {
    when(snapshotMapper.selectOne(any())).thenReturn(null);
    when(contractMapper.selectCount(any())).thenAnswer(invocation -> {
      Wrapper<?> wrapper = invocation.getArgument(0);
      assertThat(wrapper.getSqlSegment()).doesNotContain("AND  AND");
      return 0L;
    });
    when(contractMapper.selectMaps(any())).thenReturn(
        List.of(Map.of("contractAmount", BigDecimal.ZERO, "agreedVolume", BigDecimal.ZERO)));
    when(receiptMapper.selectMaps(any())).thenReturn(List.of(Map.of("total", BigDecimal.ZERO)));
    when(settlementOrderMapper.selectMaps(any())).thenReturn(List.of(Map.of("total", BigDecimal.ZERO)));
    when(snapshotMapper.insert(any(ContractStatSnapshot.class))).thenReturn(1);

    Map<String, Object> summary =
        reportService.getMonthlySummary(1L, "2026-04", ContractAccessScope.tenantWide());

    assertThat(summary.get("month")).isEqualTo("2026-04");
    assertThat(summary.get("contractCount")).isEqualTo(0);
    assertThat(summary.get("contractAmount")).isEqualTo(BigDecimal.ZERO);
  }
}
