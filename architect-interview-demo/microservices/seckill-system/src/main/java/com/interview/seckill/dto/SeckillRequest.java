package com.interview.seckill.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 秒杀请求DTO
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
public class SeckillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 秒杀商品ID
     */
    @NotNull(message = "秒杀商品ID不能为空")
    private Long seckillProductId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 用户IP
     */
    private String userIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求时间戳
     */
    private Long timestamp;
}
