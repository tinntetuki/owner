"""
数据仓库ETL管道
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import pyspark.sql.functions as F
from pyspark.sql import SparkSession, DataFrame
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, DoubleType, TimestampType
from pyspark.sql.window import Window
import yaml
import os
from pathlib import Path

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DataPipeline:
    """数据管道基类"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        """初始化数据管道
        
        Args:
            config_path: 配置文件路径
        """
        self.config = self._load_config(config_path)
        self.spark = self._create_spark_session()
        self.logger = logging.getLogger(self.__class__.__name__)
        
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
            'spark': {
                'app_name': 'DataWarehouseETL',
                'master': 'local[*]',
                'config': {
                    'spark.sql.adaptive.enabled': 'true',
                    'spark.sql.adaptive.coalescePartitions.enabled': 'true',
                    'spark.serializer': 'org.apache.spark.serializer.KryoSerializer'
                }
            },
            'data_sources': {
                'mysql': {
                    'host': 'localhost',
                    'port': 3306,
                    'database': 'ecommerce',
                    'username': 'root',
                    'password': 'password'
                },
                'redis': {
                    'host': 'localhost',
                    'port': 6379,
                    'db': 0
                }
            },
            'data_targets': {
                'hdfs': {
                    'path': 'hdfs://localhost:9000/datawarehouse'
                },
                'hive': {
                    'database': 'ecommerce_dw'
                }
            }
        }
    
    def _create_spark_session(self) -> SparkSession:
        """创建Spark会话"""
        spark_config = self.config['spark']
        
        builder = SparkSession.builder \
            .appName(spark_config['app_name']) \
            .master(spark_config['master'])
        
        # 添加配置
        for key, value in spark_config.get('config', {}).items():
            builder = builder.config(key, value)
        
        return builder.getOrCreate()
    
    def extract(self, source_config: Dict[str, Any]) -> DataFrame:
        """数据抽取
        
        Args:
            source_config: 数据源配置
            
        Returns:
            DataFrame: 抽取的数据
        """
        raise NotImplementedError("子类必须实现extract方法")
    
    def transform(self, df: DataFrame) -> DataFrame:
        """数据转换
        
        Args:
            df: 输入DataFrame
            
        Returns:
            DataFrame: 转换后的数据
        """
        raise NotImplementedError("子类必须实现transform方法")
    
    def load(self, df: DataFrame, target_config: Dict[str, Any]) -> None:
        """数据加载
        
        Args:
            df: 要加载的DataFrame
            target_config: 目标配置
        """
        raise NotImplementedError("子类必须实现load方法")
    
    def run(self) -> None:
        """运行ETL管道"""
        try:
            self.logger.info("开始运行ETL管道")
            
            # 抽取数据
            self.logger.info("开始数据抽取")
            df = self.extract(self.config['data_sources'])
            
            # 转换数据
            self.logger.info("开始数据转换")
            df = self.transform(df)
            
            # 加载数据
            self.logger.info("开始数据加载")
            self.load(df, self.config['data_targets'])
            
            self.logger.info("ETL管道运行完成")
            
        except Exception as e:
            self.logger.error(f"ETL管道运行失败: {str(e)}")
            raise
        finally:
            self.spark.stop()


class MySQLDataPipeline(DataPipeline):
    """MySQL数据管道"""
    
    def extract(self, source_config: Dict[str, Any]) -> DataFrame:
        """从MySQL抽取数据"""
        mysql_config = source_config['mysql']
        
        # 构建JDBC URL
        jdbc_url = f"jdbc:mysql://{mysql_config['host']}:{mysql_config['port']}/{mysql_config['database']}"
        
        # 读取数据
        df = self.spark.read \
            .format("jdbc") \
            .option("url", jdbc_url) \
            .option("dbtable", self.table_name) \
            .option("user", mysql_config['username']) \
            .option("password", mysql_config['password']) \
            .option("driver", "com.mysql.cj.jdbc.Driver") \
            .load()
        
        self.logger.info(f"从MySQL抽取数据完成，记录数: {df.count()}")
        return df
    
    def load(self, df: DataFrame, target_config: Dict[str, Any]) -> None:
        """加载数据到Hive"""
        hive_config = target_config['hive']
        
        # 写入Hive表
        df.write \
            .mode("overwrite") \
            .option("path", f"hdfs://localhost:9000/datawarehouse/{hive_config['database']}/{self.table_name}") \
            .saveAsTable(f"{hive_config['database']}.{self.table_name}")
        
        self.logger.info(f"数据加载到Hive完成: {hive_config['database']}.{self.table_name}")


class UserDataPipeline(MySQLDataPipeline):
    """用户数据管道"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        super().__init__(config_path)
        self.table_name = "users"
    
    def transform(self, df: DataFrame) -> DataFrame:
        """转换用户数据"""
        # 数据清洗
        df = df.filter(F.col("status") == 1)  # 只保留正常状态的用户
        
        # 数据标准化
        df = df.withColumn("email", F.lower(F.col("email")))  # 邮箱转小写
        df = df.withColumn("phone", F.regexp_replace(F.col("phone"), "[^0-9]", ""))  # 清理手机号
        
        # 添加计算字段
        df = df.withColumn("age", 
                          F.when(F.col("birthday").isNotNull(),
                                F.year(F.current_date()) - F.year(F.col("birthday")))
                          .otherwise(None))
        
        # 添加数据质量标记
        df = df.withColumn("data_quality_score", 
                          F.when(F.col("email").isNotNull() & F.col("phone").isNotNull(), 100)
                          .when(F.col("email").isNotNull() | F.col("phone").isNotNull(), 80)
                          .otherwise(60))
        
        # 添加ETL元数据
        df = df.withColumn("etl_batch_id", F.lit(datetime.now().strftime("%Y%m%d%H%M%S")))
        df = df.withColumn("etl_timestamp", F.current_timestamp())
        
        self.logger.info("用户数据转换完成")
        return df


class OrderDataPipeline(MySQLDataPipeline):
    """订单数据管道"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        super().__init__(config_path)
        self.table_name = "orders"
    
    def transform(self, df: DataFrame) -> DataFrame:
        """转换订单数据"""
        # 数据清洗
        df = df.filter(F.col("status").isin(["completed", "pending", "cancelled"]))
        
        # 数据标准化
        df = df.withColumn("order_date", F.to_date(F.col("create_time")))
        df = df.withColumn("order_hour", F.hour(F.col("create_time")))
        df = df.withColumn("order_weekday", F.dayofweek(F.col("create_time")))
        
        # 计算订单金额相关字段
        df = df.withColumn("total_amount", F.col("amount") * F.col("quantity"))
        df = df.withColumn("discount_amount", 
                          F.when(F.col("discount_rate").isNotNull(),
                                F.col("total_amount") * F.col("discount_rate") / 100)
                          .otherwise(0))
        df = df.withColumn("final_amount", F.col("total_amount") - F.col("discount_amount"))
        
        # 添加订单分类
        df = df.withColumn("order_category",
                          F.when(F.col("final_amount") >= 1000, "high_value")
                          .when(F.col("final_amount") >= 500, "medium_value")
                          .otherwise("low_value"))
        
        # 添加ETL元数据
        df = df.withColumn("etl_batch_id", F.lit(datetime.now().strftime("%Y%m%d%H%M%S")))
        df = df.withColumn("etl_timestamp", F.current_timestamp())
        
        self.logger.info("订单数据转换完成")
        return df


class ProductDataPipeline(MySQLDataPipeline):
    """商品数据管道"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        super().__init__(config_path)
        self.table_name = "products"
    
    def transform(self, df: DataFrame) -> DataFrame:
        """转换商品数据"""
        # 数据清洗
        df = df.filter(F.col("status") == 1)  # 只保留上架商品
        
        # 数据标准化
        df = df.withColumn("name", F.trim(F.col("name")))
        df = df.withColumn("description", F.trim(F.col("description")))
        
        # 计算商品评分
        df = df.withColumn("avg_rating", 
                          F.when(F.col("rating_count") > 0,
                                F.col("rating_sum") / F.col("rating_count"))
                          .otherwise(0))
        
        # 添加价格分类
        df = df.withColumn("price_category",
                          F.when(F.col("price") >= 1000, "expensive")
                          .when(F.col("price") >= 100, "medium")
                          .otherwise("cheap"))
        
        # 添加ETL元数据
        df = df.withColumn("etl_batch_id", F.lit(datetime.now().strftime("%Y%m%d%H%M%S")))
        df = df.withColumn("etl_timestamp", F.current_timestamp())
        
        self.logger.info("商品数据转换完成")
        return df


class DataWarehouseETL:
    """数据仓库ETL管理器"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config_path = config_path
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def run_full_etl(self) -> None:
        """运行完整ETL流程"""
        pipelines = [
            UserDataPipeline(self.config_path),
            OrderDataPipeline(self.config_path),
            ProductDataPipeline(self.config_path)
        ]
        
        for pipeline in pipelines:
            try:
                self.logger.info(f"开始运行{pipeline.__class__.__name__}")
                pipeline.run()
                self.logger.info(f"{pipeline.__class__.__name__}运行完成")
            except Exception as e:
                self.logger.error(f"{pipeline.__class__.__name__}运行失败: {str(e)}")
                raise
    
    def run_incremental_etl(self, table_name: str, last_update_time: str) -> None:
        """运行增量ETL"""
        # 根据表名选择对应的管道
        pipeline_map = {
            'users': UserDataPipeline,
            'orders': OrderDataPipeline,
            'products': ProductDataPipeline
        }
        
        if table_name not in pipeline_map:
            raise ValueError(f"不支持的表名: {table_name}")
        
        pipeline_class = pipeline_map[table_name]
        pipeline = pipeline_class(self.config_path)
        
        # 修改抽取逻辑以支持增量更新
        original_extract = pipeline.extract
        
        def incremental_extract(source_config):
            df = original_extract(source_config)
            # 添加时间过滤条件
            df = df.filter(F.col("update_time") > F.lit(last_update_time))
            return df
        
        pipeline.extract = incremental_extract
        
        try:
            self.logger.info(f"开始运行增量ETL: {table_name}")
            pipeline.run()
            self.logger.info(f"增量ETL完成: {table_name}")
        except Exception as e:
            self.logger.error(f"增量ETL失败: {table_name}, {str(e)}")
            raise


def main():
    """主函数"""
    etl_manager = DataWarehouseETL()
    
    # 运行完整ETL
    etl_manager.run_full_etl()
    
    # 运行增量ETL示例
    # etl_manager.run_incremental_etl('orders', '2024-01-01 00:00:00')


if __name__ == "__main__":
    main()
