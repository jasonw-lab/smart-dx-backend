package com.smartdx.retail.service;

/**
 * 決済デモサービス
 */
public interface PaymentDemoService {

    /**
     * デモ用決済を実行し、在庫が0以下のレコードを100へリセットする
     *
     * @return 生成した決済件数
     */
    int runDemoPayments();
}
