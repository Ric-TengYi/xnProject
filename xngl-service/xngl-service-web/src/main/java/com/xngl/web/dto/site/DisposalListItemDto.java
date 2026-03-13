package com.xngl.web.dto.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消纳清单单条记录 DTO，与前端 SitesDisposals 表格字段对齐。
 * 当前无 biz_disposal 表，接口以 stub 返回空列表，后续建表后接入真实数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisposalListItemDto {

  private String id;
  private String siteId;
  private String site;
  private String time;
  private String plate;
  private String project;
  private String source;
  private Integer volume;
  private String status;
}
