"""
Flink流处理应用
"""

import logging
import json
from typing import Dict, Any, List
from datetime import datetime, timedelta
from pyflink.datastream import StreamExecutionEnvironment, TimeCharacteristic
from pyflink.table import StreamTableEnvironment, EnvironmentSettings
from pyflink.table.descriptors import Schema, OldCsv, FileSystem, Json
from pyflink.table.window import Tumble
from pyflink.common.typeinfo import Types
from pyflink.common.time import Time
from pyflink.datastream.functions import MapFunction, FilterFunction, KeyedProcessFunction
from pyflink.datastream.state import ValueStateDescriptor
from pyflink.common.serialization import SimpleStringSchema
from pyflink.datastream.connectors import FlinkKafkaConsumer, FlinkKafkaProducer
from pyflink.common.typeinfo import Types
from pyflink.common.time import Time
import yaml
import os

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class EventProcessor(MapFunction):
    """事件处理器"""
    
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def map(self, value: str) -> str:
        """处理事件数据"""
        try:
            # 解析JSON数据
            event = json.loads(value)
            
            # 添加处理时间戳
            event['process_time'] = datetime.now().isoformat()
            
            # 数据验证和清洗
            if self._validate_event(event):
                event = self._clean_event(event)
                return json.dumps(event)
            else:
                self.logger.warning(f"事件验证失败: {event}")
                return None
                
        except Exception as e:
            self.logger.error(f"事件处理异常: {str(e)}")
            return None
    
    def _validate_event(self, event: Dict[str, Any]) -> bool:
        """验证事件数据"""
        required_fields = ['event_type', 'user_id', 'timestamp']
        return all(field in event for field in required_fields)
    
    def _clean_event(self, event: Dict[str, Any]) -> Dict[str, Any]:
        """清洗事件数据"""
        # 标准化时间戳
        if 'timestamp' in event:
            event['timestamp'] = self._normalize_timestamp(event['timestamp'])
        
        # 清理用户ID
        if 'user_id' in event:
            event['user_id'] = str(event['user_id']).strip()
        
        return event
    
    def _normalize_timestamp(self, timestamp: str) -> str:
        """标准化时间戳"""
        try:
            dt = datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
            return dt.isoformat()
        except:
            return datetime.now().isoformat()


class EventFilter(FilterFunction):
    """事件过滤器"""
    
    def __init__(self, event_types: List[str] = None):
        self.event_types = event_types or ['click', 'view', 'purchase', 'login']
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def filter(self, value: str) -> bool:
        """过滤事件"""
        try:
            event = json.loads(value)
            event_type = event.get('event_type')
            
            if event_type in self.event_types:
                return True
            else:
                self.logger.debug(f"过滤掉事件类型: {event_type}")
                return False
                
        except Exception as e:
            self.logger.error(f"事件过滤异常: {str(e)}")
            return False


class UserBehaviorAnalyzer(KeyedProcessFunction):
    """用户行为分析器"""
    
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.user_state = None
        self.session_state = None
    
    def open(self, runtime_context):
        """初始化状态"""
        self.user_state = runtime_context.get_state(
            ValueStateDescriptor("user_state", Types.STRING())
        )
        self.session_state = runtime_context.get_state(
            ValueStateDescriptor("session_state", Types.STRING())
        )
    
    def process_element(self, value, ctx):
        """处理元素"""
        try:
            event = json.loads(value)
            user_id = event['user_id']
            event_type = event['event_type']
            timestamp = event['timestamp']
            
            # 获取用户状态
            user_data = self.user_state.value()
            if user_data is None:
                user_data = {
                    'user_id': user_id,
                    'total_events': 0,
                    'last_activity': timestamp,
                    'session_count': 0,
                    'event_types': {}
                }
            else:
                user_data = json.loads(user_data)
            
            # 更新用户状态
            user_data['total_events'] += 1
            user_data['last_activity'] = timestamp
            user_data['event_types'][event_type] = user_data['event_types'].get(event_type, 0) + 1
            
            # 会话管理
            if self._is_new_session(user_data, timestamp):
                user_data['session_count'] += 1
                session_data = {
                    'session_id': f"{user_id}_{int(datetime.now().timestamp())}",
                    'start_time': timestamp,
                    'events': []
                }
            else:
                session_data = json.loads(self.session_state.value() or '{}')
            
            session_data['events'].append(event)
            session_data['last_event_time'] = timestamp
            
            # 保存状态
            self.user_state.update(json.dumps(user_data))
            self.session_state.update(json.dumps(session_data))
            
            # 输出分析结果
            analysis_result = {
                'user_id': user_id,
                'event_type': event_type,
                'timestamp': timestamp,
                'total_events': user_data['total_events'],
                'session_count': user_data['session_count'],
                'event_frequency': user_data['event_types'],
                'session_duration': self._calculate_session_duration(session_data)
            }
            
            yield json.dumps(analysis_result)
            
        except Exception as e:
            self.logger.error(f"用户行为分析异常: {str(e)}")
    
    def _is_new_session(self, user_data: Dict[str, Any], timestamp: str) -> bool:
        """判断是否为新会话"""
        last_activity = user_data.get('last_activity')
        if not last_activity:
            return True
        
        try:
            last_time = datetime.fromisoformat(last_activity)
            current_time = datetime.fromisoformat(timestamp)
            time_diff = (current_time - last_time).total_seconds()
            
            # 30分钟无活动视为新会话
            return time_diff > 1800
        except:
            return True
    
    def _calculate_session_duration(self, session_data: Dict[str, Any]) -> int:
        """计算会话持续时间（秒）"""
        try:
            start_time = datetime.fromisoformat(session_data['start_time'])
            last_event_time = datetime.fromisoformat(session_data['last_event_time'])
            return int((last_event_time - start_time).total_seconds())
        except:
            return 0


class RealTimeAggregator:
    """实时聚合器"""
    
    def __init__(self, env: StreamExecutionEnvironment):
        self.env = env
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def create_kafka_source(self, topic: str, bootstrap_servers: str) -> Any:
        """创建Kafka数据源"""
        kafka_consumer = FlinkKafkaConsumer(
            topics=topic,
            deserialization_schema=SimpleStringSchema(),
            properties={
                'bootstrap.servers': bootstrap_servers,
                'group.id': 'flink-consumer-group',
                'auto.offset.reset': 'latest'
            }
        )
        
        return self.env.add_source(kafka_consumer)
    
    def create_kafka_sink(self, topic: str, bootstrap_servers: str) -> Any:
        """创建Kafka数据汇"""
        kafka_producer = FlinkKafkaProducer(
            topic=topic,
            serialization_schema=SimpleStringSchema(),
            producer_config={
                'bootstrap.servers': bootstrap_servers
            }
        )
        
        return kafka_producer
    
    def process_user_events(self, source_stream, sink_stream):
        """处理用户事件流"""
        # 事件处理
        processed_stream = source_stream \
            .map(EventProcessor()) \
            .filter(EventFilter())
        
        # 用户行为分析
        analyzed_stream = processed_stream \
            .key_by(lambda x: json.loads(x)['user_id']) \
            .process(UserBehaviorAnalyzer())
        
        # 输出到Kafka
        analyzed_stream.add_sink(sink_stream)
        
        return analyzed_stream


class FlinkStreamingJob:
    """Flink流处理作业"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        self.env = StreamExecutionEnvironment.get_execution_environment()
        self.env.set_stream_time_characteristic(TimeCharacteristic.EventTime)
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
            'kafka': {
                'bootstrap_servers': 'localhost:9092',
                'input_topic': 'user_events',
                'output_topic': 'user_behavior_analysis'
            },
            'flink': {
                'parallelism': 4,
                'checkpoint_interval': 60000,
                'checkpoint_timeout': 300000
            }
        }
    
    def setup_checkpointing(self):
        """设置检查点"""
        checkpoint_config = self.config['flink']
        
        self.env.enable_checkpointing(checkpoint_config['checkpoint_interval'])
        self.env.get_checkpoint_config().set_checkpoint_timeout(
            checkpoint_config['checkpoint_timeout']
        )
        self.env.get_checkpoint_config().set_max_concurrent_checkpoints(1)
        self.env.get_checkpoint_config().set_min_pause_between_checkpoints(500)
    
    def run(self):
        """运行流处理作业"""
        try:
            self.logger.info("开始运行Flink流处理作业")
            
            # 设置检查点
            self.setup_checkpointing()
            
            # 设置并行度
            self.env.set_parallelism(self.config['flink']['parallelism'])
            
            # 创建聚合器
            aggregator = RealTimeAggregator(self.env)
            
            # 创建数据源和数据汇
            kafka_config = self.config['kafka']
            source_stream = aggregator.create_kafka_source(
                kafka_config['input_topic'],
                kafka_config['bootstrap_servers']
            )
            
            sink_stream = aggregator.create_kafka_sink(
                kafka_config['output_topic'],
                kafka_config['bootstrap_servers']
            )
            
            # 处理数据流
            aggregator.process_user_events(source_stream, sink_stream)
            
            # 执行作业
            self.env.execute("UserBehaviorAnalysis")
            
            self.logger.info("Flink流处理作业运行完成")
            
        except Exception as e:
            self.logger.error(f"Flink流处理作业运行失败: {str(e)}")
            raise


class TableStreamingJob:
    """基于Table API的流处理作业"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        self.env = StreamExecutionEnvironment.get_execution_environment()
        self.table_env = StreamTableEnvironment.create(
            self.env,
            environment_settings=EnvironmentSettings.new_instance()
                .in_streaming_mode()
                .build()
        )
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
            'kafka': {
                'bootstrap_servers': 'localhost:9092',
                'input_topic': 'user_events',
                'output_topic': 'user_behavior_analysis'
            }
        }
    
    def create_kafka_table(self, topic: str) -> str:
        """创建Kafka表"""
        table_name = f"kafka_{topic}"
        
        self.table_env.execute_sql(f"""
            CREATE TABLE {table_name} (
                event_type STRING,
                user_id STRING,
                timestamp STRING,
                properties STRING,
                process_time AS PROCTIME()
            ) WITH (
                'connector' = 'kafka',
                'topic' = '{topic}',
                'properties.bootstrap.servers' = '{self.config['kafka']['bootstrap_servers']}',
                'properties.group.id' = 'flink-table-consumer',
                'format' = 'json'
            )
        """)
        
        return table_name
    
    def create_analysis_table(self, input_table: str) -> str:
        """创建分析表"""
        output_table = "user_behavior_analysis"
        
        self.table_env.execute_sql(f"""
            CREATE TABLE {output_table} (
                user_id STRING,
                event_type STRING,
                event_count BIGINT,
                window_start TIMESTAMP(3),
                window_end TIMESTAMP(3)
            ) WITH (
                'connector' = 'kafka',
                'topic' = '{self.config['kafka']['output_topic']}',
                'properties.bootstrap.servers' = '{self.config['kafka']['bootstrap_servers']}',
                'format' = 'json'
            )
        """)
        
        # 执行分析查询
        self.table_env.execute_sql(f"""
            INSERT INTO {output_table}
            SELECT 
                user_id,
                event_type,
                COUNT(*) as event_count,
                TUMBLE_START(process_time, INTERVAL '1' MINUTE) as window_start,
                TUMBLE_END(process_time, INTERVAL '1' MINUTE) as window_end
            FROM {input_table}
            GROUP BY user_id, event_type, TUMBLE(process_time, INTERVAL '1' MINUTE)
        """)
        
        return output_table
    
    def run(self):
        """运行Table API流处理作业"""
        try:
            self.logger.info("开始运行Table API流处理作业")
            
            # 创建输入表
            input_table = self.create_kafka_table(self.config['kafka']['input_topic'])
            
            # 创建分析表
            output_table = self.create_analysis_table(input_table)
            
            # 执行作业
            self.table_env.execute("UserBehaviorAnalysisTable")
            
            self.logger.info("Table API流处理作业运行完成")
            
        except Exception as e:
            self.logger.error(f"Table API流处理作业运行失败: {str(e)}")
            raise


def main():
    """主函数"""
    # 运行DataStream API作业
    job = FlinkStreamingJob()
    job.run()
    
    # 运行Table API作业
    # table_job = TableStreamingJob()
    # table_job.run()


if __name__ == "__main__":
    main()
