# QQQ.US 量化分析系统

基于 Java 的模块化量化分析框架，支持从 Excel 读取数据，计算技术指标，执行交易策略，并输出统计结果。

## 项目结构

```
QQQ.US/
├── src/main/java/com/quant/
│   ├── Main.java                    # 主程序入口
│   ├── model/
│   │   └── StockData.java           # 股票数据模型
│   ├── loader/
│   │   └── ExcelDataLoader.java     # Excel数据加载器
│   ├── indicator/
│   │   └── TechnicalIndicators.java # 技术指标计算
│   ├── strategy/
│   │   ├── Strategy.java            # 策略接口
│   │   ├── AbstractStrategy.java    # 策略抽象基类
│   │   ├── MACrossStrategy.java     # 均线交叉策略
│   │   └── DualMAStrategy.java      # 双均线策略
│   └── statistics/
│       └── PerformanceStatistics.java # 绩效统计
├── data/
│   └── sample_data.xlsx             # 示例数据
├── pom.xml                          # Maven配置
└── README.md
```

## 环境要求

- JDK 11+
- Maven 3.6+

## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行示例

```bash
mvn exec:java -Dexec.mainClass="com.quant.Main"
```

或者打包后运行：

```bash
mvn package
java -jar target/qqq-us-quant-1.0.0.jar
```

## 使用示例

```java
// 1. 加载数据
ExcelDataLoader loader = new ExcelDataLoader();
List<StockData> dataList = loader.loadFromExcel("data/stock_data.xlsx");

// 2. 计算指标
TechnicalIndicators indicators = new TechnicalIndicators(dataList);
indicators.calculateMA(5);
indicators.calculateMA(20);

// 3. 执行策略
Strategy strategy = new MACrossStrategy(5, 20);
List<StockData> result = strategy.execute(dataList);

// 4. 输出统计
PerformanceStatistics stats = new PerformanceStatistics(result);
stats.printSummary();
System.out.println("夏普比率: " + stats.getSharpeRatio());
System.out.println("总收益率: " + stats.getTotalReturn() + "%");
```

## 模块说明

### 1. 数据获取模块 (loader)
- `ExcelDataLoader`: 从 Excel 文件读取股票数据
- 支持 `.xlsx` 和 `.xls` 格式
- 自动解析日期和数值列

### 2. 指标计算模块 (indicator)
- `TechnicalIndicators`: 技术指标计算器
  - MA (简单移动平均线)
  - EMA (指数移动平均线)
  - MACD
  - RSI
  - 布林带

### 3. 策略执行模块 (strategy)
- `Strategy`: 策略接口
- `AbstractStrategy`: 策略抽象基类，包含回测逻辑
- `MACrossStrategy`: 均线交叉策略
- `DualMAStrategy`: 双均线策略

### 4. 统计输出模块 (statistics)
- `PerformanceStatistics`: 绩效统计
  - 总收益率
  - 年化收益率
  - 夏普比率
  - 最大回撤
  - 胜率
  - 盈亏比

## 自定义策略

实现 `Strategy` 接口或继承 `AbstractStrategy` 类：

```java
public class MyCustomStrategy extends AbstractStrategy {
    
    public MyCustomStrategy() {
        super("我的自定义策略");
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        for (StockData data : dataList) {
            // 实现你的信号生成逻辑
            // data.setSignal(1);  // 买入
            // data.setSignal(-1); // 卖出
            // data.setSignal(0);  // 持有
        }
    }
}
```

## 数据格式要求

Excel 文件应包含以下列（列名不区分大小写）：

| 列名 | 说明 | 必需 |
|------|------|------|
| date / 日期 | 交易日期 | 是 |
| open / 开盘价 | 开盘价 | 是 |
| high / 最高价 | 最高价 | 是 |
| low / 最低价 | 最低价 | 是 |
| close / 收盘价 | 收盘价 | 是 |
| volume / 成交量 | 成交量 | 否 |

## License

MIT License

