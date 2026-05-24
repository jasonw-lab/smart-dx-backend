package com.smartdx.retail.service;

import com.smartdx.core.result.PageResult;
import com.smartdx.retail.model.entity.InventoryTransaction;
import com.smartdx.retail.model.form.InventoryTransactionForm;
import com.smartdx.retail.model.query.InventoryTransactionPageQuery;
import com.smartdx.retail.model.vo.InventoryTransactionPageVO;

import java.util.List;

/**
 * 入出庫履歴サービスインターフェース
 */
public interface InventoryTransactionService {

    /**
     * 入出庫履歴一覧（ページング）取得
     *
     * @param queryParams クエリパラメータ
     * @return ページング結果
     */
    PageResult<InventoryTransactionPageVO> getTransactionPage(InventoryTransactionPageQuery queryParams);

    /**
     * 入庫履歴一覧（ページング）取得
     *
     * @param queryParams クエリパラメータ
     * @return ページング結果
     */
    PageResult<InventoryTransactionPageVO> getInboundTransactionPage(InventoryTransactionPageQuery queryParams);

    /**
     * 出庫履歴一覧（ページング）取得
     *
     * @param queryParams クエリパラメータ
     * @return ページング結果
     */
    PageResult<InventoryTransactionPageVO> getOutboundTransactionPage(InventoryTransactionPageQuery queryParams);

    /**
     * 入出庫履歴一覧（全件）取得
     *
     * @return 入出庫履歴リスト
     */
    List<InventoryTransaction> listTransactions();

    /**
     * 入出庫履歴詳細取得
     *
     * @param id 入出庫履歴ID
     * @return 入出庫履歴詳細
     */
    InventoryTransaction getTransactionById(Long id);

    /**
     * 入出庫履歴新規作成
     *
     * @param form 入出庫履歴フォーム
     * @return 作成結果
     */
    boolean createTransaction(InventoryTransactionForm form);

    /**
     * 入庫履歴新規作成
     *
     * @param form 入出庫履歴フォーム
     * @return 作成結果
     */
    boolean createInboundTransaction(InventoryTransactionForm form);

    /**
     * 出庫履歴新規作成
     *
     * @param form 入出庫履歴フォーム
     * @return 作成結果
     */
    boolean createOutboundTransaction(InventoryTransactionForm form);

    /**
     * 入出庫履歴更新
     *
     * @param id   入出庫履歴ID
     * @param form 入出庫履歴フォーム
     * @return 更新結果
     */
    boolean updateTransaction(Long id, InventoryTransactionForm form);

    /**
     * 入出庫履歴削除
     *
     * @param id 入出庫履歴ID
     * @return 削除結果
     */
    boolean deleteTransaction(Long id);
}
