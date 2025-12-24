package com.quant.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 股票数据模型
 * 存储单条K线数据及相关指标
 */
public class StockData {
    
    // ========== 原始数据字段 (来自Excel) ==========
    private Long id;              // 记录ID
    private String symbol;        // 股票代码 (如 QQQ.NB)
    private Long symbolId;        // 股票ID
    private String klineType;     // K线类型 (D=日线, W=周线, M=月线)
    private Long time;            // 时间戳
    private String marketCc;      // 市场代码 (如 US_ETF)
    private LocalDate tradeDate;  // 交易日期
    private double open;          // 开盘价
    private double high;          // 最高价
    private double low;           // 最低价
    private double close;         // 收盘价
    private double vwap;          // 成交量加权平均价
    private long volume;          // 成交量
    private double amount;        // 成交额
    private long count;           // 成交笔数
    private int sessionId;        // 会话ID
    
    // ========== 技术指标 (动态存储) ==========
    private Map<String, Double> indicators = new HashMap<>();
    
    // ========== 交易信号与持仓 ==========
    // 交易信号: 1=买入, -1=卖出, 0=持有
    private int signal = 0;
    // 持仓状态: 1=持有, 0=空仓
    private int position = 0;
    
    // ========== 收益相关 ==========
    private double dailyReturn = 0;      // 日收益率
    private double strategyReturn = 0;   // 策略收益率
    private double cumulativeReturn = 1; // 累计收益
    private double portfolioValue = 0;   // 组合价值
    
    // ========== 构造函数 ==========
    public StockData() {}
    
    /**
     * 简化构造函数 (兼容旧代码)
     */
    public StockData(LocalDate date, double open, double high, double low, double close, long volume) {
        this.tradeDate = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    /**
     * 完整构造函数
     */
    public StockData(Long id, String symbol, Long symbolId, String klineType, Long time,
                     String marketCc, LocalDate tradeDate, double open, double high, 
                     double low, double close, double vwap, long volume, double amount,
                     long count, int sessionId) {
        this.id = id;
        this.symbol = symbol;
        this.symbolId = symbolId;
        this.klineType = klineType;
        this.time = time;
        this.marketCc = marketCc;
        this.tradeDate = tradeDate;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.vwap = vwap;
        this.volume = volume;
        this.amount = amount;
        this.count = count;
        this.sessionId = sessionId;
    }
    
    // ========== Getters and Setters ==========
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public Long getSymbolId() {
        return symbolId;
    }
    
    public void setSymbolId(Long symbolId) {
        this.symbolId = symbolId;
    }
    
    public String getKlineType() {
        return klineType;
    }
    
    public void setKlineType(String klineType) {
        this.klineType = klineType;
    }
    
    public Long getTime() {
        return time;
    }
    
    public void setTime(Long time) {
        this.time = time;
    }
    
    public String getMarketCc() {
        return marketCc;
    }
    
    public void setMarketCc(String marketCc) {
        this.marketCc = marketCc;
    }
    
    public LocalDate getTradeDate() {
        return tradeDate;
    }
    
    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }
    
    /**
     * 获取交易日期 (兼容旧代码的 getDate 方法)
     */
    public LocalDate getDate() {
        return tradeDate;
    }
    
    /**
     * 设置交易日期 (兼容旧代码的 setDate 方法)
     */
    public void setDate(LocalDate date) {
        this.tradeDate = date;
    }
    
    public double getOpen() {
        return open;
    }
    
    public void setOpen(double open) {
        this.open = open;
    }
    
    public double getHigh() {
        return high;
    }
    
    public void setHigh(double high) {
        this.high = high;
    }
    
    public double getLow() {
        return low;
    }
    
    public void setLow(double low) {
        this.low = low;
    }
    
    public double getClose() {
        return close;
    }
    
    public void setClose(double close) {
        this.close = close;
    }
    
    public double getVwap() {
        return vwap;
    }
    
    public void setVwap(double vwap) {
        this.vwap = vwap;
    }
    
    public long getVolume() {
        return volume;
    }
    
    public void setVolume(long volume) {
        this.volume = volume;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
    
    public int getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }
    
    public int getSignal() {
        return signal;
    }
    
    public void setSignal(int signal) {
        this.signal = signal;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public double getDailyReturn() {
        return dailyReturn;
    }
    
    public void setDailyReturn(double dailyReturn) {
        this.dailyReturn = dailyReturn;
    }
    
    public double getStrategyReturn() {
        return strategyReturn;
    }
    
    public void setStrategyReturn(double strategyReturn) {
        this.strategyReturn = strategyReturn;
    }
    
    public double getCumulativeReturn() {
        return cumulativeReturn;
    }
    
    public void setCumulativeReturn(double cumulativeReturn) {
        this.cumulativeReturn = cumulativeReturn;
    }
    
    public double getPortfolioValue() {
        return portfolioValue;
    }
    
    public void setPortfolioValue(double portfolioValue) {
        this.portfolioValue = portfolioValue;
    }
    
    public Map<String, Double> getIndicators() {
        return indicators;
    }
    
    public void setIndicators(Map<String, Double> indicators) {
        this.indicators = indicators;
    }
    
    // ========== 指标操作方法 ==========
    
    public void setIndicator(String name, Double value) {
        indicators.put(name, value);
    }
    
    public Double getIndicator(String name) {
        return indicators.get(name);
    }
    
    public boolean hasIndicator(String name) {
        return indicators.containsKey(name) && indicators.get(name) != null;
    }
    
    public Map<String, Double> getAllIndicators() {
        return new HashMap<>(indicators);
    }
    
    // ========== 便捷方法获取常用指标 ==========
    
    public Double getMA(int period) {
        return getIndicator("MA" + period);
    }
    
    public Double getEMA(int period) {
        return getIndicator("EMA" + period);
    }
    
    public Double getRSI(int period) {
        return getIndicator("RSI" + period);
    }
    
    public Double getMACD() {
        return getIndicator("MACD");
    }
    
    public Double getMACDSignal() {
        return getIndicator("MACD_SIGNAL");
    }
    
    public Double getMACDHist() {
        return getIndicator("MACD_HIST");
    }
    
    @Override
    public String toString() {
        return String.format("StockData{symbol=%s, date=%s, O=%.2f, H=%.2f, L=%.2f, C=%.2f, V=%d, signal=%d}",
                symbol, tradeDate, open, high, low, close, volume, signal);
    }
}
