package com.quant.strategy;

import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VIX状态切换策略 (VIX Regime Switching Strategy)
 * 
 * 核心思想：
 *   根据市场波动率状态（用历史波动率模拟VIX）切换不同的策略
 * 
 * 策略逻辑：
 *   - 低波动状态 (VIX < 15%)：使用买入持有，满仓参与上涨
 *   - 中波动状态 (15% < VIX < 25%)：使用趋势追踪策略，跟随趋势
 *   - 高波动状态 (VIX > 25%)：使用波动率目标策略，动态降仓位
 * 
 * 设计理念：
 *   - 不同市场环境需要不同的策略
 *   - 低波动市场：趋势明确，满仓持有收益最高
 *   - 高波动市场：风险优先，保护本金
 * 
 * 注意：
 *   - 由于没有真实VIX数据，使用20日历史波动率替代
 *   - 实际应用中应使用真实VIX指数
 */
public class VIXRegimeStrategy extends AbstractStrategy {
    
    // 市场状态枚举
    public enum MarketRegime {
        LOW_VOL("低波动"),
        MEDIUM_VOL("中波动"),
        HIGH_VOL("高波动");
        
        private final String description;
        
        MarketRegime(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 波动率阈值
    private final double lowVolThreshold;   // 低波动阈值（默认15%）
    private final double highVolThreshold;  // 高波动阈值（默认25%）
    
    // 波动率计算周期
    private final int volatilityPeriod;
    
    // 滑点
    private final double slippage;
    
    // 子策略
    private final Strategy lowVolStrategy;
    private final Strategy mediumVolStrategy;
    private final Strategy highVolStrategy;
    
    /**
     * 构造函数（使用默认参数和策略）
     */
    public VIXRegimeStrategy() {
        this(0.15, 0.25, 20, 0.0005);
    }
    
    /**
     * 构造函数（自定义阈值）
     */
    public VIXRegimeStrategy(double lowVolThreshold, double highVolThreshold, 
                             int volatilityPeriod, double slippage) {
        this(lowVolThreshold, highVolThreshold, volatilityPeriod, slippage,
             new BuyAndHoldStrategy(),
             new TrendFollowingStrategy(50, 200, slippage),
             new VolatilityTargetStrategy(20, 0.15, 1.0, 0.1, slippage, 0.1));
    }
    
    /**
     * 完整构造函数（自定义策略）
     */
    public VIXRegimeStrategy(double lowVolThreshold, double highVolThreshold,
                             int volatilityPeriod, double slippage,
                             Strategy lowVolStrategy, Strategy mediumVolStrategy, 
                             Strategy highVolStrategy) {
        super("VIX状态切换策略");
        
        this.lowVolThreshold = lowVolThreshold;
        this.highVolThreshold = highVolThreshold;
        this.volatilityPeriod = volatilityPeriod;
        this.slippage = slippage;
        this.lowVolStrategy = lowVolStrategy;
        this.mediumVolStrategy = mediumVolStrategy;
        this.highVolStrategy = highVolStrategy;
        
        parameters.put("lowVolThreshold", String.format("%.0f%%", lowVolThreshold * 100));
        parameters.put("highVolThreshold", String.format("%.0f%%", highVolThreshold * 100));
        parameters.put("volatilityPeriod", volatilityPeriod);
        parameters.put("lowVolStrategy", lowVolStrategy.getName());
        parameters.put("mediumVolStrategy", mediumVolStrategy.getName());
        parameters.put("highVolStrategy", highVolStrategy.getName());
    }
    
    @Override
    public void generateSignals(List<StockData> dataList) {
        // 计算历史波动率作为VIX替代
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateHistoricalVolatility(volatilityPeriod);
        
        String hvName = "HV" + volatilityPeriod;
        
        // 为每个子策略生成信号
        List<StockData> lowVolData = copyDataList(dataList);
        List<StockData> mediumVolData = copyDataList(dataList);
        List<StockData> highVolData = copyDataList(dataList);
        
        lowVolStrategy.execute(lowVolData);
        mediumVolStrategy.execute(mediumVolData);
        highVolStrategy.execute(highVolData);
        
        // 统计各状态天数
        int lowVolDays = 0, mediumVolDays = 0, highVolDays = 0;
        
        // 根据波动率状态选择策略
        for (int i = 0; i < dataList.size(); i++) {
            StockData data = dataList.get(i);
            Double hv = data.getIndicator(hvName);
            
            if (hv == null) {
                // 波动率还未计算出来，使用默认策略
                data.setIndicator("REGIME", 1.0);  // 中波动
                data.setIndicator("REGIME_WEIGHT", getPositionWeight(mediumVolData.get(i)));
                data.setSignal(mediumVolData.get(i).getSignal());
                continue;
            }
            
            MarketRegime regime;
            double positionWeight;
            int signal;
            
            if (hv < lowVolThreshold) {
                // 低波动：满仓持有
                regime = MarketRegime.LOW_VOL;
                positionWeight = getPositionWeight(lowVolData.get(i));
                signal = lowVolData.get(i).getSignal();
                lowVolDays++;
            } else if (hv > highVolThreshold) {
                // 高波动：使用波动率目标策略
                regime = MarketRegime.HIGH_VOL;
                positionWeight = getPositionWeight(highVolData.get(i));
                signal = highVolData.get(i).getSignal();
                highVolDays++;
            } else {
                // 中等波动：使用趋势追踪策略
                regime = MarketRegime.MEDIUM_VOL;
                positionWeight = getPositionWeight(mediumVolData.get(i));
                signal = mediumVolData.get(i).getSignal();
                mediumVolDays++;
            }
            
            // 存储状态信息
            data.setIndicator("REGIME", (double) regime.ordinal());
            data.setIndicator("REGIME_WEIGHT", positionWeight);
            data.setIndicator("SIMULATED_VIX", hv);
            data.setSignal(signal);
        }
        
        logger.info("策略 [{}] 信号生成完成", name);
        logger.info("  状态分布: 低波动={}天, 中波动={}天, 高波动={}天", 
                   lowVolDays, mediumVolDays, highVolDays);
    }
    
    /**
     * 获取仓位权重
     */
    private double getPositionWeight(StockData data) {
        // 优先使用动态仓位权重
        Double weight = data.getIndicator("POSITION_WEIGHT");
        if (weight != null) {
            return weight;
        }
        // 否则使用二元仓位
        return data.getPosition();
    }
    
    @Override
    public List<StockData> backtest(List<StockData> dataList, double initialCapital, double commission) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        
        double cumulativeReturn = 1.0;
        double prevWeight = 0;
        int regimeChangeCount = 0;
        int prevRegime = -1;
        
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
            
            // 获取当前状态和仓位
            Double regimeWeight = data.getIndicator("REGIME_WEIGHT");
            Double regime = data.getIndicator("REGIME");
            
            double currentWeight = regimeWeight != null ? regimeWeight : 0;
            int currentRegime = regime != null ? regime.intValue() : -1;
            
            // 设置持仓状态
            data.setPosition(currentWeight > 0 ? 1 : 0);
            
            // 计算策略收益
            if (i > 0) {
                double dailyReturn = data.getDailyReturn();
                double strategyReturn = prevWeight * dailyReturn;
                
                // 计算交易成本
                double weightChange = Math.abs(currentWeight - prevWeight);
                if (weightChange > 0.01) {  // 仓位变化超过1%
                    double tradeCost = weightChange * (commission + slippage);
                    strategyReturn -= tradeCost;
                }
                
                // 状态切换统计
                if (currentRegime != prevRegime && prevRegime >= 0) {
                    regimeChangeCount++;
                }
                
                data.setStrategyReturn(strategyReturn);
                cumulativeReturn *= (1 + strategyReturn);
            }
            
            data.setCumulativeReturn(cumulativeReturn);
            data.setPortfolioValue(initialCapital * cumulativeReturn);
            
            prevWeight = currentWeight;
            prevRegime = currentRegime;
        }
        
        // 输出回测摘要
        StockData lastData = dataList.get(dataList.size() - 1);
        logger.info("策略 [{}] 回测完成", name);
        logger.info("  期末组合价值: {}", String.format("%.2f", lastData.getPortfolioValue()));
        logger.info("  累计收益率: {}%", String.format("%.2f", (lastData.getCumulativeReturn() - 1) * 100));
        logger.info("  状态切换次数: {}", regimeChangeCount);
        
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
    
    // Getters
    public double getLowVolThreshold() {
        return lowVolThreshold;
    }
    
    public double getHighVolThreshold() {
        return highVolThreshold;
    }
    
    public int getVolatilityPeriod() {
        return volatilityPeriod;
    }
}

