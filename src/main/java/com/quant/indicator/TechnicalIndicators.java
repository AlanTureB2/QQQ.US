package com.quant.indicator;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.BollingerBandWidthIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 技术指标计算器 (基于 ta4j 库)
 * 负责计算各种技术指标：MA、EMA、MACD、RSI、布林带、ATR等
 * 
 * 使用 ta4j 库提供的经过验证的指标计算方法，确保计算准确性
 */
public class TechnicalIndicators {
    
    private static final Logger logger = LoggerFactory.getLogger(TechnicalIndicators.class);
    
    private final List<StockData> dataList;
    private final BarSeries barSeries;
    private final ClosePriceIndicator closePrice;
    
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
        
        // 构建 ta4j BarSeries
        this.barSeries = buildBarSeries(dataList);
        this.closePrice = new ClosePriceIndicator(barSeries);
        
        logger.debug("初始化 TechnicalIndicators，数据量: {}", dataList.size());
    }
    
    /**
     * 将 StockData 列表转换为 ta4j BarSeries
     */
    private BarSeries buildBarSeries(List<StockData> dataList) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withName("QQQ")
                .withNumTypeOf(DecimalNum.class)
                .build();
        
        for (StockData data : dataList) {
            ZonedDateTime dateTime = data.getDate() != null 
                    ? data.getDate().atStartOfDay(ZoneId.systemDefault())
                    : ZonedDateTime.now();
            
            series.addBar(
                    dateTime,
                    data.getOpen(),
                    data.getHigh(),
                    data.getLow(),
                    data.getClose(),
                    data.getVolume()
            );
        }
        
        return series;
    }
    
    /**
     * 计算简单移动平均线 (SMA) - 使用 ta4j
     * 
     * @param period 周期
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateMA(int period) {
        String indicatorName = "MA" + period;
        
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = sma.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
        return this;
    }
    
    /**
     * 计算指数移动平均线 (EMA) - 使用 ta4j
     * 
     * @param period 周期
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateEMA(int period) {
        String indicatorName = "EMA" + period;
        
        EMAIndicator ema = new EMAIndicator(closePrice, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = ema.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
        return this;
    }
    
    /**
     * 计算MACD指标 - 使用 ta4j
     * 
     * @param fastPeriod 快线周期 (默认12)
     * @param slowPeriod 慢线周期 (默认26)
     * @param signalPeriod 信号线周期 (默认9)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateMACD(int fastPeriod, int slowPeriod, int signalPeriod) {
        MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        
        int startIndex = slowPeriod - 1;
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < startIndex) {
                dataList.get(i).setIndicator("MACD", null);
                dataList.get(i).setIndicator("MACD_SIGNAL", null);
                dataList.get(i).setIndicator("MACD_HIST", null);
            } else {
                double macdValue = macd.getValue(i).doubleValue();
                dataList.get(i).setIndicator("MACD", macdValue);
                
                if (i >= startIndex + signalPeriod - 1) {
                    double signalValue = signal.getValue(i).doubleValue();
                    dataList.get(i).setIndicator("MACD_SIGNAL", signalValue);
                    dataList.get(i).setIndicator("MACD_HIST", macdValue - signalValue);
                }
            }
        }
        
        // 同时计算 EMA 指标（兼容旧代码）
        calculateEMA(fastPeriod);
        calculateEMA(slowPeriod);
        
        logger.debug("计算完成 (ta4j): MACD({}, {}, {})", fastPeriod, slowPeriod, signalPeriod);
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
     * 计算RSI指标 - 使用 ta4j
     * 
     * @param period 周期 (默认14)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateRSI(int period) {
        String indicatorName = "RSI" + period;
        
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = rsi.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
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
     * 计算布林带 - 使用 ta4j
     * 
     * @param period 周期 (默认20)
     * @param numStd 标准差倍数 (默认2)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateBollingerBands(int period, double numStd) {
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, period);
        
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, sd, DecimalNum.valueOf(numStd));
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMiddle, sd, DecimalNum.valueOf(numStd));
        BollingerBandWidthIndicator bbWidth = new BollingerBandWidthIndicator(bbUpper, bbMiddle, bbLower);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                dataList.get(i).setIndicator("BB_MIDDLE", null);
                dataList.get(i).setIndicator("BB_UPPER", null);
                dataList.get(i).setIndicator("BB_LOWER", null);
                dataList.get(i).setIndicator("BB_WIDTH", null);
            } else {
                dataList.get(i).setIndicator("BB_MIDDLE", bbMiddle.getValue(i).doubleValue());
                dataList.get(i).setIndicator("BB_UPPER", bbUpper.getValue(i).doubleValue());
                dataList.get(i).setIndicator("BB_LOWER", bbLower.getValue(i).doubleValue());
                // BB_WIDTH 在 ta4j 中是百分比形式
                dataList.get(i).setIndicator("BB_WIDTH", bbWidth.getValue(i).doubleValue() / 100);
            }
        }
        
        // 同时计算 MA（兼容旧代码）
        calculateMA(period);
        
        logger.debug("计算完成 (ta4j): Bollinger Bands({}, {})", period, numStd);
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
     * 计算ATR (平均真实波幅) - 使用 ta4j
     * 
     * @param period 周期 (默认14)
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateATR(int period) {
        String indicatorName = "ATR" + period;
        
        ATRIndicator atr = new ATRIndicator(barSeries, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = atr.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
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
     * 计算 ADX 指标 (Average Directional Index) - ta4j 新增
     * 
     * @param period 周期（默认14）
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateADX(int period) {
        String indicatorName = "ADX" + period;
        
        org.ta4j.core.indicators.adx.ADXIndicator adx = 
                new org.ta4j.core.indicators.adx.ADXIndicator(barSeries, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period * 2) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = adx.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
        return this;
    }
    
    /**
     * 计算 WMA (加权移动平均) - ta4j 新增
     * 
     * @param period 周期
     * @return 当前对象（链式调用）
     */
    public TechnicalIndicators calculateWMA(int period) {
        String indicatorName = "WMA" + period;
        
        WMAIndicator wma = new WMAIndicator(closePrice, period);
        
        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                dataList.get(i).setIndicator(indicatorName, null);
            } else {
                double value = wma.getValue(i).doubleValue();
                dataList.get(i).setIndicator(indicatorName, value);
            }
        }
        
        logger.debug("计算完成 (ta4j): {}", indicatorName);
        return this;
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
        calculateATR(14);
        calculateReturns();
        calculateHistoricalVolatility(20);  // 波动率目标策略使用
        
        logger.info("✓ 所有基础指标计算完成 (使用 ta4j)");
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
    
    /**
     * 获取 ta4j BarSeries（供高级用途）
     * 
     * @return BarSeries
     */
    public BarSeries getBarSeries() {
        return barSeries;
    }
}
