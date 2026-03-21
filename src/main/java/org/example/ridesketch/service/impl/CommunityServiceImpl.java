package org.example.ridesketch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.dto.CommentVO;
import org.example.ridesketch.dto.RoutePublishRequest;
import org.example.ridesketch.dto.RouteVO;
import org.example.ridesketch.entity.Comment;
import org.example.ridesketch.entity.Like;
import org.example.ridesketch.entity.Route;
import org.example.ridesketch.entity.User;
import org.example.ridesketch.mapper.CommentMapper;
import org.example.ridesketch.mapper.LikeMapper;
import org.example.ridesketch.mapper.RouteMapper;
import org.example.ridesketch.mapper.UserMapper;
import org.example.ridesketch.service.CommunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区服务实现类
 * 负责骑行路线的发布、浏览、点赞、评论等社区功能
 * 使用MyBatis-Plus与数据库交互，支持分页查询和事务处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final RouteMapper routeMapper;
    private final LikeMapper likeMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public RouteVO publishRoute(Long userId, RoutePublishRequest request) {
        Route route = new Route();
        route.setUserId(userId);
        route.setTitle(request.getTitle());
        route.setDescription(request.getDescription());
        route.setStartPoint(request.getStartPoint());
        route.setEndPoint(request.getEndPoint());
        route.setWaypoints(request.getWaypoints());
        route.setRoutePath(request.getRoutePath());
        route.setTotalDistance(request.getTotalDistance());
        route.setEstimatedTime(request.getEstimatedTime());
        route.setDifficulty(request.getDifficulty());
        route.setTags(request.getTags());
        route.setLikes(0);
        route.setViews(0);
        route.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 1);

        routeMapper.insert(route);

        return convertToVO(route, false);
    }

    @Override
    public List<RouteVO> getRouteList(int page, int size, Long userId) {
        Page<Route> routePage = new Page<>(page, size);
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getIsPublic, 1)
                .orderByDesc(Route::getCreateTime);

        Page<Route> result = routeMapper.selectPage(routePage, wrapper);
        List<Route> routes = result.getRecords();
        Map<Long, User> userMap = getUserMapByRoutes(routes);
        Set<Long> likedRouteIds = getLikedRouteIds(routes, userId);

        return routes.stream()
                .map(route -> convertToVO(route, userMap, likedRouteIds.contains(route.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<RouteVO> getMyRoutes(Long userId, int page, int size) {
        Page<Route> routePage = new Page<>(page, size);
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getUserId, userId)
                .orderByDesc(Route::getCreateTime);

        Page<Route> result = routeMapper.selectPage(routePage, wrapper);
        List<Route> routes = result.getRecords();
        Map<Long, User> userMap = getUserMapByRoutes(routes);

        return routes.stream()
                .map(route -> convertToVO(route, userMap, false))
                .collect(Collectors.toList());
    }

    @Override
    public RouteVO getRouteDetail(Long routeId, Long userId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        boolean liked = userId != null && hasLiked(routeId, userId);
        return convertToVO(route, liked);
    }

    @Override
    @Transactional
    public RouteVO updateRoute(Long routeId, Long userId, RoutePublishRequest request) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        if (!route.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此路线");
        }

        if (request.getTitle() != null) {
            route.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            route.setDescription(request.getDescription());
        }
        if (request.getStartPoint() != null) {
            route.setStartPoint(request.getStartPoint());
        }
        if (request.getEndPoint() != null) {
            route.setEndPoint(request.getEndPoint());
        }
        if (request.getWaypoints() != null) {
            route.setWaypoints(request.getWaypoints());
        }
        if (request.getRoutePath() != null) {
            route.setRoutePath(request.getRoutePath());
        }
        if (request.getTotalDistance() != null) {
            route.setTotalDistance(request.getTotalDistance());
        }
        if (request.getEstimatedTime() != null) {
            route.setEstimatedTime(request.getEstimatedTime());
        }
        if (request.getDifficulty() != null) {
            route.setDifficulty(request.getDifficulty());
        }
        if (request.getTags() != null) {
            route.setTags(request.getTags());
        }
        if (request.getIsPublic() != null) {
            route.setIsPublic(request.getIsPublic());
        }

        routeMapper.updateById(route);

        return convertToVO(route, false);
    }

    @Override
    @Transactional
    public boolean deleteRoute(Long routeId, Long userId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return false;
        }

        if (!route.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此路线");
        }

        // 删除相关的点赞和评论
        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getRouteId, routeId);
        likeMapper.delete(likeWrapper);

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getRouteId, routeId);
        commentMapper.delete(commentWrapper);

        return routeMapper.deleteById(routeId) > 0;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long routeId, Long userId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在");
        }

        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getRouteId, routeId)
                .eq(Like::getUserId, userId);

        Like existingLike = likeMapper.selectOne(wrapper);

        if (existingLike != null) {
            // 取消点赞
            likeMapper.deleteById(existingLike.getId());
            int currentLikes = route.getLikes() == null ? 0 : route.getLikes();
            route.setLikes(Math.max(0, currentLikes - 1));
            routeMapper.updateById(route);
            return false;
        } else {
            // 添加点赞
            Like like = new Like();
            like.setRouteId(routeId);
            like.setUserId(userId);
            likeMapper.insert(like);
            int currentLikes = route.getLikes() == null ? 0 : route.getLikes();
            route.setLikes(currentLikes + 1);
            routeMapper.updateById(route);
            return true;
        }
    }

    @Override
    public List<CommentVO> getComments(Long routeId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRouteId, routeId)
                .eq(Comment::getParentId, 0)
                .orderByAsc(Comment::getCreateTime);

        List<Comment> rootComments = commentMapper.selectList(wrapper);

        // 获取所有子评论
        LambdaQueryWrapper<Comment> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Comment::getRouteId, routeId)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> allComments = commentMapper.selectList(allWrapper);

        // 构建用户ID到用户信息的映射
        List<Long> userIds = allComments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        final Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        // 构建评论VO列表
        List<CommentVO> commentVOs = new ArrayList<>();
        Map<Long, List<Comment>> parentMap = allComments.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Comment::getParentId));

        for (Comment comment : rootComments) {
            CommentVO vo = convertToCommentVO(comment, userMap);
            // 添加回复
            List<Comment> replies = parentMap.get(comment.getId());
            if (replies != null) {
                vo.setReplies(replies.stream()
                        .map(c -> convertToCommentVO(c, userMap))
                        .collect(Collectors.toList()));
            }
            commentVOs.add(vo);
        }

        return commentVOs;
    }

    @Override
    @Transactional
    public CommentVO addComment(Long routeId, Long userId, String content, Long parentId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new RuntimeException("路线不存在");
        }

        Comment comment = new Comment();
        comment.setRouteId(routeId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId != null ? parentId : 0L);

        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setRouteId(comment.getRouteId());
        vo.setUserId(comment.getUserId());
        vo.setUsername(user != null ? user.getUsername() : "");
        vo.setNickname(user != null ? user.getNickname() : "");
        vo.setAvatar(user != null ? user.getAvatar() : "");
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());

        return vo;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }

        // 删除子评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, commentId);
        commentMapper.delete(wrapper);

        return commentMapper.deleteById(commentId) > 0;
    }

    @Override
    @Transactional
    public void incrementViews(Long routeId) {
        Route route = routeMapper.selectById(routeId);
        if (route != null) {
            route.setViews(route.getViews() + 1);
            routeMapper.updateById(route);
        }
    }

    private boolean hasLiked(Long routeId, Long userId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getRouteId, routeId)
                .eq(Like::getUserId, userId);
        return likeMapper.selectCount(wrapper) > 0;
    }

    private Map<Long, User> getUserMapByRoutes(List<Route> routes) {
        if (routes == null || routes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> userIds = routes.stream()
                .map(Route::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Set<Long> getLikedRouteIds(List<Route> routes, Long userId) {
        if (routes == null || routes.isEmpty() || userId == null) {
            return Collections.emptySet();
        }

        List<Long> routeIds = routes.stream()
                .map(Route::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (routeIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getUserId, userId)
                .in(Like::getRouteId, routeIds);

        return likeMapper.selectList(likeWrapper).stream()
                .map(Like::getRouteId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private RouteVO convertToVO(Route route, boolean liked) {
        Map<Long, User> userMap = getUserMapByRoutes(Collections.singletonList(route));
        return convertToVO(route, userMap, liked);
    }

    private RouteVO convertToVO(Route route, Map<Long, User> userMap, boolean liked) {
        RouteVO vo = new RouteVO();
        vo.setId(route.getId());
        vo.setUserId(route.getUserId());
        vo.setTitle(route.getTitle());
        vo.setDescription(route.getDescription());
        vo.setStartPoint(route.getStartPoint());
        vo.setEndPoint(route.getEndPoint());
        vo.setWaypoints(route.getWaypoints());
        vo.setRoutePath(route.getRoutePath());
        vo.setTotalDistance(route.getTotalDistance());
        vo.setEstimatedTime(route.getEstimatedTime());
        vo.setDifficulty(route.getDifficulty());
        vo.setTags(route.getTags());
        vo.setLikes(route.getLikes());
        vo.setViews(route.getViews());
        vo.setIsPublic(route.getIsPublic());
        vo.setLiked(liked);
        vo.setCreateTime(route.getCreateTime());
        vo.setUpdateTime(route.getUpdateTime());

        // 获取用户信息
        User user = userMap.get(route.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        return vo;
    }

    private CommentVO convertToCommentVO(Comment comment, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setRouteId(comment.getRouteId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        return vo;
    }
}
