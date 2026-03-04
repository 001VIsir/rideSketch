package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 *
 * 对应数据库表：comment
 * 存储用户对路线的评论信息，支持楼中楼回复
 *
 * @author rideSketch
 */
@Data
@TableName("comment")
public class Comment {

    /**
     * 评论ID（主键、自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被评论的路线ID（外键关联route表）
     */
    private Long routeId;

    /**
     * 评论用户ID（外键关联user表）
     */
    private Long userId;

    /**
     * 父评论ID（用于楼中楼回复）
     * 0或null表示顶级评论
     * 非0表示回复某条评论
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
