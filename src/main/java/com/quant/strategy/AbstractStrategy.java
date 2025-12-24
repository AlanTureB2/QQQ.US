package com.quant.strategy;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略抽象基类
 * 提供回测的默认实现，子类只需实现信号生成逻辑
 */
public abstract class AbstractStrategy implements Strategy {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final String name;
    protected final Map<String, Object> parameters = new HashMap<>();
    
    // 默认参数
    protected static final double DEFAULT_INITIAL_CAPITAL = 100000.0;
    protected static final double DEFAULT_COMMISSION = 0.001; // 0.1%
    
    /**
     * 构造函数
     * 
     * @param name 策略名称
     */
    public AbstractStrategy(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * 生成交易信号（子类必须实现）
     * 
     * @param dataList 股票数据列表
     */
    @Override
    public abstract void generateSignals(List<StockData> dataList);
    
    @Override
    public List<StockData> execute(List<StockData> dataList) {
        return execute(dataList, DEFAULT_INITIAL_CAPITAL, DEFAULT_COMMISSION);
    }
    
    /**
     * 执行策略
     * 
     * @param dataList 股票数据列表
     * @param initialCapital 初始资金
     * @param commission 手续费率
     * @return 回测后的数据列表
     */
    public List<StockData> execute(List<StockData> dataList, double initialCapital, double commission) {
        // 生成信号
        generateSignals(dataList);
        
        // 执行回测
        return backtest(dataList, initialCapital, commission);
    }
    
    @Override
    public List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        
        int position = 0; // 当前持仓状态
        double portfolioValue = initialCapital;
        double cumulativeReturn = 1.0;
        
        // 计算日收益率（如果还没计算）
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
            int signal = data.getSignal();
            int prevPosition = position;
            
            // 处理信号
            if (signal == 1 && position == 0) {
                // 买入
                position = 1;
            } else if (signal == -1 && position == 1) {
                // 卖出
                position = 0;
            }
            
            data.setPosition(position);
            
            // 计算策略收益
            if (i > 0) {
                double dailyReturn = data.getDailyReturn();
                double strategyReturn = prevPosition * dailyReturn;
                
                // 扣除交易成本
                if (signal == 1 || signal == -1) {
                    strategyReturn -= commission;
                }
                
                data.setStrategyReturn(strategyReturn);
                cumulativeReturn *= (1 + strategyReturn);
            }
            
            data.setCumulativeReturn(cumulativeReturn);
            data.setPortfolioValue(initialCapital * cumulativeReturn);
        }
        
        // 输出回测摘要
        StockData lastData = dataList.get(dataList.size() - 1);
        logger.info("策略 [{}] 回测完成", name);
        logger.info("  期末组合价值: {}", String.format("%.2f", lastData.getPortfolioValue()));
        logger.info("  累计收益率: {}%", String.format("%.2f", (lastData.getCumulativeReturn() - 1) * 100));
        
        return dataList;
    }
    
    /**
     * 获取交易统计
     * 
     * @param dataList 回测后的数据列表
     * @return 交易统计信息
     */
    public Map<String, Object> getTradeStatistics(List<StockData> dataList) {
        Map<String, Object> stats = new HashMap<>();
        
        int buySignals = 0;
        int sellSignals = 0;
        int completedTrades = 0;
        int winningTrades = 0;
        double entryPrice = 0;
        double totalProfit = 0;
        double totalLoss = 0;
        
        for (StockData data : dataList) {
            if (data.getSignal() == 1) {
                buySignals++;
                entryPrice = data.getClose();
            } else if (data.getSignal() == -1 && entryPrice > 0) {
                sellSignals++;
                completedTrades++;
                
                double profit = (data.getClose() - entryPrice) / entryPrice;
                if (profit > 0) {
                    winningTrades++;
                    totalProfit += profit;
                } else {
                    totalLoss += Math.abs(profit);
                }
                
                entryPrice = 0;
            }
        }
        
        stats.put("strategyName", name);
        stats.put("buySignals", buySignals);
        stats.put("sellSignals", sellSignals);
        stats.put("completedTrades", completedTrades);
        stats.put("winningTrades", winningTrades);
        stats.put("losingTrades", completedTrades - winningTrades);
        stats.put("winRate", completedTrades > 0 ? (double) winningTrades / completedTrades * 100 : 0);
        stats.put("avgWin", winningTrades > 0 ? totalProfit / winningTrades * 100 : 0);
        stats.put("avgLoss", (completedTrades - winningTrades) > 0 ? 
                  totalLoss / (completedTrades - winningTrades) * 100 : 0);
        
        return stats;
    }
    
    @Override
    public String toString() {
        return String.format("%s(params=%s)", name, parameters);
    }
}

