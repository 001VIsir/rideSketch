package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞实体类
 *
 * 对应数据库表：like
 * 记录用户对路线的点赞信息
 *
 * 注意：表名需要用反引号包裹，因为LIKE是SQL关键字
 *
 * @author rideSketch
 */
@Data
@TableName("`like`")
public class Like {

    /**
     * 点赞记录ID（主键、自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点赞用户ID（外键关联user表）
     */
    private Long userId;

    /**
     * 被点赞的路线ID（外键关联route表）
     */
    private Long routeId;

    /**
     * 点赞时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
