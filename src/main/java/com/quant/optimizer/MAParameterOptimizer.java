package com.quant.optimizer;

import ch.qos.logback.classic.Level;
import com.quant.indicator.TechnicalIndicators;
import com.quant.model.StockData;
import com.quant.statistics.PerformanceStatistics;
import com.quant.strategy.TrendFollowingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 均线参数优化器
 * 对趋势追踪策略的短期和长期均线参数进行网格搜索优化
 */
public class MAParameterOptimizer {
    
    private static final Logger logger = LoggerFactory.getLogger(MAParameterOptimizer.class);
    
    /**
     * 优化结果类
     */
    public static class OptimizationResult implements Comparable<OptimizationResult> {
        private final int shortPeriod;
        private final int longPeriod;
        private final double totalReturn;
        private final double maxDrawdown;
        private final double sharpeRatio;
        private final double annualizedReturn;
        private final int totalTrades;
        
        public OptimizationResult(int shortPeriod, int longPeriod, 
                                   double totalReturn, double maxDrawdown, 
                                   double sharpeRatio, double annualizedReturn,
                                   int totalTrades) {
            this.shortPeriod = shortPeriod;
            this.longPeriod = longPeriod;
            this.totalReturn = totalReturn;
            this.maxDrawdown = maxDrawdown;
            this.sharpeRatio = sharpeRatio;
            this.annualizedReturn = annualizedReturn;
            this.totalTrades = totalTrades;
        }
        
        // Getters
        public int getShortPeriod() { return shortPeriod; }
        public int getLongPeriod() { return longPeriod; }
        public double getTotalReturn() { return totalReturn; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public double getSharpeRatio() { return sharpeRatio; }
        public double getAnnualizedReturn() { return annualizedReturn; }
        public int getTotalTrades() { return totalTrades; }
        
        @Override
        public int compareTo(OptimizationResult o) {
            return Double.compare(o.totalReturn, this.totalReturn);
        }
        
        @Override
        public String toString() {
            return String.format("MA%d/MA%d: 收益=%.2f%%, 回撤=%.2f%%, 夏普=%.2f, 交易=%d次",
                    shortPeriod, longPeriod, totalReturn, maxDrawdown, sharpeRatio, totalTrades);
        }
    }
    
    private final List<StockData> originalData;
    private final List<OptimizationResult> results = new ArrayList<>();
    
    // 参数范围
    private final int minPeriod;
    private final int maxPeriod;
    private final int step;
    
    /**
     * 构造函数
     * 
     * @param dataList 原始数据
     * @param minPeriod 最小周期
     * @param maxPeriod 最大周期
     * @param step 步距
     */
    public MAParameterOptimizer(List<StockData> dataList, int minPeriod, int maxPeriod, int step) {
        this.originalData = dataList;
        this.minPeriod = minPeriod;
        this.maxPeriod = maxPeriod;
        this.step = step;
    }
    
    /**
     * 使用默认参数构造（10到1000，步距10）
     */
    public MAParameterOptimizer(List<StockData> dataList) {
        this(dataList, 10, 1000, 10);
    }
    
    /**
     * 执行优化
     */
    public void optimize() {
        results.clear();
        
        // 临时禁用策略日志（避免大量输出）
        ch.qos.logback.classic.Logger strategyLogger = 
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.quant.strategy");
        ch.qos.logback.classic.Logger indicatorLogger = 
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.quant.indicator");
        Level originalStrategyLevel = strategyLogger.getLevel();
        Level originalIndicatorLevel = indicatorLogger.getLevel();
        
        // 设置为 WARN 级别，只输出警告和错误
        strategyLogger.setLevel(Level.WARN);
        indicatorLogger.setLevel(Level.WARN);
        
        try {
            // 生成所有周期值
            List<Integer> periods = new ArrayList<>();
            for (int p = minPeriod; p <= maxPeriod; p += step) {
                periods.add(p);
            }
            
            int totalCombinations = 0;
            int validCombinations = 0;
            
            // 计算总组合数（短期 < 长期，去重）
            for (int i = 0; i < periods.size(); i++) {
                for (int j = i + 1; j < periods.size(); j++) {
                    totalCombinations++;
                }
            }
            
            System.out.println("开始优化...");
            System.out.printf("参数范围: %d ~ %d, 步距: %d\n", minPeriod, maxPeriod, step);
            System.out.printf("总组合数: %d\n", totalCombinations);
            System.out.println();
            
            int progress = 0;
            int lastPercent = 0;
            
            // 遍历所有组合（短期周期 < 长期周期）
            for (int i = 0; i < periods.size(); i++) {
                int shortPeriod = periods.get(i);
                
                for (int j = i + 1; j < periods.size(); j++) {
                    int longPeriod = periods.get(j);
                    
                    progress++;
                    int percent = progress * 100 / totalCombinations;
                    if (percent >= lastPercent + 10) {
                        System.out.printf("  进度: %d%% (%d/%d)\n", percent, progress, totalCombinations);
                        lastPercent = percent;
                    }
                    
                    // 检查数据是否足够
                    if (longPeriod >= originalData.size()) {
                        continue;
                    }
                    
                    try {
                        // 复制数据
                        List<StockData> dataCopy = copyDataList(originalData);
                        
                        // 计算所需的均线
                        TechnicalIndicators indicators = new TechnicalIndicators(dataCopy);
                        indicators.calculateMA(shortPeriod);
                        indicators.calculateMA(longPeriod);
                        
                        // 创建并执行策略
                        TrendFollowingStrategy strategy = new TrendFollowingStrategy(shortPeriod, longPeriod, 0.0005);
                        strategy.execute(dataCopy);
                        
                        // 计算绩效
                        PerformanceStatistics stats = new PerformanceStatistics(dataCopy);
                        
                        OptimizationResult result = new OptimizationResult(
                                shortPeriod,
                                longPeriod,
                                stats.getTotalReturn(),
                                stats.getMaxDrawdown(),
                                stats.getSharpeRatio(),
                                stats.getAnnualizedReturn(),
                                stats.getTotalTrades()
                        );
                        
                        results.add(result);
                        validCombinations++;
                        
                    } catch (Exception e) {
                        // 忽略无效组合
                        logger.debug("跳过组合 MA{}/MA{}: {}", shortPeriod, longPeriod, e.getMessage());
                    }
                }
            }
            
            System.out.println();
            System.out.printf("优化完成! 有效组合: %d\n", validCombinations);
            
        } finally {
            // 恢复原来的日志级别
            strategyLogger.setLevel(originalStrategyLevel);
            indicatorLogger.setLevel(originalIndicatorLevel);
        }
    }
    
    /**
     * 获取收益率最高的前N个结果
     */
    public List<OptimizationResult> getTopByReturn(int n) {
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getTotalReturn(), a.getTotalReturn()))
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取夏普比率最高的前N个结果
     */
    public List<OptimizationResult> getTopBySharpe(int n) {
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getSharpeRatio(), a.getSharpeRatio()))
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取回撤最低的前N个结果（回撤是负数，所以取最大的）
     */
    public List<OptimizationResult> getTopByDrawdown(int n) {
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getMaxDrawdown(), a.getMaxDrawdown()))
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取综合评分最高的前N个结果
     * 综合评分 = 收益率排名 + 夏普排名 + 回撤排名（排名越小越好）
     */
    public List<OptimizationResult> getTopByComposite(int n) {
        // 计算每个指标的排名
        Map<OptimizationResult, Integer> returnRank = new HashMap<>();
        Map<OptimizationResult, Integer> sharpeRank = new HashMap<>();
        Map<OptimizationResult, Integer> drawdownRank = new HashMap<>();
        
        List<OptimizationResult> byReturn = results.stream()
                .sorted((a, b) -> Double.compare(b.getTotalReturn(), a.getTotalReturn()))
                .collect(Collectors.toList());
        for (int i = 0; i < byReturn.size(); i++) {
            returnRank.put(byReturn.get(i), i + 1);
        }
        
        List<OptimizationResult> bySharpe = results.stream()
                .sorted((a, b) -> Double.compare(b.getSharpeRatio(), a.getSharpeRatio()))
                .collect(Collectors.toList());
        for (int i = 0; i < bySharpe.size(); i++) {
            sharpeRank.put(bySharpe.get(i), i + 1);
        }
        
        List<OptimizationResult> byDrawdown = results.stream()
                .sorted((a, b) -> Double.compare(b.getMaxDrawdown(), a.getMaxDrawdown()))
                .collect(Collectors.toList());
        for (int i = 0; i < byDrawdown.size(); i++) {
            drawdownRank.put(byDrawdown.get(i), i + 1);
        }
        
        // 计算综合排名（排名之和越小越好）
        return results.stream()
                .sorted((a, b) -> {
                    int scoreA = returnRank.get(a) + sharpeRank.get(a) + drawdownRank.get(a);
                    int scoreB = returnRank.get(b) + sharpeRank.get(b) + drawdownRank.get(b);
                    return Integer.compare(scoreA, scoreB);
                })
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * 打印优化结果报告
     */
    public void printReport() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("【均线参数优化报告】趋势追踪+现金管理策略");
        System.out.println("=".repeat(100));
        System.out.printf("参数范围: MA%d ~ MA%d, 步距: %d\n", minPeriod, maxPeriod, step);
        System.out.printf("有效组合数: %d\n", results.size());
        System.out.println();
        
        // 收益率Top10
        System.out.println("-".repeat(100));
        System.out.println("【收益率最高 Top 10】");
        System.out.println("-".repeat(100));
        System.out.printf("%-6s %-10s %-12s %-12s %-12s %-12s %-10s\n",
                "排名", "均线组合", "总收益率", "年化收益", "最大回撤", "夏普比率", "交易次数");
        System.out.println("-".repeat(100));
        
        List<OptimizationResult> topReturn = getTopByReturn(10);
        for (int i = 0; i < topReturn.size(); i++) {
            OptimizationResult r = topReturn.get(i);
            System.out.printf("%-6d MA%-3d/MA%-4d %10.2f%% %10.2f%% %11.2f%% %12.2f %10d\n",
                    i + 1, r.getShortPeriod(), r.getLongPeriod(),
                    r.getTotalReturn(), r.getAnnualizedReturn(),
                    r.getMaxDrawdown(), r.getSharpeRatio(), r.getTotalTrades());
        }
        
        // 夏普比率Top10
        System.out.println();
        System.out.println("-".repeat(100));
        System.out.println("【夏普比率最高 Top 10】");
        System.out.println("-".repeat(100));
        System.out.printf("%-6s %-10s %-12s %-12s %-12s %-12s %-10s\n",
                "排名", "均线组合", "夏普比率", "总收益率", "年化收益", "最大回撤", "交易次数");
        System.out.println("-".repeat(100));
        
        List<OptimizationResult> topSharpe = getTopBySharpe(10);
        for (int i = 0; i < topSharpe.size(); i++) {
            OptimizationResult r = topSharpe.get(i);
            System.out.printf("%-6d MA%-3d/MA%-4d %12.2f %10.2f%% %10.2f%% %11.2f%% %10d\n",
                    i + 1, r.getShortPeriod(), r.getLongPeriod(),
                    r.getSharpeRatio(), r.getTotalReturn(), r.getAnnualizedReturn(),
                    r.getMaxDrawdown(), r.getTotalTrades());
        }
        
        // 回撤最低Top10
        System.out.println();
        System.out.println("-".repeat(100));
        System.out.println("【回撤最低 Top 10】");
        System.out.println("-".repeat(100));
        System.out.printf("%-6s %-10s %-12s %-12s %-12s %-12s %-10s\n",
                "排名", "均线组合", "最大回撤", "总收益率", "年化收益", "夏普比率", "交易次数");
        System.out.println("-".repeat(100));
        
        List<OptimizationResult> topDrawdown = getTopByDrawdown(10);
        for (int i = 0; i < topDrawdown.size(); i++) {
            OptimizationResult r = topDrawdown.get(i);
            System.out.printf("%-6d MA%-3d/MA%-4d %11.2f%% %10.2f%% %10.2f%% %12.2f %10d\n",
                    i + 1, r.getShortPeriod(), r.getLongPeriod(),
                    r.getMaxDrawdown(), r.getTotalReturn(), r.getAnnualizedReturn(),
                    r.getSharpeRatio(), r.getTotalTrades());
        }
        
        // 综合评分Top10
        System.out.println();
        System.out.println("-".repeat(100));
        System.out.println("【综合评分最优 Top 10】（收益率、夏普、回撤三项排名之和最小）");
        System.out.println("-".repeat(100));
        System.out.printf("%-6s %-10s %-12s %-12s %-12s %-12s %-10s\n",
                "排名", "均线组合", "总收益率", "最大回撤", "夏普比率", "年化收益", "交易次数");
        System.out.println("-".repeat(100));
        
        List<OptimizationResult> topComposite = getTopByComposite(10);
        for (int i = 0; i < topComposite.size(); i++) {
            OptimizationResult r = topComposite.get(i);
            System.out.printf("%-6d MA%-3d/MA%-4d %10.2f%% %11.2f%% %12.2f %10.2f%% %10d\n",
                    i + 1, r.getShortPeriod(), r.getLongPeriod(),
                    r.getTotalReturn(), r.getMaxDrawdown(),
                    r.getSharpeRatio(), r.getAnnualizedReturn(), r.getTotalTrades());
        }
        
        // 推荐建议
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("【优化建议】");
        System.out.println("=".repeat(100));
        
        // 分析频繁出现的参数
        Map<Integer, Integer> shortPeriodCount = new HashMap<>();
        Map<Integer, Integer> longPeriodCount = new HashMap<>();
        
        // 统计前30名综合排名中出现的参数频率
        List<OptimizationResult> top30 = getTopByComposite(30);
        for (OptimizationResult r : top30) {
            shortPeriodCount.merge(r.getShortPeriod(), 1, Integer::sum);
            longPeriodCount.merge(r.getLongPeriod(), 1, Integer::sum);
        }
        
        System.out.println("\n1. 【参数频率分析】（综合排名前30中出现次数）");
        System.out.println("   短期均线出现频率最高:");
        shortPeriodCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(e -> System.out.printf("     MA%-3d: %d次\n", e.getKey(), e.getValue()));
        
        System.out.println("   长期均线出现频率最高:");
        longPeriodCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(e -> System.out.printf("     MA%-3d: %d次\n", e.getKey(), e.getValue()));
        
        // 最终推荐
        System.out.println("\n2. 【最终推荐】");
        if (!topComposite.isEmpty()) {
            OptimizationResult best = topComposite.get(0);
            System.out.printf("   ★ 综合最优: MA%d / MA%d\n", best.getShortPeriod(), best.getLongPeriod());
            System.out.printf("      收益率: %.2f%%, 回撤: %.2f%%, 夏普: %.2f\n",
                    best.getTotalReturn(), best.getMaxDrawdown(), best.getSharpeRatio());
        }
        
        if (!topReturn.isEmpty()) {
            OptimizationResult bestReturn = topReturn.get(0);
            System.out.printf("   ★ 收益最高: MA%d / MA%d (收益 %.2f%%)\n", 
                    bestReturn.getShortPeriod(), bestReturn.getLongPeriod(), bestReturn.getTotalReturn());
        }
        
        if (!topSharpe.isEmpty()) {
            OptimizationResult bestSharpe = topSharpe.get(0);
            System.out.printf("   ★ 夏普最高: MA%d / MA%d (夏普 %.2f)\n", 
                    bestSharpe.getShortPeriod(), bestSharpe.getLongPeriod(), bestSharpe.getSharpeRatio());
        }
        
        if (!topDrawdown.isEmpty()) {
            OptimizationResult bestDd = topDrawdown.get(0);
            System.out.printf("   ★ 回撤最低: MA%d / MA%d (回撤 %.2f%%)\n", 
                    bestDd.getShortPeriod(), bestDd.getLongPeriod(), bestDd.getMaxDrawdown());
        }
        
        // 当前参数对比
        System.out.println("\n3. 【当前参数 MA50/MA200 排名】");
        OptimizationResult current = results.stream()
                .filter(r -> r.getShortPeriod() == 50 && r.getLongPeriod() == 200)
                .findFirst()
                .orElse(null);
        
        if (current != null) {
            long returnRank = results.stream()
                    .filter(r -> r.getTotalReturn() > current.getTotalReturn())
                    .count() + 1;
            long sharpeRank = results.stream()
                    .filter(r -> r.getSharpeRatio() > current.getSharpeRatio())
                    .count() + 1;
            long drawdownRank = results.stream()
                    .filter(r -> r.getMaxDrawdown() > current.getMaxDrawdown())
                    .count() + 1;
            
            System.out.printf("   MA50/MA200: 收益率第%d名, 夏普第%d名, 回撤第%d名 (共%d组)\n",
                    returnRank, sharpeRank, drawdownRank, results.size());
            System.out.printf("   收益: %.2f%%, 回撤: %.2f%%, 夏普: %.2f\n",
                    current.getTotalReturn(), current.getMaxDrawdown(), current.getSharpeRatio());
        }
        
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("⚠ 注意: 过度优化可能导致过拟合，建议选择经典参数或综合排名靠前的组合");
        System.out.println("=".repeat(100));
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
    
    /**
     * 获取所有结果
     */
    public List<OptimizationResult> getAllResults() {
        return new ArrayList<>(results);
    }
}

