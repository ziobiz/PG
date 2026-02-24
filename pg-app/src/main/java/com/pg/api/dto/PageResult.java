package com.pg.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 페이지 조회 공통 응답
 */
public class PageResult<T> {
    private List<T> list;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <E, D> PageResult<D> of(Page<E> page, java.util.function.Function<E, D> mapper) {
        PageResult<D> r = new PageResult<>();
        r.setList(page.getContent().stream().map(mapper).collect(Collectors.toList()));
        r.setPage(page.getNumber() + 1);
        r.setSize(page.getSize());
        r.setTotalElements(page.getTotalElements());
        r.setTotalPages(page.getTotalPages());
        return r;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
