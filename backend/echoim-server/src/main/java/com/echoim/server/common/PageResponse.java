package com.echoim.server.common;

import java.util.List;

public class PageResponse<T> {

    private final List<T> list;
    private final long pageNo;
    private final long pageSize;
    private final long total;

    public PageResponse(List<T> list, long pageNo, long pageSize, long total) {
        this.list = list;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public long getPageNo() {
        return pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }
}
