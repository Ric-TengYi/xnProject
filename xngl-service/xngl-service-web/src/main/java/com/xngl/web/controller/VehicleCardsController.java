package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.vehicle.Vehicle;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleCard;
import com.xngl.infrastructure.persistence.entity.vehicle.VehicleCardTransaction;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleCardMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleCardTransactionMapper;
import com.xngl.infrastructure.persistence.mapper.VehicleMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.vehicle.VehicleCardBindDto;
import com.xngl.web.dto.vehicle.VehicleCardConsumeDto;
import com.xngl.web.dto.vehicle.VehicleCardListItemDto;
import com.xngl.web.dto.vehicle.VehicleCardRechargeDto;
import com.xngl.web.dto.vehicle.VehicleCardSummaryDto;
import com.xngl.web.dto.vehicle.VehicleCardTransactionListItemDto;
import com.xngl.web.dto.vehicle.VehicleCardTransactionSummaryDto;
import com.xngl.web.dto.vehicle.VehicleCardUpsertDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle-cards")
public class VehicleCardsController {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal LOW_BALANCE_THRESHOLD = BigDecimal.valueOf(500);
  private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private final VehicleCardMapper vehicleCardMapper;
  private final VehicleCardTransactionMapper transactionMapper;
  private final VehicleMapper vehicleMapper;
  private final OrgMapper orgMapper;
  private final UserContext userContext;

  public VehicleCardsController(
      VehicleCardMapper vehicleCardMapper,
      VehicleCardTransactionMapper transactionMapper,
      VehicleMapper vehicleMapper,
      OrgMapper orgMapper,
      UserContext userContext) {
    this.vehicleCardMapper = vehicleCardMapper;
    this.transactionMapper = transactionMapper;
    this.vehicleMapper = vehicleMapper;
    this.orgMapper = orgMapper;
    this.userContext = userContext;
  }

  @GetMapping
  public ApiResult<PageResult<VehicleCardListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleCardListItemDto> rows =
        new ArrayList<>(loadCards(currentUser.getTenantId(), keyword, cardType, status, orgId));
    rows.sort(
        Comparator.comparing(
                VehicleCardListItemDto::getUpdateTime, Comparator.nullsLast(String::compareTo))
            .reversed()
            .thenComparing(
                VehicleCardListItemDto::getCardNo, Comparator.nullsLast(String::compareTo)));
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/summary")
  public ApiResult<VehicleCardSummaryDto> summary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(toSummary(loadCards(currentUser.getTenantId(), keyword, cardType, status, orgId)));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long orgId,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleCardListItemDto> rows =
        loadCards(currentUser.getTenantId(), keyword, cardType, status, orgId);
    return csvResponse("vehicle_cards.csv", buildCardCsv(rows));
  }

  @GetMapping("/transactions")
  public ApiResult<PageResult<VehicleCardTransactionListItemDto>> transactions(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String txnType,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long cardId,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleCardTransactionListItemDto> rows =
        new ArrayList<>(
            loadTransactions(
                currentUser.getTenantId(),
                keyword,
                cardType,
                txnType,
                orgId,
                vehicleId,
                cardId,
                parseDate(dateFrom),
                parseDate(dateTo)));
    rows.sort(
        Comparator.comparing(
                VehicleCardTransactionListItemDto::getOccurredAt,
                Comparator.nullsLast(String::compareTo))
            .reversed());
    return ApiResult.ok(paginate(rows, pageNo, pageSize));
  }

  @GetMapping("/transactions/summary")
  public ApiResult<VehicleCardTransactionSummaryDto> transactionSummary(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String txnType,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long cardId,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(
        toTransactionSummary(
            loadTransactions(
                currentUser.getTenantId(),
                keyword,
                cardType,
                txnType,
                orgId,
                vehicleId,
                cardId,
                parseDate(dateFrom),
                parseDate(dateTo))));
  }

  @GetMapping("/transactions/export")
  public ResponseEntity<byte[]> exportTransactions(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String cardType,
      @RequestParam(required = false) String txnType,
      @RequestParam(required = false) Long orgId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) Long cardId,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<VehicleCardTransactionListItemDto> rows =
        loadTransactions(
            currentUser.getTenantId(),
            keyword,
            cardType,
            txnType,
            orgId,
            vehicleId,
            cardId,
            parseDate(dateFrom),
            parseDate(dateTo));
    return csvResponse("vehicle_card_transactions.csv", buildTransactionCsv(rows));
  }

  @PostMapping
  public ApiResult<VehicleCardListItemDto> create(
      @RequestBody VehicleCardUpsertDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    validateUpsert(body, currentUser.getTenantId(), null);
    VehicleCard entity = new VehicleCard();
    entity.setTenantId(currentUser.getTenantId());
    applyUpsert(entity, body, currentUser.getTenantId());
    vehicleCardMapper.insert(entity);
    return ApiResult.ok(loadCardDto(entity.getId(), currentUser.getTenantId()));
  }

  @PutMapping("/{id}")
  public ApiResult<VehicleCardListItemDto> update(
      @PathVariable Long id,
      @RequestBody VehicleCardUpsertDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleCard entity = requireCard(id, currentUser.getTenantId());
    validateUpsert(body, currentUser.getTenantId(), id);
    applyUpsert(entity, body, currentUser.getTenantId());
    vehicleCardMapper.updateById(entity);
    return ApiResult.ok(loadCardDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/{id}/recharge")
  public ApiResult<VehicleCardListItemDto> recharge(
      @PathVariable Long id,
      @RequestBody VehicleCardRechargeDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleCard entity = requireCard(id, currentUser.getTenantId());
    BigDecimal amount = defaultDecimal(body != null ? body.getAmount() : null);
    if (amount.compareTo(ZERO) <= 0) {
      throw new BizException(400, "充值金额必须大于0");
    }
    BigDecimal balanceBefore = defaultDecimal(entity.getBalance());
    entity.setBalance(defaultDecimal(entity.getBalance()).add(amount));
    entity.setTotalRecharge(defaultDecimal(entity.getTotalRecharge()).add(amount));
    entity.setStatus(resolveCardStatus(entity.getStatus(), entity.getBalance(), entity.getVehicleId()));
    vehicleCardMapper.updateById(entity);
    saveTransaction(
        entity,
        "RECHARGE",
        amount,
        balanceBefore,
        entity.getBalance(),
        resolveUserName(currentUser),
        trimToNull(body != null ? body.getRemark() : null));
    return ApiResult.ok(loadCardDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/{id}/consume")
  public ApiResult<VehicleCardListItemDto> consume(
      @PathVariable Long id,
      @RequestBody VehicleCardConsumeDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleCard entity = requireCard(id, currentUser.getTenantId());
    BigDecimal amount = defaultDecimal(body != null ? body.getAmount() : null);
    if (amount.compareTo(ZERO) <= 0) {
      throw new BizException(400, "消费金额必须大于0");
    }
    BigDecimal balanceBefore = defaultDecimal(entity.getBalance());
    if (balanceBefore.compareTo(amount) < 0) {
      throw new BizException(400, "当前余额不足，无法确认消费");
    }
    entity.setBalance(balanceBefore.subtract(amount));
    entity.setTotalConsume(defaultDecimal(entity.getTotalConsume()).add(amount));
    entity.setStatus(resolveCardStatus(entity.getStatus(), entity.getBalance(), entity.getVehicleId()));
    vehicleCardMapper.updateById(entity);
    saveTransaction(
        entity,
        "CONSUME",
        amount,
        balanceBefore,
        entity.getBalance(),
        resolveUserName(currentUser),
        trimToNull(body != null ? body.getRemark() : null));
    return ApiResult.ok(loadCardDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/{id}/bind")
  public ApiResult<VehicleCardListItemDto> bind(
      @PathVariable Long id,
      @RequestBody VehicleCardBindDto body,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleCard entity = requireCard(id, currentUser.getTenantId());
    if (body == null || body.getVehicleId() == null) {
      throw new BizException(400, "请选择需要绑定的车辆");
    }
    Vehicle vehicle = requireVehicle(body.getVehicleId(), currentUser.getTenantId());
    entity.setVehicleId(vehicle.getId());
    entity.setOrgId(vehicle.getOrgId());
    entity.setStatus(resolveCardStatus(entity.getStatus(), entity.getBalance(), vehicle.getId()));
    vehicleCardMapper.updateById(entity);
    return ApiResult.ok(loadCardDto(id, currentUser.getTenantId()));
  }

  @PostMapping("/{id}/unbind")
  public ApiResult<VehicleCardListItemDto> unbind(
      @PathVariable Long id, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    VehicleCard entity = requireCard(id, currentUser.getTenantId());
    entity.setVehicleId(null);
    entity.setStatus("UNBOUND");
    vehicleCardMapper.updateById(entity);
    return ApiResult.ok(loadCardDto(id, currentUser.getTenantId()));
  }

  private List<VehicleCardTransactionListItemDto> loadTransactions(
      Long tenantId,
      String keyword,
      String cardType,
      String txnType,
      Long orgId,
      Long vehicleId,
      Long cardId,
      LocalDate dateFrom,
      LocalDate dateTo) {
    List<VehicleCardTransaction> rows =
        transactionMapper.selectList(
            new LambdaQueryWrapper<VehicleCardTransaction>()
                .eq(VehicleCardTransaction::getTenantId, tenantId)
                .eq(cardId != null, VehicleCardTransaction::getCardId, cardId)
                .eq(
                    StringUtils.hasText(cardType),
                    VehicleCardTransaction::getCardType,
                    StringUtils.hasText(cardType) ? cardType.trim().toUpperCase() : null)
                .eq(
                    StringUtils.hasText(txnType),
                    VehicleCardTransaction::getTxnType,
                    StringUtils.hasText(txnType) ? txnType.trim().toUpperCase() : null)
                .eq(orgId != null, VehicleCardTransaction::getOrgId, orgId)
                .eq(vehicleId != null, VehicleCardTransaction::getVehicleId, vehicleId)
                .orderByDesc(VehicleCardTransaction::getOccurredAt)
                .orderByDesc(VehicleCardTransaction::getId));
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap =
        vehicleMapper.selectBatchIds(
                rows.stream()
                    .map(VehicleCardTransaction::getVehicleId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));
    LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
    rows.stream().map(VehicleCardTransaction::getOrgId).filter(Objects::nonNull).forEach(orgIds::add);
    vehicleMap.values().stream().map(Vehicle::getOrgId).filter(Objects::nonNull).forEach(orgIds::add);
    Map<Long, Org> orgMap =
        orgIds.isEmpty()
            ? Collections.emptyMap()
            : orgMapper.selectBatchIds(orgIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
    String keywordValue = trimToNull(keyword);
    return rows.stream()
        .map(item -> toTransactionDto(item, vehicleMap.get(item.getVehicleId()), orgMap))
        .filter(item -> matchTransactionKeyword(item, keywordValue))
        .filter(item -> matchOccurredDate(item.getOccurredAt(), dateFrom, dateTo))
        .toList();
  }

  private List<VehicleCardListItemDto> loadCards(
      Long tenantId, String keyword, String cardType, String status, Long orgId) {
    List<VehicleCard> cards =
        vehicleCardMapper.selectList(
            new LambdaQueryWrapper<VehicleCard>()
                .eq(VehicleCard::getTenantId, tenantId)
                .eq(
                    StringUtils.hasText(cardType),
                    VehicleCard::getCardType,
                    StringUtils.hasText(cardType) ? cardType.trim().toUpperCase() : null)
                .orderByDesc(VehicleCard::getUpdateTime)
                .orderByDesc(VehicleCard::getId));
    if (cards.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(cards);
    Map<Long, Org> orgMap = loadOrgMap(cards, vehicleMap.values().stream().toList());
    String keywordValue = StringUtils.hasText(keyword) ? keyword.trim() : null;
    String statusValue = StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    return cards.stream()
        .map(card -> toDto(card, vehicleMap.get(card.getVehicleId()), orgMap))
        .filter(dto -> matchKeyword(dto, keywordValue))
        .filter(dto -> !StringUtils.hasText(statusValue) || statusValue.equalsIgnoreCase(dto.getStatus()))
        .filter(dto -> orgId == null || Objects.equals(parseLong(dto.getOrgId()), orgId))
        .toList();
  }

  private VehicleCardSummaryDto toSummary(List<VehicleCardListItemDto> rows) {
    BigDecimal totalBalance =
        rows.stream()
            .map(VehicleCardListItemDto::getBalance)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal fuelBalance =
        rows.stream()
            .filter(item -> "FUEL".equalsIgnoreCase(item.getCardType()))
            .map(VehicleCardListItemDto::getBalance)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    BigDecimal electricBalance =
        rows.stream()
            .filter(item -> "ELECTRIC".equalsIgnoreCase(item.getCardType()))
            .map(VehicleCardListItemDto::getBalance)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
    return new VehicleCardSummaryDto(
        rows.size(),
        (int) rows.stream().filter(item -> "FUEL".equalsIgnoreCase(item.getCardType())).count(),
        (int)
            rows.stream().filter(item -> "ELECTRIC".equalsIgnoreCase(item.getCardType())).count(),
        (int) rows.stream().filter(item -> StringUtils.hasText(item.getVehicleId())).count(),
        (int) rows.stream().filter(item -> "LOW_BALANCE".equalsIgnoreCase(item.getStatus())).count(),
        totalBalance,
        fuelBalance,
        electricBalance);
  }

  private VehicleCardTransactionSummaryDto toTransactionSummary(
      List<VehicleCardTransactionListItemDto> rows) {
    return new VehicleCardTransactionSummaryDto(
        rows.size(),
        (int) rows.stream().filter(item -> "RECHARGE".equalsIgnoreCase(item.getTxnType())).count(),
        (int) rows.stream().filter(item -> "CONSUME".equalsIgnoreCase(item.getTxnType())).count(),
        rows.stream()
            .filter(item -> "RECHARGE".equalsIgnoreCase(item.getTxnType()))
            .map(VehicleCardTransactionListItemDto::getAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add),
        rows.stream()
            .filter(item -> "CONSUME".equalsIgnoreCase(item.getTxnType()))
            .map(VehicleCardTransactionListItemDto::getAmount)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add));
  }

  private VehicleCardListItemDto loadCardDto(Long id, Long tenantId) {
    VehicleCard entity = requireCard(id, tenantId);
    Map<Long, Vehicle> vehicleMap = loadVehicleMap(List.of(entity));
    Map<Long, Org> orgMap = loadOrgMap(List.of(entity), vehicleMap.values().stream().toList());
    return toDto(entity, vehicleMap.get(entity.getVehicleId()), orgMap);
  }

  private VehicleCardTransactionListItemDto toTransactionDto(
      VehicleCardTransaction entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehicleCardTransactionListItemDto dto = new VehicleCardTransactionListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setCardId(entity.getCardId() != null ? String.valueOf(entity.getCardId()) : null);
    dto.setCardNo(entity.getCardNo());
    dto.setCardType(entity.getCardType());
    dto.setCardTypeLabel(resolveCardTypeLabel(entity.getCardType()));
    dto.setTxnType(entity.getTxnType());
    dto.setTxnTypeLabel(resolveTxnTypeLabel(entity.getTxnType()));
    Long effectiveOrgId =
        vehicle != null && vehicle.getOrgId() != null ? vehicle.getOrgId() : entity.getOrgId();
    dto.setOrgId(effectiveOrgId != null ? String.valueOf(effectiveOrgId) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(effectiveOrgId), effectiveOrgId));
    dto.setVehicleId(vehicle != null && vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setAmount(defaultDecimal(entity.getAmount()));
    dto.setBalanceBefore(defaultDecimal(entity.getBalanceBefore()));
    dto.setBalanceAfter(defaultDecimal(entity.getBalanceAfter()));
    dto.setOccurredAt(formatDateTime(entity.getOccurredAt()));
    dto.setOperatorName(entity.getOperatorName());
    dto.setRemark(entity.getRemark());
    return dto;
  }

  private Map<Long, Vehicle> loadVehicleMap(List<VehicleCard> cards) {
    LinkedHashSet<Long> vehicleIds =
        cards.stream()
            .map(VehicleCard::getVehicleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (vehicleIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return vehicleMapper.selectBatchIds(vehicleIds).stream()
        .filter(vehicle -> vehicle.getId() != null)
        .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Long, Org> loadOrgMap(List<VehicleCard> cards, List<Vehicle> vehicles) {
    LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
    cards.stream().map(VehicleCard::getOrgId).filter(Objects::nonNull).forEach(orgIds::add);
    vehicles.stream().map(Vehicle::getOrgId).filter(Objects::nonNull).forEach(orgIds::add);
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(org -> org.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left));
  }

  private VehicleCardListItemDto toDto(VehicleCard entity, Vehicle vehicle, Map<Long, Org> orgMap) {
    VehicleCardListItemDto dto = new VehicleCardListItemDto();
    dto.setId(entity.getId() != null ? String.valueOf(entity.getId()) : null);
    dto.setCardNo(entity.getCardNo());
    dto.setCardType(entity.getCardType());
    dto.setCardTypeLabel(resolveCardTypeLabel(entity.getCardType()));
    dto.setProviderName(entity.getProviderName());
    Long effectiveOrgId =
        vehicle != null && vehicle.getOrgId() != null ? vehicle.getOrgId() : entity.getOrgId();
    dto.setOrgId(effectiveOrgId != null ? String.valueOf(effectiveOrgId) : null);
    dto.setOrgName(resolveOrgName(orgMap.get(effectiveOrgId), effectiveOrgId));
    dto.setVehicleId(vehicle != null && vehicle.getId() != null ? String.valueOf(vehicle.getId()) : null);
    dto.setPlateNo(vehicle != null ? vehicle.getPlateNo() : null);
    dto.setBalance(defaultDecimal(entity.getBalance()));
    dto.setTotalRecharge(defaultDecimal(entity.getTotalRecharge()));
    dto.setTotalConsume(defaultDecimal(entity.getTotalConsume()));
    String resolvedStatus =
        resolveCardStatus(entity.getStatus(), entity.getBalance(), entity.getVehicleId());
    dto.setStatus(resolvedStatus);
    dto.setStatusLabel(resolveStatusLabel(resolvedStatus));
    dto.setRemark(entity.getRemark());
    dto.setUpdateTime(formatDateTime(entity.getUpdateTime()));
    return dto;
  }

  private boolean matchKeyword(VehicleCardListItemDto dto, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(dto.getCardNo(), keyword)
        || contains(dto.getPlateNo(), keyword)
        || contains(dto.getOrgName(), keyword)
        || contains(dto.getProviderName(), keyword);
  }

  private void validateUpsert(VehicleCardUpsertDto body, Long tenantId, Long currentId) {
    if (body == null || !StringUtils.hasText(body.getCardNo())) {
      throw new BizException(400, "卡号不能为空");
    }
    Long existing =
        vehicleCardMapper.selectCount(
            new LambdaQueryWrapper<VehicleCard>()
                .eq(VehicleCard::getTenantId, tenantId)
                .eq(VehicleCard::getCardNo, body.getCardNo().trim())
                .ne(currentId != null, VehicleCard::getId, currentId));
    if (existing != null && existing > 0) {
      throw new BizException(400, "卡号已存在");
    }
  }

  private void applyUpsert(VehicleCard entity, VehicleCardUpsertDto body, Long tenantId) {
    entity.setCardNo(body.getCardNo() != null ? body.getCardNo().trim() : null);
    entity.setCardType(
        StringUtils.hasText(body.getCardType()) ? body.getCardType().trim().toUpperCase() : "FUEL");
    entity.setProviderName(trimToNull(body.getProviderName()));
    Vehicle vehicle = null;
    if (body.getVehicleId() != null) {
      vehicle = requireVehicle(body.getVehicleId(), tenantId);
      entity.setVehicleId(vehicle.getId());
      entity.setOrgId(vehicle.getOrgId());
    } else {
      entity.setVehicleId(null);
      entity.setOrgId(body.getOrgId());
    }
    entity.setBalance(defaultDecimal(body.getBalance()));
    entity.setTotalRecharge(defaultDecimal(body.getTotalRecharge()));
    entity.setTotalConsume(defaultDecimal(body.getTotalConsume()));
    entity.setRemark(trimToNull(body.getRemark()));
    entity.setStatus(
        resolveCardStatus(
            body.getStatus(),
            entity.getBalance(),
            vehicle != null ? vehicle.getId() : entity.getVehicleId()));
  }

  private void saveTransaction(
      VehicleCard card,
      String txnType,
      BigDecimal amount,
      BigDecimal balanceBefore,
      BigDecimal balanceAfter,
      String operatorName,
      String remark) {
    VehicleCardTransaction entity = new VehicleCardTransaction();
    entity.setTenantId(card.getTenantId());
    entity.setCardId(card.getId());
    entity.setCardNo(card.getCardNo());
    entity.setCardType(card.getCardType());
    entity.setTxnType(txnType);
    entity.setOrgId(card.getOrgId());
    entity.setVehicleId(card.getVehicleId());
    entity.setAmount(defaultDecimal(amount));
    entity.setBalanceBefore(defaultDecimal(balanceBefore));
    entity.setBalanceAfter(defaultDecimal(balanceAfter));
    entity.setOccurredAt(LocalDateTime.now());
    entity.setOperatorName(operatorName);
    entity.setRemark(remark);
    transactionMapper.insert(entity);
  }

  private String resolveCardStatus(String rawStatus, BigDecimal balance, Long vehicleId) {
    if (StringUtils.hasText(rawStatus) && "DISABLED".equalsIgnoreCase(rawStatus.trim())) {
      return "DISABLED";
    }
    if (vehicleId == null) {
      return "UNBOUND";
    }
    return defaultDecimal(balance).compareTo(LOW_BALANCE_THRESHOLD) < 0 ? "LOW_BALANCE" : "NORMAL";
  }

  private String resolveCardTypeLabel(String cardType) {
    if (!StringUtils.hasText(cardType)) {
      return "未知";
    }
    return switch (cardType.trim().toUpperCase()) {
      case "FUEL" -> "油卡";
      case "ELECTRIC" -> "电卡";
      default -> cardType;
    };
  }

  private String resolveTxnTypeLabel(String txnType) {
    if (!StringUtils.hasText(txnType)) {
      return "未知";
    }
    return switch (txnType.trim().toUpperCase()) {
      case "RECHARGE" -> "充值";
      case "CONSUME" -> "消费";
      default -> txnType;
    };
  }

  private String resolveStatusLabel(String status) {
    if (!StringUtils.hasText(status)) {
      return "未知";
    }
    return switch (status.trim().toUpperCase()) {
      case "NORMAL" -> "正常";
      case "LOW_BALANCE" -> "余额不足";
      case "UNBOUND" -> "未绑定";
      case "DISABLED" -> "停用";
      default -> status;
    };
  }

  private boolean matchTransactionKeyword(
      VehicleCardTransactionListItemDto dto, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    return contains(dto.getCardNo(), keyword)
        || contains(dto.getPlateNo(), keyword)
        || contains(dto.getOrgName(), keyword)
        || contains(dto.getOperatorName(), keyword)
        || contains(dto.getRemark(), keyword);
  }

  private boolean matchOccurredDate(String value, LocalDate from, LocalDate to) {
    if (!StringUtils.hasText(value)) {
      return from == null && to == null;
    }
    LocalDateTime dateTime = parseDateTime(value);
    if (dateTime == null) {
      return false;
    }
    LocalDate date = dateTime.toLocalDate();
    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
  }

  private String resolveOrgName(Org org, Long orgId) {
    if (org != null && StringUtils.hasText(org.getOrgName())) {
      return org.getOrgName();
    }
    if (orgId == null || orgId <= 0) {
      return "未归属单位";
    }
    return "组织#" + orgId;
  }

  private VehicleCard requireCard(Long id, Long tenantId) {
    VehicleCard entity = vehicleCardMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BizException(404, "油电卡不存在");
    }
    return entity;
  }

  private Vehicle requireVehicle(Long vehicleId, Long tenantId) {
    Vehicle vehicle = vehicleMapper.selectById(vehicleId);
    if (vehicle == null || !Objects.equals(vehicle.getTenantId(), tenantId)) {
      throw new BizException(400, "绑定车辆不存在");
    }
    return vehicle;
  }

  private <T> PageResult<T> paginate(List<T> rows, int pageNo, int pageSize) {
    int safePageNo = Math.max(pageNo, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
    int toIndex = Math.min(fromIndex + safePageSize, rows.size());
    return new PageResult<>(safePageNo, safePageSize, rows.size(), rows.subList(fromIndex, toIndex));
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private boolean contains(String source, String keyword) {
    return StringUtils.hasText(source) && source.contains(keyword);
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String resolveUserName(User user) {
    if (user == null) {
      return "系统";
    }
    return StringUtils.hasText(user.getName()) ? user.getName() : user.getUsername();
  }

  private LocalDate parseDate(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), ISO_DATE);
    } catch (Exception ex) {
      return null;
    }
  }

  private LocalDateTime parseDateTime(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.trim(), ISO_DATE_TIME);
    } catch (Exception ex) {
      return null;
    }
  }

  private Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO_DATE_TIME) : null;
  }

  private ResponseEntity<byte[]> csvResponse(String fileName, String content) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(content.getBytes(StandardCharsets.UTF_8));
  }

  private String buildCardCsv(List<VehicleCardListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("卡号,卡类型,所属单位,绑定车辆,当前余额,累计充值,累计消费,状态,发卡方,备注\n");
    for (VehicleCardListItemDto row : rows) {
      builder
          .append(csv(row.getCardNo())).append(',')
          .append(csv(row.getCardTypeLabel())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getPlateNo())).append(',')
          .append(csv(row.getBalance())).append(',')
          .append(csv(row.getTotalRecharge())).append(',')
          .append(csv(row.getTotalConsume())).append(',')
          .append(csv(row.getStatusLabel())).append(',')
          .append(csv(row.getProviderName())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
  }

  private String buildTransactionCsv(List<VehicleCardTransactionListItemDto> rows) {
    StringBuilder builder =
        new StringBuilder("发生时间,卡号,卡类型,流水类型,所属单位,绑定车辆,金额,变动前余额,变动后余额,操作人,备注\n");
    for (VehicleCardTransactionListItemDto row : rows) {
      builder
          .append(csv(row.getOccurredAt())).append(',')
          .append(csv(row.getCardNo())).append(',')
          .append(csv(row.getCardTypeLabel())).append(',')
          .append(csv(row.getTxnTypeLabel())).append(',')
          .append(csv(row.getOrgName())).append(',')
          .append(csv(row.getPlateNo())).append(',')
          .append(csv(row.getAmount())).append(',')
          .append(csv(row.getBalanceBefore())).append(',')
          .append(csv(row.getBalanceAfter())).append(',')
          .append(csv(row.getOperatorName())).append(',')
          .append(csv(row.getRemark())).append('\n');
    }
    return builder.toString();
  }

  private String csv(Object value) {
    if (value == null) {
      return "";
    }
    String text = String.valueOf(value);
    if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }
    return text;
  }
}
