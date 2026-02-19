package org.example.ridesketch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private Long routeId;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private Long parentId;

    private String content;

    private LocalDateTime createTime;

    private List<CommentVO> replies;
}
