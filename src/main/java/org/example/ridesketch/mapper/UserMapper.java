package org.example.ridesketch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ridesketch.entity.User;

/**
 * 用户Mapper接口
 *
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 无需编写SQL，MyBatis-Plus会自动生成
 *
 * 可用方法：
 * - selectById(id) - 根据ID查询
 * - selectOne(wrapper) - 条件查询单条
 * - selectList(wrapper) - 条件查询列表
 * - insert(entity) - 插入
 * - updateById(entity) - 更新
 * - deleteById(id) - 删除
 *
 * @author rideSketch
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
