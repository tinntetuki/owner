package com.interview.seckill.service;

import com.interview.seckill.dto.SeckillRequest;
import com.interview.seckill.vo.SeckillResultVO;

/**
 * 秒杀服务接口
 * 
 * @author interview
 * @since 2024-01-01
 */
public interface SeckillService {

    /**
     * 执行秒杀
     */
    SeckillResultVO executeSeckill(SeckillRequest request);

    /**
     * 预减库存
     */
    boolean preReduceStock(Long seckillProductId, Integer quantity);

    /**
     * 恢复库存
     */
    void restoreStock(Long seckillProductId, Integer quantity);

    /**
     * 检查用户是否已秒杀
     */
    boolean checkUserSeckilled(Long userId, Long seckillProductId);

    /**
     * 记录用户秒杀
     */
    void recordUserSeckill(Long userId, Long seckillProductId);

    /**
     * 获取秒杀结果
     */
    SeckillResultVO getSeckillResult(Long userId, Long seckillProductId);

    /**
     * 异步处理秒杀订单
     */
    void asyncProcessSeckillOrder(SeckillRequest request);

    /**
     * 检查秒杀商品状态
     */
    boolean checkSeckillProductStatus(Long seckillProductId);

    /**
     * 获取秒杀商品库存
     */
    Integer getSeckillProductStock(Long seckillProductId);

    /**
     * 预热秒杀商品
     */
    void warmUpSeckillProduct(Long seckillProductId);

    /**
     * 清理过期秒杀数据
     */
    void cleanExpiredSeckillData();
}
