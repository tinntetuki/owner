package com.interview.systemdesign.urlshortener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 短URL服务设计
 * 
 * 需求：
 * 1. 将长URL转换为短URL
 * 2. 通过短URL跳转到长URL
 * 3. 统计访问次数
 * 
 * 核心问题：
 * 1. 如何生成短码？
 * 2. 如何保证唯一性？
 * 3. 如何高性能？
 * 
 * 解决方案：
 * 1. 自增ID + Base62编码
 * 2. Hash算法（可能冲突）
 * 3. 预生成号段
 */
public class UrlShortener {
    
    /**
     * 方案1：自增ID + Base62编码
     * 
     * 优点：简单、无冲突、短
     * 缺点：可预测
     */
    public static class Base62UrlShortener {
        private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final int BASE = 62;
        
        private final AtomicLong idGenerator = new AtomicLong(10000000);  // 从1000万开始
        private final Map<String, String> shortToLong = new HashMap<>();
        private final Map<String, String> longToShort = new HashMap<>();
        
        /**
         * 长URL转短URL
         */
        public String shorten(String longUrl) {
            // 检查是否已存在
            if (longToShort.containsKey(longUrl)) {
                return longToShort.get(longUrl);
            }
            
            // 生成短码
            long id = idGenerator.incrementAndGet();
            String shortCode = encode(id);
            String shortUrl = "http://short.url/" + shortCode;
            
            // 存储映射
            shortToLong.put(shortCode, longUrl);
            longToShort.put(longUrl, shortUrl);
            
            return shortUrl;
        }
        
        /**
         * 短URL还原
         */
        public String expand(String shortCode) {
            return shortToLong.get(shortCode);
        }
        
        /**
         * Base62编码
         */
        private String encode(long num) {
            StringBuilder sb = new StringBuilder();
            while (num > 0) {
                sb.append(BASE62.charAt((int) (num % BASE)));
                num /= BASE;
            }
            return sb.reverse().toString();
        }
        
        /**
         * Base62解码
         */
        private long decode(String str) {
            long num = 0;
            for (char c : str.toCharArray()) {
                num = num * BASE + BASE62.indexOf(c);
            }
            return num;
        }
    }
    
    /**
     * 方案2：MD5 Hash（可能冲突）
     * 
     * 优点：不可预测
     * 缺点：可能冲突、较长
     */
    public static class HashUrlShortener {
        private final Map<String, String> shortToLong = new HashMap<>();
        
        /**
         * 长URL转短URL
         */
        public String shorten(String longUrl) {
            // 1. MD5 hash
            String hash = md5(longUrl);
            
            // 2. 取前6位作为短码
            String shortCode = hash.substring(0, 6);
            
            // 3. 冲突检测
            while (shortToLong.containsKey(shortCode)) {
                // 冲突处理：加盐重新hash
                longUrl = longUrl + System.currentTimeMillis();
                hash = md5(longUrl);
                shortCode = hash.substring(0, 6);
            }
            
            shortToLong.put(shortCode, longUrl);
            return "http://short.url/" + shortCode;
        }
        
        /**
         * MD5 hash（简化版）
         */
        private String md5(String str) {
            // 实际应该使用MessageDigest
            return Integer.toHexString(str.hashCode());
        }
    }
    
    /**
     * 完整实现 - 包含统计
     */
    public static class AdvancedUrlShortener {
        private final Base62UrlShortener shortener = new Base62UrlShortener();
        private final Map<String, UrlStats> stats = new HashMap<>();
        
        static class UrlStats {
            private final String longUrl;
            private long visitCount;
            private long createTime;
            private long lastVisitTime;
            
            public UrlStats(String longUrl) {
                this.longUrl = longUrl;
                this.visitCount = 0;
                this.createTime = System.currentTimeMillis();
            }
            
            public void visit() {
                visitCount++;
                lastVisitTime = System.currentTimeMillis();
            }
            
            @Override
            public String toString() {
                return "UrlStats{" +
                        "longUrl='" + longUrl + '\'' +
                        ", visitCount=" + visitCount +
                        ", createTime=" + createTime +
                        ", lastVisitTime=" + lastVisitTime +
                        '}';
            }
        }
        
        /**
         * 创建短URL
         */
        public String createShortUrl(String longUrl) {
            String shortUrl = shortener.shorten(longUrl);
            String shortCode = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
            stats.put(shortCode, new UrlStats(longUrl));
            return shortUrl;
        }
        
        /**
         * 访问短URL（重定向）
         */
        public String redirect(String shortCode) {
            String longUrl = shortener.expand(shortCode);
            if (longUrl != null && stats.containsKey(shortCode)) {
                stats.get(shortCode).visit();
            }
            return longUrl;
        }
        
        /**
         * 获取统计信息
         */
        public UrlStats getStats(String shortCode) {
            return stats.get(shortCode);
        }
    }
    
    /**
     * 测试
     */
    public static void main(String[] args) {
        AdvancedUrlShortener urlShortener = new AdvancedUrlShortener();
        
        // 创建短URL
        String longUrl1 = "https://www.example.com/very/long/url/with/many/parameters?id=12345&name=test";
        String longUrl2 = "https://www.github.com/user/repository/blob/master/README.md";
        
        String shortUrl1 = urlShortener.createShortUrl(longUrl1);
        String shortUrl2 = urlShortener.createShortUrl(longUrl2);
        
        System.out.println("长URL: " + longUrl1);
        System.out.println("短URL: " + shortUrl1);
        System.out.println();
        System.out.println("长URL: " + longUrl2);
        System.out.println("短URL: " + shortUrl2);
        System.out.println();
        
        // 访问短URL
        String shortCode1 = shortUrl1.substring(shortUrl1.lastIndexOf('/') + 1);
        for (int i = 0; i < 5; i++) {
            String redirectUrl = urlShortener.redirect(shortCode1);
            System.out.println("重定向到: " + redirectUrl);
        }
        
        // 查看统计
        System.out.println("\n统计信息:");
        System.out.println(urlShortener.getStats(shortCode1));
    }
}

