package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.Route;

/**
 * 路线Mapper接口
 *
 * 继承MyBatis-Plus的BaseMapper，提供骑行路线的基础CRUD操作
 *
 * @author rideSketch
 */
@Mapper
public interface RouteMapper extends BaseMapper<Route> {
}
