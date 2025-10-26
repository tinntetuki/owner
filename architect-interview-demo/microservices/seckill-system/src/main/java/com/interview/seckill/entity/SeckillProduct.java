package com.interview.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("seckill_product")
public class SeckillProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 秒杀商品ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 商品图片
     */
    @TableField("product_image")
    private String productImage;

    /**
     * 原价
     */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /**
     * 秒杀价
     */
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存
     */
    @TableField("seckill_stock")
    private Integer seckillStock;

    /**
     * 已售数量
     */
    @TableField("sold_count")
    private Integer soldCount;

    /**
     * 秒杀开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 秒杀结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 秒杀状态：0-未开始，1-进行中，2-已结束
     */
    @TableField("status")
    private Integer status;

    /**
     * 限购数量
     */
    @TableField("limit_count")
    private Integer limitCount;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
