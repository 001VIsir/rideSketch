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
 * <p>
 * 处理骑行路线的社区分享功能，包括路线的发布、浏览、编辑、删除，
 * 以及点赞、评论等社交互动功能。所有接口前缀为 /api/community
 *
 * 功能说明：
 * <ul>
 *   <li>路线发布与编辑：用户可以发布自己的骑行路线供他人浏览</li>
 *   <li>路线浏览：查看所有公开路线、他人路线详情</li>
 *   <li>社交互动：点赞、评论功能增强用户互动</li>
 *   <li>个人中心：查看自己发布的路线</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0.0
 * @see CommunityService
 * @see RouteVO
 * @see CommentVO
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 发布骑行路线接口
     * <p>
     * 将用户规划的骑行路线发布到社区，供其他用户浏览和互动。
     * 需要用户登录后才能使用此功能。
     *
     * @api POST /api/community/route
     * @param userDetails 当前登录用户的安全详情，Spring Security自动注入
     * @param request 路线发布请求体
     *                - title: 路线标题，必填
     *                - description: 路线描述（可选）
     *                - distance: 路线距离（可选）
     *                - duration: 预计骑行时间（可选）
     *                - points: 路线轨迹点列表，必填
     *                - tags: 标签列表（可选）
     *                - isPublic: 是否公开（默认true）
     * @return Result 包含RouteVO
     *         - 发布成功返回路线详情，包括id、title、author等信息
     * @throws SecurityException 用户未登录时抛出
     * @see RoutePublishRequest
     * @see RouteVO
     */
    @PostMapping("/route")
    public Result<RouteVO> publishRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RoutePublishRequest request) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        // 校验路线标题不能为空
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return Result.error("标题不能为空");
        }

        log.info("用户 {} 发布路线: {}", userDetails.getId(), request.getTitle());
        // 调用服务层发布路线
        RouteVO route = communityService.publishRoute(userDetails.getId(), request);
        return Result.success(route);
    }

    /**
     * 获取路线列表接口
     * <p>
     * 分页获取社区中所有已发布的骑行路线列表。
     * 支持查看他人路线时是否点赞的状态（需登录）。
     *
     * @api GET /api/community/routes?page=1&size=10
     * @param userDetails 当前登录用户（可选），用于获取点赞状态
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @return Result 包含List&lt;RouteVO&gt;
     *         - 路线列表，每条包含id、title、author、likes、views等信息
     * @see RouteVO
     */
    @GetMapping("/routes")
    public Result<List<RouteVO>> getRouteList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 获取当前用户ID（如果已登录），用于判断点赞状态
        Long userId = userDetails != null ? userDetails.getId() : null;
        // 调用服务层获取路线列表
        List<RouteVO> routes = communityService.getRouteList(page, size, userId);
        return Result.success(routes);
    }

    /**
     * 获取当前用户路线列表接口
     * <p>
     * 获取当前登录用户自己发布的所有路线。
     *
     * @api GET /api/community/my-routes?page=1&size=10
     * @param userDetails 当前登录用户，必填
     * @param page 页码，默认1
     * @param page size 每页数量，默认10
     * @return Result 包含List&lt;RouteVO&gt;
     *         - 当前用户发布的路线列表
     * @throws SecurityException 用户未登录时抛出
     * @see RouteVO
     */
    @GetMapping("/my-routes")
    public Result<List<RouteVO>> getMyRoutes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        // 调用服务层获取当前用户的路线列表
        List<RouteVO> routes = communityService.getMyRoutes(userDetails.getId(), page, size);
        return Result.success(routes);
    }

    /**
     * 获取路线详情接口
     * <p>
     * 获取某条路线的详细信息，包括路线轨迹、发布者信息、点赞数等。
     * 每次查看详情会增加路线浏览量。
     *
     * @api GET /api/community/route/{id}
     * @param userDetails 当前登录用户（可选），用于获取点赞状态
     * @param routeId 路线ID，URL路径参数
     * @return Result 包含RouteVO
     *         - 路线详细信息，包括轨迹点points、作者信息、点赞数、评论数等
     * @throws RuntimeException 路线不存在时抛出
     * @see RouteVO
     */
    @GetMapping("/route/{id}")
    public Result<RouteVO> getRouteDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        // 获取路线详情
        RouteVO route = communityService.getRouteDetail(routeId, userId);

        // 路线不存在时返回错误
        if (route == null) {
            return Result.error("路线不存在");
        }

        // 增加浏览量
        communityService.incrementViews(routeId);

        return Result.success(route);
    }

    /**
     * 更新路线接口
     * <p>
     * 更新已发布路线的标题、描述等信息。只有路线作者可以更新自己的路线。
     *
     * @api PUT /api/community/route/{id}
     * @param userDetails 当前登录用户，必填
     * @param routeId 路线ID，URL路径参数
     * @param request 更新请求体（同发布请求）
     * @return Result 包含RouteVO
     *         - 更新后的路线详情
     * @throws SecurityException 用户未登录或非作者时抛出
     * @see RoutePublishRequest
     */
    @PutMapping("/route/{id}")
    public Result<RouteVO> updateRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId,
            @RequestBody RoutePublishRequest request) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            // 调用服务层更新路线
            RouteVO route = communityService.updateRoute(routeId, userDetails.getId(), request);
            if (route == null) {
                return Result.error("路线不存在");
            }
            return Result.success(route);
        } catch (RuntimeException e) {
            // 返回服务层抛出的异常信息（如无权限等）
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除路线接口
     * <p>
     * 删除已发布的骑行路线。只有路线作者可以删除自己的路线。
     *
     * @api DELETE /api/community/route/{id}
     * @param userDetails 当前登录用户，必填
     * @param routeId 路线ID，URL路径参数
     * @return Result 包含Void
     *         - 删除成功返回null
     * @throws SecurityException 用户未登录或非作者时抛出
     */
    @DeleteMapping("/route/{id}")
    public Result<Void> deleteRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            // 调用服务层删除路线
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
     * 点赞/取消点赞接口
     * <p>
     * 对路线进行点赞或取消点赞。已点赞则取消，未点赞则添加。
     *
     * @api POST /api/community/route/{id}/like
     * @param userDetails 当前登录用户，必填
     * @param routeId 路线ID，URL路径参数
     * @return Result 包含Map
     *         - liked: 当前用户的点赞状态（true/false）
     *         - likes: 路线当前的总点赞数
     * @throws SecurityException 用户未登录时抛出
     */
    @PostMapping("/route/{id}/like")
    public Result<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            // 切换点赞状态
            boolean liked = communityService.toggleLike(routeId, userDetails.getId());
            // 获取更新后的路线信息以获取最新点赞数
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
     * 获取路线评论列表接口
     * <p>
     * 获取指定路线的所有评论，不要求用户登录。
     *
     * @api GET /api/community/route/{id}/comments
     * @param routeId 路线ID，URL路径参数
     * @return Result 包含List&lt;CommentVO&gt;
     *         - 评论列表，按时间正序排列
     * @see CommentVO
     */
    @GetMapping("/route/{id}/comments")
    public Result<List<CommentVO>> getComments(@PathVariable("id") Long routeId) {
        // 获取路线评论列表
        List<CommentVO> comments = communityService.getComments(routeId);
        return Result.success(comments);
    }

    /**
     * 添加评论接口
     * <p>
     * 为指定路线添加评论。支持回复功能，可以针对某条评论进行回复。
     *
     * @api POST /api/community/route/{id}/comment
     * @param userDetails 当前登录用户，必填
     * @param routeId 路线ID，URL路径参数
     * @param request 评论请求体
     *               - content: 评论内容，必填
     *               - parentId: 父评论ID（可选），用于回复
     * @return Result 包含CommentVO
     *         - 添加成功的评论详情
     * @throws SecurityException 用户未登录时抛出
     */
    @PostMapping("/route/{id}/comment")
    public Result<CommentVO> addComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long routeId,
            @RequestBody Map<String, Object> request) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        // 获取评论内容
        String content = (String) request.get("content");
        if (content == null || content.isBlank()) {
            return Result.error("评论内容不能为空");
        }

        // 解析父评论ID（用于回复功能）
        Long parentId = request.get("parentId") != null
                ? Long.parseLong(request.get("parentId").toString())
                : null;

        try {
            // 调用服务层添加评论
            CommentVO comment = communityService.addComment(routeId, userDetails.getId(), content, parentId);
            return Result.success(comment);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除评论接口
     * <p>
     * 删除指定评论。只有评论作者可以删除自己的评论。
     *
     * @api DELETE /api/community/comment/{id}
     * @param userDetails 当前登录用户，必填
     * @param commentId 评论ID，URL路径参数
     * @return Result 包含Void
     *         - 删除成功返回null
     * @throws SecurityException 用户未登录或非作者时抛出
     */
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long commentId) {
        // 校验用户登录状态
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        try {
            // 调用服务层删除评论
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
