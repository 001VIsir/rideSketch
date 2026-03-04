package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应数据库表：user
 * 存储用户基本信息、认证信息等
 *
 * @author rideSketch
 */
@Data
@TableName("user")
public class User {

    /**
     * 用户ID（主键、自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（唯一）
     * 用于登录
     */
    private String username;

    /**
     * 邮箱（唯一，可为空）
     * 用于登录和找回密码
     */
    private String email;

    /**
     * 密码
     * BCrypt加密后的密文，存储在数据库中
     */
    private String password;

    /**
     * 昵称（可选）
     * 用于显示
     */
    private String nickname;

    /**
     * 头像URL
     * 存储头像图片的链接地址
     */
    private String avatar;

    /**
     * 账号状态
     * 0-禁用（账号被封禁）
     * 1-正常（可登录）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     * 插入时自动填充
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 插入和更新时自动填充
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
