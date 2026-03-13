package com.xngl.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

  private long pageNo;
  private long pageSize;
  private long total;
  private List<T> records;
}
