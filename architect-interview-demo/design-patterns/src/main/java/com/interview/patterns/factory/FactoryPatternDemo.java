package com.interview.patterns.factory;

/**
 * 工厂模式 - 简单工厂、工厂方法、抽象工厂
 * 
 * 适用场景：
 * - 对象创建逻辑复杂
 * - 需要解耦对象的创建和使用
 * - 产品族的创建
 */
public class FactoryPatternDemo {
    
    // ========== 产品接口 ==========
    interface Product {
        void use();
    }
    
    static class ProductA implements Product {
        @Override
        public void use() {
            System.out.println("Using Product A");
        }
    }
    
    static class ProductB implements Product {
        @Override
        public void use() {
            System.out.println("Using Product B");
        }
    }
    
    // ========== 1. 简单工厂 ==========
    static class SimpleFactory {
        public static Product createProduct(String type) {
            switch (type) {
                case "A":
                    return new ProductA();
                case "B":
                    return new ProductB();
                default:
                    throw new IllegalArgumentException("Unknown product type");
            }
        }
    }
    
    // ========== 2. 工厂方法 ==========
    interface Factory {
        Product createProduct();
    }
    
    static class FactoryA implements Factory {
        @Override
        public Product createProduct() {
            return new ProductA();
        }
    }
    
    static class FactoryB implements Factory {
        @Override
        public Product createProduct() {
            return new ProductB();
        }
    }
    
    // ========== 3. 抽象工厂 ==========
    interface Button {
        void click();
    }
    
    interface Checkbox {
        void check();
    }
    
    // Windows风格
    static class WindowsButton implements Button {
        @Override
        public void click() {
            System.out.println("Windows Button Clicked");
        }
    }
    
    static class WindowsCheckbox implements Checkbox {
        @Override
        public void check() {
            System.out.println("Windows Checkbox Checked");
        }
    }
    
    // Mac风格
    static class MacButton implements Button {
        @Override
        public void click() {
            System.out.println("Mac Button Clicked");
        }
    }
    
    static class MacCheckbox implements Checkbox {
        @Override
        public void check() {
            System.out.println("Mac Checkbox Checked");
        }
    }
    
    // 抽象工厂
    interface GUIFactory {
        Button createButton();
        Checkbox createCheckbox();
    }
    
    static class WindowsFactory implements GUIFactory {
        @Override
        public Button createButton() {
            return new WindowsButton();
        }
        
        @Override
        public Checkbox createCheckbox() {
            return new WindowsCheckbox();
        }
    }
    
    static class MacFactory implements GUIFactory {
        @Override
        public Button createButton() {
            return new MacButton();
        }
        
        @Override
        public Checkbox createCheckbox() {
            return new MacCheckbox();
        }
    }
    
    public static void main(String[] args) {
        // 简单工厂
        Product product = SimpleFactory.createProduct("A");
        product.use();
        
        // 工厂方法
        Factory factory = new FactoryA();
        Product product2 = factory.createProduct();
        product2.use();
        
        // 抽象工厂
        GUIFactory guiFactory = new WindowsFactory();
        Button button = guiFactory.createButton();
        Checkbox checkbox = guiFactory.createCheckbox();
        button.click();
        checkbox.check();
    }
}

