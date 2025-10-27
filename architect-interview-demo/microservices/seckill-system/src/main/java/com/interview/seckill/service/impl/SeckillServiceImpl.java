package com.interview.seckill.service.impl;

import com.google.common.util.concurrent.RateLimiter;
import com.interview.common.exception.BusinessException;
import com.interview.common.result.ResultCode;
import com.interview.seckill.dto.SeckillRequest;
import com.interview.seckill.entity.SeckillOrder;
import com.interview.seckill.entity.SeckillProduct;
import com.interview.seckill.mapper.SeckillOrderMapper;
import com.interview.seckill.mapper.SeckillProductMapper;
import com.interview.seckill.service.SeckillService;
import com.interview.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现类
 * 
 * @author interview
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillProductMapper seckillProductMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    // 限流器：每秒允许1000个请求
    private final RateLimiter rateLimiter = RateLimiter.create(1000.0);

    // Redis键前缀
    private static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    private static final String SECKILL_USER_PREFIX = "seckill:user:";
    private static final String SECKILL_PRODUCT_PREFIX = "seckill:product:";

    @Override
    public SeckillResultVO executeSeckill(SeckillRequest request) {
        log.info("执行秒杀: userId={}, seckillProductId={}, quantity={}", 
                request.getUserId(), request.getSeckillProductId(), request.getQuantity());

        // 1. 限流检查
        if (!rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            log.warn("限流拦截: userId={}, seckillProductId={}", 
                    request.getUserId(), request.getSeckillProductId());
            return SeckillResultVO.fail("系统繁忙，请稍后重试");
        }

        // 2. 检查用户是否已秒杀
        if (checkUserSeckilled(request.getUserId(), request.getSeckillProductId())) {
            log.warn("用户重复秒杀: userId={}, seckillProductId={}", 
                    request.getUserId(), request.getSeckillProductId());
            return SeckillResultVO.fail("您已经参与过该商品的秒杀");
        }

        // 3. 检查秒杀商品状态
        if (!checkSeckillProductStatus(request.getSeckillProductId())) {
            log.warn("秒杀商品状态异常: seckillProductId={}", request.getSeckillProductId());
            return SeckillResultVO.fail("秒杀商品状态异常");
        }

        // 4. 预减库存
        if (!preReduceStock(request.getSeckillProductId(), request.getQuantity())) {
            log.warn("库存不足: seckillProductId={}, quantity={}", 
                    request.getSeckillProductId(), request.getQuantity());
            return SeckillResultVO.fail("库存不足");
        }

        try {
            // 5. 记录用户秒杀
            recordUserSeckill(request.getUserId(), request.getSeckillProductId());

            // 6. 异步处理秒杀订单
            asyncProcessSeckillOrder(request);

            log.info("秒杀成功: userId={}, seckillProductId={}", 
                    request.getUserId(), request.getSeckillProductId());
            return SeckillResultVO.success("秒杀成功，请等待订单处理");

        } catch (Exception e) {
            log.error("秒杀处理异常: userId={}, seckillProductId={}", 
                    request.getUserId(), request.getSeckillProductId(), e);
            // 恢复库存
            restoreStock(request.getSeckillProductId(), request.getQuantity());
            return SeckillResultVO.fail("秒杀失败，请重试");
        }
    }

    @Override
    public boolean preReduceStock(Long seckillProductId, Integer quantity) {
        String stockKey = SECKILL_STOCK_PREFIX + seckillProductId;
        
        try {
            // 使用Lua脚本保证原子性
            String luaScript = 
                "local stock = redis.call('get', KEYS[1]) " +
                "if stock == false then " +
                "    return 0 " +
                "end " +
                "local stockNum = tonumber(stock) " +
                "local quantity = tonumber(ARGV[1]) " +
                "if stockNum >= quantity then " +
                "    redis.call('decrby', KEYS[1], quantity) " +
                "    return 1 " +
                "else " +
                "    return 0 " +
                "end";

            Long result = (Long) redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>) connection -> 
                    connection.eval(luaScript.getBytes(), 
                        org.springframework.data.redis.connection.RedisStringCommands.ReturnType.INTEGER, 
                        1, stockKey.getBytes(), quantity.toString().getBytes())
            );

            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("预减库存异常: seckillProductId={}, quantity={}", seckillProductId, quantity, e);
            return false;
        }
    }

    @Override
    public void restoreStock(Long seckillProductId, Integer quantity) {
        String stockKey = SECKILL_STOCK_PREFIX + seckillProductId;
        try {
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.info("恢复库存成功: seckillProductId={}, quantity={}", seckillProductId, quantity);
        } catch (Exception e) {
            log.error("恢复库存异常: seckillProductId={}, quantity={}", seckillProductId, quantity, e);
        }
    }

    @Override
    public boolean checkUserSeckilled(Long userId, Long seckillProductId) {
        String userKey = SECKILL_USER_PREFIX + seckillProductId + ":" + userId;
        try {
            return redisTemplate.hasKey(userKey);
        } catch (Exception e) {
            log.error("检查用户秒杀状态异常: userId={}, seckillProductId={}", userId, seckillProductId, e);
            return false;
        }
    }

    @Override
    public void recordUserSeckill(Long userId, Long seckillProductId) {
        String userKey = SECKILL_USER_PREFIX + seckillProductId + ":" + userId;
        try {
            // 设置过期时间为秒杀结束时间
            redisTemplate.opsForValue().set(userKey, "1", 1, TimeUnit.HOURS);
            log.info("记录用户秒杀成功: userId={}, seckillProductId={}", userId, seckillProductId);
        } catch (Exception e) {
            log.error("记录用户秒杀异常: userId={}, seckillProductId={}", userId, seckillProductId, e);
        }
    }

    @Override
    public SeckillResultVO getSeckillResult(Long userId, Long seckillProductId) {
        // 这里可以从Redis或数据库查询秒杀结果
        // 简化实现，实际应该查询订单状态
        return SeckillResultVO.success("查询成功");
    }

    @Override
    public void asyncProcessSeckillOrder(SeckillRequest request) {
        // 异步处理秒杀订单
        // 这里可以发送消息到MQ，由消费者处理订单创建
        log.info("异步处理秒杀订单: userId={}, seckillProductId={}", 
                request.getUserId(), request.getSeckillProductId());
    }

    @Override
    public boolean checkSeckillProductStatus(Long seckillProductId) {
        String productKey = SECKILL_PRODUCT_PREFIX + seckillProductId;
        
        try {
            // 先从Redis查询
            Object cached = redisTemplate.opsForValue().get(productKey);
            if (cached != null) {
                SeckillProduct product = (SeckillProduct) cached;
                return checkProductStatus(product);
            }

            // Redis中没有，从数据库查询
            SeckillProduct product = seckillProductMapper.selectById(seckillProductId);
            if (product == null) {
                return false;
            }

            // 缓存到Redis
            redisTemplate.opsForValue().set(productKey, product, 1, TimeUnit.HOURS);
            
            return checkProductStatus(product);
        } catch (Exception e) {
            log.error("检查秒杀商品状态异常: seckillProductId={}", seckillProductId, e);
            return false;
        }
    }

    private boolean checkProductStatus(SeckillProduct product) {
        LocalDateTime now = LocalDateTime.now();
        return product.getEnabled() == 1 
            && product.getStatus() == 1 
            && product.getStartTime().isBefore(now) 
            && product.getEndTime().isAfter(now)
            && product.getSeckillStock() > 0;
    }

    @Override
    public Integer getSeckillProductStock(Long seckillProductId) {
        String stockKey = SECKILL_STOCK_PREFIX + seckillProductId;
        try {
            Object stock = redisTemplate.opsForValue().get(stockKey);
            if (stock != null) {
                return Integer.parseInt(stock.toString());
            }
            return 0;
        } catch (Exception e) {
            log.error("获取秒杀商品库存异常: seckillProductId={}", seckillProductId, e);
            return 0;
        }
    }

    @Override
    public void warmUpSeckillProduct(Long seckillProductId) {
        SeckillProduct product = seckillProductMapper.selectById(seckillProductId);
        if (product != null) {
            String productKey = SECKILL_PRODUCT_PREFIX + seckillProductId;
            String stockKey = SECKILL_STOCK_PREFIX + seckillProductId;
            
            try {
                // 缓存商品信息
                redisTemplate.opsForValue().set(productKey, product, 1, TimeUnit.HOURS);
                // 缓存库存信息
                redisTemplate.opsForValue().set(stockKey, product.getSeckillStock(), 1, TimeUnit.HOURS);
                
                log.info("预热秒杀商品成功: seckillProductId={}", seckillProductId);
            } catch (Exception e) {
                log.error("预热秒杀商品异常: seckillProductId={}", seckillProductId, e);
            }
        }
    }

    @Override
    public void cleanExpiredSeckillData() {
        // 清理过期的秒杀数据
        // 这里可以实现清理逻辑
        log.info("清理过期秒杀数据");
    }
}
