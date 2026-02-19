package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.Comment;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
