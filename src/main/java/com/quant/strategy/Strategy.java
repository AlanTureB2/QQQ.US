package com.quant.strategy;

import com.quant.model.StockData;

import java.util.List;
import java.util.Map;

/**
 * 策略接口
 * 所有交易策略都应该实现此接口
 */
public interface Strategy {
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getName();
    
    /**
     * 获取策略参数
     * 
     * @return 参数映射
     */
    Map<String, Object> getParameters();
    
    /**
     * 生成交易信号
     * 信号说明:
     *   - signal = 1: 买入信号
     *   - signal = -1: 卖出信号
     *   - signal = 0: 无操作/持有
     * 
     * @param dataList 股票数据列表
     */
    void generateSignals(List<StockData> dataList);
    
    /**
     * 执行策略（生成信号 + 回测）
     * 
     * @param dataList 股票数据列表
     * @return 回测后的数据列表
     */
    List<StockData> execute(List<StockData> dataList);
    
    /**
     * 执行回测
     * 
     * @param dataList 股票数据列表
     * @param initialCapital 初始资金
     * @param commission 手续费率
     * @return 回测后的数据列表
     */
    List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission);
}

