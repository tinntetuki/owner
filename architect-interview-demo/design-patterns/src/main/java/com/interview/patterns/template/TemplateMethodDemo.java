package com.interview.patterns.template;

/**
 * 模板方法模式
 * 
 * 适用场景：
 * - 算法框架固定，部分步骤可变
 * - 代码复用
 * - Spring JdbcTemplate、RestTemplate
 */
public class TemplateMethodDemo {
    
    /**
     * 抽象模板类 - 数据导入流程
     */
    static abstract class DataImporter {
        
        // 模板方法 - 定义算法骨架
        public final void importData() {
            // 1. 验证文件
            if (!validateFile()) {
                System.out.println("文件验证失败");
                return;
            }
            
            // 2. 解析文件
            String data = parseFile();
            
            // 3. 转换数据
            Object transformedData = transformData(data);
            
            // 4. 保存数据
            saveData(transformedData);
            
            // 5. 发送通知（可选）
            if (needNotify()) {
                sendNotification();
            }
        }
        
        // 基本方法 - 由子类实现
        protected abstract boolean validateFile();
        protected abstract String parseFile();
        protected abstract Object transformData(String data);
        protected abstract void saveData(Object data);
        
        // 钩子方法 - 子类可选择性覆盖
        protected boolean needNotify() {
            return false;
        }
        
        protected void sendNotification() {
            System.out.println("发送通知");
        }
    }
    
    /**
     * 具体实现 - Excel导入
     */
    static class ExcelImporter extends DataImporter {
        @Override
        protected boolean validateFile() {
            System.out.println("验证Excel文件格式");
            return true;
        }
        
        @Override
        protected String parseFile() {
            System.out.println("解析Excel文件");
            return "Excel数据";
        }
        
        @Override
        protected Object transformData(String data) {
            System.out.println("转换Excel数据");
            return data + " -> 转换后";
        }
        
        @Override
        protected void saveData(Object data) {
            System.out.println("保存数据到数据库: " + data);
        }
        
        @Override
        protected boolean needNotify() {
            return true;
        }
    }
    
    /**
     * 具体实现 - CSV导入
     */
    static class CSVImporter extends DataImporter {
        @Override
        protected boolean validateFile() {
            System.out.println("验证CSV文件格式");
            return true;
        }
        
        @Override
        protected String parseFile() {
            System.out.println("解析CSV文件");
            return "CSV数据";
        }
        
        @Override
        protected Object transformData(String data) {
            System.out.println("转换CSV数据");
            return data + " -> 转换后";
        }
        
        @Override
        protected void saveData(Object data) {
            System.out.println("保存数据到数据库: " + data);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== Excel导入 ==========");
        DataImporter excelImporter = new ExcelImporter();
        excelImporter.importData();
        
        System.out.println("\n========== CSV导入 ==========");
        DataImporter csvImporter = new CSVImporter();
        csvImporter.importData();
    }
}

