package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.List;

/**
 * RSI策略
 * 基于RSI超买超卖信号进行交易
 * - RSI低于超卖线时买入
 * - RSI高于超买线时卖出
 */
public class RSIStrategy extends AbstractStrategy {
    
    private final int period;
    private final double oversoldLevel;   // 超卖线
    private final double overboughtLevel; // 超买线
    
    /**
     * 构造函数
     * 
     * @param period RSI周期
     * @param oversoldLevel 超卖阈值 (如30)
     * @param overboughtLevel 超买阈值 (如70)
     */
    public RSIStrategy(int period, double oversoldLevel, double overboughtLevel) {
        super("RSI策略");
        
        if (oversoldLevel >= overboughtLevel) {
            throw new IllegalArgumentException("超卖阈值必须小于超买阈值");
        }
        
        this.period = period;
        this.oversoldLevel = oversoldLevel;
        this.overboughtLevel = overboughtLevel;
        
        parameters.put("period", period);
        parameters.put("oversoldLevel", oversoldLevel);
        parameters.put("overboughtLevel", overboughtLevel);
    }
    
    /**
     * 使用默认参数创建策略 (RSI14, 超卖30, 超买70)
     */
    public RSIStrategy() {
        this(14, 30, 70);
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 计算RSI
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateRSI(period);
        
        String rsiName = "RSI" + period;
        boolean inPosition = false;
        
        // 生成信号
        for (int i = period + 1; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            StockData prev = dataList.get(i - 1);
            
            Double currRSI = curr.getIndicator(rsiName);
            Double prevRSI = prev.getIndicator(rsiName);
            
            if (currRSI == null || prevRSI == null) {
                curr.setSignal(0);
                continue;
            }
            
            // 买入条件：RSI从超卖区域回升（从下方穿过超卖线）
            if (!inPosition && prevRSI <= oversoldLevel && currRSI > oversoldLevel) {
                curr.setSignal(1);
                inPosition = true;
            }
            // 卖出条件：RSI进入超买区域后回落（从上方穿过超买线）
            else if (inPosition && prevRSI >= overboughtLevel && currRSI < overboughtLevel) {
                curr.setSignal(-1);
                inPosition = false;
            }
            // 其他情况：持有
            else {
                curr.setSignal(0);
            }
        }
        
        logger.info("策略 [{}] 信号生成完成 (RSI{}, 超卖={}, 超买={})", 
                    name, period, oversoldLevel, overboughtLevel);
    }
    
    public int getPeriod() {
        return period;
    }
    
    public double getOversoldLevel() {
        return oversoldLevel;
    }
    
    public double getOverboughtLevel() {
        return overboughtLevel;
    }
}

