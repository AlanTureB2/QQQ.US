package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.List;

/**
 * 均线交叉策略
 * 当短期均线上穿长期均线时买入，下穿时卖出
 */
public class MACrossStrategy extends AbstractStrategy {
    
    private final int shortPeriod;
    private final int longPeriod;
    
    /**
     * 构造函数
     * 
     * @param shortPeriod 短期均线周期
     * @param longPeriod 长期均线周期
     */
    public MACrossStrategy(int shortPeriod, int longPeriod) {
        super("MA交叉策略");
        
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("短期周期必须小于长期周期");
        }
        
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        
        parameters.put("shortPeriod", shortPeriod);
        parameters.put("longPeriod", longPeriod);
    }
    
    /**
     * 使用默认参数创建策略 (MA5 和 MA20)
     */
    public MACrossStrategy() {
        this(5, 20);
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 确保指标已计算
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateMA(shortPeriod);
        indicators.calculateMA(longPeriod);
        
        String shortMAName = "MA" + shortPeriod;
        String longMAName = "MA" + longPeriod;
        
        // 生成信号
        for (int i = 1; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            StockData prev = dataList.get(i - 1);
            
            Double currShortMA = curr.getIndicator(shortMAName);
            Double currLongMA = curr.getIndicator(longMAName);
            Double prevShortMA = prev.getIndicator(shortMAName);
            Double prevLongMA = prev.getIndicator(longMAName);
            
            // 检查指标是否有效
            if (currShortMA == null || currLongMA == null || 
                prevShortMA == null || prevLongMA == null) {
                curr.setSignal(0);
                continue;
            }
            
            // 金叉: 短期均线上穿长期均线 -> 买入
            if (prevShortMA <= prevLongMA && currShortMA > currLongMA) {
                curr.setSignal(1);
            }
            // 死叉: 短期均线下穿长期均线 -> 卖出
            else if (prevShortMA >= prevLongMA && currShortMA < currLongMA) {
                curr.setSignal(-1);
            }
            // 其他情况: 持有
            else {
                curr.setSignal(0);
            }
        }
        
        logger.info("策略 [{}] 信号生成完成 (短期MA={}, 长期MA={})", name, shortPeriod, longPeriod);
    }
    
    public int getShortPeriod() {
        return shortPeriod;
    }
    
    public int getLongPeriod() {
        return longPeriod;
    }
}

