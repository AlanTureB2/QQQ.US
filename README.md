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

## 执行结果
```angular2html

╔══════════════════════════════════════════════════╗
║        QQQ.US 量化分析系统 v1.2.0               ║
╚══════════════════════════════════════════════════╝

【步骤1】加载数据...
  文件路径: data/sample_data.xlsx
15:10:19.134 [main] INFO  com.quant.loader.ExcelDataLoader - ✓ 成功加载数据: 3677 条记录
15:10:19.138 [main] INFO  com.quant.loader.ExcelDataLoader -   股票代码: QQQ.NB
15:10:19.138 [main] INFO  com.quant.loader.ExcelDataLoader -   日期范围: 2011-01-03 ~ 2025-08-15

【步骤2】计算技术指标...
  ✓ MA5, MA10, MA20, MA60
  ✓ MACD (12, 26, 9)
  ✓ RSI (14)
  ✓ 布林带 (20, 2)

【步骤3】执行策略回测...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: MA交叉策略
参数: {shortPeriod=5, longPeriod=20}
15:10:19.234 [main] INFO  com.quant.strategy.MACrossStrategy - 策略 [MA交叉策略] 信号生成完成 (短期MA=5, 长期MA=20)
15:10:19.238 [main] INFO  com.quant.strategy.MACrossStrategy - 策略 [MA交叉策略] 回测完成
15:10:19.239 [main] INFO  com.quant.strategy.MACrossStrategy -   期末组合价值: 348824.18
15:10:19.239 [main] INFO  com.quant.strategy.MACrossStrategy -   累计收益率: 248.82%
  收益率: 248.82% | 夏普比率: 0.44 | 最大回撤: -24.17% | 胜率: 46.94%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: 双均线趋势策略
参数: {shortPeriod=5, longPeriod=20}
15:10:19.259 [main] INFO  com.quant.strategy.DualMAStrategy - 策略 [双均线趋势策略] 信号生成完成 (短期MA=5, 长期MA=20)
15:10:19.261 [main] INFO  com.quant.strategy.DualMAStrategy - 策略 [双均线趋势策略] 回测完成
15:10:19.262 [main] INFO  com.quant.strategy.DualMAStrategy -   期末组合价值: 122374.43
15:10:19.263 [main] INFO  com.quant.strategy.DualMAStrategy -   累计收益率: 22.37%
  收益率: 22.37% | 夏普比率: -0.05 | 最大回撤: -35.76% | 胜率: 38.74%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: RSI策略
参数: {oversoldLevel=30.0, period=14, overboughtLevel=70.0}
15:10:19.280 [main] INFO  com.quant.strategy.RSIStrategy - 策略 [RSI策略] 信号生成完成 (RSI14, 超卖=30.0, 超买=70.0)
15:10:19.284 [main] INFO  com.quant.strategy.RSIStrategy - 策略 [RSI策略] 回测完成
15:10:19.284 [main] INFO  com.quant.strategy.RSIStrategy -   期末组合价值: 268864.24
15:10:19.285 [main] INFO  com.quant.strategy.RSIStrategy -   累计收益率: 168.86%
  收益率: 168.86% | 夏普比率: 0.19 | 最大回撤: -27.58% | 胜率: 82.35%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: 趋势追踪+现金管理策略
参数: {shortPeriod=50, slippage=5.0E-4, longPeriod=200}
15:10:19.297 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:19.299 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:19.299 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:19.299 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%
  收益率: 542.16% | 夏普比率: 0.70 | 最大回撤: -21.25% | 胜率: 53.85%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: 波动率目标策略
参数: {volatilityPeriod=20, targetVolatility=0.15, minWeight=0.1, maxWeight=1.0, slippage=5.0E-4, rebalanceThreshold=0.1}
15:10:19.429 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:19.442 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:19.443 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:19.444 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:19.444 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92
  收益率: 536.08% | 夏普比率: 0.80 | 最大回撤: -21.84% | 胜率: 0.00%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: 策略组合
参数: {slippage=5.0E-4, strategies=趋势追踪+现金管理策略(40%), 波动率目标策略(40%), 买入持有策略(20%), , rebalanceThreshold=0.05}
15:10:19.513 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:19.520 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:19.521 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:19.521 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%
15:10:19.581 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:19.586 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:19.587 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:19.588 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:19.588 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92
15:10:19.606 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 信号生成完成 (买入并持有)
15:10:19.612 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 回测完成
15:10:19.612 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   期末组合价值: 1043825.71
15:10:19.613 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   累计收益率: 943.83%
15:10:19.626 [main] INFO  com.quant.strategy.CombinedStrategy - 策略 [策略组合] 信号生成完成，包含 3 个子策略
15:10:19.633 [main] INFO  com.quant.strategy.CombinedStrategy - 策略 [策略组合] 回测完成
15:10:19.641 [main] INFO  com.quant.strategy.CombinedStrategy -   期末组合价值: 726899.77
15:10:19.642 [main] INFO  com.quant.strategy.CombinedStrategy -   累计收益率: 626.90%
15:10:19.646 [main] INFO  com.quant.strategy.CombinedStrategy -   调仓次数: 81
  收益率: 626.90% | 夏普比率: 0.86 | 最大回撤: -20.08% | 胜率: 0.00%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
策略: VIX状态切换策略
参数: {highVolThreshold=25%, volatilityPeriod=20, lowVolThreshold=15%, mediumVolStrategy=趋势追踪+现金管理策略, highVolStrategy=波动率目标策略, lowVolStrategy=买入持有策略}
15:10:19.765 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 信号生成完成 (买入并持有)
15:10:19.767 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 回测完成
15:10:19.767 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   期末组合价值: 1043825.71
15:10:19.767 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   累计收益率: 943.83%
15:10:19.772 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:19.775 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:19.775 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:19.775 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%
15:10:19.799 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:19.805 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:19.805 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:19.806 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:19.806 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92
15:10:19.816 [main] INFO  c.quant.strategy.VIXRegimeStrategy - 策略 [VIX状态切换策略] 信号生成完成
15:10:19.816 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   状态分布: 低波动=1628天, 中波动=1322天, 高波动=707天
15:10:19.822 [main] INFO  c.quant.strategy.VIXRegimeStrategy - 策略 [VIX状态切换策略] 回测完成
15:10:19.822 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   期末组合价值: 535548.34
15:10:19.823 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   累计收益率: 435.55%
15:10:19.823 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   状态切换次数: 205
  收益率: 435.55% | 夏普比率: 0.67 | 最大回撤: -24.49% | 胜率: 57.14%


【步骤4】进阶策略详细对比...

============================================================
【方案A】趋势追踪 + 现金管理策略
============================================================
设计理念：
  • 买入条件：价格 > MA50 且 MA50 > MA200（黄金交叉确认趋势）
  • 风控条件：价格跌破 MA200 则全仓卖出（硬止损）
  • 适用场景：长期趋势投资，可避开大幅下跌（如2022年）
  • 注意事项：震荡市会有磨损，但长期风险收益比高
15:10:19.843 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:19.849 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:19.849 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:19.850 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%

==================================================
              绩 效 统 计 报 告
==================================================

【收益指标】
  总收益率:           542.16%
  年化收益率:          13.59%
  基准收益率:         943.83%
  超额收益:          -401.67%

【风险指标】
  年化波动率:          16.67%
  最大回撤:           -21.25%
  最大回撤持续:          453 天

【风险调整收益】
  夏普比率:             0.70
  Calmar比率:           0.64

【交易统计】
  总交易次数:             13
  胜率:                53.85%
  盈亏比:              18.99

【资金状况】
  初始资金:         100,000.00
  期末资金:         642,155.64
==================================================

============================================================
【方案B】波动率目标策略 (Volatility Targeting)
============================================================
设计理念：
  • 核心公式：TargetWeight = TargetVol / CurrentVol
  • 高波动时期：自动降低仓位，减少风险敞口
  • 低波动时期：提高仓位，充分参与上涨
  • 效果：平滑收益曲线，提高夏普比率
15:10:19.917 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:19.924 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:19.924 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:19.924 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:19.924 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92

==================================================
              绩 效 统 计 报 告
==================================================

【收益指标】
  总收益率:           536.08%
  年化收益率:          13.52%
  基准收益率:         943.83%
  超额收益:          -407.74%

【风险指标】
  年化波动率:          14.46%
  最大回撤:           -21.84%
  最大回撤持续:          389 天

【风险调整收益】
  夏普比率:             0.80
  Calmar比率:           0.62

【交易统计】
  总交易次数:              0
  胜率:                 0.00%
  盈亏比:               0.00

【资金状况】
  初始资金:         100,000.00
  期末资金:         636,081.77
==================================================


【步骤5】高阶策略详细对比...

============================================================
【方案C】策略组合 (40% 趋势追踪 + 40% 波动率目标 + 20% 买入持有)
============================================================
设计理念：
  • 多策略组合降低单一策略风险
  • 不同策略相关性低，组合后可降低整体回撤
  • 收益介于各策略之间，但风险更低
15:10:19.973 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:19.978 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:19.978 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:19.979 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%
15:10:20.027 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:20.030 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:20.031 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:20.031 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:20.031 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92
15:10:20.046 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 信号生成完成 (买入并持有)
15:10:20.049 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 回测完成
15:10:20.049 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   期末组合价值: 1043825.71
15:10:20.053 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   累计收益率: 943.83%
15:10:20.060 [main] INFO  com.quant.strategy.CombinedStrategy - 策略 [策略组合] 信号生成完成，包含 3 个子策略
15:10:20.066 [main] INFO  com.quant.strategy.CombinedStrategy - 策略 [策略组合] 回测完成
15:10:20.067 [main] INFO  com.quant.strategy.CombinedStrategy -   期末组合价值: 726899.77
15:10:20.067 [main] INFO  com.quant.strategy.CombinedStrategy -   累计收益率: 626.90%
15:10:20.068 [main] INFO  com.quant.strategy.CombinedStrategy -   调仓次数: 81

==================================================
              绩 效 统 计 报 告
==================================================

【收益指标】
  总收益率:           626.90%
  年化收益率:          14.56%
  基准收益率:         943.83%
  超额收益:          -316.93%

【风险指标】
  年化波动率:          14.60%
  最大回撤:           -20.08%
  最大回撤持续:          382 天

【风险调整收益】
  夏普比率:             0.86
  Calmar比率:           0.73

【交易统计】
  总交易次数:              0
  胜率:                 0.00%
  盈亏比:               0.00

【资金状况】
  初始资金:         100,000.00
  期末资金:         726,899.77
==================================================

============================================================
【方案D】VIX状态切换策略
============================================================
设计理念：
  • 低波动 (<15%)：买入持有，满仓参与上涨
  • 中波动 (15-25%)：趋势追踪，跟随趋势
  • 高波动 (>25%)：波动率目标策略，动态降仓位
  • 根据市场环境自动切换策略
15:10:20.170 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 信号生成完成 (买入并持有)
15:10:20.173 [main] INFO  c.quant.strategy.BuyAndHoldStrategy - 策略 [买入持有策略] 回测完成
15:10:20.174 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   期末组合价值: 1043825.71
15:10:20.174 [main] INFO  c.quant.strategy.BuyAndHoldStrategy -   累计收益率: 943.83%
15:10:20.178 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 信号生成完成 (短期MA=50, 长期MA=200, 滑点=0.05%)
15:10:20.179 [main] INFO  c.q.strategy.TrendFollowingStrategy - 策略 [趋势追踪+现金管理策略] 回测完成
15:10:20.180 [main] INFO  c.q.strategy.TrendFollowingStrategy -   期末组合价值: 642155.64
15:10:20.180 [main] INFO  c.q.strategy.TrendFollowingStrategy -   累计收益率: 542.16%
15:10:20.199 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 信号生成完成 (波动率周期=20, 目标波动率=15.0%, 最大仓位=1.0)
15:10:20.201 [main] INFO  c.q.s.VolatilityTargetStrategy - 策略 [波动率目标策略] 回测完成
15:10:20.201 [main] INFO  c.q.s.VolatilityTargetStrategy -   期末组合价值: 636081.77
15:10:20.201 [main] INFO  c.q.s.VolatilityTargetStrategy -   累计收益率: 536.08%
15:10:20.201 [main] INFO  c.q.s.VolatilityTargetStrategy -   调仓次数: 92
15:10:20.203 [main] INFO  c.quant.strategy.VIXRegimeStrategy - 策略 [VIX状态切换策略] 信号生成完成
15:10:20.203 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   状态分布: 低波动=1628天, 中波动=1322天, 高波动=707天
15:10:20.206 [main] INFO  c.quant.strategy.VIXRegimeStrategy - 策略 [VIX状态切换策略] 回测完成
15:10:20.206 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   期末组合价值: 535548.34
15:10:20.206 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   累计收益率: 435.55%
15:10:20.206 [main] INFO  c.quant.strategy.VIXRegimeStrategy -   状态切换次数: 205

==================================================
              绩 效 统 计 报 告
==================================================

【收益指标】
  总收益率:           435.55%
  年化收益率:          12.19%
  基准收益率:         943.83%
  超额收益:          -508.28%

【风险指标】
  年化波动率:          15.31%
  最大回撤:           -24.49%
  最大回撤持续:          459 天

【风险调整收益】
  夏普比率:             0.67
  Calmar比率:           0.50

【交易统计】
  总交易次数:              7
  胜率:                57.14%
  盈亏比:               0.82

【资金状况】
  初始资金:         100,000.00
  期末资金:         535,548.34
==================================================

【VIX状态切换策略 - 最近5条数据】
------------------------------------------------------------------------------------------
日期                  收盘价        模拟VIX         市场状态         仓位权重
------------------------------------------------------------------------------------------
2025-08-11       572.85       12.47%          低波动      100.00%
2025-08-12       580.05       12.56%          低波动      100.00%
2025-08-13       580.34       13.16%          低波动      100.00%
2025-08-14       579.89       13.16%          低波动      100.00%
2025-08-15       577.34       13.00%          低波动      100.00%

============================================================
【基准】买入持有策略
============================================================
  基准收益率: 943.83%

============================================================
【策略对比总结】
============================================================
策略                               总收益率         最大回撤         夏普比率
------------------------------------------------------------
买入持有(基准)                      943.83%      -35.00%         0.50
趋势追踪+现金管理                     542.16%      -21.25%         0.70
波动率目标                         536.08%      -21.84%         0.80
策略组合                          626.90%      -20.08%         0.86
VIX状态切换                       435.55%      -24.49%         0.67
------------------------------------------------------------

【趋势追踪策略 - 最近5条数据】
------------------------------------------------------------------------------------------
日期                  收盘价       MA50      MA200         信号       持仓
------------------------------------------------------------------------------------------
2025-08-11       572.85     549.15     513.08        -       持有
2025-08-12       580.05     550.37     513.50        -       持有
2025-08-13       580.34     551.51     513.96        -       持有
2025-08-14       579.89     552.56     514.40        -       持有
2025-08-15       577.34     553.54     514.81        -       持有

【波动率目标策略 - 最近5条数据】
------------------------------------------------------------------------------------------
日期                  收盘价        年化波动率         目标仓位       累计收益
------------------------------------------------------------------------------------------
2025-08-11       572.85       12.47%      100.00%    531.13%
2025-08-12       580.05       12.56%      100.00%    539.07%
2025-08-13       580.34       13.16%      100.00%    539.39%
2025-08-14       579.89       13.16%      100.00%    538.89%
2025-08-15       577.34       13.00%      100.00%    536.08%
============================================================
【开发注意事项】
============================================================
1. 幸存者偏差：2010年以来数据经历长期QE，过拟合是最大风险
   • 不要微调参数（如197日均线优于200日这种没意义）
   • 使用经典参数：50/200日均线，20日波动率

2. 滑点与佣金：每笔交易已扣除0.05%滑点 + 0.1%佣金
   • 可通过策略构造函数调整滑点参数

3. 前瞻偏差：所有信号只使用当日及之前的数据
   • 波动率目标策略使用前一日波动率决定当日仓位

4. 收益与风险的权衡：
   • 策略收益低于买入持有是正常的（风险控制的代价）
   • 关注夏普比率和最大回撤，而非单纯追求高收益
   • 推荐使用策略组合或VIX状态切换来平衡风险收益




```

