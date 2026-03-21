package org.example.ridesketch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.ridesketch.dto.RouteVO;
import org.example.ridesketch.entity.Like;
import org.example.ridesketch.entity.Route;
import org.example.ridesketch.entity.User;
import org.example.ridesketch.mapper.CommentMapper;
import org.example.ridesketch.mapper.LikeMapper;
import org.example.ridesketch.mapper.RouteMapper;
import org.example.ridesketch.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock
    private RouteMapper routeMapper;

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CommunityServiceImpl communityService;

    @Test
    void getRouteListShouldBatchLoadUserAndLikeData() {
        Route route1 = buildRoute(1L, 100L, "路线1");
        Route route2 = buildRoute(2L, 101L, "路线2");
        Page<Route> routePage = new Page<>();
        routePage.setRecords(List.of(route1, route2));

        User user1 = new User();
        user1.setId(100L);
        user1.setUsername("alice");
        user1.setNickname("Alice");

        User user2 = new User();
        user2.setId(101L);
        user2.setUsername("bob");
        user2.setNickname("Bob");

        Like liked = new Like();
        liked.setRouteId(1L);
        liked.setUserId(500L);

        when(routeMapper.selectPage(any(Page.class), any())).thenReturn(routePage);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user1, user2));
        when(likeMapper.selectList(any())).thenReturn(List.of(liked));

        List<RouteVO> result = communityService.getRouteList(1, 10, 500L);

        assertEquals(2, result.size());
        assertTrue(Boolean.TRUE.equals(result.get(0).getLiked()));
        assertFalse(Boolean.TRUE.equals(result.get(1).getLiked()));
        assertEquals("Alice", result.get(0).getNickname());
        assertEquals("Bob", result.get(1).getNickname());

        verify(userMapper, times(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
        verify(likeMapper, times(1)).selectList(any());
    }

    @Test
    void toggleLikeShouldNotReduceLikesBelowZero() {
        Route route = buildRoute(9L, 200L, "不应负数点赞");
        route.setLikes(0);

        Like existingLike = new Like();
        existingLike.setId(88L);
        existingLike.setRouteId(9L);
        existingLike.setUserId(500L);

        when(routeMapper.selectById(9L)).thenReturn(route);
        when(likeMapper.selectOne(any())).thenReturn(existingLike);

        boolean liked = communityService.toggleLike(9L, 500L);

        assertFalse(liked);
        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeMapper).updateById(routeCaptor.capture());
        assertEquals(0, routeCaptor.getValue().getLikes());
    }

    private Route buildRoute(Long routeId, Long userId, String title) {
        Route route = new Route();
        route.setId(routeId);
        route.setUserId(userId);
        route.setTitle(title);
        route.setIsPublic(1);
        route.setLikes(0);
        route.setViews(0);
        return route;
    }
}
