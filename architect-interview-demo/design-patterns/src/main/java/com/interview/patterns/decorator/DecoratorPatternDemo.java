package com.interview.patterns.decorator;

/**
 * 装饰器模式 - 动态地给对象添加功能
 * 
 * 适用场景：
 * - 需要动态添加功能
 * - 避免使用继承
 * - 功能组合
 */
public class DecoratorPatternDemo {
    
    /**
     * 咖啡接口
     */
    interface Coffee {
        double getCost();
        String getDescription();
    }
    
    /**
     * 基础咖啡
     */
    static class SimpleCoffee implements Coffee {
        @Override
        public double getCost() {
            return 2.0;
        }
        
        @Override
        public String getDescription() {
            return "Simple coffee";
        }
    }
    
    /**
     * 咖啡装饰器基类
     */
    abstract static class CoffeeDecorator implements Coffee {
        protected Coffee coffee;
        
        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }
        
        @Override
        public double getCost() {
            return coffee.getCost();
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription();
        }
    }
    
    /**
     * 牛奶装饰器
     */
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 0.5;
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + ", milk";
        }
    }
    
    /**
     * 糖装饰器
     */
    static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 0.2;
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + ", sugar";
        }
    }
    
    /**
     * 香草装饰器
     */
    static class VanillaDecorator extends CoffeeDecorator {
        public VanillaDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 0.8;
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + ", vanilla";
        }
    }
    
    public static void main(String[] args) {
        // 基础咖啡
        Coffee coffee = new SimpleCoffee();
        System.out.println("Cost: " + coffee.getCost() + ", Description: " + coffee.getDescription());
        
        // 加牛奶
        coffee = new MilkDecorator(coffee);
        System.out.println("Cost: " + coffee.getCost() + ", Description: " + coffee.getDescription());
        
        // 加糖
        coffee = new SugarDecorator(coffee);
        System.out.println("Cost: " + coffee.getCost() + ", Description: " + coffee.getDescription());
        
        // 加香草
        coffee = new VanillaDecorator(coffee);
        System.out.println("Cost: " + coffee.getCost() + ", Description: " + coffee.getDescription());
        
        // 另一种组合
        Coffee anotherCoffee = new VanillaDecorator(new MilkDecorator(new SimpleCoffee()));
        System.out.println("\nAnother combination:");
        System.out.println("Cost: " + anotherCoffee.getCost() + ", Description: " + anotherCoffee.getDescription());
    }
}
