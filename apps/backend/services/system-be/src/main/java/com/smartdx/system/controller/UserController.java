package com.smartdx.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.entity.User;
import com.smartdx.system.model.query.UserQuery;
import com.smartdx.system.model.vo.UserVO;
import com.smartdx.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User management controller
 */
@Tag(name = "02. User Management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "List users with pagination")
    @GetMapping
    public PageResult<UserVO> listUsers(UserQuery query) {
        IPage<UserVO> page = userService.listUsers(query);
        return PageResult.success(page.getRecords(), page.getTotal());
    }

    @Operation(summary = "Create user")
    @PostMapping
    public Result<Long> createUser(@RequestBody User user) {
        return Result.success(userService.createUser(user));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success();
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "Delete users in batch")
    @DeleteMapping
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return Result.success();
    }

    @Operation(summary = "Reset user password")
    @PatchMapping("/{id}/password")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @Parameter(description = "New password") @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.success();
    }
}
