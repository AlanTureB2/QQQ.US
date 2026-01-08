package com.quant.strategy;

import com.quant.model.StockData;

import java.util.List;

/**
 * 买入持有策略 (Buy and Hold)
 * 
 * 最简单的策略：在第一天买入，一直持有到最后
 * 作为基准策略使用，也可用于策略组合
 */
public class BuyAndHoldStrategy extends AbstractStrategy {
    
    /**
     * 构造函数
     */
    public BuyAndHoldStrategy() {
        super("买入持有策略");
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        
        // 第一天买入
        dataList.get(0).setSignal(1);
        
        // 之后一直持有
        for (int i = 1; i < dataList.size(); i++) {
            dataList.get(i).setSignal(0);  // 持有
            dataList.get(i).setIndicator("POSITION_WEIGHT", 1.0);  // 满仓权重
        }
        
        // 第一天也设置权重
        dataList.get(0).setIndicator("POSITION_WEIGHT", 1.0);
        
        logger.info("策略 [{}] 信号生成完成 (买入并持有)", name);
    }
}




