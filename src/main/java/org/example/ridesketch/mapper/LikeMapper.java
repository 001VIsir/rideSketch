package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.Like;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
