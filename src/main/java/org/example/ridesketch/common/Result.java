package org.example.ridesketch.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 *
 * 用于封装API接口的返回数据，提供统一的响应格式
 *
 * @param <T> 响应数据的泛型类型
 * @author rideSketch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 请求是否成功
     * true: 成功 false: 失败
     */
    private boolean success;

    /**
     * 响应消息
     * 成功时返回"操作成功"，失败时返回具体错误信息
     */
    private String message;

    /**
     * 响应数据
     * 泛型类型，可以是任意对象
     */
    private T data;

    /**
     * 成功响应（带数据）
     *
     * @param data 要返回的数据
     * @param <T> 泛型类型
     * @return 成功的结果对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 自定义消息
     * @param data 要返回的数据
     * @param <T> 泛型类型
     * @return 成功的结果对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }

    /**
     * 错误响应
     *
     * @param message 错误消息
     * @param <T> 泛型类型
     * @return 失败的结果对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }

    /**
     * 错误响应（带数据）
     *
     * @param message 错误消息
     * @param data 附加数据
     * @param <T> 泛型类型
     * @return 失败的结果对象
     */
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(false, message, data);
    }
}
