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
- `RSIStrategy`: RSI超买超卖策略
- **`TrendFollowingStrategy`**: ★ 趋势追踪+现金管理策略（方案A）
- **`VolatilityTargetStrategy`**: ★ 波动率目标策略（方案B）

### 4. 统计输出模块 (statistics)
- `PerformanceStatistics`: 绩效统计
  - 总收益率
  - 年化收益率
  - 夏普比率
  - 最大回撤
  - 胜率
  - 盈亏比

## 进阶策略说明

### 方案A：趋势追踪 + 现金管理策略 (TrendFollowingStrategy)

**最适合 QQQ 的长期投资策略**

```java
// 使用默认参数 (MA50/MA200, 0.05%滑点)
Strategy strategy = new TrendFollowingStrategy();

// 或自定义参数
Strategy strategy = new TrendFollowingStrategy(50, 200, 0.0005);
```

**核心逻辑：**
- **买入条件**：价格 > MA50 且 MA50 > MA200（黄金交叉确认趋势）
- **风控条件**：价格跌破 MA200 则全仓卖出（硬止损）

**回测预期：**
- 可避开 2022 年 30% 以上的跌幅
- 震荡市会有"磨损"，但长期风险收益比极高

---

### 方案B：波动率目标策略 (VolatilityTargetStrategy)

**动态仓位管理，平滑收益曲线**

```java
// 使用默认参数 (20日波动率, 15%目标波动率)
Strategy strategy = new VolatilityTargetStrategy();

// 或自定义参数
Strategy strategy = new VolatilityTargetStrategy(
    20,     // 波动率计算周期
    0.15,   // 目标年化波动率 (15%)
    1.0,    // 最大仓位 (100%)
    0.1,    // 最小仓位 (10%)
    0.0005, // 滑点 (0.05%)
    0.1     // 调仓阈值
);
```

**核心公式：**
```
TargetWeight = TargetVolatility / CurrentVolatility
```

**效果：**
- 高波动时期（如 2020年3月、2022年）自动降低仓位
- 低波动上涨期充分参与
- 极大地平滑收益曲线，提高夏普比率

---

## 开发注意事项

### 1. 幸存者偏差 / 过拟合
- 2010 年以来数据经历长期量化宽松，过拟合是最大风险
- **不要微调参数**（如 197 日均线优于 200 日这种没意义）
- 使用经典参数：50/200 日均线，20 日波动率

### 2. 滑点与佣金
- 每笔交易默认扣除 **0.05% 滑点** + **0.1% 佣金**
- 可通过策略构造函数调整滑点参数

### 3. 前瞻偏差 (Look-ahead bias)
- 所有信号只使用当日及之前的数据
- 波动率目标策略使用前一日波动率决定当日仓位

---

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

