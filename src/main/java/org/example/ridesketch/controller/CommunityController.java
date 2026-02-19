package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.dto.CommentVO;
import org.example.ridesketch.dto.RoutePublishRequest;
import org.example.ridesketch.dto.RouteVO;
import org.example.ridesketch.security.CustomUserDetails;
import org.example.ridesketch.service.CommunityService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社区控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 发布路线
     */
    @PostMapping("/route")
    public Result<RouteVO> publishRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RoutePublishRequest request) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return Result.error("标题不能为空");
        }

        log.info("用户 {} 发布路线: {}", userDetails.getId(), request.getTitle());
        RouteVO route = communityService.publishRoute(userDetails.getId(), request);
        return Result.success(route);
    }

    /**
     * 获取路线列表
     */
    @GetMapping("/routes")
    public Result<List<RouteVO>> getRouteList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        List<RouteVO> routes = communityService.getRouteList(page, size, userId);
        return Result.success(routes);
    }

    /**
     * 获取我的路线列表
     */
    @GetMapping("/my-routes")
    public Result<List<RouteVO>> getMyRoutes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        List<RouteVO> routes = communityService.getMyRoutes(userDetails.getId(), page, size);
        return Result.success(routes);
    }

    /**
     * 获取路线详情
     */
    @GetMapping("/route/{id}")
    public Result<RouteVO> getRouteDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        RouteVO route = communityService.getRouteDetail(routeId, userId);

        if (route == null) {
            return Result.error("路线不存在");
        }

        // 增加浏览量
        communityService.incrementViews(routeId);

        return Result.success(route);
    }

    /**
     * 更新路线
     */
    @PutMapping("/route/{id}")
    public Result<RouteVO> updateRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId,
            @RequestBody RoutePublishRequest request) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            RouteVO route = communityService.updateRoute(routeId, userDetails.getId(), request);
            if (route == null) {
                return Result.error("路线不存在");
            }
            return Result.success(route);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除路线
     */
    @DeleteMapping("/route/{id}")
    public Result<Void> deleteRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            boolean success = communityService.deleteRoute(routeId, userDetails.getId());
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("路线不存在");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/route/{id}/like")
    public Result<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            boolean liked = communityService.toggleLike(routeId, userDetails.getId());
            RouteVO route = communityService.getRouteDetail(routeId, userDetails.getId());
            return Result.success(Map.of(
                    "liked", liked,
                    "likes", route != null ? route.getLikes() : 0
            ));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取路线评论
     */
    @GetMapping("/route/{id}/comments")
    public Result<List<CommentVO>> getComments(@PathVariable("id") Long routeId) {
        List<CommentVO> comments = communityService.getComments(routeId);
        return Result.success(comments);
    }

    /**
     * 添加评论
     */
    @PostMapping("/route/{id}/comment")
    public Result<CommentVO> addComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId,
            @RequestBody Map<String, Object> request) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        String content = (String) request.get("content");
        if (content == null || content.isBlank()) {
            return Result.error("评论内容不能为空");
        }

        Long parentId = request.get("parentId") != null
                ? Long.parseLong(request.get("parentId").toString())
                : null;

        try {
            CommentVO comment = communityService.addComment(routeId, userDetails.getId(), content, parentId);
            return Result.success(comment);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long commentId) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            boolean success = communityService.deleteComment(commentId, userDetails.getId());
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("评论不存在");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
