package com.xngl.manager.contract;

import java.math.BigDecimal;

public class ContractStatsResult {

  private long totalContracts;
  private long effectiveContracts;
  private BigDecimal monthlyReceiptAmount;
  private long monthlyReceiptCount;
  private BigDecimal pendingReceiptAmount;
  private long totalSettlementOrders;
  private long pendingSettlementOrders;

  public ContractStatsResult() {}

  public long getTotalContracts() { return totalContracts; }
  public void setTotalContracts(long totalContracts) { this.totalContracts = totalContracts; }
  public long getEffectiveContracts() { return effectiveContracts; }
  public void setEffectiveContracts(long effectiveContracts) { this.effectiveContracts = effectiveContracts; }
  public BigDecimal getMonthlyReceiptAmount() { return monthlyReceiptAmount; }
  public void setMonthlyReceiptAmount(BigDecimal monthlyReceiptAmount) { this.monthlyReceiptAmount = monthlyReceiptAmount; }
  public long getMonthlyReceiptCount() { return monthlyReceiptCount; }
  public void setMonthlyReceiptCount(long monthlyReceiptCount) { this.monthlyReceiptCount = monthlyReceiptCount; }
  public BigDecimal getPendingReceiptAmount() { return pendingReceiptAmount; }
  public void setPendingReceiptAmount(BigDecimal pendingReceiptAmount) { this.pendingReceiptAmount = pendingReceiptAmount; }
  public long getTotalSettlementOrders() { return totalSettlementOrders; }
  public void setTotalSettlementOrders(long totalSettlementOrders) { this.totalSettlementOrders = totalSettlementOrders; }
  public long getPendingSettlementOrders() { return pendingSettlementOrders; }
  public void setPendingSettlementOrders(long pendingSettlementOrders) { this.pendingSettlementOrders = pendingSettlementOrders; }
}
