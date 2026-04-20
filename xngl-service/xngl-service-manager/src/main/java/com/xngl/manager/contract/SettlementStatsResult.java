package com.xngl.manager.contract;

import java.math.BigDecimal;

public class SettlementStatsResult {

  private BigDecimal pendingAmount;
  private BigDecimal settledAmount;
  private long totalOrders;
  private long draftOrders;
  private long pendingOrders;
  private long settledOrders;

  public SettlementStatsResult() {}

  public SettlementStatsResult(
      BigDecimal pendingAmount,
      BigDecimal settledAmount,
      long totalOrders,
      long draftOrders,
      long pendingOrders,
      long settledOrders) {
    this.pendingAmount = pendingAmount;
    this.settledAmount = settledAmount;
    this.totalOrders = totalOrders;
    this.draftOrders = draftOrders;
    this.pendingOrders = pendingOrders;
    this.settledOrders = settledOrders;
  }

  public BigDecimal getPendingAmount() { return pendingAmount; }
  public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
  public BigDecimal getSettledAmount() { return settledAmount; }
  public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
  public long getTotalOrders() { return totalOrders; }
  public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
  public long getDraftOrders() { return draftOrders; }
  public void setDraftOrders(long draftOrders) { this.draftOrders = draftOrders; }
  public long getPendingOrders() { return pendingOrders; }
  public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
  public long getSettledOrders() { return settledOrders; }
  public void setSettledOrders(long settledOrders) { this.settledOrders = settledOrders; }
}
