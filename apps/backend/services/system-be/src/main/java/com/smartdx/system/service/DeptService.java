package com.smartdx.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartdx.core.model.Option;
import com.smartdx.system.model.entity.Dept;
import com.smartdx.system.model.form.DeptForm;
import com.smartdx.system.model.query.DeptQuery;
import com.smartdx.system.model.vo.DeptVO;

import java.util.List;

/**
 * 部門 Service
 */
public interface DeptService extends IService<Dept> {

    /**
     * 部門一覧取得
     */
    List<DeptVO> listDepts(DeptQuery queryParams);

    /**
     * 部門ドロップダウンリスト
     */
    List<Option<Long>> listDeptOptions();

    /**
     * 部門フォームデータ取得
     */
    DeptForm getDeptForm(Long id);

    /**
     * 部門保存
     */
    boolean saveDept(DeptForm deptForm);

    /**
     * 部門削除
     */
    boolean deleteDepts(String ids);
}
