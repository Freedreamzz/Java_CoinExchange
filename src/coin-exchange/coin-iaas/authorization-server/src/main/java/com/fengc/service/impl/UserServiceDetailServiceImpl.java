package com.fengc.service.impl;

import com.fengc.constant.LoginConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lfc
 */
@Service
public class UserServiceDetailServiceImpl implements UserDetailsService {
    @Resource
    private JdbcTemplate jdbcTemplate;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //获取登录类型（后台用户 还是 普通用户）
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String loginType = requestAttributes.getRequest().getParameter("login_type");
        if (StringUtils.isEmpty(loginType)) {
            throw new AuthenticationServiceException("登录类型不能为null");
        }
        UserDetails userDetails = null;
        try {
            switch (loginType) {
                case LoginConstant.ADMIN_TYPE:
                    userDetails = loadSysUsername(username);
                    break;
                case LoginConstant.MEMBER_TYPE:
                    userDetails = loadMemberUsername(username);
                     break;
                default:
                    throw new AuthenticationServiceException("暂不支持的登录方式：" + loginType);
            }
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new UsernameNotFoundException("用户名" + username + "不存在");
        }
        return userDetails;
    }

    /**
     * 普通用户登录
     *
     * @param username
     * @return
     */
    private UserDetails loadMemberUsername(String username) {
        //1.根据用户username获取userID
        return jdbcTemplate.queryForObject(LoginConstant.QUERY_MEMBER_SQL, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                if (rs.wasNull()) {
                    throw new UsernameNotFoundException("用户名" + username + "不存在");
                }
                long id = rs.getLong("id"); // 用户的id
                String password = rs.getString("password"); // 用户的密码
                int status = rs.getInt("status");
                return new User(   // 3 封装成一个UserDetails对象，返回
                        String.valueOf(id), //使用id->username
                        password,
                        status == 1,
                        true,
                        true,
                        true,
                        Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }
        },username,username);
    }

    /**
     * 后台人员登录
     *
     * @param username
     * @return
     */
    private UserDetails loadSysUsername(String username) {
        //使用用户名查询用户
        return jdbcTemplate.queryForObject(LoginConstant.QUERY_ADMIN_SQL, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                if (rs.wasNull()) {
                    throw new UsernameNotFoundException("用户名" + username + "不存在");
                }
                Long id = rs.getLong("id");
                String password = rs.getString("password");
                int status = rs.getInt("status");
                User user = new User(String.valueOf(id), password, status == 1, true, true, true, getSysUserPermissions(id));
                return user;
            }
        }, username);
        //return jdbcTemplate.queryForObject(LoginConstant.QUERY_ADMIN_SQL, new RowMapper<User>() {
        //    @Override
        //    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        //        if (rs.wasNull()) {
        //            throw new UsernameNotFoundException("用户名" + username + "不存在");
        //        }
        //        long id = rs.getLong("id"); // 用户的id
        //        String password = rs.getString("password"); // 用户的密码
        //        int status = rs.getInt("status");
        //        return new User(   // 3 封装成一个UserDetails对象，返回
        //                String.valueOf(id), //使用id->username
        //                password,
        //                status == 1,
        //                true,
        //                true,
        //                true,
        //                getSysUserPermissions(id)
        //        );
        //    }
        //}, username);
    }

    /**
     * 根据用户ID获取用户角色
     *
     * @param id
     * @return
     */
    private Collection<? extends GrantedAuthority> getSysUserPermissions(Long id) {
        List<String> permissions = null;
        String roleCode = jdbcTemplate.queryForObject(LoginConstant.QUERY_ROLE_CODE_SQL, String.class, id);
        //当用户为超级管理员时，有所有的权限
        if (LoginConstant.ADMIN_ROLE_CODE.equals(roleCode)) {
            permissions = jdbcTemplate.queryForList(LoginConstant.QUERY_ALL_PERMISSIONS, String.class);
        }
        //为普通用户的时候，需要使用角色来查询权限数据
        permissions = jdbcTemplate.queryForList(LoginConstant.QUERY_PERMISSION_SQL, String.class, id);

        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        //去重
        return permissions.stream().distinct().map(perm -> new SimpleGrantedAuthority(perm)).collect(Collectors.toSet());

    }
}
