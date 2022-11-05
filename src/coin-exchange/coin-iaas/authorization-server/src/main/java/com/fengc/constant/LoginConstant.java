package com.fengc.constant;

/**
 * @author lfc
 */
public class LoginConstant {


    /**
     * 后台管理人员
     */
    public static final String ADMIN_TYPE = "admin_type";

    /**
     * 普通的用户
     */
    public static final String MEMBER_TYPE = "member_type";

    /**
     * 根据用户名查询用户的SQl
     */
    public static final String QUERY_ADMIN_SQL =
            "SELECT `id` ,`username`, `password`, `status` FROM sys_user WHERE username = ? ";

    /**
     * 判断用户是否是管理员
     */
    public static final String QUERY_ROLE_CODE_SQL =
            "SELECT `code` FROM sys_role LEFT JOIN sys_user_role ON sys_role.id = sys_user_role.role_id WHERE sys_user_role.user_id= ?";

    public static final String QUERY_ALL_PERMISSIONS =
            "SELECT `name` FROM sys_privilege";

    public static final String QUERY_PERMISSION_SQL =
            "SELECT `name` FROM sys_privilege LEFT JOIN sys_role_privilege ON sys_role_privilege.privilege_id = sys_privilege.id LEFT JOIN sys_user_role  ON sys_role_privilege.role_id = sys_user_role.role_id WHERE sys_user_role.user_id = ?";

    public static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

    public static final String QUERY_MEMBER_SQL =
            "SELECT `id`,`password`, `status` FROM `user` WHERE mobile = ? or email = ? ";
}

