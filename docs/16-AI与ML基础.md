# AI与机器学习基础

## 目录
- [一、机器学习算法](#一机器学习算法)
- [二、深度学习基础](#二深度学习基础)
- [三、模型训练与部署](#三模型训练与部署)
- [四、高频面试题](#四高频面试题)

## 一、机器学习算法

### 1.1 监督学习

**线性回归**：
```python
from sklearn.linear_model import LinearRegression

model = LinearRegression()
model.fit(X_train, y_train)
predictions = model.predict(X_test)
```

**逻辑回归**：
```python
from sklearn.linear_model import LogisticRegression

model = LogisticRegression()
model.fit(X_train, y_train)
predictions = model.predict(X_test)
```

**决策树**：
```python
from sklearn.tree import DecisionTreeClassifier

model = DecisionTreeClassifier(max_depth=5)
model.fit(X_train, y_train)
```

**随机森林**：
```python
from sklearn.ensemble import RandomForestClassifier

model = RandomForestClassifier(n_estimators=100)
model.fit(X_train, y_train)
```

**XGBoost**：
```python
import xgboost as xgb

model = xgb.XGBClassifier(
    max_depth=5,
    learning_rate=0.1,
    n_estimators=100
)
model.fit(X_train, y_train)
```

### 1.2 无监督学习

**K-Means聚类**：
```python
from sklearn.cluster import KMeans

kmeans = KMeans(n_clusters=3)
labels = kmeans.fit_predict(X)
```

**PCA降维**：
```python
from sklearn.decomposition import PCA

pca = PCA(n_components=2)
X_reduced = pca.fit_transform(X)
```

## 二、深度学习基础

### 2.1 神经网络

**PyTorch示例**：
```python
import torch
import torch.nn as nn

class NeuralNetwork(nn.Module):
    def __init__(self, input_size, hidden_size, num_classes):
        super(NeuralNetwork, self).__init__()
        self.fc1 = nn.Linear(input_size, hidden_size)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(hidden_size, num_classes)
    
    def forward(self, x):
        out = self.fc1(x)
        out = self.relu(out)
        out = self.fc2(out)
        return out

model = NeuralNetwork(784, 128, 10)
criterion = nn.CrossEntropyLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
```

### 2.2 CNN

```python
class CNN(nn.Module):
    def __init__(self):
        super(CNN, self).__init__()
        self.conv1 = nn.Conv2d(1, 32, 3, 1)
        self.conv2 = nn.Conv2d(32, 64, 3, 1)
        self.fc1 = nn.Linear(9216, 128)
        self.fc2 = nn.Linear(128, 10)
        
    def forward(self, x):
        x = F.relu(self.conv1(x))
        x = F.max_pool2d(x, 2)
        x = F.relu(self.conv2(x))
        x = F.max_pool2d(x, 2)
        x = torch.flatten(x, 1)
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return x
```

### 2.3 RNN/LSTM

```python
class LSTM(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, num_classes):
        super(LSTM, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, num_classes)
    
    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size)
        
        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])
        return out
```

## 三、模型训练与部署

### 3.1 训练流程

```python
def train_model(model, train_loader, criterion, optimizer, num_epochs):
    for epoch in range(num_epochs):
        model.train()
        running_loss = 0.0
        
        for images, labels in train_loader:
            # 前向传播
            outputs = model(images)
            loss = criterion(outputs, labels)
            
            # 反向传播和优化
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            
            running_loss += loss.item()
        
        print(f'Epoch [{epoch+1}/{num_epochs}], Loss: {running_loss/len(train_loader):.4f}')
```

### 3.2 模型保存

```python
# PyTorch
torch.save(model.state_dict(), 'model.pth')
model.load_state_dict(torch.load('model.pth'))

# TensorFlow
model.save('model.h5')
model = tf.keras.models.load_model('model.h5')
```

### 3.3 模型部署

**ONNX导出**：
```python
import torch.onnx

dummy_input = torch.randn(1, 3, 224, 224)
torch.onnx.export(model, dummy_input, "model.onnx")
```

**TensorFlow Serving**：
```bash
docker run -p 8501:8501 \
  --mount type=bind,source=/models/my_model,target=/models/my_model \
  -e MODEL_NAME=my_model \
  -t tensorflow/serving
```

## 四、高频面试题

### Q1：过拟合和欠拟合如何解决？

**过拟合**：
- 增加数据
- 正则化（L1/L2）
- Dropout
- Early Stopping

**欠拟合**：
- 增加模型复杂度
- 增加特征
- 减少正则化

### Q2：常用的优化算法？

- **SGD**：随机梯度下降
- **Momentum**：动量
- **Adam**：自适应学习率
- **RMSprop**：均方根传播

### Q3：激活函数的作用？

- 引入非线性
- 常用：ReLU、Sigmoid、Tanh

## 五、机器学习算法深度解析

### 5.1 线性回归数学原理

**数学公式**：
```
h(x) = θ₀ + θ₁x₁ + θ₂x₂ + ... + θₙxₙ
```

**损失函数（均方误差）**：
```
J(θ) = (1/2m) * Σ(h(x⁽ⁱ⁾) - y⁽ⁱ⁾)²
```

**梯度下降更新规则**：
```
θⱼ := θⱼ - α * ∂J(θ)/∂θⱼ
```

**生产级实现**：
```python
import numpy as np
from typing import List, Tuple
import logging

class LinearRegression:
    def __init__(self, learning_rate: float = 0.01, max_iterations: int = 1000, 
                 tolerance: float = 1e-6):
        self.learning_rate = learning_rate
        self.max_iterations = max_iterations
        self.tolerance = tolerance
        self.weights = None
        self.bias = None
        self.cost_history = []
        
    def fit(self, X: np.ndarray, y: np.ndarray) -> 'LinearRegression':
        """
        训练线性回归模型
        
        Args:
            X: 特征矩阵 (m, n)
            y: 目标向量 (m,)
        """
        m, n = X.shape
        
        # 初始化参数
        self.weights = np.random.normal(0, 0.01, n)
        self.bias = 0
        
        # 梯度下降
        for i in range(self.max_iterations):
            # 前向传播
            predictions = self._predict(X)
            
            # 计算损失
            cost = self._compute_cost(predictions, y)
            self.cost_history.append(cost)
            
            # 计算梯度
            dw = (1/m) * np.dot(X.T, (predictions - y))
            db = (1/m) * np.sum(predictions - y)
            
            # 更新参数
            self.weights -= self.learning_rate * dw
            self.bias -= self.learning_rate * db
            
            # 检查收敛
            if i > 0 and abs(self.cost_history[-2] - self.cost_history[-1]) < self.tolerance:
                logging.info(f"Converged after {i+1} iterations")
                break
                
        return self
    
    def predict(self, X: np.ndarray) -> np.ndarray:
        """预测"""
        return self._predict(X)
    
    def _predict(self, X: np.ndarray) -> np.ndarray:
        """内部预测方法"""
        return np.dot(X, self.weights) + self.bias
    
    def _compute_cost(self, predictions: np.ndarray, y: np.ndarray) -> float:
        """计算均方误差"""
        m = len(y)
        return (1/(2*m)) * np.sum((predictions - y) ** 2)
    
    def get_coefficients(self) -> Tuple[np.ndarray, float]:
        """获取模型系数"""
        return self.weights, self.bias
```

### 5.2 逻辑回归深度实现

**Sigmoid函数**：
```
σ(z) = 1 / (1 + e^(-z))
```

**损失函数（交叉熵）**：
```
J(θ) = -(1/m) * Σ[y⁽ⁱ⁾log(h(x⁽ⁱ⁾)) + (1-y⁽ⁱ⁾)log(1-h(x⁽ⁱ⁾))]
```

**生产级实现**：
```python
class LogisticRegression:
    def __init__(self, learning_rate: float = 0.01, max_iterations: int = 1000,
                 regularization: str = 'l2', lambda_reg: float = 0.01):
        self.learning_rate = learning_rate
        self.max_iterations = max_iterations
        self.regularization = regularization
        self.lambda_reg = lambda_reg
        self.weights = None
        self.bias = None
        self.cost_history = []
        
    def _sigmoid(self, z: np.ndarray) -> np.ndarray:
        """Sigmoid激活函数"""
        # 防止溢出
        z = np.clip(z, -500, 500)
        return 1 / (1 + np.exp(-z))
    
    def fit(self, X: np.ndarray, y: np.ndarray) -> 'LogisticRegression':
        """训练逻辑回归模型"""
        m, n = X.shape
        
        # 初始化参数
        self.weights = np.random.normal(0, 0.01, n)
        self.bias = 0
        
        for i in range(self.max_iterations):
            # 前向传播
            z = np.dot(X, self.weights) + self.bias
            predictions = self._sigmoid(z)
            
            # 计算损失
            cost = self._compute_cost(predictions, y)
            self.cost_history.append(cost)
            
            # 计算梯度
            dw = (1/m) * np.dot(X.T, (predictions - y))
            db = (1/m) * np.sum(predictions - y)
            
            # 正则化
            if self.regularization == 'l2':
                dw += self.lambda_reg * self.weights
            elif self.regularization == 'l1':
                dw += self.lambda_reg * np.sign(self.weights)
            
            # 更新参数
            self.weights -= self.learning_rate * dw
            self.bias -= self.learning_rate * db
            
        return self
    
    def predict(self, X: np.ndarray) -> np.ndarray:
        """预测概率"""
        z = np.dot(X, self.weights) + self.bias
        return self._sigmoid(z)
    
    def predict_class(self, X: np.ndarray, threshold: float = 0.5) -> np.ndarray:
        """预测类别"""
        probabilities = self.predict(X)
        return (probabilities >= threshold).astype(int)
    
    def _compute_cost(self, predictions: np.ndarray, y: np.ndarray) -> float:
        """计算交叉熵损失"""
        m = len(y)
        # 防止log(0)
        predictions = np.clip(predictions, 1e-15, 1-1e-15)
        cost = -(1/m) * np.sum(y * np.log(predictions) + (1-y) * np.log(1-predictions))
        
        # 正则化项
        if self.regularization == 'l2':
            cost += self.lambda_reg * np.sum(self.weights ** 2)
        elif self.regularization == 'l1':
            cost += self.lambda_reg * np.sum(np.abs(self.weights))
            
        return cost
```

### 5.3 决策树算法实现

**信息增益**：
```
IG(S,A) = H(S) - Σ(|Sv|/|S|) * H(Sv)
```

**基尼不纯度**：
```
Gini(S) = 1 - Σ(pi)²
```

**生产级实现**：
```python
from typing import Optional, Union
import pandas as pd

class DecisionNode:
    def __init__(self, feature_idx: int = None, threshold: float = None,
                 left: Optional['DecisionNode'] = None, 
                 right: Optional['DecisionNode'] = None,
                 value: Optional[Union[int, float]] = None):
        self.feature_idx = feature_idx
        self.threshold = threshold
        self.left = left
        self.right = right
        self.value = value

class DecisionTree:
    def __init__(self, max_depth: int = 10, min_samples_split: int = 2,
                 min_samples_leaf: int = 1, criterion: str = 'gini'):
        self.max_depth = max_depth
        self.min_samples_split = min_samples_split
        self.min_samples_leaf = min_samples_leaf
        self.criterion = criterion
        self.root = None
        
    def fit(self, X: np.ndarray, y: np.ndarray) -> 'DecisionTree':
        """训练决策树"""
        self.root = self._build_tree(X, y, depth=0)
        return self
    
    def _build_tree(self, X: np.ndarray, y: np.ndarray, depth: int) -> DecisionNode:
        """递归构建决策树"""
        n_samples, n_features = X.shape
        
        # 停止条件
        if (depth >= self.max_depth or 
            n_samples < self.min_samples_split or
            len(np.unique(y)) == 1):
            return DecisionNode(value=self._most_common_label(y))
        
        # 寻找最佳分割
        best_feature, best_threshold = self._find_best_split(X, y)
        
        if best_feature is None:
            return DecisionNode(value=self._most_common_label(y))
        
        # 分割数据
        left_indices = X[:, best_feature] <= best_threshold
        right_indices = ~left_indices
        
        if (len(y[left_indices]) < self.min_samples_leaf or 
            len(y[right_indices]) < self.min_samples_leaf):
            return DecisionNode(value=self._most_common_label(y))
        
        # 递归构建子树
        left_subtree = self._build_tree(X[left_indices], y[left_indices], depth + 1)
        right_subtree = self._build_tree(X[right_indices], y[right_indices], depth + 1)
        
        return DecisionNode(best_feature, best_threshold, left_subtree, right_subtree)
    
    def _find_best_split(self, X: np.ndarray, y: np.ndarray) -> Tuple[int, float]:
        """寻找最佳分割点"""
        best_gini = float('inf')
        best_feature = None
        best_threshold = None
        
        for feature_idx in range(X.shape[1]):
            thresholds = np.unique(X[:, feature_idx])
            
            for threshold in thresholds:
                left_indices = X[:, feature_idx] <= threshold
                right_indices = ~left_indices
                
                if len(y[left_indices]) == 0 or len(y[right_indices]) == 0:
                    continue
                
                gini = self._compute_gini(y[left_indices], y[right_indices])
                
                if gini < best_gini:
                    best_gini = gini
                    best_feature = feature_idx
                    best_threshold = threshold
        
        return best_feature, best_threshold
    
    def _compute_gini(self, left_y: np.ndarray, right_y: np.ndarray) -> float:
        """计算基尼不纯度"""
        n_left, n_right = len(left_y), len(right_y)
        n_total = n_left + n_right
        
        gini_left = 1 - sum((np.sum(left_y == label) / n_left) ** 2 
                           for label in np.unique(left_y))
        gini_right = 1 - sum((np.sum(right_y == label) / n_right) ** 2 
                            for label in np.unique(right_y))
        
        return (n_left / n_total) * gini_left + (n_right / n_total) * gini_right
    
    def _most_common_label(self, y: np.ndarray) -> Union[int, float]:
        """返回最常见的标签"""
        if len(y) == 0:
            return 0
        
        if y.dtype.kind in ['i', 'u']:  # 整数类型
            return np.bincount(y).argmax()
        else:  # 浮点类型
            return np.mean(y)
    
    def predict(self, X: np.ndarray) -> np.ndarray:
        """预测"""
        predictions = []
        for sample in X:
            predictions.append(self._predict_sample(sample, self.root))
        return np.array(predictions)
    
    def _predict_sample(self, sample: np.ndarray, node: DecisionNode) -> Union[int, float]:
        """预测单个样本"""
        if node.value is not None:
            return node.value
        
        if sample[node.feature_idx] <= node.threshold:
            return self._predict_sample(sample, node.left)
        else:
            return self._predict_sample(sample, node.right)
```

## 六、深度学习框架深度对比

### 6.1 PyTorch vs TensorFlow

**PyTorch优势**：
- 动态计算图，调试友好
- Python原生支持
- 研究社区活跃
- 内存使用效率高

**TensorFlow优势**：
- 生产环境成熟
- 分布式训练支持好
- TensorBoard可视化
- 移动端部署支持

**性能对比代码**：
```python
import time
import torch
import tensorflow as tf
import numpy as np

def benchmark_pytorch():
    """PyTorch性能测试"""
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    # 创建模型
    model = torch.nn.Sequential(
        torch.nn.Linear(784, 256),
        torch.nn.ReLU(),
        torch.nn.Linear(256, 128),
        torch.nn.ReLU(),
        torch.nn.Linear(128, 10)
    ).to(device)
    
    criterion = torch.nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
    
    # 生成数据
    X = torch.randn(1000, 784).to(device)
    y = torch.randint(0, 10, (1000,)).to(device)
    
    # 训练循环
    start_time = time.time()
    for epoch in range(100):
        optimizer.zero_grad()
        outputs = model(X)
        loss = criterion(outputs, y)
        loss.backward()
        optimizer.step()
    
    pytorch_time = time.time() - start_time
    return pytorch_time

def benchmark_tensorflow():
    """TensorFlow性能测试"""
    # 创建模型
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(256, activation='relu', input_shape=(784,)),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dense(10)
    ])
    
    model.compile(optimizer='adam',
                  loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=['accuracy'])
    
    # 生成数据
    X = np.random.randn(1000, 784).astype(np.float32)
    y = np.random.randint(0, 10, (1000,))
    
    # 训练
    start_time = time.time()
    model.fit(X, y, epochs=100, verbose=0)
    tensorflow_time = time.time() - start_time
    
    return tensorflow_time

# 运行基准测试
if __name__ == "__main__":
    pytorch_time = benchmark_pytorch()
    tensorflow_time = benchmark_tensorflow()
    
    print(f"PyTorch训练时间: {pytorch_time:.2f}秒")
    print(f"TensorFlow训练时间: {tensorflow_time:.2f}秒")
    print(f"性能比: {tensorflow_time/pytorch_time:.2f}x")
```

### 6.2 模型训练优化策略

**学习率调度**：
```python
import torch.optim.lr_scheduler as lr_scheduler

class LearningRateScheduler:
    def __init__(self, optimizer, scheduler_type: str = 'step'):
        self.optimizer = optimizer
        self.scheduler_type = scheduler_type
        
        if scheduler_type == 'step':
            self.scheduler = lr_scheduler.StepLR(optimizer, step_size=30, gamma=0.1)
        elif scheduler_type == 'cosine':
            self.scheduler = lr_scheduler.CosineAnnealingLR(optimizer, T_max=100)
        elif scheduler_type == 'plateau':
            self.scheduler = lr_scheduler.ReduceLROnPlateau(optimizer, mode='min', patience=10)
    
    def step(self, metric=None):
        if self.scheduler_type == 'plateau':
            self.scheduler.step(metric)
        else:
            self.scheduler.step()
    
    def get_lr(self):
        return self.optimizer.param_groups[0]['lr']
```

**梯度裁剪**：
```python
def train_with_gradient_clipping(model, train_loader, criterion, optimizer, max_grad_norm=1.0):
    """带梯度裁剪的训练"""
    model.train()
    total_loss = 0
    
    for batch_idx, (data, target) in enumerate(train_loader):
        optimizer.zero_grad()
        output = model(data)
        loss = criterion(output, target)
        loss.backward()
        
        # 梯度裁剪
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_grad_norm)
        
        optimizer.step()
        total_loss += loss.item()
    
    return total_loss / len(train_loader)
```

**混合精度训练**：
```python
from torch.cuda.amp import autocast, GradScaler

def train_with_mixed_precision(model, train_loader, criterion, optimizer):
    """混合精度训练"""
    scaler = GradScaler()
    model.train()
    
    for data, target in train_loader:
        optimizer.zero_grad()
        
        with autocast():
            output = model(data)
            loss = criterion(output, target)
        
        scaler.scale(loss).backward()
        scaler.step(optimizer)
        scaler.update()
```

## 七、特征工程完整实践

### 7.1 特征选择方法

**单变量特征选择**：
```python
from sklearn.feature_selection import SelectKBest, f_classif, mutual_info_classif
from sklearn.feature_selection import RFE, SelectFromModel
from sklearn.ensemble import RandomForestClassifier

class FeatureSelector:
    def __init__(self, method: str = 'univariate', k: int = 10):
        self.method = method
        self.k = k
        self.selector = None
        
    def fit_transform(self, X, y):
        """特征选择"""
        if self.method == 'univariate':
            self.selector = SelectKBest(score_func=f_classif, k=self.k)
        elif self.method == 'mutual_info':
            self.selector = SelectKBest(score_func=mutual_info_classif, k=self.k)
        elif self.method == 'rfe':
            estimator = RandomForestClassifier(n_estimators=100, random_state=42)
            self.selector = RFE(estimator, n_features_to_select=self.k)
        elif self.method == 'embedded':
            estimator = RandomForestClassifier(n_estimators=100, random_state=42)
            self.selector = SelectFromModel(estimator, threshold='median')
        
        return self.selector.fit_transform(X, y)
    
    def get_feature_scores(self):
        """获取特征重要性分数"""
        if hasattr(self.selector, 'scores_'):
            return self.selector.scores_
        elif hasattr(self.selector, 'feature_importances_'):
            return self.selector.feature_importances_
        return None
```

**特征变换**：
```python
from sklearn.preprocessing import StandardScaler, MinMaxScaler, RobustScaler
from sklearn.preprocessing import LabelEncoder, OneHotEncoder
from sklearn.decomposition import PCA, TruncatedSVD

class FeatureTransformer:
    def __init__(self, scaling_method: str = 'standard', 
                 encoding_method: str = 'onehot', 
                 dimensionality_reduction: str = None):
        self.scaling_method = scaling_method
        self.encoding_method = encoding_method
        self.dimensionality_reduction = dimensionality_reduction
        
        # 初始化变换器
        if scaling_method == 'standard':
            self.scaler = StandardScaler()
        elif scaling_method == 'minmax':
            self.scaler = MinMaxScaler()
        elif scaling_method == 'robust':
            self.scaler = RobustScaler()
        
        if encoding_method == 'onehot':
            self.encoder = OneHotEncoder(sparse=False, handle_unknown='ignore')
        elif encoding_method == 'label':
            self.encoder = LabelEncoder()
        
        if dimensionality_reduction == 'pca':
            self.reducer = PCA(n_components=0.95)  # 保留95%方差
        elif dimensionality_reduction == 'svd':
            self.reducer = TruncatedSVD(n_components=50)
    
    def fit_transform(self, X_numerical, X_categorical=None):
        """特征变换"""
        # 数值特征缩放
        X_scaled = self.scaler.fit_transform(X_numerical)
        
        # 分类特征编码
        if X_categorical is not None:
            X_encoded = self.encoder.fit_transform(X_categorical)
            X_transformed = np.hstack([X_scaled, X_encoded])
        else:
            X_transformed = X_scaled
        
        # 降维
        if self.dimensionality_reduction:
            X_transformed = self.reducer.fit_transform(X_transformed)
        
        return X_transformed
```

### 7.2 时间序列特征工程

**时间特征提取**：
```python
import pandas as pd
from datetime import datetime

class TimeSeriesFeatureExtractor:
    def __init__(self):
        self.feature_names = []
    
    def extract_time_features(self, df: pd.DataFrame, time_col: str) -> pd.DataFrame:
        """提取时间特征"""
        df = df.copy()
        df[time_col] = pd.to_datetime(df[time_col])
        
        # 基础时间特征
        df['year'] = df[time_col].dt.year
        df['month'] = df[time_col].dt.month
        df['day'] = df[time_col].dt.day
        df['hour'] = df[time_col].dt.hour
        df['dayofweek'] = df[time_col].dt.dayofweek
        df['dayofyear'] = df[time_col].dt.dayofyear
        df['weekofyear'] = df[time_col].dt.isocalendar().week
        
        # 周期性特征
        df['month_sin'] = np.sin(2 * np.pi * df['month'] / 12)
        df['month_cos'] = np.cos(2 * np.pi * df['month'] / 12)
        df['day_sin'] = np.sin(2 * np.pi * df['day'] / 31)
        df['day_cos'] = np.cos(2 * np.pi * df['day'] / 31)
        df['hour_sin'] = np.sin(2 * np.pi * df['hour'] / 24)
        df['hour_cos'] = np.cos(2 * np.pi * df['hour'] / 24)
        
        # 工作日/周末
        df['is_weekend'] = (df['dayofweek'] >= 5).astype(int)
        
        # 节假日特征（简化版）
        df['is_holiday'] = self._is_holiday(df[time_col])
        
        return df
    
    def _is_holiday(self, dates: pd.Series) -> pd.Series:
        """判断是否为节假日（简化版）"""
        # 这里可以集成更复杂的节假日判断逻辑
        holidays = ['2023-01-01', '2023-12-25']  # 示例
        return dates.dt.date.astype(str).isin(holidays).astype(int)
    
    def create_lag_features(self, df: pd.DataFrame, value_col: str, 
                           lags: list = [1, 2, 3, 7, 14, 30]) -> pd.DataFrame:
        """创建滞后特征"""
        df = df.copy()
        
        for lag in lags:
            df[f'{value_col}_lag_{lag}'] = df[value_col].shift(lag)
        
        return df
    
    def create_rolling_features(self, df: pd.DataFrame, value_col: str,
                               windows: list = [3, 7, 14, 30]) -> pd.DataFrame:
        """创建滚动窗口特征"""
        df = df.copy()
        
        for window in windows:
            df[f'{value_col}_rolling_mean_{window}'] = df[value_col].rolling(window).mean()
            df[f'{value_col}_rolling_std_{window}'] = df[value_col].rolling(window).std()
            df[f'{value_col}_rolling_max_{window}'] = df[value_col].rolling(window).max()
            df[f'{value_col}_rolling_min_{window}'] = df[value_col].rolling(window).min()
        
        return df
```

## 八、模型评估与选择

### 8.1 交叉验证实现

**时间序列交叉验证**：
```python
from sklearn.model_selection import TimeSeriesSplit
from sklearn.metrics import mean_squared_error, mean_absolute_error

class TimeSeriesValidator:
    def __init__(self, n_splits: int = 5):
        self.n_splits = n_splits
        self.tscv = TimeSeriesSplit(n_splits=n_splits)
    
    def validate(self, model, X, y):
        """时间序列交叉验证"""
        scores = []
        
        for train_idx, val_idx in self.tscv.split(X):
            X_train, X_val = X[train_idx], X[val_idx]
            y_train, y_val = y[train_idx], y[val_idx]
            
            # 训练模型
            model.fit(X_train, y_train)
            
            # 预测
            y_pred = model.predict(X_val)
            
            # 评估
            mse = mean_squared_error(y_val, y_pred)
            mae = mean_absolute_error(y_val, y_pred)
            
            scores.append({'mse': mse, 'mae': mae})
        
        return scores
```

**分层交叉验证**：
```python
from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score

class ClassificationValidator:
    def __init__(self, n_splits: int = 5):
        self.n_splits = n_splits
        self.skf = StratifiedKFold(n_splits=n_splits, shuffle=True, random_state=42)
    
    def validate(self, model, X, y):
        """分类模型交叉验证"""
        scores = []
        
        for train_idx, val_idx in self.skf.split(X, y):
            X_train, X_val = X[train_idx], X[val_idx]
            y_train, y_val = y[train_idx], y[val_idx]
            
            # 训练模型
            model.fit(X_train, y_train)
            
            # 预测
            y_pred = model.predict(X_val)
            
            # 评估指标
            accuracy = accuracy_score(y_val, y_pred)
            precision = precision_score(y_val, y_pred, average='weighted')
            recall = recall_score(y_val, y_pred, average='weighted')
            f1 = f1_score(y_val, y_pred, average='weighted')
            
            scores.append({
                'accuracy': accuracy,
                'precision': precision,
                'recall': recall,
                'f1': f1
            })
        
        return scores
```

### 8.2 超参数优化

**网格搜索**：
```python
from sklearn.model_selection import GridSearchCV
from sklearn.ensemble import RandomForestClassifier

class HyperparameterOptimizer:
    def __init__(self, cv_folds: int = 5):
        self.cv_folds = cv_folds
    
    def grid_search(self, model, param_grid: dict, X, y, scoring: str = 'accuracy'):
        """网格搜索超参数优化"""
        grid_search = GridSearchCV(
            estimator=model,
            param_grid=param_grid,
            cv=self.cv_folds,
            scoring=scoring,
            n_jobs=-1,
            verbose=1
        )
        
        grid_search.fit(X, y)
        
        return {
            'best_params': grid_search.best_params_,
            'best_score': grid_search.best_score_,
            'best_estimator': grid_search.best_estimator_,
            'cv_results': grid_search.cv_results_
        }
    
    def random_search(self, model, param_distributions: dict, 
                     n_iter: int = 100, X=None, y=None, scoring: str = 'accuracy'):
        """随机搜索超参数优化"""
        from sklearn.model_selection import RandomizedSearchCV
        
        random_search = RandomizedSearchCV(
            estimator=model,
            param_distributions=param_distributions,
            n_iter=n_iter,
            cv=self.cv_folds,
            scoring=scoring,
            n_jobs=-1,
            random_state=42,
            verbose=1
        )
        
        random_search.fit(X, y)
        
        return {
            'best_params': random_search.best_params_,
            'best_score': random_search.best_score_,
            'best_estimator': random_search.best_estimator_,
            'cv_results': random_search.cv_results_
        }
```

**贝叶斯优化**：
```python
from skopt import gp_minimize
from skopt.space import Real, Integer, Categorical
from skopt.utils import use_named_args

class BayesianOptimizer:
    def __init__(self, n_calls: int = 50):
        self.n_calls = n_calls
    
    def optimize(self, model_class, param_space: list, X, y, scoring_func):
        """贝叶斯优化"""
        @use_named_args(param_space)
        def objective(**params):
            model = model_class(**params)
            scores = []
            
            # 交叉验证
            skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
            for train_idx, val_idx in skf.split(X, y):
                X_train, X_val = X[train_idx], X[val_idx]
                y_train, y_val = y[train_idx], y[val_idx]
                
                model.fit(X_train, y_train)
                y_pred = model.predict(X_val)
                score = scoring_func(y_val, y_pred)
                scores.append(score)
            
            return -np.mean(scores)  # 最小化负分数
        
        result = gp_minimize(objective, param_space, n_calls=self.n_calls)
        
        return {
            'best_params': dict(zip([dim.name for dim in param_space], result.x)),
            'best_score': -result.fun,
            'optimization_history': result.func_vals
        }
```

## 九、AutoML工具实战

### 9.1 Auto-Sklearn实现

```python
import autosklearn.classification
import autosklearn.regression
from sklearn.model_selection import train_test_split

class AutoMLPipeline:
    def __init__(self, task_type: str = 'classification', time_limit: int = 300):
        self.task_type = task_type
        self.time_limit = time_limit
        self.automl = None
        
    def fit(self, X, y):
        """训练AutoML模型"""
        if self.task_type == 'classification':
            self.automl = autosklearn.classification.AutoSklearnClassifier(
                time_left_for_this_task=self.time_limit,
                per_run_time_limit=30,
                memory_limit=3072,
                n_jobs=-1,
                ensemble_size=1,
                initial_configurations_via_metalearning=0,
                include_estimators=['random_forest', 'extra_trees', 'gradient_boosting'],
                include_preprocessors=['feature_selection', 'polynomial', 'pca']
            )
        else:
            self.automl = autosklearn.regression.AutoSklearnRegressor(
                time_left_for_this_task=self.time_limit,
                per_run_time_limit=30,
                memory_limit=3072,
                n_jobs=-1,
                ensemble_size=1,
                initial_configurations_via_metalearning=0
            )
        
        self.automl.fit(X, y)
        return self
    
    def predict(self, X):
        """预测"""
        return self.automl.predict(X)
    
    def predict_proba(self, X):
        """预测概率（仅分类）"""
        if self.task_type == 'classification':
            return self.automl.predict_proba(X)
        else:
            raise ValueError("predict_proba only available for classification")
    
    def get_models_with_weights(self):
        """获取模型权重"""
        return self.automl.get_models_with_weights()
    
    def get_configuration_space(self):
        """获取配置空间"""
        return self.automl.get_configuration_space()
```

### 9.2 TPOT实现

```python
from tpot import TPOTClassifier, TPOTRegressor

class TPOTPipeline:
    def __init__(self, task_type: str = 'classification', generations: int = 10,
                 population_size: int = 20, cv: int = 5):
        self.task_type = task_type
        self.generations = generations
        self.population_size = population_size
        self.cv = cv
        self.tpot = None
        
    def fit(self, X, y):
        """训练TPOT模型"""
        if self.task_type == 'classification':
            self.tpot = TPOTClassifier(
                generations=self.generations,
                population_size=self.population_size,
                cv=self.cv,
                random_state=42,
                verbosity=2,
                n_jobs=-1
            )
        else:
            self.tpot = TPOTRegressor(
                generations=self.generations,
                population_size=self.population_size,
                cv=self.cv,
                random_state=42,
                verbosity=2,
                n_jobs=-1
            )
        
        self.tpot.fit(X, y)
        return self
    
    def predict(self, X):
        """预测"""
        return self.tpot.predict(X)
    
    def score(self, X, y):
        """评分"""
        return self.tpot.score(X, y)
    
    def export(self, pipeline_file: str):
        """导出最佳管道"""
        self.tpot.export(pipeline_file)
```

## 十、生产级模型部署

### 10.1 模型服务化架构

**Flask API服务**：
```python
from flask import Flask, request, jsonify
import joblib
import numpy as np
import logging
from typing import Dict, Any
import traceback

class ModelService:
    def __init__(self, model_path: str, scaler_path: str = None):
        self.model = joblib.load(model_path)
        self.scaler = joblib.load(scaler_path) if scaler_path else None
        self.logger = logging.getLogger(__name__)
        
    def preprocess(self, data: Dict[str, Any]) -> np.ndarray:
        """数据预处理"""
        # 特征提取和转换
        features = []
        
        # 数值特征
        numerical_features = ['feature1', 'feature2', 'feature3']
        for feature in numerical_features:
            features.append(data.get(feature, 0))
        
        # 分类特征编码
        categorical_features = ['category1', 'category2']
        for feature in categorical_features:
            value = data.get(feature, 'unknown')
            # 这里可以添加更复杂的编码逻辑
            features.append(hash(value) % 100)
        
        features_array = np.array(features).reshape(1, -1)
        
        # 特征缩放
        if self.scaler:
            features_array = self.scaler.transform(features_array)
        
        return features_array
    
    def predict(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """预测"""
        try:
            # 预处理
            features = self.preprocess(data)
            
            # 预测
            prediction = self.model.predict(features)[0]
            probability = None
            
            # 获取预测概率（如果模型支持）
            if hasattr(self.model, 'predict_proba'):
                probability = self.model.predict_proba(features)[0].tolist()
            
            return {
                'prediction': prediction,
                'probability': probability,
                'status': 'success'
            }
            
        except Exception as e:
            self.logger.error(f"Prediction error: {str(e)}")
            self.logger.error(traceback.format_exc())
            return {
                'prediction': None,
                'probability': None,
                'status': 'error',
                'error': str(e)
            }

# Flask应用
app = Flask(__name__)
model_service = ModelService('model.pkl', 'scaler.pkl')

@app.route('/predict', methods=['POST'])
def predict():
    """预测接口"""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        result = model_service.predict(data)
        
        if result['status'] == 'error':
            return jsonify(result), 500
        
        return jsonify(result)
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return jsonify({'status': 'healthy'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
```

**Docker部署**：
```dockerfile
FROM python:3.9-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制模型和代码
COPY model.pkl scaler.pkl app.py ./

# 创建非root用户
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# 暴露端口
EXPOSE 5000

# 启动命令
CMD ["python", "app.py"]
```

### 10.2 模型监控

**模型性能监控**：
```python
import time
import psutil
import logging
from datetime import datetime
from typing import Dict, List
import json

class ModelMonitor:
    def __init__(self, model_name: str):
        self.model_name = model_name
        self.logger = logging.getLogger(__name__)
        self.metrics_history = []
        
    def log_prediction(self, input_data: Dict, prediction: Any, 
                      processing_time: float, timestamp: datetime = None):
        """记录预测日志"""
        if timestamp is None:
            timestamp = datetime.now()
        
        log_entry = {
            'timestamp': timestamp.isoformat(),
            'model_name': self.model_name,
            'input_hash': hash(str(input_data)),
            'prediction': str(prediction),
            'processing_time': processing_time,
            'memory_usage': psutil.Process().memory_info().rss / 1024 / 1024,  # MB
            'cpu_usage': psutil.cpu_percent()
        }
        
        self.metrics_history.append(log_entry)
        self.logger.info(f"Prediction logged: {json.dumps(log_entry)}")
    
    def get_performance_metrics(self, time_window_minutes: int = 60) -> Dict:
        """获取性能指标"""
        cutoff_time = datetime.now() - timedelta(minutes=time_window_minutes)
        recent_metrics = [
            m for m in self.metrics_history 
            if datetime.fromisoformat(m['timestamp']) > cutoff_time
        ]
        
        if not recent_metrics:
            return {}
        
        processing_times = [m['processing_time'] for m in recent_metrics]
        memory_usage = [m['memory_usage'] for m in recent_metrics]
        cpu_usage = [m['cpu_usage'] for m in recent_metrics]
        
        return {
            'total_predictions': len(recent_metrics),
            'avg_processing_time': np.mean(processing_times),
            'max_processing_time': np.max(processing_times),
            'avg_memory_usage': np.mean(memory_usage),
            'max_memory_usage': np.max(memory_usage),
            'avg_cpu_usage': np.mean(cpu_usage),
            'max_cpu_usage': np.max(cpu_usage)
        }
    
    def detect_drift(self, reference_data: List[Dict], 
                    current_data: List[Dict], threshold: float = 0.1) -> bool:
        """检测数据漂移"""
        # 简化的漂移检测实现
        # 实际应用中可以使用更复杂的统计方法
        
        if len(reference_data) == 0 or len(current_data) == 0:
            return False
        
        # 计算特征分布差异
        drift_score = 0
        feature_names = reference_data[0].keys()
        
        for feature in feature_names:
            ref_values = [d[feature] for d in reference_data if feature in d]
            curr_values = [d[feature] for d in current_data if feature in d]
            
            if ref_values and curr_values:
                ref_mean = np.mean(ref_values)
                curr_mean = np.mean(curr_values)
                
                if ref_mean != 0:
                    drift_score += abs(curr_mean - ref_mean) / abs(ref_mean)
        
        avg_drift = drift_score / len(feature_names)
        return avg_drift > threshold
```

## 十一、高频面试题深度解析

### Q1：机器学习中的过拟合和欠拟合如何解决？

**过拟合解决方案**：

1. **数据层面**：
   - 增加训练数据
   - 数据增强（图像旋转、翻转等）
   - 收集更多相关数据

2. **模型层面**：
   - 减少模型复杂度
   - 使用更简单的模型
   - 减少网络层数或神经元数量

3. **正则化技术**：
   ```python
   # L1正则化
   from sklearn.linear_model import Lasso
   lasso = Lasso(alpha=0.1)
   
   # L2正则化
   from sklearn.linear_model import Ridge
   ridge = Ridge(alpha=0.1)
   
   # Dropout（深度学习）
   import torch.nn as nn
   dropout = nn.Dropout(0.5)
   ```

4. **集成方法**：
   - Bagging（随机森林）
   - Boosting（AdaBoost、XGBoost）
   - Stacking

**欠拟合解决方案**：

1. **增加模型复杂度**：
   - 增加网络层数
   - 增加神经元数量
   - 使用更复杂的模型

2. **特征工程**：
   - 增加更多特征
   - 特征组合
   - 多项式特征

3. **减少正则化**：
   - 降低正则化参数
   - 移除Dropout
   - 减少L1/L2惩罚

### Q2：常用的优化算法及其特点？

**SGD（随机梯度下降）**：
```python
# 特点：简单但可能震荡
optimizer = torch.optim.SGD(model.parameters(), lr=0.01, momentum=0.9)
```

**Adam（自适应矩估计）**：
```python
# 特点：自适应学习率，收敛快
optimizer = torch.optim.Adam(model.parameters(), lr=0.001, betas=(0.9, 0.999))
```

**RMSprop**：
```python
# 特点：适合处理非平稳目标
optimizer = torch.optim.RMSprop(model.parameters(), lr=0.01, alpha=0.99)
```

**AdaGrad**：
```python
# 特点：自适应学习率，适合稀疏数据
optimizer = torch.optim.Adagrad(model.parameters(), lr=0.01)
```

### Q3：激活函数的作用和选择？

**激活函数作用**：
1. 引入非线性，使神经网络能够学习复杂模式
2. 决定神经元是否被激活
3. 影响梯度传播

**常用激活函数**：

```python
import torch.nn.functional as F

# ReLU：最常用，计算简单
x = F.relu(x)

# Sigmoid：输出0-1，适合二分类
x = torch.sigmoid(x)

# Tanh：输出-1到1，零中心化
x = torch.tanh(x)

# Leaky ReLU：解决ReLU死神经元问题
x = F.leaky_relu(x, negative_slope=0.01)

# Swish：Google提出，性能更好
def swish(x):
    return x * torch.sigmoid(x)
```

### Q4：如何处理类别不平衡问题？

**数据层面**：
```python
from imblearn.over_sampling import SMOTE
from imblearn.under_sampling import RandomUnderSampler

# 过采样
smote = SMOTE(random_state=42)
X_resampled, y_resampled = smote.fit_resample(X, y)

# 欠采样
undersampler = RandomUnderSampler(random_state=42)
X_resampled, y_resampled = undersampler.fit_resample(X, y)
```

**算法层面**：
```python
from sklearn.ensemble import RandomForestClassifier

# 调整类别权重
rf = RandomForestClassifier(class_weight='balanced')

# 自定义权重
class_weights = {0: 1, 1: 10}  # 少数类权重更高
rf = RandomForestClassifier(class_weight=class_weights)
```

**评估指标**：
```python
from sklearn.metrics import classification_report, confusion_matrix

# 使用F1-score、Precision、Recall
print(classification_report(y_true, y_pred))

# 混淆矩阵
cm = confusion_matrix(y_true, y_pred)
```

### Q5：深度学习中的梯度消失和梯度爆炸问题？

**梯度消失**：
- 原因：深层网络中梯度在反向传播时逐渐减小
- 解决方案：
  - 使用ReLU激活函数
  - 批归一化（Batch Normalization）
  - 残差连接（ResNet）
  - LSTM/GRU（RNN中）

**梯度爆炸**：
- 原因：梯度在反向传播时指数级增长
- 解决方案：
  - 梯度裁剪
  - 权重初始化（Xavier、He初始化）
  - 学习率调整

```python
# 梯度裁剪
torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)

# 批归一化
import torch.nn as nn
bn = nn.BatchNorm1d(128)

# 残差连接
class ResidualBlock(nn.Module):
    def __init__(self, in_channels, out_channels):
        super().__init__()
        self.conv1 = nn.Conv2d(in_channels, out_channels, 3, padding=1)
        self.conv2 = nn.Conv2d(out_channels, out_channels, 3, padding=1)
        self.bn1 = nn.BatchNorm2d(out_channels)
        self.bn2 = nn.BatchNorm2d(out_channels)
        
    def forward(self, x):
        residual = x
        out = F.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))
        out += residual  # 残差连接
        return F.relu(out)
```

---

**关键字**：机器学习、深度学习、PyTorch、TensorFlow、CNN、LSTM、特征工程、模型评估、超参数优化、AutoML、模型部署、过拟合、欠拟合、优化算法、激活函数、类别不平衡、梯度消失、梯度爆炸

