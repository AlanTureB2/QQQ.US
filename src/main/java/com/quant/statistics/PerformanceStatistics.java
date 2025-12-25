package com.quant.statistics;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 绩效统计模块
 * 计算策略的各种绩效指标：收益率、夏普比率、最大回撤、胜率等
 */
public class PerformanceStatistics {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceStatistics.class);
    
    // 假设年化交易日数
    private static final int TRADING_DAYS_PER_YEAR = 252;
    
    // 无风险利率 (年化)
    private static final double RISK_FREE_RATE = 0.02; // 2%
    
    private final List<StockData> dataList;
    private final double initialCapital;
    
    // 缓存计算结果
    private Double totalReturn;
    private Double annualizedReturn;
    private Double sharpeRatio;
    private Double maxDrawdown;
    private Double volatility;
    private Double winRate;
    private Integer totalTrades;
    
    /**
     * 构造函数
     * 
     * @param dataList 回测后的股票数据列表
     */
    public PerformanceStatistics(List<StockData> dataList) {
        this(dataList, 100000.0);
    }
    
    /**
     * 构造函数
     * 
     * @param dataList 回测后的股票数据列表
     * @param initialCapital 初始资金
     */
    public PerformanceStatistics(List<StockData> dataList, double initialCapital) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表不能为空");
        }
        this.dataList = dataList;
        this.initialCapital = initialCapital;
    }
    
    /**
     * 计算总收益率
     * 
     * @return 总收益率 (百分比)
     */
    public double getTotalReturn() {
        if (totalReturn == null) {
            StockData lastData = dataList.get(dataList.size() - 1);
            totalReturn = (lastData.getCumulativeReturn() - 1) * 100;
        }
        return totalReturn;
    }
    
    /**
     * 计算年化收益率
     * 
     * @return 年化收益率 (百分比)
     */
    public double getAnnualizedReturn() {
        if (annualizedReturn == null) {
            double totalReturnDecimal = getTotalReturn() / 100;
            int tradingDays = dataList.size();
            double years = (double) tradingDays / TRADING_DAYS_PER_YEAR;
            
            if (years > 0 && totalReturnDecimal > -1) {
                annualizedReturn = (Math.pow(1 + totalReturnDecimal, 1 / years) - 1) * 100;
            } else {
                annualizedReturn = 0.0;
            }
        }
        return annualizedReturn;
    }
    
    /**
     * 计算夏普比率
     * Sharpe Ratio = (策略年化收益率 - 无风险利率) / 策略波动率
     * 
     * @return 夏普比率
     */
    public double getSharpeRatio() {
        if (sharpeRatio == null) {
            double annualReturn = getAnnualizedReturn() / 100;
            double vol = getVolatility() / 100;
            
            if (vol > 0) {
                sharpeRatio = (annualReturn - RISK_FREE_RATE) / vol;
            } else {
                sharpeRatio = 0.0;
            }
        }
        return sharpeRatio;
    }
    
    /**
     * 计算年化波动率
     * 
     * @return 年化波动率 (百分比)
     */
    public double getVolatility() {
        if (volatility == null) {
            // 收集策略日收益率
            List<Double> returns = new ArrayList<>();
            for (StockData data : dataList) {
                if (data.getStrategyReturn() != 0 || data.getPosition() == 1) {
                    returns.add(data.getStrategyReturn());
                }
            }
            
            if (returns.size() < 2) {
                volatility = 0.0;
            } else {
                // 计算平均收益率
                double mean = returns.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0);
                
                // 计算标准差
                double sumSquares = 0;
                for (double r : returns) {
                    sumSquares += Math.pow(r - mean, 2);
                }
                double dailyVol = Math.sqrt(sumSquares / (returns.size() - 1));
                
                // 年化波动率
                volatility = dailyVol * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100;
            }
        }
        return volatility;
    }
    
    /**
     * 计算最大回撤
     * 
     * @return 最大回撤 (百分比，负值)
     */
    public double getMaxDrawdown() {
        if (maxDrawdown == null) {
            double peak = 0;
            double maxDd = 0;
            
            for (StockData data : dataList) {
                double value = data.getCumulativeReturn();
                if (value > peak) {
                    peak = value;
                }
                double drawdown = (value - peak) / peak;
                if (drawdown < maxDd) {
                    maxDd = drawdown;
                }
            }
            
            maxDrawdown = maxDd * 100;
        }
        return maxDrawdown;
    }
    
    /**
     * 计算最大回撤持续天数
     * 
     * @return 最大回撤持续天数
     */
    public int getMaxDrawdownDuration() {
        double peak = 0;
        int maxDuration = 0;
        int currentDuration = 0;
        
        for (StockData data : dataList) {
            double value = data.getCumulativeReturn();
            if (value >= peak) {
                peak = value;
                currentDuration = 0;
            } else {
                currentDuration++;
                if (currentDuration > maxDuration) {
                    maxDuration = currentDuration;
                }
            }
        }
        
        return maxDuration;
    }
    
    /**
     * 计算胜率
     * 
     * @return 胜率 (百分比)
     */
    public double getWinRate() {
        if (winRate == null) {
            calculateTradeStatistics();
        }
        return winRate;
    }
    
    /**
     * 获取总交易次数
     * 
     * @return 总交易次数
     */
    public int getTotalTrades() {
        if (totalTrades == null) {
            calculateTradeStatistics();
        }
        return totalTrades;
    }
    
    /**
     * 计算交易统计
     */
    private void calculateTradeStatistics() {
        int completedTrades = 0;
        int winningTrades = 0;
        double entryPrice = 0;
        
        for (StockData data : dataList) {
            if (data.getSignal() == 1) {
                entryPrice = data.getClose();
            } else if (data.getSignal() == -1 && entryPrice > 0) {
                completedTrades++;
                if (data.getClose() > entryPrice) {
                    winningTrades++;
                }
                entryPrice = 0;
            }
        }
        
        totalTrades = completedTrades;
        winRate = completedTrades > 0 ? (double) winningTrades / completedTrades * 100 : 0;
    }
    
    /**
     * 计算盈亏比
     * 
     * @return 盈亏比
     */
    public double getProfitLossRatio() {
        double totalProfit = 0;
        double totalLoss = 0;
        int winCount = 0;
        int lossCount = 0;
        double entryPrice = 0;
        
        for (StockData data : dataList) {
            if (data.getSignal() == 1) {
                entryPrice = data.getClose();
            } else if (data.getSignal() == -1 && entryPrice > 0) {
                double pnl = data.getClose() - entryPrice;
                if (pnl > 0) {
                    totalProfit += pnl;
                    winCount++;
                } else {
                    totalLoss += Math.abs(pnl);
                    lossCount++;
                }
                entryPrice = 0;
            }
        }
        
        double avgWin = winCount > 0 ? totalProfit / winCount : 0;
        double avgLoss = lossCount > 0 ? totalLoss / lossCount : 0;
        
        return avgLoss > 0 ? avgWin / avgLoss : 0;
    }
    
    /**
     * 计算Calmar比率
     * Calmar Ratio = 年化收益率 / |最大回撤|
     * 
     * @return Calmar比率
     */
    public double getCalmarRatio() {
        double maxDd = Math.abs(getMaxDrawdown());
        if (maxDd == 0) {
            return 0;
        }
        return getAnnualizedReturn() / maxDd;
    }
    
    /**
     * 计算基准收益率 (买入持有)
     * 
     * @return 基准收益率 (百分比)
     */
    public double getBenchmarkReturn() {
        if (dataList.size() < 2) {
            return 0;
        }
        
        double startPrice = dataList.get(0).getClose();
        double endPrice = dataList.get(dataList.size() - 1).getClose();
        
        return (endPrice - startPrice) / startPrice * 100;
    }
    
    /**
     * 计算超额收益 (相对于基准)
     * 
     * @return 超额收益 (百分比)
     */
    public double getExcessReturn() {
        return getTotalReturn() - getBenchmarkReturn();
    }
    
    /**
     * 打印绩效摘要
     */
    public void printSummary() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("              绩 效 统 计 报 告");
        System.out.println("=".repeat(50));
        
        System.out.println("\n【收益指标】");
        System.out.printf("  总收益率:         %8.2f%%\n", getTotalReturn());
        System.out.printf("  年化收益率:       %8.2f%%\n", getAnnualizedReturn());
        System.out.printf("  基准收益率:       %8.2f%%\n", getBenchmarkReturn());
        System.out.printf("  超额收益:         %8.2f%%\n", getExcessReturn());
        
        System.out.println("\n【风险指标】");
        System.out.printf("  年化波动率:       %8.2f%%\n", getVolatility());
        System.out.printf("  最大回撤:         %8.2f%%\n", getMaxDrawdown());
        System.out.printf("  最大回撤持续:     %8d 天\n", getMaxDrawdownDuration());
        
        System.out.println("\n【风险调整收益】");
        System.out.printf("  夏普比率:         %8.2f\n", getSharpeRatio());
        System.out.printf("  Calmar比率:       %8.2f\n", getCalmarRatio());
        
        System.out.println("\n【交易统计】");
        System.out.printf("  总交易次数:       %8d\n", getTotalTrades());
        System.out.printf("  胜率:             %8.2f%%\n", getWinRate());
        System.out.printf("  盈亏比:           %8.2f\n", getProfitLossRatio());
        
        System.out.println("\n【资金状况】");
        System.out.printf("  初始资金:         %,.2f\n", initialCapital);
        System.out.printf("  期末资金:         %,.2f\n", 
                dataList.get(dataList.size() - 1).getPortfolioValue());
        
        System.out.println("=".repeat(50) + "\n");
    }
    
    /**
     * 获取绩效摘要文本
     * 
     * @return 绩效摘要字符串
     */
    public String getSummaryText() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("绩效统计报告\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("总收益率: %.2f%%\n", getTotalReturn()));
        sb.append(String.format("年化收益率: %.2f%%\n", getAnnualizedReturn()));
        sb.append(String.format("夏普比率: %.2f\n", getSharpeRatio()));
        sb.append(String.format("最大回撤: %.2f%%\n", getMaxDrawdown()));
        sb.append(String.format("胜率: %.2f%%\n", getWinRate()));
        sb.append(String.format("盈亏比: %.2f\n", getProfitLossRatio()));
        sb.append(String.format("总交易次数: %d\n", getTotalTrades()));
        
        return sb.toString();
    }
    
    // ================== 年度统计相关方法 ==================
    
    /**
     * 年度统计数据类
     */
    public static class YearlyStats {
        private int year;
        private double yearReturn;        // 年度收益率
        private double maxDrawdown;       // 年度最大回撤
        private double sharpeRatio;       // 年度夏普比率
        private double volatility;        // 年度波动率
        private int tradingDays;          // 交易日数
        private double startValue;        // 年初值
        private double endValue;          // 年末值
        
        public YearlyStats(int year) {
            this.year = year;
        }
        
        // Getters and Setters
        public int getYear() { return year; }
        public double getYearReturn() { return yearReturn; }
        public void setYearReturn(double yearReturn) { this.yearReturn = yearReturn; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public void setMaxDrawdown(double maxDrawdown) { this.maxDrawdown = maxDrawdown; }
        public double getSharpeRatio() { return sharpeRatio; }
        public void setSharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; }
        public double getVolatility() { return volatility; }
        public void setVolatility(double volatility) { this.volatility = volatility; }
        public int getTradingDays() { return tradingDays; }
        public void setTradingDays(int tradingDays) { this.tradingDays = tradingDays; }
        public double getStartValue() { return startValue; }
        public void setStartValue(double startValue) { this.startValue = startValue; }
        public double getEndValue() { return endValue; }
        public void setEndValue(double endValue) { this.endValue = endValue; }
    }
    
    /**
     * 计算年度统计数据
     * 
     * @return 按年份排序的年度统计Map
     */
    public Map<Integer, YearlyStats> calculateYearlyStats() {
        Map<Integer, YearlyStats> yearlyStatsMap = new LinkedHashMap<>();
        Map<Integer, List<StockData>> yearDataMap = new LinkedHashMap<>();
        
        // 按年份分组数据
        for (StockData data : dataList) {
            LocalDate date = data.getDate();
            if (date == null) continue;
            
            int year = date.getYear();
            yearDataMap.computeIfAbsent(year, k -> new ArrayList<>()).add(data);
        }
        
        // 计算每年的统计数据
        for (Map.Entry<Integer, List<StockData>> entry : yearDataMap.entrySet()) {
            int year = entry.getKey();
            List<StockData> yearData = entry.getValue();
            
            if (yearData.isEmpty()) continue;
            
            YearlyStats stats = new YearlyStats(year);
            stats.setTradingDays(yearData.size());
            
            // 年初和年末累计收益值
            double startCumReturn = yearData.get(0).getCumulativeReturn();
            double endCumReturn = yearData.get(yearData.size() - 1).getCumulativeReturn();
            
            stats.setStartValue(startCumReturn);
            stats.setEndValue(endCumReturn);
            
            // 年度收益率 = (年末累计收益 / 年初累计收益 - 1) * 100
            double yearReturn = (endCumReturn / startCumReturn - 1) * 100;
            stats.setYearReturn(yearReturn);
            
            // 计算年度最大回撤
            double peak = 0;
            double maxDd = 0;
            for (StockData data : yearData) {
                double value = data.getCumulativeReturn();
                if (value > peak) {
                    peak = value;
                }
                double drawdown = (value - peak) / peak;
                if (drawdown < maxDd) {
                    maxDd = drawdown;
                }
            }
            stats.setMaxDrawdown(maxDd * 100);
            
            // 计算年度波动率和夏普比率
            List<Double> dailyReturns = new ArrayList<>();
            for (StockData data : yearData) {
                double sr = data.getStrategyReturn();
                if (sr != 0 || data.getPosition() == 1) {
                    dailyReturns.add(sr);
                }
            }
            
            if (dailyReturns.size() > 1) {
                // 计算平均日收益率
                double mean = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                
                // 计算标准差
                double sumSquares = 0;
                for (double r : dailyReturns) {
                    sumSquares += Math.pow(r - mean, 2);
                }
                double dailyVol = Math.sqrt(sumSquares / (dailyReturns.size() - 1));
                
                // 年化波动率
                double annualizedVol = dailyVol * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100;
                stats.setVolatility(annualizedVol);
                
                // 年化夏普比率
                if (annualizedVol > 0) {
                    double annualizedReturn = yearReturn / 100;
                    double sharpe = (annualizedReturn - RISK_FREE_RATE) / (annualizedVol / 100);
                    stats.setSharpeRatio(sharpe);
                }
            }
            
            yearlyStatsMap.put(year, stats);
        }
        
        return yearlyStatsMap;
    }
    
    /**
     * 打印年度统计对比表格
     * 
     * @param strategyName 策略名称
     */
    public void printYearlyStats(String strategyName) {
        Map<Integer, YearlyStats> yearlyStats = calculateYearlyStats();
        
        System.out.println("\n【" + strategyName + " - 年度统计】");
        System.out.println("-".repeat(70));
        System.out.printf("%-6s %12s %12s %12s %12s\n", 
                "年份", "年度收益率", "最大回撤", "夏普比率", "波动率");
        System.out.println("-".repeat(70));
        
        for (YearlyStats stats : yearlyStats.values()) {
            System.out.printf("%-6d %11.2f%% %11.2f%% %12.2f %11.2f%%\n",
                    stats.getYear(),
                    stats.getYearReturn(),
                    stats.getMaxDrawdown(),
                    stats.getSharpeRatio(),
                    stats.getVolatility());
        }
        System.out.println("-".repeat(70));
    }
    
    /**
     * 获取年度统计数据（供外部调用）
     */
    public Map<Integer, YearlyStats> getYearlyStats() {
        return calculateYearlyStats();
    }
}

