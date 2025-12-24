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
│   │   ├── DualMAStrategy.java      # 双均线策略
│   │   ├── RSIStrategy.java         # RSI策略
│   │   ├── TrendFollowingStrategy.java  # 趋势追踪策略
│   │   ├── VolatilityTargetStrategy.java # 波动率目标策略
│   │   ├── BuyAndHoldStrategy.java  # 买入持有策略
│   │   └── CombinedStrategy.java    # 策略组合 ★推荐★
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

## 策略说明

### 方案A：趋势追踪 + 现金管理策略

```java
Strategy strategy = new TrendFollowingStrategy(50, 200, 0.0005);
```

- **买入条件**：价格 > MA50 且 MA50 > MA200
- **风控条件**：价格跌破 MA200 则全仓卖出
- **回测结果**：收益率 542%，夏普比率 0.70，最大回撤 -21%

### 方案B：波动率目标策略

```java
Strategy strategy = new VolatilityTargetStrategy(20, 0.15, 1.0, 0.1, 0.0005, 0.1);
```

- **核心公式**：TargetWeight = TargetVolatility / CurrentVolatility
- **效果**：高波动时自动降仓，低波动时满仓
- **回测结果**：收益率 536%，夏普比率 0.80，最大回撤 -22%

### 方案C：策略组合 ★推荐★

```java
Strategy strategy = CombinedStrategy.createDefaultCombination();
// 40% 趋势追踪 + 40% 波动率目标 + 20% 买入持有
```

- **设计理念**：多策略分散风险，简单有效
- **回测结果**：收益率 627%，夏普比率 0.86，最大回撤 -20%

## 策略对比

| 策略 | 总收益率 | 最大回撤 | 夏普比率 |
|------|---------|---------|---------|
| 买入持有(基准) | 943.83% | -35.00% | 0.50 |
| 趋势追踪+现金管理 | 542.16% | -21.25% | 0.70 |
| 波动率目标 | 536.08% | -21.84% | 0.80 |
| **★策略组合★** | **626.90%** | **-20.08%** | **0.86** |

## 开发注意事项

1. **幸存者偏差**：不要微调参数，使用经典参数（50/200日均线）
2. **滑点与佣金**：每笔交易扣除 0.05% 滑点 + 0.1% 佣金
3. **前瞻偏差**：所有信号只使用当日及之前的数据
4. **收益与风险的权衡**：策略收益低于买入持有是正常的（风险控制的代价）

## 自定义策略

```java
public class MyCustomStrategy extends AbstractStrategy {
    
    public MyCustomStrategy() {
        super("我的自定义策略");
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        for (StockData data : dataList) {
            // data.setSignal(1);  // 买入
            // data.setSignal(-1); // 卖出
            // data.setSignal(0);  // 持有
        }
    }
}
```

## License

MIT License
