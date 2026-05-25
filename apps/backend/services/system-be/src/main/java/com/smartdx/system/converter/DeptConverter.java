package com.smartdx.system.converter;

import com.smartdx.system.model.entity.Dept;
import com.smartdx.system.model.form.DeptForm;
import com.smartdx.system.model.vo.DeptVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 部門コンバーター
 */
@Mapper(componentModel = "spring")
public interface DeptConverter {

    DeptVO toVo(Dept dept);

    List<DeptVO> toVoList(List<Dept> depts);

    Dept toEntity(DeptForm form);

    DeptForm toForm(Dept entity);
}
