package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Sales;

import java.util.List;

/**
 * Sales Service Interface
 */
public interface SalesService {

    List<Sales> listSales(Long storeId);

    Sales getSalesById(Long id);

    boolean createSales(Sales sales);
}
