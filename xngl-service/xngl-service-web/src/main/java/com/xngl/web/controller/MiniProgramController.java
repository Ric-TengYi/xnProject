package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xngl.infrastructure.persistence.entity.contract.Contract;
import com.xngl.infrastructure.persistence.entity.contract.ContractTicket;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniSmsCodeRecord;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniUserBinding;
import com.xngl.infrastructure.persistence.entity.organization.Org;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.entity.project.Project;
import com.xngl.infrastructure.persistence.entity.site.Site;
import com.xngl.infrastructure.persistence.entity.site.SiteDevice;
import com.xngl.infrastructure.persistence.entity.site.SitePersonnelConfig;
import com.xngl.infrastructure.persistence.mapper.ContractMapper;
import com.xngl.infrastructure.persistence.mapper.ContractTicketMapper;
import com.xngl.infrastructure.persistence.mapper.MiniSmsCodeRecordMapper;
import com.xngl.infrastructure.persistence.mapper.MiniUserBindingMapper;
import com.xngl.infrastructure.persistence.mapper.OrgMapper;
import com.xngl.infrastructure.persistence.mapper.ProjectMapper;
import com.xngl.infrastructure.persistence.mapper.SiteDeviceMapper;
import com.xngl.infrastructure.persistence.mapper.SiteMapper;
import com.xngl.infrastructure.persistence.mapper.SitePersonnelConfigMapper;
import com.xngl.manager.auth.AuthService;
import com.xngl.manager.site.SiteService;
import com.xngl.manager.user.UserService;
import com.xngl.web.auth.JwtUtils;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.PageResult;
import com.xngl.web.dto.mini.MiniAccessibleSiteDto;
import com.xngl.web.dto.mini.MiniAccountBindDto;
import com.xngl.web.dto.mini.MiniAuthCodeRequestDto;
import com.xngl.web.dto.mini.MiniAuthCodeResponseDto;
import com.xngl.web.dto.mini.MiniExcavationOrgDto;
import com.xngl.web.dto.mini.MiniExcavationProjectDto;
import com.xngl.web.dto.mini.MiniLoginRequestDto;
import com.xngl.web.dto.mini.MiniLoginResponseDto;
import com.xngl.web.dto.mini.MiniOpenIdLoginRequestDto;
import com.xngl.web.dto.mini.MiniPasswordChangeDto;
import com.xngl.web.dto.mini.MiniSiteOverviewDto;
import com.xngl.web.dto.mini.MiniUserProfileDto;
import com.xngl.web.dto.project.ProjectDetailDto;
import com.xngl.web.dto.site.DisposalListItemDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini")
public class MiniProgramController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final long PERMISSION_VERSION = 1L;
  private static final Set<String> SUPPORTED_EXCAVATION_ORG_TYPES =
      Set.of("CONSTRUCTION_UNIT", "BUILDER_UNIT", "TRANSPORT_COMPANY");

  private final AuthService authService;
  private final UserService userService;
  private final UserContext userContext;
  private final JwtUtils jwtUtils;
  private final MiniSmsCodeRecordMapper miniSmsCodeRecordMapper;
  private final MiniUserBindingMapper miniUserBindingMapper;
  private final SitePersonnelConfigMapper sitePersonnelConfigMapper;
  private final SiteService siteService;
  private final SiteMapper siteMapper;
  private final SiteDeviceMapper siteDeviceMapper;
  private final ContractMapper contractMapper;
  private final ContractTicketMapper contractTicketMapper;
  private final ProjectMapper projectMapper;
  private final OrgMapper orgMapper;
  private final ProjectsController projectsController;
  private final Environment environment;

  public MiniProgramController(
      AuthService authService,
      UserService userService,
      JwtUtils jwtUtils,
      MiniSmsCodeRecordMapper miniSmsCodeRecordMapper,
      MiniUserBindingMapper miniUserBindingMapper,
      SitePersonnelConfigMapper sitePersonnelConfigMapper,
      SiteService siteService,
      SiteMapper siteMapper,
      SiteDeviceMapper siteDeviceMapper,
      ContractMapper contractMapper,
      ContractTicketMapper contractTicketMapper,
      ProjectMapper projectMapper,
      OrgMapper orgMapper,
      ProjectsController projectsController,
      Environment environment,
      UserContext userContext) {
    this.authService = authService;
    this.userService = userService;
    this.jwtUtils = jwtUtils;
    this.miniSmsCodeRecordMapper = miniSmsCodeRecordMapper;
    this.miniUserBindingMapper = miniUserBindingMapper;
    this.sitePersonnelConfigMapper = sitePersonnelConfigMapper;
    this.siteService = siteService;
    this.siteMapper = siteMapper;
    this.siteDeviceMapper = siteDeviceMapper;
    this.contractMapper = contractMapper;
    this.contractTicketMapper = contractTicketMapper;
    this.projectMapper = projectMapper;
    this.orgMapper = orgMapper;
    this.projectsController = projectsController;
    this.environment = environment;
    this.userContext = userContext;
  }

  @PostMapping("/auth/send-sms-code")
  public ApiResult<MiniAuthCodeResponseDto> sendSmsCode(
      @RequestBody MiniAuthCodeRequestDto body, HttpServletRequest request) {
    User user = validateLoginUser(body != null ? body.getTenantId() : null, body != null ? body.getUsername() : null, body != null ? body.getPassword() : null);
    String mobile = resolveMobile(user, body != null ? body.getMobile() : null);
    String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

    MiniSmsCodeRecord record = new MiniSmsCodeRecord();
    record.setTenantId(user.getTenantId());
    record.setUserId(user.getId());
    record.setMobile(mobile);
    record.setBizType("LOGIN");
    record.setVerifyCode(verifyCode);
    record.setExpiresAt(expiresAt);
    record.setUsedFlag(0);
    record.setSendStatus("SENT");
    record.setChannel("SMS");
    record.setRemark("mini login second factor");
    miniSmsCodeRecordMapper.insert(record);

    upsertBinding(user, mobile, body != null ? body.getOpenId() : null, null, request, null);
    return ApiResult.ok(
        new MiniAuthCodeResponseDto(
            mobile,
            expiresAt.format(ISO),
            "SENT",
            isLocalProfile() ? verifyCode : null));
  }

  @PostMapping("/auth/login")
  public ApiResult<MiniLoginResponseDto> login(
      @RequestBody MiniLoginRequestDto body, HttpServletRequest request) {
    User user = validateLoginUser(body != null ? body.getTenantId() : null, body != null ? body.getUsername() : null, body != null ? body.getPassword() : null);
    String mobile = resolveMobile(user, body != null ? body.getMobile() : null);
    verifySmsCode(user, mobile, body != null ? body.getSmsCode() : null);

    MiniUserBinding binding =
        upsertBinding(
            user,
            mobile,
            body != null ? body.getOpenId() : null,
            body != null ? body.getUnionId() : null,
            request,
            body != null ? body.getDeviceName() : null);

    String token = jwtUtils.createToken(user.getUsername(), String.valueOf(user.getId()));
    long expiresIn =
        jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    User update = new User();
    update.setId(user.getId());
    update.setLastLoginTime(LocalDateTime.now());
    userService.update(update);

    return ApiResult.ok(
        new MiniLoginResponseDto(
            token,
            "Bearer",
            expiresIn / 1000,
            toMiniUserProfile(user, binding),
            loadAccessibleSites(user)));
  }

  @PostMapping("/auth/openid-login")
  public ApiResult<MiniLoginResponseDto> openIdLogin(
      @RequestBody MiniOpenIdLoginRequestDto body, HttpServletRequest request) {
    if (body == null || !StringUtils.hasText(body.getOpenId())) {
      throw new BizException(400, "openId 不能为空");
    }
    MiniUserBinding binding =
        miniUserBindingMapper.selectOne(
            new LambdaQueryWrapper<MiniUserBinding>()
                .eq(MiniUserBinding::getOpenId, body.getOpenId().trim())
                .eq(MiniUserBinding::getStatus, "BOUND")
                .orderByDesc(MiniUserBinding::getId)
                .last("limit 1"));
    if (binding == null || binding.getUserId() == null) {
      throw new BizException(404, "当前微信账号未绑定平台用户");
    }
    User user = userService.getById(binding.getUserId());
    if (user == null || !"ENABLED".equalsIgnoreCase(user.getStatus())) {
      throw new BizException(403, "绑定用户不可用");
    }
    MiniUserBinding latestBinding =
        upsertBinding(
            user,
            resolveMobile(user, binding.getMobile()),
            body.getOpenId(),
            body.getUnionId(),
            request,
            body.getDeviceName());
    String token = jwtUtils.createToken(user.getUsername(), String.valueOf(user.getId()));
    long expiresIn =
        jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    User update = new User();
    update.setId(user.getId());
    update.setLastLoginTime(LocalDateTime.now());
    userService.update(update);
    return ApiResult.ok(
        new MiniLoginResponseDto(
            token,
            "Bearer",
            expiresIn / 1000,
            toMiniUserProfile(user, latestBinding),
            loadAccessibleSites(user)));
  }

  @GetMapping("/me")
  public ApiResult<MiniUserProfileDto> me(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniUserBinding binding = loadBinding(currentUser.getTenantId(), currentUser.getId());
    return ApiResult.ok(toMiniUserProfile(currentUser, binding));
  }

  @GetMapping("/sites")
  public ApiResult<List<MiniAccessibleSiteDto>> listSites(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    return ApiResult.ok(loadAccessibleSites(currentUser));
  }

  @GetMapping("/sites/{siteId}")
  public ApiResult<MiniSiteOverviewDto> siteOverview(
      @PathVariable Long siteId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureSiteAccessible(currentUser, siteId);
    Site site = siteMapper.selectById(siteId);
    if (site == null || Objects.equals(site.getDeleted(), 1)) {
      throw new BizException(404, "场地不存在");
    }
    return ApiResult.ok(toMiniSiteOverview(site, currentUser.getTenantId()));
  }

  @GetMapping("/sites/{siteId}/disposals")
  public ApiResult<PageResult<DisposalListItemDto>> siteDisposals(
      @PathVariable Long siteId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int pageNo,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureSiteAccessible(currentUser, siteId);
    return ApiResult.ok(loadMiniDisposals(currentUser.getTenantId(), siteId, keyword, status, pageNo, pageSize));
  }

  @GetMapping("/excavation-orgs/current")
  public ApiResult<MiniExcavationOrgDto> currentExcavationOrg(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    Org org = resolveCurrentOrg(currentUser);
    if (org == null) {
      throw new BizException(404, "当前账号未绑定出土单位");
    }
    return ApiResult.ok(toMiniExcavationOrgDto(org, currentUser));
  }

  @GetMapping("/excavation-orgs/projects")
  public ApiResult<List<MiniExcavationProjectDto>> excavationProjects(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<Project> projects = loadAccessibleProjects(currentUser, keyword, status);
    if (projects.isEmpty()) {
      return ApiResult.ok(Collections.emptyList());
    }
    Map<Long, Org> orgMap = loadOrgMap(projects);
    Map<Long, List<Contract>> contractsByProject = loadContractsByProject(currentUser.getTenantId(), projects);
    Map<Long, BigDecimal> disposedVolumeMap = loadDisposedVolumeMap(contractsByProject);
    return ApiResult.ok(
        projects.stream()
            .map(
                project ->
                    toMiniExcavationProjectDto(
                        project,
                        orgMap.get(project.getOrgId()),
                        contractsByProject.getOrDefault(project.getId(), Collections.emptyList()),
                        disposedVolumeMap.getOrDefault(project.getId(), BigDecimal.ZERO)))
            .toList());
  }

  @GetMapping("/excavation-orgs/projects/{projectId}")
  public ApiResult<ProjectDetailDto> excavationProjectDetail(
      @PathVariable Long projectId, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    ensureProjectAccessible(currentUser, projectId);
    return projectsController.get(projectId, request);
  }

  @PostMapping("/account/send-password-code")
  public ApiResult<MiniAuthCodeResponseDto> sendPasswordCode(HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    String mobile = resolveMobile(currentUser, null);
    String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

    MiniSmsCodeRecord record = new MiniSmsCodeRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setUserId(currentUser.getId());
    record.setMobile(mobile);
    record.setBizType("PASSWORD_CHANGE");
    record.setVerifyCode(verifyCode);
    record.setExpiresAt(expiresAt);
    record.setUsedFlag(0);
    record.setSendStatus("SENT");
    record.setChannel("SMS");
    record.setRemark("mini password change");
    miniSmsCodeRecordMapper.insert(record);
    return ApiResult.ok(
        new MiniAuthCodeResponseDto(
            mobile,
            expiresAt.format(ISO),
            "SENT",
            isLocalProfile() ? verifyCode : null));
  }

  @PostMapping("/account/password")
  public ApiResult<Void> changePassword(
      @RequestBody MiniPasswordChangeDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || !StringUtils.hasText(body.getNewPassword())) {
      throw new BizException(400, "新密码不能为空");
    }
    if (StringUtils.hasText(body.getOldPassword())) {
      if (!authService.checkPassword(currentUser, body.getOldPassword().trim())) {
        throw new BizException(401, "旧密码错误");
      }
    } else {
      verifySmsCode(currentUser, resolveMobile(currentUser, null), body != null ? body.getSmsCode() : null, "PASSWORD_CHANGE");
    }
    userService.resetPassword(currentUser.getId(), body.getNewPassword().trim());
    return ApiResult.ok();
  }

  @PostMapping("/account/bind")
  public ApiResult<MiniUserProfileDto> bindAccount(
      @RequestBody MiniAccountBindDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || !StringUtils.hasText(body.getOpenId())) {
      throw new BizException(400, "openId 不能为空");
    }
    MiniUserBinding binding =
        upsertBinding(
            currentUser,
            resolveMobile(currentUser, null),
            body.getOpenId(),
            body.getUnionId(),
            request,
            body.getDeviceName());
    return ApiResult.ok(toMiniUserProfile(currentUser, binding));
  }

  private User validateLoginUser(String tenantIdText, String username, String password) {
    if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
      throw new BizException(400, "账号和密码不能为空");
    }
    Long tenantId = parseTenantId(tenantIdText);
    User user = authService.getByTenantAndUsername(tenantId, username.trim());
    if (user == null || !authService.checkPassword(user, password.trim())) {
      throw new BizException(401, "用户名或密码错误");
    }
    if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
      throw new BizException(403, "用户已被禁用");
    }
    return user;
  }

  private String resolveMobile(User user, String requestMobile) {
    String effectiveMobile = StringUtils.hasText(requestMobile) ? requestMobile.trim() : user.getMobile();
    if (!StringUtils.hasText(effectiveMobile)) {
      throw new BizException(400, "用户未配置手机号");
    }
    if (StringUtils.hasText(user.getMobile()) && StringUtils.hasText(requestMobile) && !user.getMobile().trim().equals(requestMobile.trim())) {
      throw new BizException(400, "手机号与平台账号不匹配");
    }
    return effectiveMobile;
  }

  private void verifySmsCode(User user, String mobile, String smsCode) {
    verifySmsCode(user, mobile, smsCode, "LOGIN");
  }

  private void verifySmsCode(User user, String mobile, String smsCode, String bizType) {
    if (!StringUtils.hasText(smsCode)) {
      throw new BizException(400, "短信验证码不能为空");
    }
    MiniSmsCodeRecord record =
        miniSmsCodeRecordMapper.selectOne(
            new LambdaQueryWrapper<MiniSmsCodeRecord>()
                .eq(MiniSmsCodeRecord::getTenantId, user.getTenantId())
                .eq(MiniSmsCodeRecord::getMobile, mobile)
                .eq(MiniSmsCodeRecord::getBizType, bizType)
                .eq(MiniSmsCodeRecord::getUsedFlag, 0)
                .orderByDesc(MiniSmsCodeRecord::getId)
                .last("limit 1"));
    if (record == null) {
      throw new BizException(404, "请先获取短信验证码");
    }
    if (record.getExpiresAt() == null || record.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BizException(401, "短信验证码已过期");
    }
    if (!smsCode.trim().equals(record.getVerifyCode())) {
      throw new BizException(401, "短信验证码错误");
    }
    record.setUsedFlag(1);
    record.setUsedTime(LocalDateTime.now());
    miniSmsCodeRecordMapper.updateById(record);
  }

  private MiniUserBinding upsertBinding(
      User user,
      String mobile,
      String openId,
      String unionId,
      HttpServletRequest request,
      String deviceName) {
    MiniUserBinding binding = loadBinding(user.getTenantId(), user.getId());
    if (binding == null) {
      binding = new MiniUserBinding();
      binding.setTenantId(user.getTenantId());
      binding.setUserId(user.getId());
      binding.setUsername(user.getUsername());
      binding.setSourceChannel("MINI");
      binding.setStatus("BOUND");
    }
    binding.setMobile(mobile);
    binding.setOpenId(StringUtils.hasText(openId) ? openId.trim() : binding.getOpenId());
    binding.setUnionId(StringUtils.hasText(unionId) ? unionId.trim() : binding.getUnionId());
    binding.setLastLoginTime(LocalDateTime.now());
    binding.setLastLoginIp(request != null ? request.getRemoteAddr() : null);
    binding.setLastLoginDevice(StringUtils.hasText(deviceName) ? deviceName.trim() : binding.getLastLoginDevice());
    if (binding.getId() == null) {
      miniUserBindingMapper.insert(binding);
    } else {
      miniUserBindingMapper.updateById(binding);
    }
    return binding;
  }

  private MiniUserBinding loadBinding(Long tenantId, Long userId) {
    return miniUserBindingMapper.selectOne(
        new LambdaQueryWrapper<MiniUserBinding>()
            .eq(MiniUserBinding::getTenantId, tenantId)
            .eq(MiniUserBinding::getUserId, userId)
            .last("limit 1"));
  }

  private MiniUserProfileDto toMiniUserProfile(User user, MiniUserBinding binding) {
    return new MiniUserProfileDto(
        String.valueOf(user.getId()),
        user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null,
        user.getUsername(),
        user.getName(),
        user.getMobile(),
        user.getUserType(),
        user.getMainOrgId() != null ? String.valueOf(user.getMainOrgId()) : null,
        binding != null ? binding.getOpenId() : null,
        binding != null ? binding.getStatus() : "UNBOUND",
        binding != null && binding.getLastLoginTime() != null ? binding.getLastLoginTime().format(ISO) : null);
  }

  private Org resolveCurrentOrg(User user) {
    Long mainOrgId =
        user.getMainOrgId() != null ? user.getMainOrgId() : userService.getMainOrgIdByUserId(user.getId());
    if (mainOrgId != null) {
      Org mainOrg = orgMapper.selectById(mainOrgId);
      if (mainOrg != null
          && !Objects.equals(mainOrg.getDeleted(), 1)
          && SUPPORTED_EXCAVATION_ORG_TYPES.contains(mainOrg.getOrgType())) {
        return mainOrg;
      }
    }
    List<Org> candidateOrgs =
        (isAdminUser(user)
                ? orgMapper.selectList(
                    new LambdaQueryWrapper<Org>().in(Org::getOrgType, SUPPORTED_EXCAVATION_ORG_TYPES))
                : orgMapper.selectBatchIds(resolveAccessibleOrgIds(user)))
            .stream()
        .filter(org -> org.getId() != null && !Objects.equals(org.getDeleted(), 1))
        .filter(org -> SUPPORTED_EXCAVATION_ORG_TYPES.contains(org.getOrgType()))
        .toList();
    if (candidateOrgs.isEmpty()) {
      return null;
    }
    return candidateOrgs.stream()
        .max(
            java.util.Comparator.comparingLong(
                org ->
                    projectMapper.selectCount(
                        new LambdaQueryWrapper<Project>().eq(Project::getOrgId, org.getId()))))
        .orElse(null);
  }

  private MiniExcavationOrgDto toMiniExcavationOrgDto(Org org, User user) {
    long projectCount = resolveAccessibleProjectIds(user).size();
    return new MiniExcavationOrgDto(
        String.valueOf(org.getId()),
        org.getOrgCode(),
        org.getOrgName(),
        org.getOrgType(),
        org.getContactPerson(),
        org.getContactPhone(),
        org.getAddress(),
        org.getStatus(),
        Objects.equals(org.getId(), user.getMainOrgId()),
        projectCount);
  }

  private List<Project> loadAccessibleProjects(User user, String keyword, Integer status) {
    LinkedHashSet<Long> accessibleProjectIds = resolveAccessibleProjectIds(user);
    if (!isAdminUser(user) && accessibleProjectIds.isEmpty()) {
      return Collections.emptyList();
    }
    LambdaQueryWrapper<Project> query = new LambdaQueryWrapper<>();
    if (!isAdminUser(user)) {
      query.in(Project::getId, accessibleProjectIds);
    }
    if (status != null) {
      query.eq(Project::getStatus, status);
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      query.and(
          wrapper ->
              wrapper
                  .like(Project::getName, effectiveKeyword)
                  .or()
                  .like(Project::getCode, effectiveKeyword)
                  .or()
                  .like(Project::getAddress, effectiveKeyword));
    }
    query.orderByDesc(Project::getUpdateTime).orderByDesc(Project::getId);
    return projectMapper.selectList(query);
  }

  private LinkedHashSet<Long> resolveAccessibleOrgIds(User user) {
    LinkedHashSet<Long> orgIds =
        userService.listOrgIdsByUserId(user.getId()).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (user.getMainOrgId() != null) {
      orgIds.add(user.getMainOrgId());
    }
    Long mainOrgId = userService.getMainOrgIdByUserId(user.getId());
    if (mainOrgId != null) {
      orgIds.add(mainOrgId);
    }
    return orgIds;
  }

  private LinkedHashSet<Long> resolveAccessibleProjectIds(User user) {
    if (isAdminUser(user)) {
      return projectMapper.selectList(new LambdaQueryWrapper<Project>()).stream()
          .map(Project::getId)
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    LinkedHashSet<Long> orgIds = resolveAccessibleOrgIds(user);
    LinkedHashSet<Long> projectIds =
        projectMapper.selectList(
                new LambdaQueryWrapper<Project>().in(!orgIds.isEmpty(), Project::getOrgId, orgIds))
            .stream()
            .map(Project::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return projectIds;
    }
    contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, user.getTenantId())
                .and(
                    wrapper ->
                        wrapper
                            .in(Contract::getConstructionOrgId, orgIds)
                            .or()
                            .in(Contract::getTransportOrgId, orgIds)
                            .or()
                            .in(Contract::getSiteOperatorOrgId, orgIds)
                            .or()
                            .in(Contract::getPartyId, orgIds)))
        .stream()
        .map(Contract::getProjectId)
        .filter(Objects::nonNull)
        .forEach(projectIds::add);
    return projectIds;
  }

  private void ensureProjectAccessible(User user, Long projectId) {
    if (projectId == null) {
      throw new BizException(400, "项目不能为空");
    }
    Project project = projectMapper.selectById(projectId);
    if (project == null || Objects.equals(project.getDeleted(), 1)) {
      throw new BizException(404, "项目不存在");
    }
    if (isAdminUser(user)) {
      return;
    }
    if (!resolveAccessibleProjectIds(user).contains(projectId)) {
      throw new BizException(403, "当前账号无权查看该项目");
    }
  }

  private Map<Long, Org> loadOrgMap(List<Project> projects) {
    LinkedHashSet<Long> orgIds =
        projects.stream()
            .map(Project::getOrgId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .filter(org -> org.getId() != null)
        .collect(Collectors.toMap(Org::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, List<Contract>> loadContractsByProject(Long tenantId, List<Project> projects) {
    LinkedHashSet<Long> projectIds =
        projects.stream()
            .map(Project::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (projectIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .in(Contract::getProjectId, projectIds))
        .stream()
        .filter(contract -> contract.getProjectId() != null)
        .collect(Collectors.groupingBy(Contract::getProjectId, LinkedHashMap::new, Collectors.toList()));
  }

  private Map<Long, BigDecimal> loadDisposedVolumeMap(Map<Long, List<Contract>> contractsByProject) {
    LinkedHashSet<Long> contractIds =
        contractsByProject.values().stream()
            .flatMap(List::stream)
            .map(Contract::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (contractIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Long> contractProjectMap = new LinkedHashMap<>();
    contractsByProject.forEach(
        (projectId, contracts) ->
            contracts.forEach(
                contract -> {
                  if (contract.getId() != null) {
                    contractProjectMap.put(contract.getId(), projectId);
                  }
                }));
    Map<Long, BigDecimal> result = new LinkedHashMap<>();
    contractTicketMapper.selectList(
            new LambdaQueryWrapper<ContractTicket>()
                .in(ContractTicket::getContractId, contractIds)
                .notIn(ContractTicket::getStatus, List.of("VOID", "CANCELLED")))
        .forEach(
            ticket -> {
              Long projectId = contractProjectMap.get(ticket.getContractId());
              if (projectId != null) {
                result.merge(projectId, defaultDecimal(ticket.getVolume()), BigDecimal::add);
              }
            });
    return result;
  }

  private MiniExcavationProjectDto toMiniExcavationProjectDto(
      Project project, Org org, List<Contract> contracts, BigDecimal disposedVolume) {
    LinkedHashSet<Long> siteIds =
        contracts.stream()
            .map(Contract::getSiteId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    BigDecimal agreedVolume =
        contracts.stream()
            .map(Contract::getAgreedVolume)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remainingVolume = agreedVolume.subtract(defaultDecimal(disposedVolume));
    if (remainingVolume.signum() < 0) {
      remainingVolume = BigDecimal.ZERO;
    }
    return new MiniExcavationProjectDto(
        String.valueOf(project.getId()),
        project.getCode(),
        project.getName(),
        project.getAddress(),
        project.getStatus(),
        resolveProjectStatusLabel(project.getStatus()),
        project.getOrgId() != null ? String.valueOf(project.getOrgId()) : null,
        org != null ? org.getOrgName() : null,
        (long) contracts.size(),
        (long) siteIds.size(),
        agreedVolume,
        defaultDecimal(disposedVolume),
        remainingVolume);
  }

  private List<MiniAccessibleSiteDto> loadAccessibleSites(User user) {
    Map<Long, SitePersonnelConfig> personnelMap = loadPersonnelMap(user);
    LinkedHashSet<Long> accessibleSiteIds =
        isAdminUser(user)
            ? siteService.list().stream()
                .map(Site::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
            : personnelMap.keySet().stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    if (accessibleSiteIds.isEmpty()) {
      return Collections.emptyList();
    }

    List<Site> sites =
        siteMapper.selectBatchIds(accessibleSiteIds).stream()
            .filter(site -> site.getId() != null && !Objects.equals(site.getDeleted(), 1))
            .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
            .toList();
    Map<Long, TicketStats> todayStats = loadTicketStats(accessibleSiteIds, LocalDate.now());
    Map<Long, Integer> onlineDeviceCounts = loadOnlineDeviceCount(accessibleSiteIds);

    return sites.stream()
        .map(
            site -> {
              TicketStats stats = todayStats.getOrDefault(site.getId(), TicketStats.EMPTY);
              SitePersonnelConfig personnel = personnelMap.get(site.getId());
              return new MiniAccessibleSiteDto(
                  String.valueOf(site.getId()),
                  site.getName(),
                  site.getCode(),
                  personnel != null ? personnel.getRoleType() : (isAdminUser(user) ? "ADMIN" : null),
                  personnel != null ? personnel.getDutyScope() : "全部场地",
                  stats.count(),
                  stats.volume(),
                  onlineDeviceCounts.getOrDefault(site.getId(), 0));
            })
        .toList();
  }

  private MiniSiteOverviewDto toMiniSiteOverview(Site site, Long tenantId) {
    LinkedHashSet<Long> siteIds = new LinkedHashSet<>();
    siteIds.add(site.getId());
    Map<Long, TicketStats> todayStats = loadTicketStats(siteIds, LocalDate.now());
    Map<Long, TicketStats> totalStats = loadTicketStats(siteIds, null);
    Map<Long, Integer> onlineDeviceCount = loadOnlineDeviceCount(siteIds);
    Map<Long, Integer> personnelCount = loadPersonnelCount(siteIds, tenantId);
    TicketStats today = todayStats.getOrDefault(site.getId(), TicketStats.EMPTY);
    TicketStats total = totalStats.getOrDefault(site.getId(), TicketStats.EMPTY);
    return new MiniSiteOverviewDto(
        String.valueOf(site.getId()),
        site.getName(),
        site.getCode(),
        site.getAddress(),
        site.getSiteType(),
        site.getStatus(),
        site.getCapacity(),
        site.getSiteLevel(),
        site.getManagementArea(),
        today.count(),
        today.volume(),
        total.count(),
        total.volume(),
        onlineDeviceCount.getOrDefault(site.getId(), 0),
        personnelCount.getOrDefault(site.getId(), 0));
  }

  private PageResult<DisposalListItemDto> loadMiniDisposals(
      Long tenantId, Long siteId, String keyword, String status, int pageNo, int pageSize) {
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .eq(Contract::getSiteId, siteId));
    if (contracts.isEmpty()) {
      return new PageResult<>((long) pageNo, (long) pageSize, 0L, Collections.emptyList());
    }

    Map<Long, Contract> contractMap =
        contracts.stream()
            .filter(contract -> contract.getId() != null)
            .collect(Collectors.toMap(Contract::getId, Function.identity(), (left, right) -> left));

    LinkedHashSet<Long> projectIds =
        contracts.stream()
            .map(Contract::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? Collections.emptyMap()
            : projectMapper.selectBatchIds(projectIds).stream()
                .filter(project -> project.getId() != null)
                .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left));

    LambdaQueryWrapper<ContractTicket> query =
        new LambdaQueryWrapper<ContractTicket>()
            .eq(ContractTicket::getTenantId, tenantId)
            .in(ContractTicket::getContractId, contractMap.keySet())
            .orderByDesc(ContractTicket::getTicketDate)
            .orderByDesc(ContractTicket::getId);
    if (StringUtils.hasText(status)) {
      query.eq(ContractTicket::getStatus, status.trim().toUpperCase());
    }
    if (StringUtils.hasText(keyword)) {
      String effectiveKeyword = keyword.trim();
      LinkedHashSet<Long> matchedContractIds =
          contracts.stream()
              .filter(
                  contract ->
                      contains(contract.getName(), effectiveKeyword)
                          || contains(contract.getContractNo(), effectiveKeyword)
                          || contains(contract.getCode(), effectiveKeyword)
                          || contains(projectMap.get(contract.getProjectId()) != null
                                  ? projectMap.get(contract.getProjectId()).getName()
                                  : null,
                              effectiveKeyword))
              .map(Contract::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      query.and(
          wrapper -> {
            wrapper.like(ContractTicket::getTicketNo, effectiveKeyword);
            if (!matchedContractIds.isEmpty()) {
              wrapper.or().in(ContractTicket::getContractId, matchedContractIds);
            }
          });
    }

    IPage<ContractTicket> page =
        contractTicketMapper.selectPage(new Page<>(pageNo, pageSize), query);
    Site site = siteMapper.selectById(siteId);
    List<DisposalListItemDto> records =
        page.getRecords().stream()
            .map(ticket -> toDisposalItem(ticket, contractMap.get(ticket.getContractId()), site, projectMap))
            .toList();
    return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
  }

  private DisposalListItemDto toDisposalItem(
      ContractTicket ticket, Contract contract, Site site, Map<Long, Project> projectMap) {
    Project project = contract != null ? projectMap.get(contract.getProjectId()) : null;
    return new DisposalListItemDto(
        ticket.getId() != null ? String.valueOf(ticket.getId()) : null,
        site != null && site.getId() != null ? String.valueOf(site.getId()) : null,
        site != null ? site.getName() : null,
        ticket.getTicketDate() != null ? ticket.getTicketDate().toString() : null,
        contract != null ? firstNonBlank(contract.getContractNo(), contract.getCode()) : null,
        project != null ? project.getName() : (contract != null ? contract.getName() : null),
        contract != null ? firstNonBlank(contract.getSourceType(), contract.getContractType()) : null,
        ticket.getVolume() != null ? ticket.getVolume().intValue() : 0,
        normalizeTicketStatus(ticket.getStatus()));
  }

  private Map<Long, SitePersonnelConfig> loadPersonnelMap(User user) {
    if (isAdminUser(user)) {
      return Collections.emptyMap();
    }
    return sitePersonnelConfigMapper.selectList(
            new LambdaQueryWrapper<SitePersonnelConfig>()
                .eq(SitePersonnelConfig::getTenantId, user.getTenantId())
                .eq(SitePersonnelConfig::getUserId, user.getId())
                .eq(SitePersonnelConfig::getAccountEnabled, 1))
        .stream()
        .filter(config -> config.getSiteId() != null)
        .collect(Collectors.toMap(SitePersonnelConfig::getSiteId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, TicketStats> loadTicketStats(Set<Long> siteIds, LocalDate targetDate) {
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Contract> contracts =
        contractMapper.selectList(
            new LambdaQueryWrapper<Contract>().in(Contract::getSiteId, siteIds));
    if (contracts.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Long> contractSiteMap =
        contracts.stream()
            .filter(contract -> contract.getId() != null && contract.getSiteId() != null)
            .collect(Collectors.toMap(Contract::getId, Contract::getSiteId, (left, right) -> left));
    LambdaQueryWrapper<ContractTicket> query =
        new LambdaQueryWrapper<ContractTicket>().in(ContractTicket::getContractId, contractSiteMap.keySet());
    if (targetDate != null) {
      query.eq(ContractTicket::getTicketDate, targetDate);
    }
    return contractTicketMapper.selectList(query).stream()
        .collect(
            Collectors.groupingBy(
                ticket -> contractSiteMap.get(ticket.getContractId()),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    rows ->
                        new TicketStats(
                            rows.size(),
                            rows.stream()
                                .map(ContractTicket::getVolume)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))));
  }

  private Map<Long, Integer> loadOnlineDeviceCount(Set<Long> siteIds) {
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return siteDeviceMapper.selectList(
            new LambdaQueryWrapper<SiteDevice>()
                .in(SiteDevice::getSiteId, siteIds)
                .in(SiteDevice::getStatus, Arrays.asList("ONLINE", "ACTIVE", "ENABLED")))
        .stream()
        .filter(device -> device.getSiteId() != null)
        .collect(Collectors.toMap(SiteDevice::getSiteId, device -> 1, Integer::sum, LinkedHashMap::new));
  }

  private Map<Long, Integer> loadPersonnelCount(Set<Long> siteIds, Long tenantId) {
    if (siteIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return sitePersonnelConfigMapper.selectList(
            new LambdaQueryWrapper<SitePersonnelConfig>()
                .eq(SitePersonnelConfig::getTenantId, tenantId)
                .eq(SitePersonnelConfig::getAccountEnabled, 1)
                .in(SitePersonnelConfig::getSiteId, siteIds))
        .stream()
        .filter(config -> config.getSiteId() != null)
        .collect(Collectors.toMap(SitePersonnelConfig::getSiteId, config -> 1, Integer::sum, LinkedHashMap::new));
  }

  private void ensureSiteAccessible(User user, Long siteId) {
    if (siteId == null) {
      throw new BizException(400, "场地不能为空");
    }
    if (isAdminUser(user)) {
      return;
    }
    boolean accessible =
        sitePersonnelConfigMapper.selectCount(
                new LambdaQueryWrapper<SitePersonnelConfig>()
                    .eq(SitePersonnelConfig::getTenantId, user.getTenantId())
                    .eq(SitePersonnelConfig::getUserId, user.getId())
                    .eq(SitePersonnelConfig::getSiteId, siteId)
                    .eq(SitePersonnelConfig::getAccountEnabled, 1))
            > 0;
    if (!accessible) {
      throw new BizException(403, "当前账号无权访问该场地");
    }
  }

  private boolean isAdminUser(User user) {
    return user != null
        && StringUtils.hasText(user.getUserType())
        && Arrays.asList("TENANT_ADMIN", "SUPER_ADMIN", "ADMIN").contains(user.getUserType().trim().toUpperCase());
  }

  private String normalizeTicketStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "正常";
    }
    String normalized = status.trim().toUpperCase();
    if (Arrays.asList("VOID", "REJECTED", "ABNORMAL").contains(normalized)) {
      return "异常";
    }
    return "正常";
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private boolean contains(String raw, String keyword) {
    return StringUtils.hasText(raw) && raw.contains(keyword);
  }

  private String resolveProjectStatusLabel(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 0 -> "立项";
      case 1 -> "在建";
      case 2 -> "预警";
      case 3 -> "完工";
      default -> "状态" + status;
    };
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private Long parseTenantId(String tenantIdText) {
    if (!StringUtils.hasText(tenantIdText)) {
      return null;
    }
    try {
      return Long.parseLong(tenantIdText.trim());
    } catch (NumberFormatException ex) {
      throw new BizException(400, "tenantId 格式错误");
    }
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }

  private boolean isLocalProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("local");
  }

  private record TicketStats(int count, BigDecimal volume) {
    private static final TicketStats EMPTY = new TicketStats(0, BigDecimal.ZERO);
  }
}
