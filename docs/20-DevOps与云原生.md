# DevOps与云原生

## 目录
- [一、Docker](#一docker)
- [二、Kubernetes](#二kubernetes)
- [三、CI/CD](#三cicd)
- [四、监控体系](#四监控体系)

## 一、Docker

### 1.1 Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 1.2 Docker Compose

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
    depends_on:
      - mysql
      - redis
  
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: mydb
    volumes:
      - mysql-data:/var/lib/mysql
  
  redis:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  mysql-data:
```

## 二、Kubernetes

### 2.1 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: app
        image: myapp:1.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

### 2.2 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: app-service
spec:
  type: LoadBalancer
  selector:
    app: myapp
  ports:
  - port: 80
    targetPort: 8080
```

### 2.3 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yaml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql:3306/mydb
```

## 三、CI/CD

### 3.1 Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/user/repo.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Docker Build') {
            steps {
                sh 'docker build -t myapp:${BUILD_NUMBER} .'
            }
        }
        
        stage('Deploy') {
            steps {
                sh 'kubectl apply -f k8s/'
                sh 'kubectl set image deployment/app-deployment app=myapp:${BUILD_NUMBER}'
            }
        }
    }
}
```

### 3.2 GitLab CI

```yaml
stages:
  - build
  - test
  - deploy

build:
  stage: build
  script:
    - mvn clean package
  artifacts:
    paths:
      - target/*.jar

test:
  stage: test
  script:
    - mvn test

deploy:
  stage: deploy
  script:
    - docker build -t myapp:$CI_COMMIT_SHA .
    - docker push myapp:$CI_COMMIT_SHA
    - kubectl set image deployment/app app=myapp:$CI_COMMIT_SHA
  only:
    - main
```

## 四、监控体系

### 4.1 Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

### 4.2 Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Application Metrics",
    "panels": [
      {
        "title": "QPS",
        "targets": [
          {
            "expr": "rate(http_requests_total[1m])"
          }
        ]
      },
      {
        "title": "Response Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_request_duration_seconds_bucket)"
          }
        ]
      }
    ]
  }
}
```

### 4.3 日志收集（ELK）

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.log

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"
```

---

**关键字**：Docker、Kubernetes、CI/CD、Jenkins、Prometheus、Grafana

