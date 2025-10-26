"""
特征存储平台
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional, Union
from datetime import datetime, timedelta
import redis
import pymongo
from pymongo import MongoClient
import json
import yaml
import os
from pathlib import Path
import hashlib
import pickle
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
class Feature:
    """特征定义"""
    name: str
    value: Any
    timestamp: datetime
    entity_id: str
    feature_type: str
    metadata: Dict[str, Any] = None


@dataclass
class FeatureGroup:
    """特征组定义"""
    name: str
    features: List[Feature]
    created_at: datetime
    updated_at: datetime
    version: str
    description: str = ""


class FeatureStore(ABC):
    """特征存储抽象基类"""
    
    @abstractmethod
    def save_feature(self, feature: Feature) -> bool:
        """保存特征"""
        pass
    
    @abstractmethod
    def get_feature(self, name: str, entity_id: str, timestamp: Optional[datetime] = None) -> Optional[Feature]:
        """获取特征"""
        pass
    
    @abstractmethod
    def get_features(self, names: List[str], entity_id: str, timestamp: Optional[datetime] = None) -> Dict[str, Feature]:
        """获取多个特征"""
        pass
    
    @abstractmethod
    def delete_feature(self, name: str, entity_id: str) -> bool:
        """删除特征"""
        pass


class RedisFeatureStore(FeatureStore):
    """Redis特征存储实现"""
    
    def __init__(self, host: str = 'localhost', port: int = 6379, db: int = 0, password: str = None):
        self.redis_client = redis.Redis(host=host, port=port, db=db, password=password, decode_responses=True)
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def _get_key(self, name: str, entity_id: str) -> str:
        """生成Redis键"""
        return f"feature:{name}:{entity_id}"
    
    def _get_timestamp_key(self, name: str, entity_id: str, timestamp: datetime) -> str:
        """生成带时间戳的Redis键"""
        timestamp_str = timestamp.strftime("%Y%m%d%H%M%S")
        return f"feature:{name}:{entity_id}:{timestamp_str}"
    
    def save_feature(self, feature: Feature) -> bool:
        """保存特征到Redis"""
        try:
            key = self._get_key(feature.name, feature.entity_id)
            
            # 序列化特征数据
            feature_data = {
                'name': feature.name,
                'value': feature.value,
                'timestamp': feature.timestamp.isoformat(),
                'entity_id': feature.entity_id,
                'feature_type': feature.feature_type,
                'metadata': feature.metadata or {}
            }
            
            # 保存到Redis
            self.redis_client.hset(key, mapping=feature_data)
            self.redis_client.expire(key, 86400 * 7)  # 7天过期
            
            # 保存时间戳索引
            timestamp_key = self._get_timestamp_key(feature.name, feature.entity_id, feature.timestamp)
            self.redis_client.set(timestamp_key, key, ex=86400 * 7)
            
            self.logger.info(f"特征保存成功: {feature.name}:{feature.entity_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"特征保存失败: {str(e)}")
            return False
    
    def get_feature(self, name: str, entity_id: str, timestamp: Optional[datetime] = None) -> Optional[Feature]:
        """从Redis获取特征"""
        try:
            if timestamp:
                # 获取指定时间戳的特征
                timestamp_key = self._get_timestamp_key(name, entity_id, timestamp)
                key = self.redis_client.get(timestamp_key)
                if not key:
                    return None
            else:
                # 获取最新特征
                key = self._get_key(name, entity_id)
            
            feature_data = self.redis_client.hgetall(key)
            if not feature_data:
                return None
            
            # 反序列化特征数据
            feature = Feature(
                name=feature_data['name'],
                value=json.loads(feature_data['value']) if feature_data['value'].startswith('[') or feature_data['value'].startswith('{') else feature_data['value'],
                timestamp=datetime.fromisoformat(feature_data['timestamp']),
                entity_id=feature_data['entity_id'],
                feature_type=feature_data['feature_type'],
                metadata=json.loads(feature_data['metadata']) if feature_data['metadata'] else {}
            )
            
            return feature
            
        except Exception as e:
            self.logger.error(f"特征获取失败: {str(e)}")
            return None
    
    def get_features(self, names: List[str], entity_id: str, timestamp: Optional[datetime] = None) -> Dict[str, Feature]:
        """获取多个特征"""
        features = {}
        for name in names:
            feature = self.get_feature(name, entity_id, timestamp)
            if feature:
                features[name] = feature
        return features
    
    def delete_feature(self, name: str, entity_id: str) -> bool:
        """删除特征"""
        try:
            key = self._get_key(name, entity_id)
            result = self.redis_client.delete(key)
            return result > 0
        except Exception as e:
            self.logger.error(f"特征删除失败: {str(e)}")
            return False


class MongoFeatureStore(FeatureStore):
    """MongoDB特征存储实现"""
    
    def __init__(self, connection_string: str, database: str = 'feature_store'):
        self.client = MongoClient(connection_string)
        self.db = self.client[database]
        self.collection = self.db['features']
        self.logger = logging.getLogger(self.__class__.__name__)
        
        # 创建索引
        self.collection.create_index([("name", 1), ("entity_id", 1), ("timestamp", -1)])
        self.collection.create_index([("entity_id", 1), ("timestamp", -1)])
    
    def save_feature(self, feature: Feature) -> bool:
        """保存特征到MongoDB"""
        try:
            feature_doc = {
                'name': feature.name,
                'value': feature.value,
                'timestamp': feature.timestamp,
                'entity_id': feature.entity_id,
                'feature_type': feature.feature_type,
                'metadata': feature.metadata or {},
                'created_at': datetime.now()
            }
            
            # 使用upsert更新或插入
            self.collection.update_one(
                {'name': feature.name, 'entity_id': feature.entity_id},
                {'$set': feature_doc},
                upsert=True
            )
            
            self.logger.info(f"特征保存成功: {feature.name}:{feature.entity_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"特征保存失败: {str(e)}")
            return False
    
    def get_feature(self, name: str, entity_id: str, timestamp: Optional[datetime] = None) -> Optional[Feature]:
        """从MongoDB获取特征"""
        try:
            query = {'name': name, 'entity_id': entity_id}
            if timestamp:
                query['timestamp'] = timestamp
            
            doc = self.collection.find_one(query, sort=[('timestamp', -1)])
            if not doc:
                return None
            
            feature = Feature(
                name=doc['name'],
                value=doc['value'],
                timestamp=doc['timestamp'],
                entity_id=doc['entity_id'],
                feature_type=doc['feature_type'],
                metadata=doc.get('metadata', {})
            )
            
            return feature
            
        except Exception as e:
            self.logger.error(f"特征获取失败: {str(e)}")
            return None
    
    def get_features(self, names: List[str], entity_id: str, timestamp: Optional[datetime] = None) -> Dict[str, Feature]:
        """获取多个特征"""
        features = {}
        for name in names:
            feature = self.get_feature(name, entity_id, timestamp)
            if feature:
                features[name] = feature
        return features
    
    def delete_feature(self, name: str, entity_id: str) -> bool:
        """删除特征"""
        try:
            result = self.collection.delete_many({'name': name, 'entity_id': entity_id})
            return result.deleted_count > 0
        except Exception as e:
            self.logger.error(f"特征删除失败: {str(e)}")
            return False


class FeatureStoreManager:
    """特征存储管理器"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        self.stores = {}
        self.logger = logging.getLogger(self.__class__.__name__)
        self._initialize_stores()
    
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
            return config
        except FileNotFoundError:
            self.logger.warning(f"配置文件 {config_path} 不存在，使用默认配置")
            return self._get_default_config()
    
    def _get_default_config(self) -> Dict[str, Any]:
        """获取默认配置"""
        return {
            'feature_stores': {
                'redis': {
                    'host': 'localhost',
                    'port': 6379,
                    'db': 0,
                    'password': None
                },
                'mongodb': {
                    'connection_string': 'mongodb://localhost:27017',
                    'database': 'feature_store'
                }
            },
            'default_store': 'redis'
        }
    
    def _initialize_stores(self):
        """初始化特征存储"""
        stores_config = self.config['feature_stores']
        
        # 初始化Redis存储
        if 'redis' in stores_config:
            redis_config = stores_config['redis']
            self.stores['redis'] = RedisFeatureStore(
                host=redis_config['host'],
                port=redis_config['port'],
                db=redis_config['db'],
                password=redis_config.get('password')
            )
        
        # 初始化MongoDB存储
        if 'mongodb' in stores_config:
            mongo_config = stores_config['mongodb']
            self.stores['mongodb'] = MongoFeatureStore(
                connection_string=mongo_config['connection_string'],
                database=mongo_config['database']
            )
    
    def get_store(self, store_name: str = None) -> FeatureStore:
        """获取特征存储"""
        if store_name is None:
            store_name = self.config['default_store']
        
        if store_name not in self.stores:
            raise ValueError(f"未找到特征存储: {store_name}")
        
        return self.stores[store_name]
    
    def save_feature(self, feature: Feature, store_name: str = None) -> bool:
        """保存特征"""
        store = self.get_store(store_name)
        return store.save_feature(feature)
    
    def get_feature(self, name: str, entity_id: str, timestamp: Optional[datetime] = None, store_name: str = None) -> Optional[Feature]:
        """获取特征"""
        store = self.get_store(store_name)
        return store.get_feature(name, entity_id, timestamp)
    
    def get_features(self, names: List[str], entity_id: str, timestamp: Optional[datetime] = None, store_name: str = None) -> Dict[str, Feature]:
        """获取多个特征"""
        store = self.get_store(store_name)
        return store.get_features(names, entity_id, timestamp)
    
    def delete_feature(self, name: str, entity_id: str, store_name: str = None) -> bool:
        """删除特征"""
        store = self.get_store(store_name)
        return store.delete_feature(name, entity_id)


class FeatureEngineering:
    """特征工程"""
    
    def __init__(self, feature_store_manager: FeatureStoreManager):
        self.fsm = feature_store_manager
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def create_user_features(self, user_id: str, user_data: Dict[str, Any]) -> List[Feature]:
        """创建用户特征"""
        features = []
        current_time = datetime.now()
        
        # 基础特征
        features.append(Feature(
            name="user_age",
            value=user_data.get('age', 0),
            timestamp=current_time,
            entity_id=user_id,
            feature_type="numerical"
        ))
        
        features.append(Feature(
            name="user_gender",
            value=user_data.get('gender', 'unknown'),
            timestamp=current_time,
            entity_id=user_id,
            feature_type="categorical"
        ))
        
        features.append(Feature(
            name="user_city",
            value=user_data.get('city', 'unknown'),
            timestamp=current_time,
            entity_id=user_id,
            feature_type="categorical"
        ))
        
        # 计算特征
        if 'registration_date' in user_data:
            reg_date = datetime.fromisoformat(user_data['registration_date'])
            days_since_registration = (current_time - reg_date).days
            features.append(Feature(
                name="days_since_registration",
                value=days_since_registration,
                timestamp=current_time,
                entity_id=user_id,
                feature_type="numerical"
            ))
        
        return features
    
    def create_product_features(self, product_id: str, product_data: Dict[str, Any]) -> List[Feature]:
        """创建商品特征"""
        features = []
        current_time = datetime.now()
        
        # 基础特征
        features.append(Feature(
            name="product_price",
            value=product_data.get('price', 0),
            timestamp=current_time,
            entity_id=product_id,
            feature_type="numerical"
        ))
        
        features.append(Feature(
            name="product_category",
            value=product_data.get('category', 'unknown'),
            timestamp=current_time,
            entity_id=product_id,
            feature_type="categorical"
        ))
        
        features.append(Feature(
            name="product_brand",
            value=product_data.get('brand', 'unknown'),
            timestamp=current_time,
            entity_id=product_id,
            feature_type="categorical"
        ))
        
        # 计算特征
        if 'rating_count' in product_data and 'rating_sum' in product_data:
            rating_count = product_data['rating_count']
            rating_sum = product_data['rating_sum']
            if rating_count > 0:
                avg_rating = rating_sum / rating_count
                features.append(Feature(
                    name="product_avg_rating",
                    value=avg_rating,
                    timestamp=current_time,
                    entity_id=product_id,
                    feature_type="numerical"
                ))
        
        return features
    
    def create_interaction_features(self, user_id: str, product_id: str, interaction_data: Dict[str, Any]) -> List[Feature]:
        """创建交互特征"""
        features = []
        current_time = datetime.now()
        
        # 交互类型特征
        features.append(Feature(
            name="interaction_type",
            value=interaction_data.get('type', 'unknown'),
            timestamp=current_time,
            entity_id=f"{user_id}_{product_id}",
            feature_type="categorical"
        ))
        
        # 时间特征
        if 'timestamp' in interaction_data:
            interaction_time = datetime.fromisoformat(interaction_data['timestamp'])
            hour = interaction_time.hour
            weekday = interaction_time.weekday()
            
            features.append(Feature(
                name="interaction_hour",
                value=hour,
                timestamp=current_time,
                entity_id=f"{user_id}_{product_id}",
                feature_type="numerical"
            ))
            
            features.append(Feature(
                name="interaction_weekday",
                value=weekday,
                timestamp=current_time,
                entity_id=f"{user_id}_{product_id}",
                feature_type="numerical"
            ))
        
        return features
    
    def batch_create_features(self, features: List[Feature], store_name: str = None) -> int:
        """批量创建特征"""
        success_count = 0
        for feature in features:
            if self.fsm.save_feature(feature, store_name):
                success_count += 1
        return success_count


class FeatureServing:
    """特征服务"""
    
    def __init__(self, feature_store_manager: FeatureStoreManager):
        self.fsm = feature_store_manager
        self.logger = logging.getLogger(self.__class__.__name__)
        self.cache = {}
        self.cache_ttl = 300  # 5分钟缓存
    
    def get_feature_vector(self, entity_id: str, feature_names: List[str], 
                          timestamp: Optional[datetime] = None, store_name: str = None) -> Dict[str, Any]:
        """获取特征向量"""
        try:
            # 检查缓存
            cache_key = f"{entity_id}:{':'.join(feature_names)}"
            if cache_key in self.cache:
                cached_data, cached_time = self.cache[cache_key]
                if time.time() - cached_time < self.cache_ttl:
                    return cached_data
            
            # 从特征存储获取特征
            features = self.fsm.get_features(feature_names, entity_id, timestamp, store_name)
            
            # 构建特征向量
            feature_vector = {}
            for name in feature_names:
                if name in features:
                    feature_vector[name] = features[name].value
                else:
                    feature_vector[name] = None
            
            # 缓存结果
            self.cache[cache_key] = (feature_vector, time.time())
            
            return feature_vector
            
        except Exception as e:
            self.logger.error(f"获取特征向量失败: {str(e)}")
            return {}
    
    def get_batch_feature_vectors(self, entity_ids: List[str], feature_names: List[str],
                                 timestamp: Optional[datetime] = None, store_name: str = None) -> Dict[str, Dict[str, Any]]:
        """批量获取特征向量"""
        results = {}
        for entity_id in entity_ids:
            results[entity_id] = self.get_feature_vector(entity_id, feature_names, timestamp, store_name)
        return results


def main():
    """主函数"""
    # 创建特征存储管理器
    fsm = FeatureStoreManager()
    
    # 创建特征工程
    fe = FeatureEngineering(fsm)
    
    # 创建特征服务
    fs = FeatureServing(fsm)
    
    # 示例：创建用户特征
    user_data = {
        'age': 25,
        'gender': 'male',
        'city': 'beijing',
        'registration_date': '2023-01-01T00:00:00'
    }
    
    user_features = fe.create_user_features('user_001', user_data)
    fe.batch_create_features(user_features)
    
    # 示例：获取特征向量
    feature_vector = fs.get_feature_vector('user_001', ['user_age', 'user_gender', 'user_city'])
    print(f"用户特征向量: {feature_vector}")


if __name__ == "__main__":
    main()
