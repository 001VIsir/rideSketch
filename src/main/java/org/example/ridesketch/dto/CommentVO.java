package org.example.ridesketch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象（VO）
 *
 * 用于返回给前端的评论详细信息
 * 包含评论本身的字段以及关联的用户信息和回复列表
 *
 * @author rideSketch
 */
@Data
public class CommentVO {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 被评论的路线ID
     */
    private Long routeId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论用户名
     */
    private String username;

    /**
     * 评论用户昵称
     */
    private String nickname;

    /**
     * 评论用户头像
     */
    private String avatar;

    /**
     * 父评论ID（用于楼中楼）
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;

    /**
     * 回复列表（楼中楼回复）
     */
    private List<CommentVO> replies;
}
