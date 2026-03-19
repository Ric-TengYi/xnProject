package com.xngl.manager.contract;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractMaterialVo {
  private Long id;
  private Long contractId;
  private String materialName;
  private String materialType;
  private String fileUrl;
  private Long fileSize;
  private Long uploaderId;
  private String uploaderName;
  private LocalDateTime uploadTime;
  private String remark;
}