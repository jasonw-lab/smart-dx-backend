package com.smartdx.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartdx.core.exception.BusinessException;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.mapper.UserMapper;
import com.smartdx.system.model.dto.CurrentUserDTO;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.query.UserQuery;
import com.smartdx.system.model.vo.UserVO;
import com.smartdx.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return convertToVO(user);
    }

    @Override
    public IPage<UserVO> listUsers(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), User::getUsername, query.getUsername())
                .like(StrUtil.isNotBlank(query.getNickname()), User::getNickname, query.getNickname())
                .like(StrUtil.isNotBlank(query.getMobile()), User::getMobile, query.getMobile())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, User::getDeptId, query.getDeptId())
                .orderByDesc(User::getCreateTime);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        return userPage.convert(this::convertToVO);
    }

    @Override
    @Transactional
    public Long createUser(User user) {
        // Encode password
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // Default password
            user.setPassword(passwordEncoder.encode("123456"));
        }

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        // Don't update password here
        user.setPassword(null);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        userMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String password) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    @Override
    public CurrentUserDTO getCurrentUserInfo() {
        com.smartdx.security.model.UserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) {
            throw new BusinessException("User not authenticated");
        }

        CurrentUserDTO dto = new CurrentUserDTO();
        dto.setUserId(userDetails.getUserId());
        dto.setUsername(userDetails.getUsername());
        dto.setNickname(userDetails.getNickname());
        dto.setCanSwitchTenant(userDetails.getCanSwitchTenant());
        dto.setRoles(userDetails.getRoleCodes());
        // perms can be populated later if needed
        dto.setPerms(null);

        // Get avatar from database
        User user = userMapper.selectById(userDetails.getUserId());
        if (user != null) {
            dto.setAvatar(user.getAvatar());
        }

        return dto;
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
