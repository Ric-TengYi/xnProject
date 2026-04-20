package com.xngl.manager.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.SettlementOrder;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContractAccessScopeTest {

  @Test
  void contractShouldMatchWhenAnyScopedRelationIsVisible() {
    ContractAccessScope scope =
        ContractAccessScope.scoped(Set.of(10L), Set.of(20L), Set.of(30L));

    Contract byProject = new Contract();
    byProject.setProjectId(20L);
    assertThat(scope.matchesContract(byProject)).isTrue();

    Contract bySite = new Contract();
    bySite.setSiteId(30L);
    assertThat(scope.matchesContract(bySite)).isTrue();

    Contract byConstructionOrg = new Contract();
    byConstructionOrg.setConstructionOrgId(10L);
    assertThat(scope.matchesContract(byConstructionOrg)).isTrue();

    Contract byTransportOrg = new Contract();
    byTransportOrg.setTransportOrgId(10L);
    assertThat(scope.matchesContract(byTransportOrg)).isTrue();

    Contract bySiteOperatorOrg = new Contract();
    bySiteOperatorOrg.setSiteOperatorOrgId(10L);
    assertThat(scope.matchesContract(bySiteOperatorOrg)).isTrue();

    Contract byThirdPartyOrg = new Contract();
    byThirdPartyOrg.setPartyId(10L);
    assertThat(scope.matchesContract(byThirdPartyOrg)).isTrue();
  }

  @Test
  void contractShouldNotMatchWhenOutsideScopedRelations() {
    ContractAccessScope scope =
        ContractAccessScope.scoped(Set.of(10L), Set.of(20L), Set.of(30L));

    Contract hidden = new Contract();
    hidden.setProjectId(200L);
    hidden.setSiteId(300L);
    hidden.setConstructionOrgId(100L);
    hidden.setTransportOrgId(101L);
    hidden.setSiteOperatorOrgId(102L);
    hidden.setPartyId(103L);

    assertThat(scope.matchesContract(hidden)).isFalse();
  }

  @Test
  void settlementShouldMatchWhenProjectOrSiteIsVisible() {
    ContractAccessScope scope =
        ContractAccessScope.scoped(Set.of(10L), Set.of(20L), Set.of(30L));

    SettlementOrder byProject = new SettlementOrder();
    byProject.setTargetProjectId(20L);
    assertThat(scope.matchesSettlement(byProject)).isTrue();

    SettlementOrder bySite = new SettlementOrder();
    bySite.setTargetSiteId(30L);
    assertThat(scope.matchesSettlement(bySite)).isTrue();

    SettlementOrder hidden = new SettlementOrder();
    hidden.setTargetProjectId(200L);
    hidden.setTargetSiteId(300L);
    assertThat(scope.matchesSettlement(hidden)).isFalse();
  }

  @Test
  void tenantWideScopeShouldMatchAnyContractAndSettlement() {
    ContractAccessScope scope = ContractAccessScope.tenantWide();

    assertThat(scope.matchesContract(new Contract())).isTrue();
    assertThat(scope.matchesSettlement(new SettlementOrder())).isTrue();
    assertThat(scope.hasAnyAccess()).isTrue();
  }
}
