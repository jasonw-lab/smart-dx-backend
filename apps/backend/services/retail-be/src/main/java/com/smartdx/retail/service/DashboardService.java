package com.smartdx.retail.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service Interface
 */
public interface DashboardService {

    Map<String, Object> getKpi();

    List<Map<String, Object>> getSalesTrend(LocalDate startDate, LocalDate endDate);
}
