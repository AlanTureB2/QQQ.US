package com.quant.strategy;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略组合 (Combined Strategy / Portfolio Strategy)
 * 
 * 将多个策略按权重组合，实现风险分散
 * 
 * 核心思想：
 *   - 不同策略之间相关性不高，组合后可以降低整体风险
 *   - 最终仓位 = Σ(策略权重 × 策略仓位)
 * 
 * 示例配置：
 *   - 40% 趋势追踪策略
 *   - 40% 波动率目标策略
 *   - 20% 买入持有（满仓）
 * 
 * 效果：
 *   - 降低最大回撤
 *   - 提高夏普比率
 *   - 收益介于各策略之间
 */
public class CombinedStrategy extends AbstractStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CombinedStrategy.class);
    
    // 子策略列表及其权重
    private final List<StrategyWeight> strategyWeights;
    
    // 滑点
    private final double slippage;
    
    // 调仓阈值（仓位变化超过此值才调仓）
    private final double rebalanceThreshold;
    
    /**
     * 策略权重类
     */
    public static class StrategyWeight {
        private final Strategy strategy;
        private final double weight;
        
        public StrategyWeight(Strategy strategy, double weight) {
            this.strategy = strategy;
            this.weight = weight;
        }
        
        public Strategy getStrategy() {
            return strategy;
        }
        
        public double getWeight() {
            return weight;
        }
    }
    
    /**
     * 构造函数
     * 
     * @param strategyWeights 策略权重列表
     */
    public CombinedStrategy(List<StrategyWeight> strategyWeights) {
        this(strategyWeights, 0.0005, 0.05);
    }
    
    /**
     * 完整构造函数
     * 
     * @param strategyWeights 策略权重列表
     * @param slippage 滑点
     * @param rebalanceThreshold 调仓阈值
     */
    public CombinedStrategy(List<StrategyWeight> strategyWeights, double slippage, double rebalanceThreshold) {
        super("策略组合");
        
        this.strategyWeights = new ArrayList<>(strategyWeights);
        this.slippage = slippage;
        this.rebalanceThreshold = rebalanceThreshold;
        
        // 验证权重总和
        double totalWeight = strategyWeights.stream().mapToDouble(StrategyWeight::getWeight).sum();
        if (Math.abs(totalWeight - 1.0) > 0.001) {
            logger.warn("策略权重总和为 {}，不等于1.0，将自动归一化", totalWeight);
        }
        
        // 记录参数
        StringBuilder sb = new StringBuilder();
        for (StrategyWeight sw : strategyWeights) {
            sb.append(String.format("%s(%.0f%%), ", sw.getStrategy().getName(), sw.getWeight() * 100));
        }
        parameters.put("strategies", sb.toString());
        parameters.put("slippage", slippage);
        parameters.put("rebalanceThreshold", rebalanceThreshold);
    }
    
    /**
     * 便捷工厂方法：创建默认组合（趋势追踪40% + 波动率目标40% + 买入持有20%）
     */
    public static CombinedStrategy createDefaultCombination() {
        List<StrategyWeight> weights = new ArrayList<>();
        weights.add(new StrategyWeight(new TrendFollowingStrategy(), 0.4));
        weights.add(new StrategyWeight(new VolatilityTargetStrategy(), 0.4));
        weights.add(new StrategyWeight(new BuyAndHoldStrategy(), 0.2));
        return new CombinedStrategy(weights);
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 为每个子策略生成信号（在各自的数据副本上）
        Map<Strategy, List<StockData>> strategyResults = new HashMap<>();
        
        for (StrategyWeight sw : strategyWeights) {
            Strategy strategy = sw.getStrategy();
            
            // 复制数据
            List<StockData> dataCopy = copyDataList(dataList);
            
            // 生成信号并回测
            strategy.execute(dataCopy);
            
            strategyResults.put(strategy, dataCopy);
            
            logger.debug("子策略 [{}] 信号生成完成", strategy.getName());
        }
        
        // 计算组合仓位权重
        for (int i = 0; i < dataList.size(); i++) {
            double combinedWeight = 0;
            
            for (StrategyWeight sw : strategyWeights) {
                Strategy strategy = sw.getStrategy();
                List<StockData> result = strategyResults.get(strategy);
                
                // 获取该策略在第 i 天的仓位
                double strategyPosition = 0;
                
                // 检查是否有动态仓位权重（如波动率目标策略）
                Double positionWeight = result.get(i).getIndicator("POSITION_WEIGHT");
                if (positionWeight != null) {
                    strategyPosition = positionWeight;
                } else {
                    // 使用二元仓位（0或1）
                    strategyPosition = result.get(i).getPosition();
                }
                
                // 加权求和
                combinedWeight += sw.getWeight() * strategyPosition;
            }
            
            // 存储组合仓位权重
            dataList.get(i).setIndicator("COMBINED_WEIGHT", combinedWeight);
            
            // 设置信号（用于统计）
            if (combinedWeight > 0) {
                dataList.get(i).setSignal(1);
            } else {
                dataList.get(i).setSignal(0);
            }
        }
        
        logger.info("策略 [{}] 信号生成完成，包含 {} 个子策略", name, strategyWeights.size());
    }
    
    @Override
    public List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        
        double cumulativeReturn = 1.0;
        double prevWeight = 0;
        int rebalanceCount = 0;
        
        // 计算日收益率
        for (int i = 1; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            StockData prev = dataList.get(i - 1);
            
            if (curr.getDailyReturn() == 0) {
                double dailyReturn = (curr.getClose() - prev.getClose()) / prev.getClose();
                curr.setDailyReturn(dailyReturn);
            }
        }
        
        // 执行回测
        for (int i = 0; i < dataList.size(); i++) {
            StockData data = dataList.get(i);
            
            // 获取组合仓位权重
            Double combinedWeight = data.getIndicator("COMBINED_WEIGHT");
            double currentWeight = combinedWeight != null ? combinedWeight : 0;
            
            // 设置持仓状态
            data.setPosition(currentWeight > 0 ? 1 : 0);
            
            // 计算策略收益
            if (i > 0) {
                double dailyReturn = data.getDailyReturn();
                
                // 使用前一天的权重计算今天的收益
                double strategyReturn = prevWeight * dailyReturn;
                
                // 检查是否需要调仓
                double weightChange = Math.abs(currentWeight - prevWeight);
                if (weightChange > rebalanceThreshold) {
                    // 调仓成本
                    double rebalanceCost = weightChange * (commission + slippage);
                    strategyReturn -= rebalanceCost;
                    rebalanceCount++;
                }
                
                data.setStrategyReturn(strategyReturn);
                cumulativeReturn *= (1 + strategyReturn);
            }
            
            data.setCumulativeReturn(cumulativeReturn);
            data.setPortfolioValue(initialCapital * cumulativeReturn);
            
            prevWeight = currentWeight;
        }
        
        // 输出回测摘要
        StockData lastData = dataList.get(dataList.size() - 1);
        logger.info("策略 [{}] 回测完成", name);
        logger.info("  期末组合价值: {}", String.format("%.2f", lastData.getPortfolioValue()));
        logger.info("  累计收益率: {}%", String.format("%.2f", (lastData.getCumulativeReturn() - 1) * 100));
        logger.info("  调仓次数: {}", rebalanceCount);
        
        return dataList;
    }
    
    /**
     * 复制数据列表
     */
    private List<StockData> copyDataList(List<StockData> original) {
        List<StockData> copy = new ArrayList<>();
        for (StockData data : original) {
            StockData newData = new StockData();
            newData.setId(data.getId());
            newData.setSymbol(data.getSymbol());
            newData.setSymbolId(data.getSymbolId());
            newData.setKlineType(data.getKlineType());
            newData.setTime(data.getTime());
            newData.setMarketCc(data.getMarketCc());
            newData.setTradeDate(data.getTradeDate());
            newData.setOpen(data.getOpen());
            newData.setHigh(data.getHigh());
            newData.setLow(data.getLow());
            newData.setClose(data.getClose());
            newData.setVwap(data.getVwap());
            newData.setVolume(data.getVolume());
            newData.setAmount(data.getAmount());
            newData.setCount(data.getCount());
            newData.setSessionId(data.getSessionId());
            newData.setDailyReturn(data.getDailyReturn());
            
            for (Map.Entry<String, Double> entry : data.getAllIndicators().entrySet()) {
                newData.setIndicator(entry.getKey(), entry.getValue());
            }
            copy.add(newData);
        }
        return copy;
    }
    
    public List<StrategyWeight> getStrategyWeights() {
        return new ArrayList<>(strategyWeights);
    }
}


