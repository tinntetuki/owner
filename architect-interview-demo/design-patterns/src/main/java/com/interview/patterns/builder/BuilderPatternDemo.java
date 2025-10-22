package com.interview.patterns.builder;

/**
 * 建造者模式
 * 
 * 适用场景：
 * - 对象创建参数多
 * - 参数有必填和可选
 * - 需要按步骤创建对象
 * 
 * 例如：StringBuilder, Lombok @Builder
 */
public class BuilderPatternDemo {
    
    /**
     * 产品类 - 用户对象
     */
    static class User {
        // 必填参数
        private final String username;
        private final String password;
        
        // 可选参数
        private final String email;
        private final String phone;
        private final int age;
        private final String address;
        
        private User(Builder builder) {
            this.username = builder.username;
            this.password = builder.password;
            this.email = builder.email;
            this.phone = builder.phone;
            this.age = builder.age;
            this.address = builder.address;
        }
        
        @Override
        public String toString() {
            return "User{" +
                    "username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", phone='" + phone + '\'' +
                    ", age=" + age +
                    ", address='" + address + '\'' +
                    '}';
        }
        
        /**
         * 建造者
         */
        public static class Builder {
            // 必填参数
            private final String username;
            private final String password;
            
            // 可选参数 - 设置默认值
            private String email = "";
            private String phone = "";
            private int age = 0;
            private String address = "";
            
            public Builder(String username, String password) {
                this.username = username;
                this.password = password;
            }
            
            public Builder email(String email) {
                this.email = email;
                return this;
            }
            
            public Builder phone(String phone) {
                this.phone = phone;
                return this;
            }
            
            public Builder age(int age) {
                this.age = age;
                return this;
            }
            
            public Builder address(String address) {
                this.address = address;
                return this;
            }
            
            public User build() {
                // 可以在这里添加参数校验
                if (username == null || username.isEmpty()) {
                    throw new IllegalArgumentException("Username cannot be empty");
                }
                return new User(this);
            }
        }
    }
    
    /**
     * 链式调用示例 - HTTP请求构建器
     */
    static class HttpRequest {
        private final String url;
        private final String method;
        private final String body;
        private final java.util.Map<String, String> headers;
        private final int timeout;
        
        private HttpRequest(Builder builder) {
            this.url = builder.url;
            this.method = builder.method;
            this.body = builder.body;
            this.headers = builder.headers;
            this.timeout = builder.timeout;
        }
        
        public static class Builder {
            private String url;
            private String method = "GET";
            private String body = "";
            private java.util.Map<String, String> headers = new java.util.HashMap<>();
            private int timeout = 3000;
            
            public Builder url(String url) {
                this.url = url;
                return this;
            }
            
            public Builder method(String method) {
                this.method = method;
                return this;
            }
            
            public Builder body(String body) {
                this.body = body;
                return this;
            }
            
            public Builder header(String key, String value) {
                this.headers.put(key, value);
                return this;
            }
            
            public Builder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }
            
            public HttpRequest build() {
                if (url == null) {
                    throw new IllegalStateException("URL is required");
                }
                return new HttpRequest(this);
            }
        }
        
        @Override
        public String toString() {
            return "HttpRequest{" +
                    "url='" + url + '\'' +
                    ", method='" + method + '\'' +
                    ", headers=" + headers +
                    ", timeout=" + timeout +
                    '}';
        }
    }
    
    public static void main(String[] args) {
        // 示例1: 创建用户
        User user = new User.Builder("john", "password123")
                .email("john@example.com")
                .phone("13800138000")
                .age(25)
                .address("北京市朝阳区")
                .build();
        
        System.out.println(user);
        
        // 示例2: 只设置必填参数
        User simpleUser = new User.Builder("jane", "pass456").build();
        System.out.println(simpleUser);
        
        // 示例3: HTTP请求
        HttpRequest request = new HttpRequest.Builder()
                .url("https://api.example.com/users")
                .method("POST")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer token123")
                .body("{\"name\":\"John\"}")
                .timeout(5000)
                .build();
        
        System.out.println(request);
    }
}

