package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.Comment;

/**
 * 评论Mapper接口
 *
 * 继承MyBatis-Plus的BaseMapper，提供评论的基础CRUD操作
 *
 * @author rideSketch
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
