/*
 * Copyright (c) 2022 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */

package cn.enaium.community.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.enaium.community.annotation.RequestParamMap;
import cn.enaium.community.mapper.RoleMapper;
import cn.enaium.community.mapper.UserMapper;
import cn.enaium.community.model.entity.UserEntity;
import cn.enaium.community.model.result.Result;
import cn.enaium.community.util.AuthUtil;
import cn.enaium.community.util.ParamMap;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.val;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

import static cn.enaium.community.util.WrapperUtil.queryWrapper;

/**
 * @author Enaium
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserController(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @PostMapping("/info")
    public Result<UserEntity> info(@RequestParamMap ParamMap<String, Object> params) {
        if (params.containsKey("id")) {
            val user = userMapper.selectById(params.getLong("id"));
            if (user == null) {
                return Result.fail(Result.Code.USER_NOT_EXIST);
            }
            user.setPassword(null);
            return Result.success(user);
        }
        val data = userMapper.selectById(StpUtil.getLoginIdAsLong());
        data.setPassword(null);
        return Result.success(data);
    }

    @PostMapping("/update")
    public Result<Object> update(@RequestParamMap ParamMap<String, Object> params) {
        val id = params.getLong("id", AuthUtil.getId());

        if (userMapper.selectById(id) == null) {
            return Result.fail(Result.Code.USER_NOT_EXIST);
        }

        if (StpUtil.hasRole("user")) {
            if (!id.equals(AuthUtil.getId())) {
                return Result.fail(Result.Code.NO_PERMISSION);
            }
        }

        val userEntity = new UserEntity();

        userEntity.setId(id);

        if (params.has("avatar")) {
            val avatar = params.getString("avatar");
            userEntity.setAvatar((avatar == null || avatar.isBlank()) ? null : avatar);
        }

        if (params.has("username")) {
            val username = params.getString("username");

            if (userMapper.selectOne(queryWrapper(query -> query.eq("username", username))) != null) {
                return Result.fail(Result.Code.USER_ALREADY_EXIST);
            }

            if (!username.isBlank()) {
                userEntity.setUsername(username);
            }
        }

        if (params.has("ban")) {
            StpUtil.checkPermission("user.ban");
            val ban = params.getBoolean("ban");
            userEntity.setBanned(ban);
            roleMapper.updateByUserId(id, ban ? 4 : 3);
        }
        userEntity.setUpdateTime(new Date());
        return Result.success(userMapper.updateById(userEntity));
    }

    /**
     * get all user
     *
     * @param params map param
     * @return all user
     */
    @PostMapping("/users")
    @SaCheckPermission("user.query")
    public Result<Page<UserEntity>> users(@RequestParamMap ParamMap<String, Object> params) {
        return Result.success(userMapper.selectPage(new Page<>(params.getInt("current", 1), Math.min(params.getInt("size", 10), 20)), queryWrapper(query -> {

        })));
    }
}
