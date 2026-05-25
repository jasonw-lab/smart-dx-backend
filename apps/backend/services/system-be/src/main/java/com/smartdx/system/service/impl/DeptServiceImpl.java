package com.smartdx.system.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartdx.core.model.Option;
import com.smartdx.system.converter.DeptConverter;
import com.smartdx.system.mapper.DeptMapper;
import com.smartdx.system.model.entity.Dept;
import com.smartdx.system.model.form.DeptForm;
import com.smartdx.system.model.query.DeptQuery;
import com.smartdx.system.model.vo.DeptVO;
import com.smartdx.system.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部門 Service 実装
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final DeptConverter deptConverter;

    @Override
    public List<DeptVO> listDepts(DeptQuery queryParams) {
        List<Dept> deptList = this.list(new LambdaQueryWrapper<Dept>()
                .like(StrUtil.isNotBlank(queryParams.getKeywords()), Dept::getName, queryParams.getKeywords())
                .eq(queryParams.getStatus() != null, Dept::getStatus, queryParams.getStatus())
                .orderByAsc(Dept::getSort)
        );

        List<DeptVO> voList = deptConverter.toVoList(deptList);
        return buildDeptTree(voList);
    }

    @Override
    public List<Option<Long>> listDeptOptions() {
        List<Dept> deptList = this.list(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getStatus, 1)
                .select(Dept::getId, Dept::getParentId, Dept::getName)
                .orderByAsc(Dept::getSort)
        );
        return buildOptionTree(deptList, 0L);
    }

    @Override
    public DeptForm getDeptForm(Long id) {
        Dept dept = this.getById(id);
        return deptConverter.toForm(dept);
    }

    @Override
    public boolean saveDept(DeptForm deptForm) {
        Long deptId = deptForm.getId();
        String deptCode = deptForm.getCode();

        // コード重複チェック
        if (StrUtil.isNotBlank(deptCode)) {
            long count = this.count(new LambdaQueryWrapper<Dept>()
                    .ne(deptId != null, Dept::getId, deptId)
                    .eq(Dept::getCode, deptCode)
            );
            Assert.isTrue(count == 0, "部門コードが既に存在します");
        }

        Dept dept = deptConverter.toEntity(deptForm);

        // ツリーパスの生成
        if (dept.getParentId() != null && dept.getParentId() > 0) {
            Dept parent = this.getById(dept.getParentId());
            if (parent != null) {
                dept.setTreePath(parent.getTreePath() + "," + parent.getId());
            }
        } else {
            dept.setTreePath("0");
            dept.setParentId(0L);
        }

        return this.saveOrUpdate(dept);
    }

    @Override
    public boolean deleteDepts(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "削除する部門IDは必須です");

        List<Long> deptIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        for (Long deptId : deptIds) {
            // 子部門の存在チェック
            long childCount = this.count(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, deptId));
            Assert.isTrue(childCount == 0, "子部門が存在するため削除できません");
        }

        return this.removeByIds(deptIds);
    }

    /**
     * 部門ツリー構築
     */
    private List<DeptVO> buildDeptTree(List<DeptVO> depts) {
        Map<Long, DeptVO> deptMap = depts.stream()
                .collect(Collectors.toMap(DeptVO::getId, d -> d));

        List<DeptVO> result = new ArrayList<>();
        for (DeptVO dept : depts) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                result.add(dept);
            } else {
                DeptVO parent = deptMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        return result;
    }

    /**
     * オプションツリー構築
     */
    private List<Option<Long>> buildOptionTree(List<Dept> depts, Long parentId) {
        List<Option<Long>> options = new ArrayList<>();
        for (Dept dept : depts) {
            if (Objects.equals(dept.getParentId(), parentId)) {
                List<Option<Long>> children = buildOptionTree(depts, dept.getId());
                Option<Long> option = new Option<>(dept.getId(), dept.getName(),
                        children.isEmpty() ? null : children);
                options.add(option);
            }
        }
        return options;
    }
}
