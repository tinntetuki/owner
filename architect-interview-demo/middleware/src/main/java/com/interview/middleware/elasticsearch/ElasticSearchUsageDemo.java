package com.interview.middleware.elasticsearch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ElasticSearch使用示例
 * 
 * 核心功能：
 * 1. 索引管理
 * 2. 文档CRUD操作
 * 3. 搜索查询
 * 4. 聚合分析
 * 5. 全文搜索
 */
public class ElasticSearchUsageDemo {
    
    /**
     * 文档实体
     */
    static class Document {
        private String id;
        private String index;
        private Map<String, Object> fields;
        private long timestamp;
        
        public Document(String id, String index) {
            this.id = id;
            this.index = index;
            this.fields = new ConcurrentHashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addField(String key, Object value) {
            fields.put(key, value);
        }
        
        public Object getField(String key) {
            return fields.get(key);
        }
        
        // getters
        public String getId() { return id; }
        public String getIndex() { return index; }
        public Map<String, Object> getFields() { return fields; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 搜索请求
     */
    static class SearchRequest {
        private String index;
        private String query;
        private Map<String, Object> filters;
        private int from;
        private int size;
        private List<String> sortFields;
        
        public SearchRequest(String index) {
            this.index = index;
            this.filters = new HashMap<>();
            this.from = 0;
            this.size = 10;
            this.sortFields = new ArrayList<>();
        }
        
        public SearchRequest query(String query) {
            this.query = query;
            return this;
        }
        
        public SearchRequest filter(String field, Object value) {
            this.filters.put(field, value);
            return this;
        }
        
        public SearchRequest from(int from) {
            this.from = from;
            return this;
        }
        
        public SearchRequest size(int size) {
            this.size = size;
            return this;
        }
        
        public SearchRequest sortBy(String field) {
            this.sortFields.add(field);
            return this;
        }
        
        // getters
        public String getIndex() { return index; }
        public String getQuery() { return query; }
        public Map<String, Object> getFilters() { return filters; }
        public int getFrom() { return from; }
        public int getSize() { return size; }
        public List<String> getSortFields() { return sortFields; }
    }
    
    /**
     * 搜索结果
     */
    static class SearchResult {
        private List<Document> documents;
        private long totalHits;
        private long took;
        
        public SearchResult(List<Document> documents, long totalHits, long took) {
            this.documents = documents;
            this.totalHits = totalHits;
            this.took = took;
        }
        
        // getters
        public List<Document> getDocuments() { return documents; }
        public long getTotalHits() { return totalHits; }
        public long getTook() { return took; }
    }
    
    /**
     * ElasticSearch客户端
     */
    static class ElasticSearchClient {
        private final Map<String, Map<String, Document>> indices = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Object>> mappings = new ConcurrentHashMap<>();
        
        /**
         * 创建索引
         */
        public boolean createIndex(String indexName, Map<String, Object> mapping) {
            if (indices.containsKey(indexName)) {
                return false; // 索引已存在
            }
            
            indices.put(indexName, new ConcurrentHashMap<>());
            mappings.put(indexName, mapping);
            
            System.out.println("Created index: " + indexName);
            return true;
        }
        
        /**
         * 删除索引
         */
        public boolean deleteIndex(String indexName) {
            if (!indices.containsKey(indexName)) {
                return false; // 索引不存在
            }
            
            indices.remove(indexName);
            mappings.remove(indexName);
            
            System.out.println("Deleted index: " + indexName);
            return true;
        }
        
        /**
         * 索引文档
         */
        public boolean indexDocument(Document document) {
            String indexName = document.getIndex();
            if (!indices.containsKey(indexName)) {
                return false; // 索引不存在
            }
            
            Map<String, Document> index = indices.get(indexName);
            index.put(document.getId(), document);
            
            System.out.println("Indexed document: " + document.getId() + " to index: " + indexName);
            return true;
        }
        
        /**
         * 获取文档
         */
        public Document getDocument(String indexName, String documentId) {
            Map<String, Document> index = indices.get(indexName);
            if (index == null) {
                return null;
            }
            
            return index.get(documentId);
        }
        
        /**
         * 更新文档
         */
        public boolean updateDocument(String indexName, String documentId, Map<String, Object> updates) {
            Document document = getDocument(indexName, documentId);
            if (document == null) {
                return false;
            }
            
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                document.addField(entry.getKey(), entry.getValue());
            }
            
            System.out.println("Updated document: " + documentId + " in index: " + indexName);
            return true;
        }
        
        /**
         * 删除文档
         */
        public boolean deleteDocument(String indexName, String documentId) {
            Map<String, Document> index = indices.get(indexName);
            if (index == null) {
                return false;
            }
            
            Document removed = index.remove(documentId);
            if (removed != null) {
                System.out.println("Deleted document: " + documentId + " from index: " + indexName);
                return true;
            }
            
            return false;
        }
        
        /**
         * 搜索文档
         */
        public SearchResult search(SearchRequest request) {
            long startTime = System.currentTimeMillis();
            
            Map<String, Document> index = indices.get(request.getIndex());
            if (index == null) {
                return new SearchResult(new ArrayList<>(), 0, 0);
            }
            
            List<Document> allDocuments = new ArrayList<>(index.values());
            List<Document> filteredDocuments = new ArrayList<>();
            
            // 应用过滤器
            for (Document doc : allDocuments) {
                boolean matches = true;
                
                // 应用字段过滤器
                for (Map.Entry<String, Object> filter : request.getFilters().entrySet()) {
                    String field = filter.getKey();
                    Object expectedValue = filter.getValue();
                    Object actualValue = doc.getField(field);
                    
                    if (!Objects.equals(expectedValue, actualValue)) {
                        matches = false;
                        break;
                    }
                }
                
                // 应用文本搜索
                if (matches && request.getQuery() != null && !request.getQuery().isEmpty()) {
                    boolean textMatch = false;
                    for (Object value : doc.getFields().values()) {
                        if (value != null && value.toString().toLowerCase().contains(request.getQuery().toLowerCase())) {
                            textMatch = true;
                            break;
                        }
                    }
                    matches = textMatch;
                }
                
                if (matches) {
                    filteredDocuments.add(doc);
                }
            }
            
            // 排序
            if (!request.getSortFields().isEmpty()) {
                filteredDocuments.sort((d1, d2) -> {
                    for (String sortField : request.getSortFields()) {
                        Object v1 = d1.getField(sortField);
                        Object v2 = d2.getField(sortField);
                        
                        if (v1 instanceof Comparable && v2 instanceof Comparable) {
                            int result = ((Comparable) v1).compareTo(v2);
                            if (result != 0) {
                                return result;
                            }
                        }
                    }
                    return 0;
                });
            }
            
            // 分页
            int from = Math.min(request.getFrom(), filteredDocuments.size());
            int to = Math.min(from + request.getSize(), filteredDocuments.size());
            List<Document> pagedDocuments = filteredDocuments.subList(from, to);
            
            long took = System.currentTimeMillis() - startTime;
            
            System.out.println("Search completed: " + filteredDocuments.size() + " hits, took " + took + "ms");
            
            return new SearchResult(pagedDocuments, filteredDocuments.size(), took);
        }
        
        /**
         * 批量操作
         */
        public void bulkIndex(List<Document> documents) {
            System.out.println("Bulk indexing " + documents.size() + " documents");
            
            for (Document doc : documents) {
                indexDocument(doc);
            }
            
            System.out.println("Bulk indexing completed");
        }
        
        /**
         * 获取索引统计
         */
        public Map<String, Object> getIndexStats(String indexName) {
            Map<String, Document> index = indices.get(indexName);
            if (index == null) {
                return new HashMap<>();
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("document_count", index.size());
            stats.put("index_name", indexName);
            
            return stats;
        }
    }
    
    /**
     * 聚合分析
     */
    static class AggregationAnalyzer {
        private final ElasticSearchClient client;
        
        public AggregationAnalyzer(ElasticSearchClient client) {
            this.client = client;
        }
        
        /**
         * 词频统计
         */
        public Map<String, Integer> termFrequency(String indexName, String field) {
            Map<String, Document> index = client.indices.get(indexName);
            if (index == null) {
                return new HashMap<>();
            }
            
            Map<String, Integer> termCount = new HashMap<>();
            
            for (Document doc : index.values()) {
                Object value = doc.getField(field);
                if (value != null) {
                    String term = value.toString();
                    termCount.merge(term, 1, Integer::sum);
                }
            }
            
            return termCount.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        /**
         * 数值统计
         */
        public Map<String, Double> numericStats(String indexName, String field) {
            Map<String, Document> index = client.indices.get(indexName);
            if (index == null) {
                return new HashMap<>();
            }
            
            List<Double> values = new ArrayList<>();
            
            for (Document doc : index.values()) {
                Object value = doc.getField(field);
                if (value instanceof Number) {
                    values.add(((Number) value).doubleValue());
                }
            }
            
            if (values.isEmpty()) {
                return new HashMap<>();
            }
            
            Map<String, Double> stats = new HashMap<>();
            stats.put("count", (double) values.size());
            stats.put("sum", values.stream().mapToDouble(Double::doubleValue).sum());
            stats.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            stats.put("min", values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
            stats.put("max", values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            
            return stats;
        }
        
        /**
         * 日期范围分析
         */
        public Map<String, Integer> dateRangeAnalysis(String indexName, String dateField) {
            Map<String, Document> index = client.indices.get(indexName);
            if (index == null) {
                return new HashMap<>();
            }
            
            Map<String, Integer> rangeCount = new HashMap<>();
            
            for (Document doc : index.values()) {
                Object value = doc.getField(dateField);
                if (value instanceof Long) {
                    long timestamp = (Long) value;
                    long daysAgo = (System.currentTimeMillis() - timestamp) / (24 * 60 * 60 * 1000);
                    
                    String range;
                    if (daysAgo < 1) {
                        range = "Today";
                    } else if (daysAgo < 7) {
                        range = "This Week";
                    } else if (daysAgo < 30) {
                        range = "This Month";
                    } else {
                        range = "Older";
                    }
                    
                    rangeCount.merge(range, 1, Integer::sum);
                }
            }
            
            return rangeCount;
        }
    }
    
    /**
     * 全文搜索
     */
    static class FullTextSearch {
        private final ElasticSearchClient client;
        
        public FullTextSearch(ElasticSearchClient client) {
            this.client = client;
        }
        
        /**
         * 全文搜索
         */
        public SearchResult fullTextSearch(String indexName, String query, int size) {
            SearchRequest request = new SearchRequest(indexName)
                .query(query)
                .size(size);
            
            return client.search(request);
        }
        
        /**
         * 模糊搜索
         */
        public SearchResult fuzzySearch(String indexName, String query, int size) {
            // 简单的模糊搜索实现
            Map<String, Document> index = client.indices.get(indexName);
            if (index == null) {
                return new SearchResult(new ArrayList<>(), 0, 0);
            }
            
            List<Document> results = new ArrayList<>();
            
            for (Document doc : index.values()) {
                boolean matches = false;
                
                for (Object value : doc.getFields().values()) {
                    if (value != null) {
                        String text = value.toString().toLowerCase();
                        String queryLower = query.toLowerCase();
                        
                        // 简单的模糊匹配
                        if (text.contains(queryLower) || 
                            calculateSimilarity(text, queryLower) > 0.6) {
                            matches = true;
                            break;
                        }
                    }
                }
                
                if (matches) {
                    results.add(doc);
                }
            }
            
            return new SearchResult(results.subList(0, Math.min(size, results.size())), results.size(), 0);
        }
        
        /**
         * 计算字符串相似度
         */
        private double calculateSimilarity(String s1, String s2) {
            int maxLength = Math.max(s1.length(), s2.length());
            if (maxLength == 0) {
                return 1.0;
            }
            
            int distance = levenshteinDistance(s1, s2);
            return 1.0 - (double) distance / maxLength;
        }
        
        /**
         * 计算编辑距离
         */
        private int levenshteinDistance(String s1, String s2) {
            int[][] dp = new int[s1.length() + 1][s2.length() + 1];
            
            for (int i = 0; i <= s1.length(); i++) {
                dp[i][0] = i;
            }
            
            for (int j = 0; j <= s2.length(); j++) {
                dp[0][j] = j;
            }
            
            for (int i = 1; i <= s1.length(); i++) {
                for (int j = 1; j <= s2.length(); j++) {
                    if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                    }
                }
            }
            
            return dp[s1.length()][s2.length()];
        }
    }
    
    /**
     * ElasticSearch使用演示
     */
    public static class ElasticSearchDemo {
        public void runDemo() {
            System.out.println("=== ElasticSearch Usage Demo ===");
            
            ElasticSearchClient client = new ElasticSearchClient();
            
            // 创建索引
            System.out.println("\n=== Index Management ===");
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("title", "text");
            mapping.put("content", "text");
            mapping.put("category", "keyword");
            mapping.put("price", "double");
            mapping.put("created_at", "date");
            
            client.createIndex("products", mapping);
            client.createIndex("articles", mapping);
            
            // 索引文档
            System.out.println("\n=== Document Indexing ===");
            Document doc1 = new Document("1", "products");
            doc1.addField("title", "iPhone 15");
            doc1.addField("content", "Latest iPhone with advanced features");
            doc1.addField("category", "Electronics");
            doc1.addField("price", 999.0);
            doc1.addField("created_at", System.currentTimeMillis());
            
            Document doc2 = new Document("2", "products");
            doc2.addField("title", "MacBook Pro");
            doc2.addField("content", "Professional laptop for developers");
            doc2.addField("category", "Electronics");
            doc2.addField("price", 1999.0);
            doc2.addField("created_at", System.currentTimeMillis() - 86400000);
            
            Document doc3 = new Document("3", "articles");
            doc3.addField("title", "Java Programming");
            doc3.addField("content", "Learn Java programming from scratch");
            doc3.addField("category", "Education");
            doc3.addField("price", 29.0);
            doc3.addField("created_at", System.currentTimeMillis() - 172800000);
            
            client.indexDocument(doc1);
            client.indexDocument(doc2);
            client.indexDocument(doc3);
            
            // 搜索
            System.out.println("\n=== Search Operations ===");
            SearchRequest searchRequest = new SearchRequest("products")
                .query("iPhone")
                .size(10);
            
            SearchResult result = client.search(searchRequest);
            System.out.println("Search results: " + result.getTotalHits() + " hits");
            for (Document doc : result.getDocuments()) {
                System.out.println("- " + doc.getField("title") + ": " + doc.getField("content"));
            }
            
            // 过滤搜索
            System.out.println("\n=== Filtered Search ===");
            SearchRequest filteredRequest = new SearchRequest("products")
                .filter("category", "Electronics")
                .size(10);
            
            SearchResult filteredResult = client.search(filteredRequest);
            System.out.println("Filtered results: " + filteredResult.getTotalHits() + " hits");
            
            // 聚合分析
            System.out.println("\n=== Aggregation Analysis ===");
            AggregationAnalyzer analyzer = new AggregationAnalyzer(client);
            
            Map<String, Integer> categoryStats = analyzer.termFrequency("products", "category");
            System.out.println("Category distribution: " + categoryStats);
            
            Map<String, Double> priceStats = analyzer.numericStats("products", "price");
            System.out.println("Price statistics: " + priceStats);
            
            Map<String, Integer> dateStats = analyzer.dateRangeAnalysis("products", "created_at");
            System.out.println("Date distribution: " + dateStats);
            
            // 全文搜索
            System.out.println("\n=== Full Text Search ===");
            FullTextSearch fullTextSearch = new FullTextSearch(client);
            
            SearchResult fullTextResult = fullTextSearch.fullTextSearch("products", "iPhone", 5);
            System.out.println("Full text search results: " + fullTextResult.getTotalHits() + " hits");
            
            SearchResult fuzzyResult = fullTextSearch.fuzzySearch("products", "iphone", 5);
            System.out.println("Fuzzy search results: " + fuzzyResult.getTotalHits() + " hits");
            
            // 更新文档
            System.out.println("\n=== Document Update ===");
            Map<String, Object> updates = new HashMap<>();
            updates.put("price", 899.0);
            client.updateDocument("products", "1", updates);
            
            // 获取更新后的文档
            Document updatedDoc = client.getDocument("products", "1");
            System.out.println("Updated document price: " + updatedDoc.getField("price"));
            
            // 批量操作
            System.out.println("\n=== Bulk Operations ===");
            List<Document> bulkDocs = Arrays.asList(
                new Document("4", "products"),
                new Document("5", "products"),
                new Document("6", "articles")
            );
            
            bulkDocs.get(0).addField("title", "Samsung Galaxy");
            bulkDocs.get(0).addField("category", "Electronics");
            bulkDocs.get(0).addField("price", 799.0);
            
            bulkDocs.get(1).addField("title", "iPad");
            bulkDocs.get(1).addField("category", "Electronics");
            bulkDocs.get(1).addField("price", 599.0);
            
            bulkDocs.get(2).addField("title", "Python Tutorial");
            bulkDocs.get(2).addField("category", "Education");
            bulkDocs.get(2).addField("price", 19.0);
            
            client.bulkIndex(bulkDocs);
            
            // 索引统计
            System.out.println("\n=== Index Statistics ===");
            Map<String, Object> productStats = client.getIndexStats("products");
            System.out.println("Products index stats: " + productStats);
            
            Map<String, Object> articleStats = client.getIndexStats("articles");
            System.out.println("Articles index stats: " + articleStats);
        }
    }
    
    public static void main(String[] args) {
        ElasticSearchDemo demo = new ElasticSearchDemo();
        demo.runDemo();
    }
}
