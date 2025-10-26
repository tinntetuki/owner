package com.interview.seckill.controller;

import com.interview.common.result.Result;
import com.interview.seckill.dto.SeckillRequest;
import com.interview.seckill.service.SeckillService;
import com.interview.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 秒杀控制器
 * 
 * @author interview
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Validated
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行秒杀
     */
    @PostMapping("/execute")
    public Result<SeckillResultVO> executeSeckill(@Valid @RequestBody SeckillRequest request,
                                                 HttpServletRequest httpRequest) {
        log.info("秒杀请求: userId={}, seckillProductId={}, quantity={}", 
                request.getUserId(), request.getSeckillProductId(), request.getQuantity());

        // 设置请求信息
        request.setUserIp(getClientIp(httpRequest));
        request.setUserAgent(httpRequest.getHeader("User-Agent"));
        request.setTimestamp(System.currentTimeMillis());

        SeckillResultVO result = seckillService.executeSeckill(request);
        return Result.success(result);
    }

    /**
     * 获取秒杀结果
     */
    @GetMapping("/result")
    public Result<SeckillResultVO> getSeckillResult(@RequestParam @NotNull Long userId,
                                                   @RequestParam @NotNull Long seckillProductId) {
        log.info("查询秒杀结果: userId={}, seckillProductId={}", userId, seckillProductId);
        SeckillResultVO result = seckillService.getSeckillResult(userId, seckillProductId);
        return Result.success(result);
    }

    /**
     * 检查用户是否已秒杀
     */
    @GetMapping("/check")
    public Result<Boolean> checkUserSeckilled(@RequestParam @NotNull Long userId,
                                            @RequestParam @NotNull Long seckillProductId) {
        log.info("检查用户秒杀状态: userId={}, seckillProductId={}", userId, seckillProductId);
        boolean seckilled = seckillService.checkUserSeckilled(userId, seckillProductId);
        return Result.success(seckilled);
    }

    /**
     * 获取秒杀商品库存
     */
    @GetMapping("/stock/{seckillProductId}")
    public Result<Integer> getSeckillProductStock(@PathVariable @NotNull Long seckillProductId) {
        log.info("查询秒杀商品库存: seckillProductId={}", seckillProductId);
        Integer stock = seckillService.getSeckillProductStock(seckillProductId);
        return Result.success(stock);
    }

    /**
     * 检查秒杀商品状态
     */
    @GetMapping("/product-status/{seckillProductId}")
    public Result<Boolean> checkSeckillProductStatus(@PathVariable @NotNull Long seckillProductId) {
        log.info("检查秒杀商品状态: seckillProductId={}", seckillProductId);
        boolean status = seckillService.checkSeckillProductStatus(seckillProductId);
        return Result.success(status);
    }

    /**
     * 预热秒杀商品
     */
    @PostMapping("/warm-up/{seckillProductId}")
    public Result<Void> warmUpSeckillProduct(@PathVariable @NotNull Long seckillProductId) {
        log.info("预热秒杀商品: seckillProductId={}", seckillProductId);
        seckillService.warmUpSeckillProduct(seckillProductId);
        return Result.success("预热成功");
    }

    /**
     * 清理过期秒杀数据
     */
    @PostMapping("/clean")
    public Result<Void> cleanExpiredSeckillData() {
        log.info("清理过期秒杀数据");
        seckillService.cleanExpiredSeckillData();
        return Result.success("清理成功");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
