package com.yunshu.zhixue.platform.web;

import java.util.List;

/** 总数通过十进制字符串传输，避免 JavaScript 精度丢失。 */
public record PageResponse<T>(List<T> items, int page, int page_size, String total, String total_pages) {}
