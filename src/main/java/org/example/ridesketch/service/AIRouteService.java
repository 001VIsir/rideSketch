package org.example.ridesketch.service;

import org.example.ridesketch.dto.AIRoutePlanningRequest;
import org.example.ridesketch.dto.AIRoutePlanningResult;

/**
 * AI路线规划服务接口
 */
public interface AIRouteService {

    /**
     * AI智能路线规划
     *
     * @param request AI路线规划请求
     * @return AI路线规划结果
     */
    AIRoutePlanningResult planAIRoute(AIRoutePlanningRequest request);
}
