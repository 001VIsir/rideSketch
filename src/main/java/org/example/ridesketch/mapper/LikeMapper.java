package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.Like;

/**
 * 点赞Mapper接口
 *
 * 继承MyBatis-Plus的BaseMapper，提供点赞记录的基础CRUD操作
 *
 * @author rideSketch
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
