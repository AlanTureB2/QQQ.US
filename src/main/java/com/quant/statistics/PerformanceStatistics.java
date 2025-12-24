package com.quant.statistics;

import com.quant.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
}

