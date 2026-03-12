package org.example.ridesketch.service;

import org.example.ridesketch.dto.CommentVO;
import org.example.ridesketch.dto.RoutePublishRequest;
import org.example.ridesketch.dto.RouteVO;

import java.util.List;

public interface CommunityService {

    /**
     * 发布路线
     *
     * @param userId  用户ID
     * @param request 发布请求
     * @return 发布的路线
     */
    RouteVO publishRoute(Long userId, RoutePublishRequest request);

    /**
     * 获取路线列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param userId   当前登录用户ID（可选，用于判断是否点赞）
     * @return 路线列表
     */
    List<RouteVO> getRouteList(int page, int size, Long userId);

    /**
     * 获取我的路线列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 路线列表
     */
    List<RouteVO> getMyRoutes(Long userId, int page, int size);

    /**
     * 获取路线详情
     *
     * @param routeId 路线ID
     * @param userId  当前登录用户ID（可选）
     * @return 路线详情
     */
    RouteVO getRouteDetail(Long routeId, Long userId);

    /**
     * 更新路线
     *
     * @param routeId 路线ID
     * @param userId  用户ID
     * @param request 更新请求
     * @return 更新后的路线
     */
    RouteVO updateRoute(Long routeId, Long userId, RoutePublishRequest request);

    /**
     * 删除路线
     *
     * @param routeId 路线ID
     * @param userId  用户ID
     * @return 是否删除成功
     */
    boolean deleteRoute(Long routeId, Long userId);

    /**
     * 点赞/取消点赞
     *
     * @param routeId 路线ID
     * @param userId  用户ID
     * @return 点赞状态（true-已点赞，false-未点赞）
     */
    boolean toggleLike(Long routeId, Long userId);

    /**
     * 获取路线评论
     *
     * @param routeId 路线ID
     * @return 评论列表
     */
    List<CommentVO> getComments(Long routeId);

    /**
     * 添加评论
     *
     * @param routeId  路线ID
     * @param userId   用户ID
     * @param content  评论内容
     * @param parentId 父评论ID（可选）
     * @return 评论
     */
    CommentVO addComment(Long routeId, Long userId, String content, Long parentId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);

    /**
     * 增加浏览量
     *
     * @param routeId 路线ID
     */
    void incrementViews(Long routeId);
}
