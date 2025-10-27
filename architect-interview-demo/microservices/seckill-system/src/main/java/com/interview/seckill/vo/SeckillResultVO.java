package com.interview.seckill.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀结果VO
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
public class SeckillResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 秒杀商品ID
     */
    private Long seckillProductId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 秒杀价格
     */
    private java.math.BigDecimal seckillPrice;

    /**
     * 总金额
     */
    private java.math.BigDecimal totalAmount;

    /**
     * 结果码
     */
    private String resultCode;

    /**
     * 时间戳
     */
    private Long timestamp;

    public SeckillResultVO() {
        this.timestamp = System.currentTimeMillis();
    }

    public SeckillResultVO(Boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public static SeckillResultVO success(String message) {
        return new SeckillResultVO(true, message);
    }

    public static SeckillResultVO success(String message, Long orderId) {
        SeckillResultVO vo = new SeckillResultVO(true, message);
        vo.setOrderId(orderId);
        return vo;
    }

    public static SeckillResultVO fail(String message) {
        return new SeckillResultVO(false, message);
    }

    public static SeckillResultVO fail(String message, String resultCode) {
        SeckillResultVO vo = new SeckillResultVO(false, message);
        vo.setResultCode(resultCode);
        return vo;
    }
}
