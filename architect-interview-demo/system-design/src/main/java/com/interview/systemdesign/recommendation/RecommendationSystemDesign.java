package com.interview.systemdesign.recommendation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 推荐系统设计
 * 
 * 核心算法：
 * 1. 协同过滤（UserCF、ItemCF）
 * 2. 基于内容的推荐
 * 3. 混合推荐
 * 4. 实时推荐
 */
public class RecommendationSystemDesign {
    
    /**
     * 用户实体
     */
    static class User {
        private String userId;
        private String username;
        private Map<String, Double> preferences; // 用户偏好
        private List<String> viewedItems;
        private List<String> purchasedItems;
        
        public User(String userId, String username) {
            this.userId = userId;
            this.username = username;
            this.preferences = new ConcurrentHashMap<>();
            this.viewedItems = new ArrayList<>();
            this.purchasedItems = new ArrayList<>();
        }
        
        public void addPreference(String category, double score) {
            preferences.put(category, score);
        }
        
        public void viewItem(String itemId) {
            if (!viewedItems.contains(itemId)) {
                viewedItems.add(itemId);
            }
        }
        
        public void purchaseItem(String itemId) {
            if (!purchasedItems.contains(itemId)) {
                purchasedItems.add(itemId);
            }
        }
        
        // getters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public Map<String, Double> getPreferences() { return preferences; }
        public List<String> getViewedItems() { return viewedItems; }
        public List<String> getPurchasedItems() { return purchasedItems; }
    }
    
    /**
     * 商品实体
     */
    static class Item {
        private String itemId;
        private String name;
        private String category;
        private Map<String, Double> features; // 商品特征
        private double price;
        private int popularity;
        
        public Item(String itemId, String name, String category, double price) {
            this.itemId = itemId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.features = new ConcurrentHashMap<>();
            this.popularity = 0;
        }
        
        public void addFeature(String feature, double value) {
            features.put(feature, value);
        }
        
        public void incrementPopularity() {
            popularity++;
        }
        
        // getters
        public String getItemId() { return itemId; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public Map<String, Double> getFeatures() { return features; }
        public double getPrice() { return price; }
        public int getPopularity() { return popularity; }
    }
    
    /**
     * 评分实体
     */
    static class Rating {
        private String userId;
        private String itemId;
        private double score;
        private long timestamp;
        
        public Rating(String userId, String itemId, double score) {
            this.userId = userId;
            this.itemId = itemId;
            this.score = score;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getters
        public String getUserId() { return userId; }
        public String getItemId() { return itemId; }
        public double getScore() { return score; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 用户协同过滤（UserCF）
     */
    static class UserCollaborativeFiltering {
        private final Map<String, Map<String, Double>> userItemMatrix = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Double>> userSimilarity = new ConcurrentHashMap<>();
        
        /**
         * 添加评分
         */
        public void addRating(String userId, String itemId, double score) {
            userItemMatrix.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(itemId, score);
        }
        
        /**
         * 计算用户相似度（余弦相似度）
         */
        public double calculateUserSimilarity(String userId1, String userId2) {
            Map<String, Double> user1Ratings = userItemMatrix.get(userId1);
            Map<String, Double> user2Ratings = userItemMatrix.get(userId2);
            
            if (user1Ratings == null || user2Ratings == null) {
                return 0.0;
            }
            
            // 找到共同评分的商品
            Set<String> commonItems = new HashSet<>(user1Ratings.keySet());
            commonItems.retainAll(user2Ratings.keySet());
            
            if (commonItems.isEmpty()) {
                return 0.0;
            }
            
            // 计算余弦相似度
            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;
            
            for (String itemId : commonItems) {
                double rating1 = user1Ratings.get(itemId);
                double rating2 = user2Ratings.get(itemId);
                
                dotProduct += rating1 * rating2;
                norm1 += rating1 * rating1;
                norm2 += rating2 * rating2;
            }
            
            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }
            
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }
        
        /**
         * 获取相似用户
         */
        public List<String> getSimilarUsers(String userId, int topK) {
            return userItemMatrix.keySet().stream()
                .filter(id -> !id.equals(userId))
                .map(id -> new AbstractMap.SimpleEntry<>(id, calculateUserSimilarity(userId, id)))
                .filter(entry -> entry.getValue() > 0.1) // 过滤低相似度
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
        }
        
        /**
         * 预测用户对商品的评分
         */
        public double predictRating(String userId, String itemId) {
            Map<String, Double> userRatings = userItemMatrix.get(userId);
            if (userRatings != null && userRatings.containsKey(itemId)) {
                return userRatings.get(itemId); // 用户已评分
            }
            
            List<String> similarUsers = getSimilarUsers(userId, 10);
            if (similarUsers.isEmpty()) {
                return 0.0;
            }
            
            double weightedSum = 0.0;
            double similaritySum = 0.0;
            
            for (String similarUserId : similarUsers) {
                Map<String, Double> similarUserRatings = userItemMatrix.get(similarUserId);
                if (similarUserRatings != null && similarUserRatings.containsKey(itemId)) {
                    double similarity = calculateUserSimilarity(userId, similarUserId);
                    double rating = similarUserRatings.get(itemId);
                    
                    weightedSum += similarity * rating;
                    similaritySum += Math.abs(similarity);
                }
            }
            
            return similaritySum > 0 ? weightedSum / similaritySum : 0.0;
        }
        
        /**
         * 为用户推荐商品
         */
        public List<String> recommendItems(String userId, int topK) {
            Set<String> userRatedItems = userItemMatrix.getOrDefault(userId, new ConcurrentHashMap<>()).keySet();
            Set<String> allItems = new HashSet<>();
            
            // 收集所有商品
            for (Map<String, Double> ratings : userItemMatrix.values()) {
                allItems.addAll(ratings.keySet());
            }
            
            // 过滤用户已评分的商品
            Set<String> candidateItems = new HashSet<>(allItems);
            candidateItems.removeAll(userRatedItems);
            
            // 计算预测评分并排序
            return candidateItems.stream()
                .map(itemId -> new AbstractMap.SimpleEntry<>(itemId, predictRating(userId, itemId)))
                .filter(entry -> entry.getValue() > 0.0)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 商品协同过滤（ItemCF）
     */
    static class ItemCollaborativeFiltering {
        private final Map<String, Map<String, Double>> itemUserMatrix = new ConcurrentHashMap<>();
        
        /**
         * 添加评分
         */
        public void addRating(String userId, String itemId, double score) {
            itemUserMatrix.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, score);
        }
        
        /**
         * 计算商品相似度
         */
        public double calculateItemSimilarity(String itemId1, String itemId2) {
            Map<String, Double> item1Ratings = itemUserMatrix.get(itemId1);
            Map<String, Double> item2Ratings = itemUserMatrix.get(itemId2);
            
            if (item1Ratings == null || item2Ratings == null) {
                return 0.0;
            }
            
            Set<String> commonUsers = new HashSet<>(item1Ratings.keySet());
            commonUsers.retainAll(item2Ratings.keySet());
            
            if (commonUsers.isEmpty()) {
                return 0.0;
            }
            
            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;
            
            for (String userId : commonUsers) {
                double rating1 = item1Ratings.get(userId);
                double rating2 = item2Ratings.get(userId);
                
                dotProduct += rating1 * rating2;
                norm1 += rating1 * rating1;
                norm2 += rating2 * rating2;
            }
            
            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }
            
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }
        
        /**
         * 基于商品相似度推荐
         */
        public List<String> recommendItems(String userId, List<String> userRatedItems, int topK) {
            if (userRatedItems.isEmpty()) {
                return new ArrayList<>();
            }
            
            Map<String, Double> itemScores = new ConcurrentHashMap<>();
            
            for (String ratedItem : userRatedItems) {
                Map<String, Double> itemRatings = itemUserMatrix.get(ratedItem);
                if (itemRatings != null) {
                    double userRating = itemRatings.getOrDefault(userId, 0.0);
                    
                    // 找到与已评分商品相似的商品
                    for (String candidateItem : itemUserMatrix.keySet()) {
                        if (!userRatedItems.contains(candidateItem)) {
                            double similarity = calculateItemSimilarity(ratedItem, candidateItem);
                            if (similarity > 0.1) {
                                itemScores.merge(candidateItem, similarity * userRating, Double::sum);
                            }
                        }
                    }
                }
            }
            
            return itemScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 基于内容的推荐
     */
    static class ContentBasedRecommendation {
        private final Map<String, Item> items = new ConcurrentHashMap<>();
        private final Map<String, User> users = new ConcurrentHashMap<>();
        
        /**
         * 添加商品
         */
        public void addItem(Item item) {
            items.put(item.getItemId(), item);
        }
        
        /**
         * 添加用户
         */
        public void addUser(User user) {
            users.put(user.getUserId(), user);
        }
        
        /**
         * 计算用户偏好向量
         */
        public Map<String, Double> calculateUserProfile(String userId) {
            User user = users.get(userId);
            if (user == null) {
                return new ConcurrentHashMap<>();
            }
            
            Map<String, Double> profile = new ConcurrentHashMap<>();
            Map<String, Integer> categoryCount = new ConcurrentHashMap<>();
            
            // 基于用户浏览和购买历史计算偏好
            for (String itemId : user.getViewedItems()) {
                Item item = items.get(itemId);
                if (item != null) {
                    categoryCount.merge(item.getCategory(), 1, Integer::sum);
                }
            }
            
            for (String itemId : user.getPurchasedItems()) {
                Item item = items.get(itemId);
                if (item != null) {
                    categoryCount.merge(item.getCategory(), 2, Integer::sum); // 购买权重更高
                }
            }
            
            // 归一化
            int totalCount = categoryCount.values().stream().mapToInt(Integer::intValue).sum();
            if (totalCount > 0) {
                for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                    profile.put(entry.getKey(), (double) entry.getValue() / totalCount);
                }
            }
            
            return profile;
        }
        
        /**
         * 基于内容推荐商品
         */
        public List<String> recommendItems(String userId, int topK) {
            Map<String, Double> userProfile = calculateUserProfile(userId);
            User user = users.get(userId);
            
            if (userProfile.isEmpty() || user == null) {
                return new ArrayList<>();
            }
            
            Set<String> userInteractedItems = new HashSet<>();
            userInteractedItems.addAll(user.getViewedItems());
            userInteractedItems.addAll(user.getPurchasedItems());
            
            return items.values().stream()
                .filter(item -> !userInteractedItems.contains(item.getItemId()))
                .map(item -> new AbstractMap.SimpleEntry<>(item.getItemId(), calculateItemScore(item, userProfile)))
                .filter(entry -> entry.getValue() > 0.0)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
        }
        
        /**
         * 计算商品与用户偏好的匹配度
         */
        private double calculateItemScore(Item item, Map<String, Double> userProfile) {
            double score = 0.0;
            
            // 基于类别的匹配
            Double categoryPreference = userProfile.get(item.getCategory());
            if (categoryPreference != null) {
                score += categoryPreference * 0.7; // 类别权重
            }
            
            // 基于特征的匹配
            for (Map.Entry<String, Double> feature : item.getFeatures().entrySet()) {
                Double userPreference = userProfile.get(feature.getKey());
                if (userPreference != null) {
                    score += userPreference * feature.getValue() * 0.3; // 特征权重
                }
            }
            
            return score;
        }
    }
    
    /**
     * 混合推荐系统
     */
    static class HybridRecommendationSystem {
        private final UserCollaborativeFiltering userCF;
        private final ItemCollaborativeFiltering itemCF;
        private final ContentBasedRecommendation contentBased;
        
        public HybridRecommendationSystem() {
            this.userCF = new UserCollaborativeFiltering();
            this.itemCF = new ItemCollaborativeFiltering();
            this.contentBased = new ContentBasedRecommendation();
        }
        
        /**
         * 混合推荐
         */
        public List<String> hybridRecommend(String userId, int topK) {
            // 获取各种推荐结果
            List<String> userCFRecommendations = userCF.recommendItems(userId, topK * 2);
            List<String> contentRecommendations = contentBased.recommendItems(userId, topK * 2);
            
            // 合并和去重
            Map<String, Double> combinedScores = new ConcurrentHashMap<>();
            
            // UserCF权重
            for (int i = 0; i < userCFRecommendations.size(); i++) {
                String itemId = userCFRecommendations.get(i);
                double score = (topK * 2 - i) / (double) (topK * 2); // 排名分数
                combinedScores.merge(itemId, score * 0.4, Double::sum); // UserCF权重40%
            }
            
            // Content-based权重
            for (int i = 0; i < contentRecommendations.size(); i++) {
                String itemId = contentRecommendations.get(i);
                double score = (topK * 2 - i) / (double) (topK * 2);
                combinedScores.merge(itemId, score * 0.6, Double::sum); // Content权重60%
            }
            
            return combinedScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 推荐系统演示
     */
    public static class RecommendationDemo {
        public void runDemo() {
            System.out.println("=== Recommendation System Demo ===");
            
            // 创建用户和商品
            User user1 = new User("user1", "Alice");
            User user2 = new User("user2", "Bob");
            User user3 = new User("user3", "Charlie");
            
            Item item1 = new Item("item1", "iPhone", "Electronics", 999.0);
            Item item2 = new Item("item2", "MacBook", "Electronics", 1999.0);
            Item item3 = new Item("item3", "Book", "Education", 29.0);
            Item item4 = new Item("item4", "Laptop", "Electronics", 1299.0);
            Item item5 = new Item("item5", "Tablet", "Electronics", 599.0);
            
            // 设置商品特征
            item1.addFeature("brand", 1.0);
            item1.addFeature("price_range", 0.8);
            item2.addFeature("brand", 1.0);
            item2.addFeature("price_range", 1.0);
            item3.addFeature("brand", 0.3);
            item3.addFeature("price_range", 0.2);
            
            // 用户行为
            user1.viewItem("item1");
            user1.purchaseItem("item1");
            user1.viewItem("item2");
            
            user2.viewItem("item1");
            user2.purchaseItem("item2");
            user2.viewItem("item4");
            
            user3.viewItem("item3");
            user3.purchaseItem("item3");
            user3.viewItem("item5");
            
            // 添加评分数据
            UserCollaborativeFiltering userCF = new UserCollaborativeFiltering();
            userCF.addRating("user1", "item1", 5.0);
            userCF.addRating("user1", "item2", 4.0);
            userCF.addRating("user2", "item1", 4.0);
            userCF.addRating("user2", "item2", 5.0);
            userCF.addRating("user2", "item4", 3.0);
            userCF.addRating("user3", "item3", 5.0);
            userCF.addRating("user3", "item5", 4.0);
            
            // UserCF推荐
            System.out.println("\n=== User Collaborative Filtering ===");
            List<String> userCFRecs = userCF.recommendItems("user1", 3);
            System.out.println("Recommendations for user1: " + userCFRecs);
            
            // Content-based推荐
            ContentBasedRecommendation contentBased = new ContentBasedRecommendation();
            contentBased.addUser(user1);
            contentBased.addUser(user2);
            contentBased.addUser(user3);
            contentBased.addItem(item1);
            contentBased.addItem(item2);
            contentBased.addItem(item3);
            contentBased.addItem(item4);
            contentBased.addItem(item5);
            
            System.out.println("\n=== Content-Based Recommendation ===");
            List<String> contentRecs = contentBased.recommendItems("user1", 3);
            System.out.println("Content-based recommendations for user1: " + contentRecs);
            
            // 混合推荐
            HybridRecommendationSystem hybrid = new HybridRecommendationSystem();
            System.out.println("\n=== Hybrid Recommendation ===");
            List<String> hybridRecs = hybrid.hybridRecommend("user1", 3);
            System.out.println("Hybrid recommendations for user1: " + hybridRecs);
        }
    }
    
    public static void main(String[] args) {
        RecommendationDemo demo = new RecommendationDemo();
        demo.runDemo();
    }
}
