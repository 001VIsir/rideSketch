package org.example.ridesketch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * rideSketch 骑迹 - 智能骑行路线规划与社区分享平台
 *
 * Spring Boot 应用入口类
 * 负责启动整个后端服务
 *
 * @author rideSketch
 * @version 1.0.0
 */
@SpringBootApplication
public class RideSketchApplication {

    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RideSketchApplication.class, args);
    }

}
