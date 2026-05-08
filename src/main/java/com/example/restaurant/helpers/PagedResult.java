package com.example.restaurant.helpers;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {
  private List<T> items;
  private int pageNumber;
  private int pageSize;
  private long totalCount;
  private int totalPages;
  private boolean hasPreviousPage;
  private boolean hasNextPage;

  public PagedResult(Page<T> page) {
    this.items = page.getContent();
    this.pageNumber = page.getNumber() + 1;
    this.pageSize = page.getSize();
    this.totalCount = page.getTotalElements();
    this.totalPages = page.getTotalPages();
    this.hasPreviousPage = page.hasPrevious();
    this.hasNextPage = page.hasNext();
  }
}
