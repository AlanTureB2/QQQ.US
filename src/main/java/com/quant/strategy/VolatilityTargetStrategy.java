package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.List;

/**
 * 方案B：波动率目标策略 (Volatility Targeting)
 * 
 * 核心思想：
 *   - 不要始终满仓，根据过去 N 天的波动率动态调整仓位
 *   - 公式：TargetWeight = TargetVolatility / CurrentVolatility
 *   - 如果市场波动加剧，仓位自动下降
 *   - 如果市场平稳上涨，仓位上升（可配置是否允许杠杆）
 * 
 * 效果：
 *   - 极大地平滑收益曲线
 *   - 在高波动时期（如2020年3月、2022年）自动降低风险敞口
 *   - 在低波动上涨期充分参与
 * 
 * 参数说明：
 *   - volatilityPeriod: 计算波动率的回看周期（默认20天）
 *   - targetVolatility: 目标年化波动率（默认15%）
 *   - maxWeight: 最大仓位权重（默认1.0，设为>1允许杠杆）
 *   - minWeight: 最小仓位权重（默认0.1，不完全空仓）
 * 
 * 注意事项：
 *   - 前瞻偏差：第 i 天的仓位只使用第 i-1 天及之前的数据计算
 *   - 滑点：每次仓位调整都需要扣除滑点成本
 */
public class VolatilityTargetStrategy extends AbstractStrategy {
    
    // 波动率计算周期（默认20天）
    private final int volatilityPeriod;
    
    // 目标年化波动率（默认15%）
    private final double targetVolatility;
    
    // 最大仓位权重（1.0 = 满仓，>1 = 杠杆）
    private final double maxWeight;
    
    // 最小仓位权重（避免完全空仓）
    private final double minWeight;
    
    // 滑点（默认0.05%）
    private final double slippage;
    
    // 仓位调整阈值（仓位变化超过此值才调仓，减少交易频率）
    private final double rebalanceThreshold;
    
    // 年化因子（假设252个交易日）
    private static final double ANNUALIZATION_FACTOR = Math.sqrt(252);
    
    /**
     * 构造函数（使用默认参数）
     */
    public VolatilityTargetStrategy() {
        this(20, 0.15, 1.0, 0.1, 0.0005, 0.1);
    }
    
    /**
     * 简化构造函数
     * 
     * @param targetVolatility 目标年化波动率（如0.15表示15%）
     */
    public VolatilityTargetStrategy(double targetVolatility) {
        this(20, targetVolatility, 1.0, 0.1, 0.0005, 0.1);
    }
    
    /**
     * 完整构造函数
     * 
     * @param volatilityPeriod 波动率计算周期
     * @param targetVolatility 目标年化波动率
     * @param maxWeight 最大仓位权重
     * @param minWeight 最小仓位权重
     * @param slippage 滑点
     * @param rebalanceThreshold 仓位调整阈值
     */
    public VolatilityTargetStrategy(int volatilityPeriod, double targetVolatility, 
                                    double maxWeight, double minWeight, 
                                    double slippage, double rebalanceThreshold) {
        super("波动率目标策略");
        
        this.volatilityPeriod = volatilityPeriod;
        this.targetVolatility = targetVolatility;
        this.maxWeight = maxWeight;
        this.minWeight = minWeight;
        this.slippage = slippage;
        this.rebalanceThreshold = rebalanceThreshold;
        
        parameters.put("volatilityPeriod", volatilityPeriod);
        parameters.put("targetVolatility", targetVolatility);
        parameters.put("maxWeight", maxWeight);
        parameters.put("minWeight", minWeight);
        parameters.put("slippage", slippage);
        parameters.put("rebalanceThreshold", rebalanceThreshold);
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 先计算日收益率
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateReturns();
        
        // 计算滚动波动率并存储目标仓位
        calculateRollingVolatility(dataList);
        
        // 波动率目标策略不使用传统的买卖信号
        // 而是使用动态仓位权重，这里设置signal=1表示持仓
        for (int i = volatilityPeriod; i < dataList.size(); i++) {
            StockData data = dataList.get(i);
            Double weight = data.getIndicator("TARGET_WEIGHT");
            
            if (weight != null && weight > 0) {
                data.setSignal(1);  // 持仓信号
            } else {
                data.setSignal(0);
            }
        }
        
        logger.info("策略 [{}] 信号生成完成 (波动率周期={}, 目标波动率={}%, 最大仓位={})", 
                name, volatilityPeriod, targetVolatility * 100, maxWeight);
    }
    
    /**
     * 计算滚动波动率和目标仓位权重
     * 
     * 重要：避免前瞻偏差
     * 第 i 天的仓位使用第 i-1 天的波动率计算（因为第 i 天收盘才知道第 i 天的收益）
     */
    private void calculateRollingVolatility(List<StockData> dataList) {
        for (int i = volatilityPeriod; i < dataList.size(); i++) {
            // 计算过去 N 天的波动率（使用 i-1 之前的数据，避免前瞻偏差）
            double sumSquares = 0;
            double sum = 0;
            int count = 0;
            
            // 使用 [i-volatilityPeriod, i-1] 的收益率计算波动率
            for (int j = i - volatilityPeriod; j < i; j++) {
                double ret = dataList.get(j).getDailyReturn();
                sum += ret;
                count++;
            }
            
            double mean = count > 0 ? sum / count : 0;
            
            for (int j = i - volatilityPeriod; j < i; j++) {
                double ret = dataList.get(j).getDailyReturn();
                sumSquares += Math.pow(ret - mean, 2);
            }
            
            // 日波动率（标准差）
            double dailyVol = count > 1 ? Math.sqrt(sumSquares / (count - 1)) : 0;
            
            // 年化波动率
            double annualizedVol = dailyVol * ANNUALIZATION_FACTOR;
            
            // 存储当前波动率
            dataList.get(i).setIndicator("REALIZED_VOL", annualizedVol);
            
            // 计算目标仓位权重
            double targetWeight;
            if (annualizedVol > 0) {
                targetWeight = targetVolatility / annualizedVol;
            } else {
                targetWeight = maxWeight;  // 波动率为0时使用最大仓位
            }
            
            // 应用仓位限制
            targetWeight = Math.max(minWeight, Math.min(maxWeight, targetWeight));
            
            // 存储目标仓位
            dataList.get(i).setIndicator("TARGET_WEIGHT", targetWeight);
            
            logger.debug("日期={}: 实际波动率={}%, 目标仓位={}", 
                    dataList.get(i).getDate(), 
                    String.format("%.2f", annualizedVol * 100),
                    String.format("%.2f", targetWeight));
        }
    }
    
    /**
     * 重写回测方法，支持动态仓位
     */
    @Override
    public List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        
        double cumulativeReturn = 1.0;
        double prevWeight = 0;
        int tradeCount = 0;
        
        // 执行回测
        for (int i = 0; i < dataList.size(); i++) {
            StockData data = dataList.get(i);
            
            // 获取目标仓位权重
            Double targetWeight = data.getIndicator("TARGET_WEIGHT");
            double currentWeight = targetWeight != null ? targetWeight : 0;
            
            // 存储当前仓位（用于报告）
            data.setPosition(currentWeight > 0 ? 1 : 0);
            data.setIndicator("POSITION_WEIGHT", currentWeight);
            
            // 计算策略收益
            if (i > 0) {
                double dailyReturn = data.getDailyReturn();
                
                // 使用前一天的权重计算今天的收益（避免前瞻偏差）
                double strategyReturn = prevWeight * dailyReturn;
                
                // 检查是否需要调仓（仓位变化超过阈值）
                double weightChange = Math.abs(currentWeight - prevWeight);
                if (weightChange > rebalanceThreshold) {
                    // 调仓成本 = 仓位变化 × (佣金 + 滑点)
                    double rebalanceCost = weightChange * (commission + slippage);
                    strategyReturn -= rebalanceCost;
                    tradeCount++;
                    
                    logger.debug("日期={}: 调仓 {} -> {}, 成本={}%", 
                            data.getDate(), 
                            String.format("%.2f", prevWeight),
                            String.format("%.2f", currentWeight),
                            String.format("%.4f", rebalanceCost * 100));
                }
                
                data.setStrategyReturn(strategyReturn);
                cumulativeReturn *= (1 + strategyReturn);
            }
            
            data.setCumulativeReturn(cumulativeReturn);
            data.setPortfolioValue(initialCapital * cumulativeReturn);
            
            // 更新前一权重
            prevWeight = currentWeight;
        }
        
        // 输出回测摘要
        StockData lastData = dataList.get(dataList.size() - 1);
        logger.info("策略 [{}] 回测完成", name);
        logger.info("  期末组合价值: {}", String.format("%.2f", lastData.getPortfolioValue()));
        logger.info("  累计收益率: {}%", String.format("%.2f", (lastData.getCumulativeReturn() - 1) * 100));
        logger.info("  调仓次数: {}", tradeCount);
        
        return dataList;
    }
    
    // ========== Getters ==========
    
    public int getVolatilityPeriod() {
        return volatilityPeriod;
    }
    
    public double getTargetVolatility() {
        return targetVolatility;
    }
    
    public double getMaxWeight() {
        return maxWeight;
    }
    
    public double getMinWeight() {
        return minWeight;
    }
    
    public double getSlippage() {
        return slippage;
    }
    
    public double getRebalanceThreshold() {
        return rebalanceThreshold;
    }
}




