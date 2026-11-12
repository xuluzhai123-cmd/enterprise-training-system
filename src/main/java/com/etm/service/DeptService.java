package com.etm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etm.common.BizException;
import com.etm.entity.SysDept;
import com.etm.entity.SysUser;
import com.etm.mapper.SysDeptMapper;
import com.etm.mapper.SysUserMapper;
import com.etm.vo.DeptTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /** 扁平列表 */
    public List<SysDept> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
    }

    /** 部门树 */
    public List<DeptTreeVO> tree() {
        List<SysDept> all = list();
        Map<Long, DeptTreeVO> map = new HashMap<>();
        for (SysDept dept : all) {
            DeptTreeVO vo = new DeptTreeVO();
            vo.setId(dept.getId());
            vo.setName(dept.getName());
            vo.setParentId(dept.getParentId());
            vo.setSort(dept.getSort());
            vo.setStatus(dept.getStatus());
            map.put(dept.getId(), vo);
        }
        List<DeptTreeVO> roots = new ArrayList<>();
        for (SysDept dept : all) {
            DeptTreeVO vo = map.get(dept.getId());
            Long parentId = dept.getParentId();
            DeptTreeVO parent = parentId == null ? null : map.get(parentId);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        roots.sort(Comparator.comparing(DeptTreeVO::getSort, Comparator.nullsLast(Integer::compareTo)));
        return roots;
    }

    @Transactional
    public void create(SysDept dept) {
        dept.setId(null);
        checkNameUnique(dept, null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(0);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        deptMapper.insert(dept);
    }

    @Transactional
    public void update(SysDept dept) {
        if (dept.getId() == null) {
            throw new BizException("部门 id 不能为空");
        }
        SysDept old = deptMapper.selectById(dept.getId());
        if (old == null) {
            throw new BizException("部门不存在");
        }
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BizException("上级部门不能是自己");
        }
        checkNameUnique(dept, dept.getId());
        deptMapper.updateById(dept);
    }

    @Transactional
    public void delete(Long id) {
        Long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("请先删除下级部门");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BizException("该部门下还有员工，不能删除");
        }
        deptMapper.deleteById(id);
    }

    private void checkNameUnique(SysDept dept, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getName, dept.getName())
                .eq(SysDept::getParentId, dept.getParentId() == null ? 0L : dept.getParentId());
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        Long count = deptMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("同级下已存在同名部门");
        }
    }
}
