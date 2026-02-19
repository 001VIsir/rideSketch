package org.example.ridesketch.service;

import org.example.ridesketch.dto.PatternRouteRequest;
import org.example.ridesketch.dto.PatternRouteResult;

/**
 * 图案路书服务接口
 */
public interface PatternRouteService {

    /**
     * 生成图案路书
     *
     * @param request 图案路书请求
     * @return 图案路书结果
     */
    PatternRouteResult generatePatternRoute(PatternRouteRequest request);
}
