package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.system.model.entity.Log;
import com.smartdx.system.model.query.LogQuery;
import com.smartdx.system.model.vo.LogPageVO;

/**
 * ログサービス
 */
public interface LogService extends IService<Log> {

    /**
     * ログページリスト取得
     *
     * @param query クエリパラメータ
     * @return ページリスト
     */
    Page<LogPageVO> getLogPage(LogQuery query);
}
