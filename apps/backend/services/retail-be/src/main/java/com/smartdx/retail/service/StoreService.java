package com.smartdx.retail.service;

import com.smartdx.retail.model.entity.Store;

import java.util.List;

/**
 * Store Service Interface
 */
public interface StoreService {

    List<Store> listStores();

    Store getStoreById(Long id);

    boolean createStore(Store store);

    boolean updateStore(Long id, Store store);

    boolean deleteStore(Long id);
}
