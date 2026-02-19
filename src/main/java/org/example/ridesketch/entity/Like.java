package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`like`")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long routeId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
