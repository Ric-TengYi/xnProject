package com.xngl.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniSafetyCourse;
import com.xngl.infrastructure.persistence.entity.miniprogram.MiniSafetyLearningRecord;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.infrastructure.persistence.mapper.MiniSafetyCourseMapper;
import com.xngl.infrastructure.persistence.mapper.MiniSafetyLearningRecordMapper;
import com.xngl.web.dto.ApiResult;
import com.xngl.web.dto.mini.MiniFaceCheckDto;
import com.xngl.web.dto.mini.MiniSafetyCourseDto;
import com.xngl.web.dto.mini.MiniSafetyLearningCompleteDto;
import com.xngl.web.dto.mini.MiniSafetyLearningRecordDto;
import com.xngl.web.dto.mini.MiniSafetyLearningStartDto;
import com.xngl.web.exception.BizException;
import com.xngl.web.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/safety-education")
public class MiniSafetyEducationController {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final MiniSafetyCourseMapper miniSafetyCourseMapper;
  private final MiniSafetyLearningRecordMapper miniSafetyLearningRecordMapper;
  private final UserContext userContext;

  public MiniSafetyEducationController(
      MiniSafetyCourseMapper miniSafetyCourseMapper,
      MiniSafetyLearningRecordMapper miniSafetyLearningRecordMapper,
      UserContext userContext) {
    this.miniSafetyCourseMapper = miniSafetyCourseMapper;
    this.miniSafetyLearningRecordMapper = miniSafetyLearningRecordMapper;
    this.userContext = userContext;
  }

  @GetMapping("/courses")
  public ApiResult<List<MiniSafetyCourseDto>> listCourses(
      @RequestParam(required = false) String courseType, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniSafetyCourse> rows =
        miniSafetyCourseMapper.selectList(
            new LambdaQueryWrapper<MiniSafetyCourse>()
                .eq(MiniSafetyCourse::getTenantId, currentUser.getTenantId())
                .eq(MiniSafetyCourse::getStatus, "PUBLISHED")
                .eq(StringUtils.hasText(courseType), MiniSafetyCourse::getCourseType, courseType != null ? courseType.trim().toUpperCase() : null)
                .orderByAsc(MiniSafetyCourse::getCourseType)
                .orderByAsc(MiniSafetyCourse::getId));
    return ApiResult.ok(rows.stream().map(this::toCourseDto).toList());
  }

  @GetMapping("/learning-records")
  public ApiResult<List<MiniSafetyLearningRecordDto>> listLearningRecords(
      @RequestParam(required = false) String status, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    List<MiniSafetyLearningRecord> rows =
        miniSafetyLearningRecordMapper.selectList(
            new LambdaQueryWrapper<MiniSafetyLearningRecord>()
                .eq(MiniSafetyLearningRecord::getTenantId, currentUser.getTenantId())
                .eq(MiniSafetyLearningRecord::getUserId, currentUser.getId())
                .eq(StringUtils.hasText(status), MiniSafetyLearningRecord::getStatus, status != null ? status.trim().toUpperCase() : null)
                .orderByDesc(MiniSafetyLearningRecord::getUpdateTime)
                .orderByDesc(MiniSafetyLearningRecord::getId));
    return ApiResult.ok(toLearningRecordDtos(currentUser.getTenantId(), rows));
  }

  @PostMapping("/learning-records/start")
  public ApiResult<MiniSafetyLearningRecordDto> startLearning(
      @RequestBody MiniSafetyLearningStartDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    if (body == null || body.getCourseId() == null) {
      throw new BizException(400, "课程不能为空");
    }
    MiniSafetyCourse course = requireCourse(body.getCourseId(), currentUser.getTenantId());
    MiniSafetyLearningRecord record = new MiniSafetyLearningRecord();
    record.setTenantId(currentUser.getTenantId());
    record.setCourseId(course.getId());
    record.setUserId(currentUser.getId());
    record.setLearnerName(resolveUserDisplayName(currentUser));
    record.setStatus("LEARNING");
    record.setStudiedMinutes(0);
    record.setProgressPercent(0);
    record.setFaceCheckCount(0);
    record.setStartTime(LocalDateTime.now());
    record.setLastStudyTime(LocalDateTime.now());
    record.setNextFaceCheckTime(nextFaceCheckTime(course));
    record.setRemark(trimToNull(body.getRemark()));
    miniSafetyLearningRecordMapper.insert(record);
    return ApiResult.ok(toLearningRecordDto(record, course));
  }

  @PostMapping("/learning-records/{id}/face-check")
  public ApiResult<MiniSafetyLearningRecordDto> faceCheck(
      @PathVariable Long id, @RequestBody(required = false) MiniFaceCheckDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniSafetyLearningRecord record = requireLearningRecord(id, currentUser);
    MiniSafetyCourse course = requireCourse(record.getCourseId(), currentUser.getTenantId());
    if (body != null && Boolean.FALSE.equals(body.getPassed())) {
      throw new BizException(400, "人脸校验未通过");
    }
    record.setFaceCheckCount(defaultInt(record.getFaceCheckCount()) + 1);
    record.setLastFaceCheckTime(LocalDateTime.now());
    record.setLastStudyTime(LocalDateTime.now());
    record.setNextFaceCheckTime(nextFaceCheckTime(course));
    record.setRemark(joinRemark(record.getRemark(), body != null ? body.getRemark() : null));
    miniSafetyLearningRecordMapper.updateById(record);
    return ApiResult.ok(toLearningRecordDto(record, course));
  }

  @PostMapping("/learning-records/{id}/complete")
  public ApiResult<MiniSafetyLearningRecordDto> completeLearning(
      @PathVariable Long id, @RequestBody(required = false) MiniSafetyLearningCompleteDto body, HttpServletRequest request) {
    User currentUser = requireCurrentUser(request);
    MiniSafetyLearningRecord record = requireLearningRecord(id, currentUser);
    MiniSafetyCourse course = requireCourse(record.getCourseId(), currentUser.getTenantId());
    if (Objects.equals(course.getFaceCheckRequired(), 1) && defaultInt(record.getFaceCheckCount()) <= 0) {
      throw new BizException(400, "当前课程要求至少完成一次人脸校验");
    }
    int studiedMinutes = body != null && body.getStudiedMinutes() != null ? body.getStudiedMinutes() : defaultInt(course.getDurationMinutes());
    record.setStudiedMinutes(Math.max(defaultInt(record.getStudiedMinutes()), studiedMinutes));
    record.setProgressPercent(100);
    record.setStatus("COMPLETED");
    record.setCompleteTime(LocalDateTime.now());
    record.setLastStudyTime(LocalDateTime.now());
    record.setNextFaceCheckTime(null);
    record.setRemark(joinRemark(record.getRemark(), body != null ? body.getRemark() : null));
    miniSafetyLearningRecordMapper.updateById(record);
    return ApiResult.ok(toLearningRecordDto(record, course));
  }

  private List<MiniSafetyLearningRecordDto> toLearningRecordDtos(
      Long tenantId, List<MiniSafetyLearningRecord> rows) {
    if (rows == null || rows.isEmpty()) {
      return Collections.emptyList();
    }
    LinkedHashSet<Long> courseIds =
        rows.stream().map(MiniSafetyLearningRecord::getCourseId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, MiniSafetyCourse> courseMap =
        courseIds.isEmpty()
            ? Collections.emptyMap()
            : miniSafetyCourseMapper.selectBatchIds(courseIds).stream()
                .filter(item -> Objects.equals(item.getTenantId(), tenantId))
                .collect(Collectors.toMap(MiniSafetyCourse::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    return rows.stream().map(item -> toLearningRecordDto(item, courseMap.get(item.getCourseId()))).toList();
  }

  private MiniSafetyCourse requireCourse(Long id, Long tenantId) {
    MiniSafetyCourse course = miniSafetyCourseMapper.selectById(id);
    if (course == null || !Objects.equals(course.getTenantId(), tenantId)) {
      throw new BizException(404, "安全教育课程不存在");
    }
    return course;
  }

  private MiniSafetyLearningRecord requireLearningRecord(Long id, User currentUser) {
    MiniSafetyLearningRecord record = miniSafetyLearningRecordMapper.selectById(id);
    if (record == null
        || !Objects.equals(record.getTenantId(), currentUser.getTenantId())
        || !Objects.equals(record.getUserId(), currentUser.getId())) {
      throw new BizException(404, "学习记录不存在");
    }
    return record;
  }

  private LocalDateTime nextFaceCheckTime(MiniSafetyCourse course) {
    int range = Math.max(defaultInt(course.getRandomCheckMinutes()), 1);
    int offset = ThreadLocalRandom.current().nextInt(1, range + 1);
    return LocalDateTime.now().plusMinutes(offset);
  }

  private MiniSafetyCourseDto toCourseDto(MiniSafetyCourse course) {
    MiniSafetyCourseDto dto = new MiniSafetyCourseDto();
    dto.setId(course.getId() != null ? String.valueOf(course.getId()) : null);
    dto.setCourseCode(course.getCourseCode());
    dto.setTitle(course.getTitle());
    dto.setCourseType(course.getCourseType());
    dto.setCoverUrl(course.getCoverUrl());
    dto.setFileUrl(course.getFileUrl());
    dto.setDurationMinutes(course.getDurationMinutes());
    dto.setRandomCheckMinutes(course.getRandomCheckMinutes());
    dto.setFaceCheckRequired(Objects.equals(course.getFaceCheckRequired(), 1));
    dto.setDescription(course.getDescription());
    dto.setStatus(course.getStatus());
    return dto;
  }

  private MiniSafetyLearningRecordDto toLearningRecordDto(
      MiniSafetyLearningRecord record, MiniSafetyCourse course) {
    MiniSafetyLearningRecordDto dto = new MiniSafetyLearningRecordDto();
    dto.setId(record.getId() != null ? String.valueOf(record.getId()) : null);
    dto.setCourseId(record.getCourseId() != null ? String.valueOf(record.getCourseId()) : null);
    dto.setCourseTitle(course != null ? course.getTitle() : null);
    dto.setCourseType(course != null ? course.getCourseType() : null);
    dto.setLearnerName(record.getLearnerName());
    dto.setStatus(record.getStatus());
    dto.setStudiedMinutes(record.getStudiedMinutes());
    dto.setProgressPercent(record.getProgressPercent());
    dto.setFaceCheckCount(record.getFaceCheckCount());
    dto.setLastFaceCheckTime(formatDateTime(record.getLastFaceCheckTime()));
    dto.setNextFaceCheckTime(formatDateTime(record.getNextFaceCheckTime()));
    dto.setStartTime(formatDateTime(record.getStartTime()));
    dto.setCompleteTime(formatDateTime(record.getCompleteTime()));
    dto.setLastStudyTime(formatDateTime(record.getLastStudyTime()));
    dto.setRemark(record.getRemark());
    return dto;
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(ISO) : null;
  }

  private int defaultInt(Integer value) {
    return value != null ? value : 0;
  }

  private String resolveUserDisplayName(User user) {
    if (user == null) {
      return null;
    }
    return StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getUsername();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String joinRemark(String current, String append) {
    String left = trimToNull(current);
    String right = trimToNull(append);
    if (!StringUtils.hasText(left)) {
      return right;
    }
    if (!StringUtils.hasText(right)) {
      return left;
    }
    return left + " | " + right;
  }

  private User requireCurrentUser(HttpServletRequest request) {
    return userContext.requireCurrentUser(request);
  }
}
