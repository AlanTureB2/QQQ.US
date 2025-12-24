package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.List;

/**
 * 双均线趋势策略
 * 基于两条均线的相对位置判断趋势，并结合价格位置生成信号
 * - 当价格在短期均线之上，且短期均线在长期均线之上时，认为是上升趋势
 * - 当价格跌破短期均线时，卖出
 */
public class DualMAStrategy extends AbstractStrategy {
    
    private final int shortPeriod;
    private final int longPeriod;
    
    /**
     * 构造函数
     * 
     * @param shortPeriod 短期均线周期
     * @param longPeriod 长期均线周期
     */
    public DualMAStrategy(int shortPeriod, int longPeriod) {
        super("双均线趋势策略");
        
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("短期周期必须小于长期周期");
        }
        
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        
        parameters.put("shortPeriod", shortPeriod);
        parameters.put("longPeriod", longPeriod);
    }
    
    /**
     * 使用默认参数创建策略 (MA10 和 MA30)
     */
    public DualMAStrategy() {
        this(10, 30);
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 确保指标已计算
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateMA(shortPeriod);
        indicators.calculateMA(longPeriod);
        
        String shortMAName = "MA" + shortPeriod;
        String longMAName = "MA" + longPeriod;
        
        boolean inPosition = false;
        
        // 生成信号
        for (int i = longPeriod; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            StockData prev = dataList.get(i - 1);
            
            Double currShortMA = curr.getIndicator(shortMAName);
            Double currLongMA = curr.getIndicator(longMAName);
            Double prevShortMA = prev.getIndicator(shortMAName);
            
            if (currShortMA == null || currLongMA == null || prevShortMA == null) {
                curr.setSignal(0);
                continue;
            }
            
            double price = curr.getClose();
            double prevPrice = prev.getClose();
            
            // 买入条件：
            // 1. 未持仓
            // 2. 短期均线在长期均线之上（趋势向上）
            // 3. 价格从下方突破短期均线
            if (!inPosition && 
                currShortMA > currLongMA && 
                prevPrice <= prevShortMA && 
                price > currShortMA) {
                curr.setSignal(1);
                inPosition = true;
            }
            // 卖出条件：
            // 1. 持仓中
            // 2. 价格跌破短期均线 或 短期均线跌破长期均线
            else if (inPosition && 
                     (price < currShortMA || currShortMA < currLongMA)) {
                curr.setSignal(-1);
                inPosition = false;
            }
            // 其他情况：持有
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

