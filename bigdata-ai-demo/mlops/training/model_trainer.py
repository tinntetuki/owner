"""
模型训练模块
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional, Tuple
from datetime import datetime
import joblib
import json
import yaml
import os
from pathlib import Path
import mlflow
import mlflow.sklearn
import mlflow.tensorflow
import mlflow.pytorch
from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler, LabelEncoder
import xgboost as xgb
import lightgbm as lgb
import tensorflow as tf
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
import optuna
from optuna.integration.mlflow import MLflowCallback
import wandb
from dataclasses import dataclass
from abc import ABC, abstractmethod
import asyncio
import aiohttp
from concurrent.futures import ThreadPoolExecutor
import threading
import time

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class TrainingConfig:
    """训练配置"""
    model_name: str
    model_type: str
    dataset_path: str
    target_column: str
    test_size: float = 0.2
    random_state: int = 42
    cv_folds: int = 5
    hyperparameters: Dict[str, Any] = None
    feature_columns: List[str] = None
    preprocessing_steps: List[str] = None


@dataclass
class ModelMetrics:
    """模型指标"""
    accuracy: float
    precision: float
    recall: float
    f1_score: float
    roc_auc: float
    training_time: float
    model_size: float
    cross_val_scores: List[float] = None


class BaseModel(ABC):
    """模型基类"""
    
    def __init__(self, config: TrainingConfig):
        self.config = config
        self.model = None
        self.scaler = None
        self.label_encoder = None
        self.feature_columns = None
        self.logger = logging.getLogger(self.__class__.__name__)
    
    @abstractmethod
    def build_model(self, **kwargs) -> Any:
        """构建模型"""
        pass
    
    @abstractmethod
    def train(self, X_train: pd.DataFrame, y_train: pd.Series, X_val: pd.DataFrame = None, y_val: pd.Series = None) -> None:
        """训练模型"""
        pass
    
    @abstractmethod
    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """预测"""
        pass
    
    @abstractmethod
    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """预测概率"""
        pass
    
    def save_model(self, model_path: str) -> None:
        """保存模型"""
        model_data = {
            'model': self.model,
            'scaler': self.scaler,
            'label_encoder': self.label_encoder,
            'feature_columns': self.feature_columns,
            'config': self.config
        }
        joblib.dump(model_data, model_path)
        self.logger.info(f"模型保存成功: {model_path}")
    
    def load_model(self, model_path: str) -> None:
        """加载模型"""
        model_data = joblib.load(model_path)
        self.model = model_data['model']
        self.scaler = model_data['scaler']
        self.label_encoder = model_data['label_encoder']
        self.feature_columns = model_data['feature_columns']
        self.logger.info(f"模型加载成功: {model_path}")


class SklearnModel(BaseModel):
    """Scikit-learn模型"""
    
    def build_model(self, **kwargs) -> Any:
        """构建模型"""
        model_type = self.config.model_type.lower()
        
        if model_type == 'random_forest':
            return RandomForestClassifier(**kwargs)
        elif model_type == 'gradient_boosting':
            return GradientBoostingClassifier(**kwargs)
        elif model_type == 'logistic_regression':
            return LogisticRegression(**kwargs)
        else:
            raise ValueError(f"不支持的模型类型: {model_type}")
    
    def train(self, X_train: pd.DataFrame, y_train: pd.Series, X_val: pd.DataFrame = None, y_val: pd.Series = None) -> None:
        """训练模型"""
        # 数据预处理
        X_train_processed, self.scaler, self.label_encoder = self._preprocess_data(X_train, y_train)
        
        # 构建模型
        self.model = self.build_model(**self.config.hyperparameters or {})
        
        # 训练模型
        start_time = time.time()
        self.model.fit(X_train_processed, y_train)
        training_time = time.time() - start_time
        
        self.logger.info(f"模型训练完成，耗时: {training_time:.2f}秒")
    
    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """预测"""
        X_processed = self._preprocess_features(X)
        return self.model.predict(X_processed)
    
    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """预测概率"""
        X_processed = self._preprocess_features(X)
        return self.model.predict_proba(X_processed)
    
    def _preprocess_data(self, X: pd.DataFrame, y: pd.Series) -> Tuple[pd.DataFrame, StandardScaler, LabelEncoder]:
        """数据预处理"""
        # 选择特征列
        if self.config.feature_columns:
            X = X[self.config.feature_columns]
        else:
            X = X.select_dtypes(include=[np.number])
        
        self.feature_columns = X.columns.tolist()
        
        # 处理缺失值
        X = X.fillna(X.mean())
        
        # 标准化
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
        X_scaled = pd.DataFrame(X_scaled, columns=X.columns, index=X.index)
        
        # 标签编码
        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y)
        
        return X_scaled, scaler, label_encoder
    
    def _preprocess_features(self, X: pd.DataFrame) -> pd.DataFrame:
        """预处理特征"""
        if self.feature_columns:
            X = X[self.feature_columns]
        
        X = X.fillna(X.mean())
        X_scaled = self.scaler.transform(X)
        return pd.DataFrame(X_scaled, columns=X.columns, index=X.index)


class XGBoostModel(BaseModel):
    """XGBoost模型"""
    
    def build_model(self, **kwargs) -> Any:
        """构建模型"""
        default_params = {
            'objective': 'binary:logistic',
            'eval_metric': 'logloss',
            'random_state': self.config.random_state
        }
        default_params.update(kwargs)
        return xgb.XGBClassifier(**default_params)
    
    def train(self, X_train: pd.DataFrame, y_train: pd.Series, X_val: pd.DataFrame = None, y_val: pd.Series = None) -> None:
        """训练模型"""
        # 数据预处理
        X_train_processed, self.scaler, self.label_encoder = self._preprocess_data(X_train, y_train)
        
        # 构建模型
        self.model = self.build_model(**self.config.hyperparameters or {})
        
        # 训练模型
        start_time = time.time()
        
        if X_val is not None and y_val is not None:
            X_val_processed = self._preprocess_features(X_val)
            self.model.fit(
                X_train_processed, y_train,
                eval_set=[(X_val_processed, y_val)],
                early_stopping_rounds=10,
                verbose=False
            )
        else:
            self.model.fit(X_train_processed, y_train)
        
        training_time = time.time() - start_time
        self.logger.info(f"XGBoost模型训练完成，耗时: {training_time:.2f}秒")
    
    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """预测"""
        X_processed = self._preprocess_features(X)
        return self.model.predict(X_processed)
    
    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """预测概率"""
        X_processed = self._preprocess_features(X)
        return self.model.predict_proba(X_processed)
    
    def _preprocess_data(self, X: pd.DataFrame, y: pd.Series) -> Tuple[pd.DataFrame, StandardScaler, LabelEncoder]:
        """数据预处理"""
        if self.config.feature_columns:
            X = X[self.config.feature_columns]
        else:
            X = X.select_dtypes(include=[np.number])
        
        self.feature_columns = X.columns.tolist()
        
        # 处理缺失值
        X = X.fillna(X.mean())
        
        # 标准化
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
        X_scaled = pd.DataFrame(X_scaled, columns=X.columns, index=X.index)
        
        # 标签编码
        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y)
        
        return X_scaled, scaler, label_encoder
    
    def _preprocess_features(self, X: pd.DataFrame) -> pd.DataFrame:
        """预处理特征"""
        if self.feature_columns:
            X = X[self.feature_columns]
        
        X = X.fillna(X.mean())
        X_scaled = self.scaler.transform(X)
        return pd.DataFrame(X_scaled, columns=X.columns, index=X.index)


class TensorFlowModel(BaseModel):
    """TensorFlow模型"""
    
    def build_model(self, input_dim: int, **kwargs) -> Any:
        """构建模型"""
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(128, activation='relu', input_shape=(input_dim,)),
            tf.keras.layers.Dropout(0.3),
            tf.keras.layers.Dense(64, activation='relu'),
            tf.keras.layers.Dropout(0.3),
            tf.keras.layers.Dense(32, activation='relu'),
            tf.keras.layers.Dense(1, activation='sigmoid')
        ])
        
        model.compile(
            optimizer='adam',
            loss='binary_crossentropy',
            metrics=['accuracy']
        )
        
        return model
    
    def train(self, X_train: pd.DataFrame, y_train: pd.Series, X_val: pd.DataFrame = None, y_val: pd.Series = None) -> None:
        """训练模型"""
        # 数据预处理
        X_train_processed, self.scaler, self.label_encoder = self._preprocess_data(X_train, y_train)
        
        # 构建模型
        self.model = self.build_model(input_dim=X_train_processed.shape[1])
        
        # 训练模型
        start_time = time.time()
        
        callbacks = [
            tf.keras.callbacks.EarlyStopping(patience=10, restore_best_weights=True),
            tf.keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=5)
        ]
        
        if X_val is not None and y_val is not None:
            X_val_processed = self._preprocess_features(X_val)
            self.model.fit(
                X_train_processed, y_train,
                validation_data=(X_val_processed, y_val),
                epochs=100,
                batch_size=32,
                callbacks=callbacks,
                verbose=0
            )
        else:
            self.model.fit(
                X_train_processed, y_train,
                epochs=100,
                batch_size=32,
                callbacks=callbacks,
                verbose=0
            )
        
        training_time = time.time() - start_time
        self.logger.info(f"TensorFlow模型训练完成，耗时: {training_time:.2f}秒")
    
    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """预测"""
        X_processed = self._preprocess_features(X)
        predictions = self.model.predict(X_processed)
        return (predictions > 0.5).astype(int).flatten()
    
    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """预测概率"""
        X_processed = self._preprocess_features(X)
        predictions = self.model.predict(X_processed)
        return np.column_stack([1 - predictions, predictions])
    
    def _preprocess_data(self, X: pd.DataFrame, y: pd.Series) -> Tuple[pd.DataFrame, StandardScaler, LabelEncoder]:
        """数据预处理"""
        if self.config.feature_columns:
            X = X[self.config.feature_columns]
        else:
            X = X.select_dtypes(include=[np.number])
        
        self.feature_columns = X.columns.tolist()
        
        # 处理缺失值
        X = X.fillna(X.mean())
        
        # 标准化
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
        X_scaled = pd.DataFrame(X_scaled, columns=X.columns, index=X.index)
        
        # 标签编码
        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y)
        
        return X_scaled, scaler, label_encoder
    
    def _preprocess_features(self, X: pd.DataFrame) -> pd.DataFrame:
        """预处理特征"""
        if self.feature_columns:
            X = X[self.feature_columns]
        
        X = X.fillna(X.mean())
        X_scaled = self.scaler.transform(X)
        return pd.DataFrame(X_scaled, columns=X.columns, index=X.index)


class ModelTrainer:
    """模型训练器"""
    
    def __init__(self, config: TrainingConfig):
        self.config = config
        self.logger = logging.getLogger(self.__class__.__name__)
        self.model = None
        self.metrics = None
    
    def load_data(self) -> Tuple[pd.DataFrame, pd.Series]:
        """加载数据"""
        try:
            data = pd.read_csv(self.config.dataset_path)
            
            # 分离特征和目标
            if self.config.feature_columns:
                X = data[self.config.feature_columns]
            else:
                X = data.drop(columns=[self.config.target_column])
            
            y = data[self.config.target_column]
            
            self.logger.info(f"数据加载成功，形状: {X.shape}")
            return X, y
            
        except Exception as e:
            self.logger.error(f"数据加载失败: {str(e)}")
            raise
    
    def split_data(self, X: pd.DataFrame, y: pd.Series) -> Tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
        """分割数据"""
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, 
            test_size=self.config.test_size, 
            random_state=self.config.random_state,
            stratify=y
        )
        
        self.logger.info(f"数据分割完成，训练集: {X_train.shape}, 测试集: {X_test.shape}")
        return X_train, X_test, y_train, y_test
    
    def create_model(self) -> BaseModel:
        """创建模型"""
        model_type = self.config.model_type.lower()
        
        if model_type in ['random_forest', 'gradient_boosting', 'logistic_regression']:
            return SklearnModel(self.config)
        elif model_type == 'xgboost':
            return XGBoostModel(self.config)
        elif model_type == 'tensorflow':
            return TensorFlowModel(self.config)
        else:
            raise ValueError(f"不支持的模型类型: {model_type}")
    
    def train_model(self, X_train: pd.DataFrame, y_train: pd.Series, X_val: pd.DataFrame = None, y_val: pd.Series = None) -> BaseModel:
        """训练模型"""
        self.model = self.create_model()
        
        # 开始MLflow实验
        with mlflow.start_run(run_name=f"{self.config.model_name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"):
            # 记录参数
            mlflow.log_params({
                'model_type': self.config.model_type,
                'test_size': self.config.test_size,
                'random_state': self.config.random_state,
                'cv_folds': self.config.cv_folds
            })
            
            if self.config.hyperparameters:
                mlflow.log_params(self.config.hyperparameters)
            
            # 训练模型
            start_time = time.time()
            self.model.train(X_train, y_train, X_val, y_val)
            training_time = time.time() - start_time
            
            # 记录训练时间
            mlflow.log_metric("training_time", training_time)
            
            # 保存模型
            model_path = f"models/{self.config.model_name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.joblib"
            os.makedirs(os.path.dirname(model_path), exist_ok=True)
            self.model.save_model(model_path)
            
            # 记录模型
            if self.config.model_type.lower() == 'tensorflow':
                mlflow.tensorflow.log_model(self.model.model, "model")
            else:
                mlflow.sklearn.log_model(self.model.model, "model")
        
        return self.model
    
    def evaluate_model(self, X_test: pd.DataFrame, y_test: pd.Series) -> ModelMetrics:
        """评估模型"""
        if self.model is None:
            raise ValueError("模型未训练")
        
        # 预测
        y_pred = self.model.predict(X_test)
        y_pred_proba = self.model.predict_proba(X_test)
        
        # 计算指标
        accuracy = accuracy_score(y_test, y_pred)
        precision = precision_score(y_test, y_pred, average='weighted')
        recall = recall_score(y_test, y_pred, average='weighted')
        f1 = f1_score(y_test, y_pred, average='weighted')
        roc_auc = roc_auc_score(y_test, y_pred_proba[:, 1]) if y_pred_proba.shape[1] == 2 else 0
        
        # 交叉验证
        X_combined = pd.concat([X_test, X_test])  # 简化示例
        y_combined = pd.concat([y_test, y_test])
        cv_scores = cross_val_score(self.model.model, X_combined, y_combined, cv=self.config.cv_folds)
        
        # 模型大小
        model_size = self._calculate_model_size()
        
        self.metrics = ModelMetrics(
            accuracy=accuracy,
            precision=precision,
            recall=recall,
            f1_score=f1,
            roc_auc=roc_auc,
            training_time=0,  # 在训练时记录
            model_size=model_size,
            cross_val_scores=cv_scores.tolist()
        )
        
        # 记录到MLflow
        with mlflow.start_run():
            mlflow.log_metrics({
                'accuracy': accuracy,
                'precision': precision,
                'recall': recall,
                'f1_score': f1,
                'roc_auc': roc_auc,
                'cv_mean': cv_scores.mean(),
                'cv_std': cv_scores.std()
            })
        
        self.logger.info(f"模型评估完成，准确率: {accuracy:.4f}")
        return self.metrics
    
    def _calculate_model_size(self) -> float:
        """计算模型大小（MB）"""
        try:
            import sys
            return sys.getsizeof(self.model.model) / (1024 * 1024)
        except:
            return 0.0
    
    def hyperparameter_tuning(self, X_train: pd.DataFrame, y_train: pd.Series) -> Dict[str, Any]:
        """超参数调优"""
        if self.config.model_type.lower() not in ['random_forest', 'gradient_boosting', 'logistic_regression']:
            self.logger.warning("当前模型类型不支持超参数调优")
            return {}
        
        # 定义参数网格
        param_grids = {
            'random_forest': {
                'n_estimators': [100, 200, 300],
                'max_depth': [10, 20, None],
                'min_samples_split': [2, 5, 10]
            },
            'gradient_boosting': {
                'n_estimators': [100, 200, 300],
                'learning_rate': [0.01, 0.1, 0.2],
                'max_depth': [3, 5, 7]
            },
            'logistic_regression': {
                'C': [0.1, 1, 10],
                'penalty': ['l1', 'l2'],
                'solver': ['liblinear', 'saga']
            }
        }
        
        param_grid = param_grids.get(self.config.model_type.lower(), {})
        if not param_grid:
            return {}
        
        # 网格搜索
        model = self.create_model()
        grid_search = GridSearchCV(
            model.model,
            param_grid,
            cv=self.config.cv_folds,
            scoring='accuracy',
            n_jobs=-1
        )
        
        grid_search.fit(X_train, y_train)
        
        self.logger.info(f"最佳参数: {grid_search.best_params_}")
        self.logger.info(f"最佳得分: {grid_search.best_score_:.4f}")
        
        return grid_search.best_params_
    
    def run_training_pipeline(self) -> Tuple[BaseModel, ModelMetrics]:
        """运行完整训练流程"""
        try:
            # 加载数据
            X, y = self.load_data()
            
            # 分割数据
            X_train, X_test, y_train, y_test = self.split_data(X, y)
            
            # 超参数调优
            best_params = self.hyperparameter_tuning(X_train, y_train)
            if best_params:
                self.config.hyperparameters = best_params
            
            # 训练模型
            model = self.train_model(X_train, y_train, X_test, y_test)
            
            # 评估模型
            metrics = self.evaluate_model(X_test, y_test)
            
            self.logger.info("训练流程完成")
            return model, metrics
            
        except Exception as e:
            self.logger.error(f"训练流程失败: {str(e)}")
            raise


def main():
    """主函数"""
    # 配置MLflow
    mlflow.set_tracking_uri("http://localhost:5000")
    mlflow.set_experiment("model_training")
    
    # 创建训练配置
    config = TrainingConfig(
        model_name="user_behavior_classifier",
        model_type="random_forest",
        dataset_path="data/user_behavior.csv",
        target_column="label",
        test_size=0.2,
        random_state=42,
        cv_folds=5,
        hyperparameters={
            'n_estimators': 100,
            'max_depth': 10,
            'random_state': 42
        }
    )
    
    # 创建训练器
    trainer = ModelTrainer(config)
    
    # 运行训练流程
    model, metrics = trainer.run_training_pipeline()
    
    print(f"训练完成，准确率: {metrics.accuracy:.4f}")


if __name__ == "__main__":
    main()
