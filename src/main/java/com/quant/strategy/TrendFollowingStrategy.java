package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.List;

/**
 * 方案A：趋势追踪 + 现金管理策略（最适合 QQQ）
 * 
 * 买入条件：
 *   - 价格 > 50日均线 且 50日均线 > 200日均线
 * 
 * 风控/卖出条件：
 *   - 一旦价格跌破 200 日均线，全仓切换至空仓（或货币基金 SHV）
 * 
 * 设计理念：
 *   - 利用经典的黄金交叉（50日均线在200日均线上方）判断长期趋势
 *   - 使用200日均线作为硬止损线，避免大幅回撤
 *   - 2022年这种策略可以避开30%以上的跌幅
 *   - 震荡市会有"磨损"，但长期风险收益比极高
 * 
 * 注意事项：
 *   - 避免过拟合：使用经典的50/200日均线参数，不做微调
 *   - 前瞻偏差：信号基于当日收盘价，次日开盘执行
 */
public class TrendFollowingStrategy extends AbstractStrategy {
    
    private final int shortPeriod;  // 短期均线周期（默认50）
    private final int longPeriod;   // 长期均线周期（默认200）
    
    // 滑点设置（默认0.05%）
    private final double slippage;
    
    /**
     * 构造函数（使用默认参数 MA50/MA200）
     */
    public TrendFollowingStrategy() {
        this(50, 200, 0.0005);
    }
    
    /**
     * 构造函数
     * 
     * @param shortPeriod 短期均线周期（默认50）
     * @param longPeriod 长期均线周期（默认200）
     */
    public TrendFollowingStrategy(int shortPeriod, int longPeriod) {
        this(shortPeriod, longPeriod, 0.0005);
    }
    
    /**
     * 完整构造函数
     * 
     * @param shortPeriod 短期均线周期
     * @param longPeriod 长期均线周期
     * @param slippage 滑点比例（默认0.0005即0.05%）
     */
    public TrendFollowingStrategy(int shortPeriod, int longPeriod, double slippage) {
        super("趋势追踪+现金管理策略");
        
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("短期周期必须小于长期周期");
        }
        
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.slippage = slippage;
        
        parameters.put("shortPeriod", shortPeriod);
        parameters.put("longPeriod", longPeriod);
        parameters.put("slippage", slippage);
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
        
        // 从 longPeriod 开始生成信号（确保均线数据有效）
        // 注意：避免前瞻偏差，第 i 天的信号只使用第 i 天及之前的数据
        for (int i = longPeriod; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            
            Double currShortMA = curr.getIndicator(shortMAName);
            Double currLongMA = curr.getIndicator(longMAName);
            
            if (currShortMA == null || currLongMA == null) {
                curr.setSignal(0);
                continue;
            }
            
            double price = curr.getClose();
            
            // ============ 核心逻辑 ============
            // 买入条件：价格 > MA50 且 MA50 > MA200（趋势确认）
            boolean buyCondition = price > currShortMA && currShortMA > currLongMA;
            
            // 风控/卖出条件：价格跌破 MA200（硬止损）
            boolean riskOffCondition = price < currLongMA;
            
            if (!inPosition && buyCondition) {
                // 买入信号
                curr.setSignal(1);
                inPosition = true;
                logger.debug("日期={}: 买入信号 - 价格({})>MA{}({})>MA{}({})", 
                        curr.getDate(), price, shortPeriod, currShortMA, longPeriod, currLongMA);
            } else if (inPosition && riskOffCondition) {
                // 风控卖出：价格跌破200日均线
                curr.setSignal(-1);
                inPosition = false;
                logger.debug("日期={}: 风控卖出 - 价格({}) < MA{}({})", 
                        curr.getDate(), price, longPeriod, currLongMA);
            } else if (inPosition && !buyCondition) {
                // 常规卖出：趋势不再满足（但价格还未跌破200日线）
                // 这里我们选择持有，只有跌破200日线才卖出（更稳健的策略）
                curr.setSignal(0);
            } else {
                curr.setSignal(0);
            }
        }
        
        logger.info("策略 [{}] 信号生成完成 (短期MA={}, 长期MA={}, 滑点={}%)", 
                name, shortPeriod, longPeriod, slippage * 100);
    }
    
    /**
     * 重写回测方法，加入滑点计算
     */
    @Override
    public List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        
        int position = 0;
        double cumulativeReturn = 1.0;
        
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
            int signal = data.getSignal();
            int prevPosition = position;
            
            // 处理信号
            if (signal == 1 && position == 0) {
                position = 1;
            } else if (signal == -1 && position == 1) {
                position = 0;
            }
            
            data.setPosition(position);
            
            // 计算策略收益
            if (i > 0) {
                double dailyReturn = data.getDailyReturn();
                double strategyReturn = prevPosition * dailyReturn;
                
                // 扣除交易成本 = 佣金 + 滑点
                if (signal == 1 || signal == -1) {
                    double totalCost = commission + slippage;
                    strategyReturn -= totalCost;
                    logger.debug("日期={}: 交易成本扣除 {}% (佣金{}% + 滑点{}%)", 
                            data.getDate(), totalCost * 100, commission * 100, slippage * 100);
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
    
    public int getShortPeriod() {
        return shortPeriod;
    }
    
    public int getLongPeriod() {
        return longPeriod;
    }
    
    public double getSlippage() {
        return slippage;
    }
}

