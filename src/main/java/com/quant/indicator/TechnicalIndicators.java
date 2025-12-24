package com.quant.indicator;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 技术指标计算器
 * 负责计算各种技术指标：MA、EMA、MACD、RSI、布林带等
 */
public class TechnicalIndicators {
    
    private static final Logger logger = LoggerFactory.getLogger(TechnicalIndicators.class);
    
    private final List<StockData> dataList;
    
    /**
     * 构造函数
     * 
     * @param dataList 股票数据列表
     */
    public TechnicalIndicators(List<StockData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        this.dataList = dataList;
    }
    
    /**
     * 计算简单移动平均线 (SMA)
     * 
     * @param period 周期
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateMA(int period) {
        String indicatorName = "MA" + period;
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += dataList.get(j).getClose();
                }
                dataList.get(i).setIndicator(indicatorName, sum / period);
            }
        }
        
        logger.debug("计算完成: {}", indicatorName);
        return this;
    }
    
    /**
     * 计算指数移动平均线 (EMA)
     * 
     * @param period 周期
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateEMA(int period) {
        String indicatorName = "EMA" + period;
        double multiplier = 2.0 / (period + 1);
        
        // 第一个值使用SMA
        double sum = 0;
        for (int i = 0; i < period && i < dataList.size(); i++) {
            sum += dataList.get(i).getClose();
        }
        
        if (dataList.size() >= period) {
            double ema = sum / period;
            dataList.get(period - 1).setIndicator(indicatorName, ema);
            
            // 后续使用EMA公式
            for (int i = period; i < dataList.size(); i++) {
                double prevEma = dataList.get(i - 1).getIndicator(indicatorName);
                ema = (dataList.get(i).getClose() - prevEma) * multiplier + prevEma;
                dataList.get(i).setIndicator(indicatorName, ema);
            }
        }
        
        // 前面的值设为null
        for (int i = 0; i < period - 1 && i < dataList.size(); i++) {
            dataList.get(i).setIndicator(indicatorName, null);
        }
        
        logger.debug("计算完成: {}", indicatorName);
        return this;
    }
    
    /**
     * 计算MACD指标
     * 
     * @param fastPeriod 快线周期 (默认12)
     * @param slowPeriod 慢线周期 (默认26)
     * @param signalPeriod 信号线周期 (默认9)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateMACD(int fastPeriod, int slowPeriod, int signalPeriod) {
        // 先计算快慢EMA
        calculateEMA(fastPeriod);
        calculateEMA(slowPeriod);
        
        String fastEmaName = "EMA" + fastPeriod;
        String slowEmaName = "EMA" + slowPeriod;
        
        // 计算MACD线 (DIF)
        for (int i = 0; i < dataList.size(); i++) {
            Double fastEma = dataList.get(i).getIndicator(fastEmaName);
            Double slowEma = dataList.get(i).getIndicator(slowEmaName);
            
            if (fastEma != null && slowEma != null) {
                dataList.get(i).setIndicator("MACD", fastEma - slowEma);
            }
        }
        
        // 计算信号线 (DEA) - MACD的EMA
        double multiplier = 2.0 / (signalPeriod + 1);
        int startIndex = slowPeriod - 1;
        
        if (dataList.size() > startIndex + signalPeriod - 1) {
            // 计算初始值
            double sum = 0;
            for (int i = startIndex; i < startIndex + signalPeriod; i++) {
                Double macd = dataList.get(i).getIndicator("MACD");
                if (macd != null) {
                    sum += macd;
                }
            }
            
            double signal = sum / signalPeriod;
            int signalStartIndex = startIndex + signalPeriod - 1;
            dataList.get(signalStartIndex).setIndicator("MACD_SIGNAL", signal);
            
            // 计算后续信号线
            for (int i = signalStartIndex + 1; i < dataList.size(); i++) {
                Double macd = dataList.get(i).getIndicator("MACD");
                Double prevSignal = dataList.get(i - 1).getIndicator("MACD_SIGNAL");
                
                if (macd != null && prevSignal != null) {
                    signal = (macd - prevSignal) * multiplier + prevSignal;
                    dataList.get(i).setIndicator("MACD_SIGNAL", signal);
                }
            }
        }
        
        // 计算MACD柱状图 (MACD Histogram)
        for (int i = 0; i < dataList.size(); i++) {
            Double macd = dataList.get(i).getIndicator("MACD");
            Double signal = dataList.get(i).getIndicator("MACD_SIGNAL");
            
            if (macd != null && signal != null) {
                dataList.get(i).setIndicator("MACD_HIST", macd - signal);
            }
        }
        
        logger.debug("计算完成: MACD({}, {}, {})", fastPeriod, slowPeriod, signalPeriod);
        return this;
    }
    
    /**
     * 计算MACD指标 (使用默认参数)
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateMACD() {
        return calculateMACD(12, 26, 9);
    }
    
    /**
     * 计算RSI指标
     * 
     * @param period 周期 (默认14)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateRSI(int period) {
        String indicatorName = "RSI" + period;
        
        // 计算价格变动
        double[] changes = new double[dataList.size()];
        for (int i = 1; i < dataList.size(); i++) {
            changes[i] = dataList.get(i).getClose() - dataList.get(i - 1).getClose();
        }
        
        // 计算平均涨跌幅
        for (int i = period; i < dataList.size(); i++) {
            double avgGain = 0;
            double avgLoss = 0;
            
            for (int j = i - period + 1; j <= i; j++) {
                if (changes[j] > 0) {
                    avgGain += changes[j];
                } else {
                    avgLoss += Math.abs(changes[j]);
                }
            }
            
            avgGain /= period;
            avgLoss /= period;
            
            double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            double rsi = 100 - (100 / (1 + rs));
            
            dataList.get(i).setIndicator(indicatorName, rsi);
        }
        
        logger.debug("计算完成: {}", indicatorName);
        return this;
    }
    
    /**
     * 计算RSI指标 (使用默认周期14)
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateRSI() {
        return calculateRSI(14);
    }
    
    /**
     * 计算布林带
     * 
     * @param period 周期 (默认20)
     * @param numStd 标准差倍数 (默认2)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateBollingerBands(int period, double numStd) {
        // 先计算MA
        calculateMA(period);
        String maName = "MA" + period;
        
        for (int i = period - 1; i < dataList.size(); i++) {
            Double ma = dataList.get(i).getIndicator(maName);
            if (ma == null) continue;
            
            // 计算标准差
            double sumSquares = 0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = dataList.get(j).getClose() - ma;
                sumSquares += diff * diff;
            }
            double std = Math.sqrt(sumSquares / period);
            
            // 设置布林带值
            dataList.get(i).setIndicator("BB_MIDDLE", ma);
            dataList.get(i).setIndicator("BB_UPPER", ma + numStd * std);
            dataList.get(i).setIndicator("BB_LOWER", ma - numStd * std);
            dataList.get(i).setIndicator("BB_WIDTH", (2 * numStd * std) / ma);
        }
        
        logger.debug("计算完成: Bollinger Bands({}, {})", period, numStd);
        return this;
    }
    
    /**
     * 计算布林带 (使用默认参数)
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateBollingerBands() {
        return calculateBollingerBands(20, 2.0);
    }
    
    /**
     * 计算ATR (平均真实波幅)
     * 
     * @param period 周期 (默认14)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateATR(int period) {
        String indicatorName = "ATR" + period;
        
        // 计算True Range
        double[] tr = new double[dataList.size()];
        for (int i = 1; i < dataList.size(); i++) {
            StockData curr = dataList.get(i);
            StockData prev = dataList.get(i - 1);
            
            double tr1 = curr.getHigh() - curr.getLow();
            double tr2 = Math.abs(curr.getHigh() - prev.getClose());
            double tr3 = Math.abs(curr.getLow() - prev.getClose());
            
            tr[i] = Math.max(tr1, Math.max(tr2, tr3));
        }
        
        // 计算ATR (SMA of TR)
        for (int i = period; i < dataList.size(); i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += tr[j];
            }
            dataList.get(i).setIndicator(indicatorName, sum / period);
        }
        
        logger.debug("计算完成: {}", indicatorName);
        return this;
    }
    
    /**
     * 计算日收益率
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateReturns() {
        for (int i = 1; i < dataList.size(); i++) {
            double prevClose = dataList.get(i - 1).getClose();
            double currClose = dataList.get(i).getClose();
            double dailyReturn = (currClose - prevClose) / prevClose;
            dataList.get(i).setDailyReturn(dailyReturn);
        }
        
        logger.debug("计算完成: 日收益率");
        return this;
    }
    
    /**
     * 计算历史波动率 (Historical Volatility)
     * 用于波动率目标策略
     * 
     * @param period 周期（默认20天）
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateHistoricalVolatility(int period) {
        String indicatorName = "HV" + period;
        
        // 先确保日收益率已计算
        calculateReturns();
        
        // 年化因子
        double annualizationFactor = Math.sqrt(252);
        
        for (int i = period; i < dataList.size(); i++) {
            // 计算过去 period 天的收益率标准差
            double sum = 0;
            int count = 0;
            
            for (int j = i - period; j < i; j++) {
                sum += dataList.get(j).getDailyReturn();
                count++;
            }
            
            double mean = count > 0 ? sum / count : 0;
            
            double sumSquares = 0;
            for (int j = i - period; j < i; j++) {
                double diff = dataList.get(j).getDailyReturn() - mean;
                sumSquares += diff * diff;
            }
            
            // 日波动率
            double dailyVol = count > 1 ? Math.sqrt(sumSquares / (count - 1)) : 0;
            
            // 年化波动率
            double annualizedVol = dailyVol * annualizationFactor;
            
            dataList.get(i).setIndicator(indicatorName, annualizedVol);
        }
        
        logger.debug("计算完成: {} (历史波动率)", indicatorName);
        return this;
    }
    
    /**
     * 计算历史波动率（使用默认20天周期）
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateHistoricalVolatility() {
        return calculateHistoricalVolatility(20);
    }
    
    /**
     * 计算所有基础指标
     * 
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateAllBasic() {
        calculateMA(5);
        calculateMA(10);
        calculateMA(20);
        calculateMA(60);
        calculateMA(50);   // 趋势追踪策略使用
        calculateMA(200);  // 趋势追踪策略使用
        calculateEMA(12);
        calculateEMA(26);
        calculateMACD();
        calculateRSI();
        calculateBollingerBands();
        calculateReturns();
        calculateHistoricalVolatility(20);  // 波动率目标策略使用
        
        logger.info("✓ 所有基础指标计算完成");
        return this;
    }
    
    /**
     * 获取数据列表
     * 
     * @return 股票数据列表
     */
    public List<StockData> getDataList() {
        return dataList;
    }
}

