package com.smartdx.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.query.UserQuery;
import com.smartdx.system.model.vo.UserVO;

import java.util.List;

/**
 * User service interface
 */
public interface UserService {

    /**
     * Get user by ID
     */
    UserVO getUserById(Long id);

    /**
     * List users with pagination
     */
    IPage<UserVO> listUsers(UserQuery query);

    /**
     * Create user
     */
    Long createUser(User user);

    /**
     * Update user
     */
    void updateUser(User user);

    /**
     * Delete user
     */
    void deleteUser(Long id);

    /**
     * Delete users in batch
     */
    void deleteUsers(List<Long> ids);

    /**
     * Reset user password
     */
    void resetPassword(Long id, String password);
}
