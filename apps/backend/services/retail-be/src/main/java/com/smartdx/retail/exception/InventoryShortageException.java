package com.smartdx.retail.exception;

/**
 * 在庫残量不足例外（HTTP 409 Conflict）
 * <p>
 * 廃棄要求数量が対象ロットの現在数量を超過した場合にスローする。
 * 同時更新による残量不足を 400（形式違反）と区別する。
 * </p>
 */
public class InventoryShortageException extends RuntimeException {

    public InventoryShortageException(String message) {
        super(message);
    }
}
