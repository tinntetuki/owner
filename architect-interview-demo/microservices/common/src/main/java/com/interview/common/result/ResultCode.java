package com.interview.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回状态码枚举
 * 
 * @author interview
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    ERROR(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 方法不允许
     */
    METHOD_NOT_ALLOWED(405, "方法不允许"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 冲突
     */
    CONFLICT(409, "冲突"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(1000, "业务异常"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(1001, "数据不存在"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(1002, "数据已存在"),

    /**
     * 数据格式错误
     */
    DATA_FORMAT_ERROR(1003, "数据格式错误"),

    /**
     * 数据状态错误
     */
    DATA_STATE_ERROR(1004, "数据状态错误"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(2001, "用户不存在"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(2002, "用户已存在"),

    /**
     * 用户状态异常
     */
    USER_STATE_ERROR(2003, "用户状态异常"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(2004, "密码错误"),

    /**
     * 登录过期
     */
    LOGIN_EXPIRED(2005, "登录过期"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(2006, "权限不足"),

    /**
     * 商品不存在
     */
    PRODUCT_NOT_FOUND(3001, "商品不存在"),

    /**
     * 商品已下架
     */
    PRODUCT_OFFLINE(3002, "商品已下架"),

    /**
     * 库存不足
     */
    STOCK_NOT_ENOUGH(3003, "库存不足"),

    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND(4001, "订单不存在"),

    /**
     * 订单状态错误
     */
    ORDER_STATE_ERROR(4002, "订单状态错误"),

    /**
     * 订单已支付
     */
    ORDER_ALREADY_PAID(4003, "订单已支付"),

    /**
     * 订单已取消
     */
    ORDER_ALREADY_CANCELLED(4004, "订单已取消"),

    /**
     * 支付失败
     */
    PAYMENT_FAILED(5001, "支付失败"),

    /**
     * 支付超时
     */
    PAYMENT_TIMEOUT(5002, "支付超时"),

    /**
     * 支付金额错误
     */
    PAYMENT_AMOUNT_ERROR(5003, "支付金额错误"),

    /**
     * 秒杀未开始
     */
    SECKILL_NOT_STARTED(6001, "秒杀未开始"),

    /**
     * 秒杀已结束
     */
    SECKILL_ENDED(6002, "秒杀已结束"),

    /**
     * 秒杀商品不存在
     */
    SECKILL_PRODUCT_NOT_FOUND(6003, "秒杀商品不存在"),

    /**
     * 秒杀库存不足
     */
    SECKILL_STOCK_NOT_ENOUGH(6004, "秒杀库存不足"),

    /**
     * 重复秒杀
     */
    SECKILL_REPEAT(6005, "重复秒杀"),

    /**
     * 系统繁忙
     */
    SYSTEM_BUSY(9001, "系统繁忙，请稍后重试"),

    /**
     * 网络异常
     */
    NETWORK_ERROR(9002, "网络异常"),

    /**
     * 数据库异常
     */
    DATABASE_ERROR(9003, "数据库异常"),

    /**
     * 缓存异常
     */
    CACHE_ERROR(9004, "缓存异常"),

    /**
     * 消息队列异常
     */
    MQ_ERROR(9005, "消息队列异常"),

    /**
     * 第三方服务异常
     */
    THIRD_PARTY_ERROR(9006, "第三方服务异常");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态消息
     */
    private final String message;
}
