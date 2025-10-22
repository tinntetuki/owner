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

---

**关键字**：机器学习、深度学习、PyTorch、TensorFlow、CNN、LSTM

