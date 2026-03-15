package com.xngl.manager.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractReceipt;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractReceiptMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

  @Mock private ContractMapper contractMapper;
  @Mock private ContractReceiptMapper contractReceiptMapper;

  @InjectMocks private ContractServiceImpl contractService;

  @Test
  void createReceiptShouldPersistReceiptAndUpdateContractAmount() {
    Contract contract = new Contract();
    contract.setId(1L);
    contract.setTenantId(1L);
    contract.setAmount(new BigDecimal("1000.00"));
    contract.setReceivedAmount(new BigDecimal("200.00"));
    contract.setContractStatus("EFFECTIVE");
    when(contractMapper.selectById(1L)).thenReturn(contract);
    when(contractReceiptMapper.insert(any(ContractReceipt.class)))
        .thenAnswer(
            invocation -> {
              ContractReceipt receipt = invocation.getArgument(0);
              receipt.setId(10L);
              return 1;
            });

    long receiptId =
        contractService.createReceipt(
            1L,
            99L,
            1L,
            new CreateContractReceiptCommand(
                new BigDecimal("100.00"),
                LocalDate.of(2026, 3, 15),
                "manual",
                "V001",
                "B001",
                "首次入账"));

    assertThat(receiptId).isEqualTo(10L);

    ArgumentCaptor<ContractReceipt> receiptCaptor = ArgumentCaptor.forClass(ContractReceipt.class);
    verify(contractReceiptMapper).insert(receiptCaptor.capture());
    ContractReceipt insertedReceipt = receiptCaptor.getValue();
    assertThat(insertedReceipt.getTenantId()).isEqualTo(1L);
    assertThat(insertedReceipt.getContractId()).isEqualTo(1L);
    assertThat(insertedReceipt.getAmount()).isEqualByComparingTo("100.00");
    assertThat(insertedReceipt.getReceiptType()).isEqualTo("MANUAL");
    assertThat(insertedReceipt.getStatus()).isEqualTo("NORMAL");
    assertThat(insertedReceipt.getReceiptNo()).startsWith("CR");

    ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
    verify(contractMapper).updateById(contractCaptor.capture());
    Contract updatedContract = contractCaptor.getValue();
    assertThat(updatedContract.getId()).isEqualTo(1L);
    assertThat(updatedContract.getReceivedAmount()).isEqualByComparingTo("300.00");
  }

  @Test
  void cancelReceiptShouldCreateReversalAndRollbackContractAmount() {
    Contract contract = new Contract();
    contract.setId(1L);
    contract.setTenantId(1L);
    contract.setReceivedAmount(new BigDecimal("300.00"));
    when(contractMapper.selectById(1L)).thenReturn(contract);

    ContractReceipt original = new ContractReceipt();
    original.setId(11L);
    original.setTenantId(1L);
    original.setContractId(1L);
    original.setReceiptNo("CR202603150001");
    original.setReceiptDate(LocalDate.of(2026, 3, 15));
    original.setAmount(new BigDecimal("100.00"));
    original.setReceiptType("MANUAL");
    original.setVoucherNo("V001");
    original.setBankFlowNo("B001");
    original.setStatus("NORMAL");
    original.setRemark("首次入账");
    when(contractReceiptMapper.selectById(11L)).thenReturn(original);
    when(contractReceiptMapper.insert(any(ContractReceipt.class)))
        .thenAnswer(
            invocation -> {
              ContractReceipt receipt = invocation.getArgument(0);
              receipt.setId(12L);
              return 1;
            });

    long reversalId = contractService.cancelReceipt(11L, 99L, 1L, "重复录入");

    assertThat(reversalId).isEqualTo(12L);

    ArgumentCaptor<ContractReceipt> cancelledCaptor = ArgumentCaptor.forClass(ContractReceipt.class);
    verify(contractReceiptMapper).updateById(cancelledCaptor.capture());
    ContractReceipt cancelled = cancelledCaptor.getValue();
    assertThat(cancelled.getId()).isEqualTo(11L);
    assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    assertThat(cancelled.getRemark()).contains("重复录入");

    ArgumentCaptor<ContractReceipt> reversalCaptor = ArgumentCaptor.forClass(ContractReceipt.class);
    verify(contractReceiptMapper).insert(reversalCaptor.capture());
    ContractReceipt reversal = reversalCaptor.getValue();
    assertThat(reversal.getAmount()).isEqualByComparingTo("-100.00");
    assertThat(reversal.getReceiptType()).isEqualTo("REVERSAL");
    assertThat(reversal.getRemark()).contains("CR202603150001");

    ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
    verify(contractMapper).updateById(contractCaptor.capture());
    Contract updatedContract = contractCaptor.getValue();
    assertThat(updatedContract.getReceivedAmount()).isEqualByComparingTo("200.00");
  }

  @Test
  void createReceiptShouldRejectAmountGreaterThanContractAmount() {
    Contract contract = new Contract();
    contract.setId(1L);
    contract.setTenantId(1L);
    contract.setAmount(new BigDecimal("1000.00"));
    contract.setReceivedAmount(new BigDecimal("950.00"));
    contract.setContractStatus("EFFECTIVE");
    when(contractMapper.selectById(1L)).thenReturn(contract);

    assertThatThrownBy(
            () ->
                contractService.createReceipt(
                    1L,
                    99L,
                    1L,
                    new CreateContractReceiptCommand(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 3, 15),
                        "MANUAL",
                        null,
                        null,
                        null)))
        .isInstanceOf(ContractServiceException.class)
        .hasMessage("累计入账金额不能超过合同总金额");
  }
}
