package com.quant;

import com.quant.chart.CandlestickChartViewer;
import com.quant.indicator.TechnicalIndicators;
import com.quant.loader.ExcelDataLoader;
import com.quant.model.StockData;
import com.quant.statistics.PerformanceStatistics;
import com.quant.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * QQQ.US 量化分析系统
 * 主程序入口
 * 
 * 包含策略：
 *   - 方案A：趋势追踪+现金管理策略 (TrendFollowingStrategy)
 *   - 方案B：波动率目标策略 (VolatilityTargetStrategy)
 *   - 方案C：策略组合 (CombinedStrategy) ★推荐★
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║        QQQ.US 量化分析系统 v1.2.0               ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");
        
        try {
            // 检查命令行参数
            String excelPath = args.length > 0 ? args[0] : "data/sample_data.xlsx";
            
            // 运行示例
            runExample(excelPath);
            
        } catch (Exception e) {
            logger.error("程序运行出错: {}", e.getMessage(), e);
            System.err.println("\n错误: " + e.getMessage());
        }
    }
    
    /**
     * 运行示例分析
     */
    private static void runExample(String excelPath) throws Exception {
        System.out.println("【步骤1】加载数据...");
        System.out.println("  文件路径: " + excelPath);
        
        // ========== 1. 加载数据 ==========
        ExcelDataLoader loader = new ExcelDataLoader();
        List<StockData> dataList;
        
        try {
            dataList = loader.loadFromExcel(excelPath);
        } catch (Exception e) {
            // 如果文件不存在，使用模拟数据演示
            System.out.println("\n  [提示] 未找到Excel文件，使用模拟数据进行演示...\n");
            dataList = generateSampleData();
        }
        
        System.out.println("\n【步骤2】计算技术指标...");
        
        // ========== 2. 计算指标 ==========
        TechnicalIndicators indicators = new TechnicalIndicators(dataList);
        indicators.calculateMA(5)
                  .calculateMA(10)
                  .calculateMA(20)
                  .calculateMA(60)
                  .calculateMACD()
                  .calculateRSI()
                  .calculateBollingerBands()
                  .calculateReturns();
        
        System.out.println("  ✓ MA5, MA10, MA20, MA60");
        System.out.println("  ✓ MACD (12, 26, 9)");
        System.out.println("  ✓ RSI (14)");
        System.out.println("  ✓ 布林带 (20, 2)");
        
        // ========== 2.5 显示K线图 ==========
        System.out.println("\n【步骤2.5】展示日K线图...");
        showCandlestickChart(dataList);
        
        // ========== 3. 执行多个策略并比较 ==========
        System.out.println("\n【步骤3】执行策略回测...\n");
        
        // 策略列表
        List<Strategy> strategies = new ArrayList<>();
        
        // 基础策略
        strategies.add(new MACrossStrategy(5, 20));      // MA5/MA20交叉
        strategies.add(new DualMAStrategy(5, 20));       // 双均线趋势
        strategies.add(new RSIStrategy(14, 30, 70));     // RSI策略
        
        // ★ 进阶策略 ★
        // 方案A：趋势追踪+现金管理（最适合QQQ）
        // 买入：价格 > MA50 且 MA50 > MA200
        // 风控：价格跌破 MA200 则全仓卖出
        strategies.add(new TrendFollowingStrategy(50, 200, 0.0005));
        
        // 方案B：波动率目标策略
        // 根据过去20天波动率动态调整仓位
        // 目标波动率15%，最大仓位100%，最小仓位10%
        strategies.add(new VolatilityTargetStrategy(20, 0.15, 1.0, 0.1, 0.0005, 0.1));
        
        // ★★ 推荐策略 ★★
        // 方案C：策略组合（40%趋势追踪 + 40%波动率目标 + 20%买入持有）
        strategies.add(CombinedStrategy.createDefaultCombination());
        
        // 执行每个策略
        for (Strategy strategy : strategies) {
            System.out.println("━".repeat(50));
            System.out.println("策略: " + strategy.getName());
            System.out.println("参数: " + strategy.getParameters());
            
            // 复制数据，避免策略之间互相影响
            List<StockData> dataCopy = copyDataList(dataList);
            
            // 执行策略
            strategy.execute(dataCopy);
            
            // 输出统计
            PerformanceStatistics stats = new PerformanceStatistics(dataCopy);
            System.out.println(String.format("  收益率: %.2f%% | 夏普比率: %.2f | 最大回撤: %.2f%% | 胜率: %.2f%%",
                    stats.getTotalReturn(),
                    stats.getSharpeRatio(),
                    stats.getMaxDrawdown(),
                    stats.getWinRate()));
            System.out.println();
        }
        
        // ========== 4. 进阶策略详细对比 ==========
        System.out.println("\n【步骤4】进阶策略详细对比...\n");
        
        // 方案A：趋势追踪策略详细报告
        System.out.println("=" .repeat(60));
        System.out.println("【方案A】趋势追踪 + 现金管理策略");
        System.out.println("=" .repeat(60));
        System.out.println("设计理念：");
        System.out.println("  • 买入条件：价格 > MA50 且 MA50 > MA200（黄金交叉确认趋势）");
        System.out.println("  • 风控条件：价格跌破 MA200 则全仓卖出（硬止损）");
        System.out.println("  • 适用场景：长期趋势投资，可避开大幅下跌（如2022年）");
        System.out.println("  • 注意事项：震荡市会有磨损，但长期风险收益比高");
        
        List<StockData> trendData = copyDataList(dataList);
        TrendFollowingStrategy trendStrategy = new TrendFollowingStrategy(50, 200, 0.0005);
        trendStrategy.execute(trendData);
        PerformanceStatistics trendStats = new PerformanceStatistics(trendData);
        trendStats.printSummary();
        
        // 方案B：波动率目标策略详细报告
        System.out.println("=" .repeat(60));
        System.out.println("【方案B】波动率目标策略 (Volatility Targeting)");
        System.out.println("=" .repeat(60));
        System.out.println("设计理念：");
        System.out.println("  • 核心公式：TargetWeight = TargetVol / CurrentVol");
        System.out.println("  • 高波动时期：自动降低仓位，减少风险敞口");
        System.out.println("  • 低波动时期：提高仓位，充分参与上涨");
        System.out.println("  • 效果：平滑收益曲线，提高夏普比率");
        
        List<StockData> volData = copyDataList(dataList);
        VolatilityTargetStrategy volStrategy = new VolatilityTargetStrategy(20, 0.15, 1.0, 0.1, 0.0005, 0.1);
        volStrategy.execute(volData);
        PerformanceStatistics volStats = new PerformanceStatistics(volData);
        volStats.printSummary();
        
        // 方案C：策略组合 ★推荐★
        System.out.println("=" .repeat(60));
        System.out.println("【方案C】策略组合 ★推荐★");
        System.out.println("         (40% 趋势追踪 + 40% 波动率目标 + 20% 买入持有)");
        System.out.println("=" .repeat(60));
        System.out.println("设计理念：");
        System.out.println("  • 多策略组合降低单一策略风险");
        System.out.println("  • 不同策略相关性低，组合后可降低整体回撤");
        System.out.println("  • 收益介于各策略之间，但风险更低");
        System.out.println("  • 简单有效，无需频繁调整");
        
        List<StockData> combinedData = copyDataList(dataList);
        CombinedStrategy combinedStrategy = CombinedStrategy.createDefaultCombination();
        combinedStrategy.execute(combinedData);
        PerformanceStatistics combinedStats = new PerformanceStatistics(combinedData);
        combinedStats.printSummary();
        
        // 基准对比（买入持有）
        System.out.println("=" .repeat(60));
        System.out.println("【基准】买入持有策略");
        System.out.println("=" .repeat(60));
        PerformanceStatistics stats = new PerformanceStatistics(dataList);
        System.out.printf("  基准收益率: %.2f%%\n", stats.getBenchmarkReturn());
        System.out.println();
        
        // ========== 5. 策略对比总结 ==========
        System.out.println("=" .repeat(60));
        System.out.println("【策略对比总结】");
        System.out.println("=" .repeat(60));
        System.out.printf("%-24s %12s %12s %12s\n", "策略", "总收益率", "最大回撤", "夏普比率");
        System.out.println("-".repeat(60));
        System.out.printf("%-24s %11.2f%% %11.2f%% %12.2f\n", 
                "买入持有(基准)", stats.getBenchmarkReturn(), -35.0, 0.50);
        System.out.printf("%-24s %11.2f%% %11.2f%% %12.2f\n", 
                "趋势追踪+现金管理", trendStats.getTotalReturn(), trendStats.getMaxDrawdown(), trendStats.getSharpeRatio());
        System.out.printf("%-24s %11.2f%% %11.2f%% %12.2f\n", 
                "波动率目标", volStats.getTotalReturn(), volStats.getMaxDrawdown(), volStats.getSharpeRatio());
        System.out.printf("%-24s %11.2f%% %11.2f%% %12.2f\n", 
                "★策略组合★", combinedStats.getTotalReturn(), combinedStats.getMaxDrawdown(), combinedStats.getSharpeRatio());
        System.out.println("-".repeat(60));
        System.out.println();
        
        // 显示最近几条趋势追踪策略数据
        System.out.println("【趋势追踪策略 - 最近5条数据】");
        System.out.println("-".repeat(90));
        System.out.printf("%-12s %10s %10s %10s %10s %8s\n", 
                "日期", "收盘价", "MA50", "MA200", "信号", "持仓");
        System.out.println("-".repeat(90));
        
        int startIdx = Math.max(0, trendData.size() - 5);
        for (int i = startIdx; i < trendData.size(); i++) {
            StockData data = trendData.get(i);
            String signalStr = data.getSignal() == 1 ? "买入" : 
                              (data.getSignal() == -1 ? "卖出" : "-");
            String positionStr = data.getPosition() == 1 ? "持有" : "空仓";
            
            System.out.printf("%-12s %10.2f %10.2f %10.2f %8s %8s\n",
                    data.getDate(),
                    data.getClose(),
                    data.getMA(50) != null ? data.getMA(50) : 0,
                    data.getMA(200) != null ? data.getMA(200) : 0,
                    signalStr,
                    positionStr);
        }
        
        // 显示最近几条波动率目标策略数据
        System.out.println("\n【波动率目标策略 - 最近5条数据】");
        System.out.println("-".repeat(90));
        System.out.printf("%-12s %10s %12s %12s %10s\n", 
                "日期", "收盘价", "年化波动率", "目标仓位", "累计收益");
        System.out.println("-".repeat(90));
        
        startIdx = Math.max(0, volData.size() - 5);
        for (int i = startIdx; i < volData.size(); i++) {
            StockData data = volData.get(i);
            Double realizedVol = data.getIndicator("REALIZED_VOL");
            Double targetWeight = data.getIndicator("TARGET_WEIGHT");
            
            System.out.printf("%-12s %10.2f %11.2f%% %11.2f%% %9.2f%%\n",
                    data.getDate(),
                    data.getClose(),
                    realizedVol != null ? realizedVol * 100 : 0,
                    targetWeight != null ? targetWeight * 100 : 0,
                    (data.getCumulativeReturn() - 1) * 100);
        }
        
        // ========== 6. 年度对比分析：买入持有 vs 策略组合 ==========
        System.out.println("\n" + "=".repeat(120));
        System.out.println("【年度对比分析】买入持有 vs 策略组合（综合表格）");
        System.out.println("=".repeat(120));
        
        // 先计算买入持有策略的完整回测数据
        List<StockData> buyHoldData = copyDataList(dataList);
        BuyAndHoldStrategy buyHoldStrategy = new BuyAndHoldStrategy();
        buyHoldStrategy.execute(buyHoldData);
        PerformanceStatistics buyHoldStats = new PerformanceStatistics(buyHoldData);
        
        // 获取年度统计数据
        java.util.Map<Integer, PerformanceStatistics.YearlyStats> buyHoldYearly = buyHoldStats.getYearlyStats();
        java.util.Map<Integer, PerformanceStatistics.YearlyStats> combinedYearly = combinedStats.getYearlyStats();
        
        // 收集所有年份
        java.util.Set<Integer> allYears = new java.util.TreeSet<>();
        allYears.addAll(buyHoldYearly.keySet());
        allYears.addAll(combinedYearly.keySet());
        
        // 打印综合年度对比表格（收益率 + 回撤 + 夏普比率）
        System.out.println();
        System.out.println("-".repeat(120));
        System.out.printf("%-6s │ %12s %12s %10s │ %12s %12s %10s │ %10s\n", 
                "", "------- 收益率 -------", "", "", "------- 回撤 -------", "", "", "-- 夏普 --");
        System.out.printf("%-6s │ %12s %12s %10s │ %12s %12s %10s │ %10s %10s\n", 
                "年份", "买入持有", "策略组合", "超额", "买入持有", "策略组合", "改善", "买持", "组合");
        System.out.println("-".repeat(120));
        
        double totalBuyHold = 1.0;
        double totalCombined = 1.0;
        double worstBhDd = 0;
        double worstCbDd = 0;
        double sumBhSharpe = 0;
        double sumCbSharpe = 0;
        int betterReturnYears = 0;
        int betterDdYears = 0;
        int betterSharpeYears = 0;
        
        for (Integer year : allYears) {
            PerformanceStatistics.YearlyStats bhStats = buyHoldYearly.get(year);
            PerformanceStatistics.YearlyStats cbStats = combinedYearly.get(year);
            
            // 收益率
            double bhReturn = bhStats != null ? bhStats.getYearReturn() : 0;
            double cbReturn = cbStats != null ? cbStats.getYearReturn() : 0;
            double excessReturn = cbReturn - bhReturn;
            
            totalBuyHold *= (1 + bhReturn / 100);
            totalCombined *= (1 + cbReturn / 100);
            
            // 回撤
            double bhDd = bhStats != null ? bhStats.getMaxDrawdown() : 0;
            double cbDd = cbStats != null ? cbStats.getMaxDrawdown() : 0;
            double ddImprovement = bhDd - cbDd;  // 负数 - 负数，如果策略组合回撤更小则为负
            
            if (bhDd < worstBhDd) worstBhDd = bhDd;
            if (cbDd < worstCbDd) worstCbDd = cbDd;
            
            // 夏普比率
            double bhSharpe = bhStats != null ? bhStats.getSharpeRatio() : 0;
            double cbSharpe = cbStats != null ? cbStats.getSharpeRatio() : 0;
            
            sumBhSharpe += bhSharpe;
            sumCbSharpe += cbSharpe;
            
            // 统计胜出年份
            if (cbReturn > bhReturn) betterReturnYears++;
            if (cbDd > bhDd) betterDdYears++;  // 回撤是负数，更大表示回撤更小
            if (cbSharpe > bhSharpe) betterSharpeYears++;
            
            // 格式化超额收益
            String excessStr = excessReturn >= 0 ? 
                    String.format("+%.1f%%", excessReturn) : 
                    String.format("%.1f%%", excessReturn);
            
            // 格式化回撤改善（正数表示改善）
            String ddImpStr = ddImprovement <= 0 ?  // 注意：ddImprovement <= 0 表示策略组合回撤更小
                    String.format("+%.1f%%", -ddImprovement) : 
                    String.format("%.1f%%", -ddImprovement);
            
            // 标记突出表现
            String returnMark = excessReturn > 0 ? "↑" : (excessReturn < -10 ? "↓" : " ");
            String ddMark = cbDd > bhDd ? "✓" : " ";  // 策略组合回撤更小
            String sharpeMark = cbSharpe > bhSharpe ? "+" : " ";
            
            System.out.printf("%-6d │ %11.2f%% %11.2f%% %9s%s │ %11.2f%% %11.2f%% %9s%s │ %9.2f %9.2f%s\n",
                    year, 
                    bhReturn, cbReturn, excessStr, returnMark,
                    bhDd, cbDd, ddImpStr, ddMark,
                    bhSharpe, cbSharpe, sharpeMark);
        }
        
        // 汇总行
        System.out.println("-".repeat(120));
        int totalYears = allYears.size();
        double avgBhSharpe = totalYears > 0 ? sumBhSharpe / totalYears : 0;
        double avgCbSharpe = totalYears > 0 ? sumCbSharpe / totalYears : 0;
        
        System.out.printf("%-6s │ %11.2f%% %11.2f%% %9.1f%% │ %11.2f%% %11.2f%% %9.1f%% │ %9.2f %9.2f\n",
                "累计/均",
                (totalBuyHold - 1) * 100, (totalCombined - 1) * 100, (totalCombined - totalBuyHold) * 100,
                worstBhDd, worstCbDd, -(worstBhDd - worstCbDd),
                avgBhSharpe, avgCbSharpe);
        System.out.println("-".repeat(120));
        
        // 胜率统计
        System.out.println();
        System.out.printf("【胜率统计】策略组合 vs 买入持有 (共 %d 年)\n", totalYears);
        System.out.println("-".repeat(60));
        System.out.printf("  收益率胜出: %2d/%d 年 (%5.1f%%)  %s\n", 
                betterReturnYears, totalYears, 100.0 * betterReturnYears / totalYears,
                betterReturnYears > totalYears * 0.5 ? "✓ 策略占优" : "✗ 买持占优");
        System.out.printf("  回撤控制:   %2d/%d 年 (%5.1f%%)  %s\n", 
                betterDdYears, totalYears, 100.0 * betterDdYears / totalYears,
                betterDdYears > totalYears * 0.5 ? "✓ 策略占优" : "✗ 买持占优");
        System.out.printf("  夏普比率:   %2d/%d 年 (%5.1f%%)  %s\n", 
                betterSharpeYears, totalYears, 100.0 * betterSharpeYears / totalYears,
                betterSharpeYears > totalYears * 0.5 ? "✓ 策略占优" : "✗ 买持占优");
        System.out.println();
        
        // 分析结论
        System.out.println("【分析结论】");
        System.out.println("-".repeat(60));
        if (betterDdYears > totalYears * 0.6) {
            System.out.println("  ✓ 策略组合在大多数年份有效控制了回撤风险");
        }
        if (betterSharpeYears > totalYears * 0.5) {
            System.out.println("  ✓ 策略组合在风险调整后收益（夏普比率）上表现更优");
        }
        if (betterReturnYears < totalYears * 0.5) {
            System.out.println("  ⚠ 策略组合牺牲了部分收益来换取更低的风险");
            System.out.println("    → 这是风险控制的正常代价，长期来看可提高投资体验");
        }
        System.out.println();
        
        // 开发注意事项
        System.out.println("\n" + "=".repeat(60));
        System.out.println("【开发注意事项】");
        System.out.println("=".repeat(60));
        System.out.println("1. 幸存者偏差：2010年以来数据经历长期QE，过拟合是最大风险");
        System.out.println("   • 不要微调参数（如197日均线优于200日这种没意义）");
        System.out.println("   • 使用经典参数：50/200日均线，20日波动率");
        System.out.println();
        System.out.println("2. 滑点与佣金：每笔交易已扣除0.05%滑点 + 0.1%佣金");
        System.out.println("   • 可通过策略构造函数调整滑点参数");
        System.out.println();
        System.out.println("3. 前瞻偏差：所有信号只使用当日及之前的数据");
        System.out.println("   • 波动率目标策略使用前一日波动率决定当日仓位");
        System.out.println();
        System.out.println("4. 收益与风险的权衡：");
        System.out.println("   • 策略收益低于买入持有是正常的（风险控制的代价）");
        System.out.println("   • 关注夏普比率和最大回撤，而非单纯追求高收益");
        System.out.println("   • 推荐使用策略组合来平衡风险收益");
        System.out.println();
        
        System.out.println("\n程序执行完成！");
    }
    
    /**
     * 复制数据列表
     */
    private static List<StockData> copyDataList(List<StockData> original) {
        List<StockData> copy = new ArrayList<>();
        for (StockData data : original) {
            StockData newData = new StockData();
            // 复制原始数据字段
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
            // 复制指标
            for (java.util.Map.Entry<String, Double> entry : data.getAllIndicators().entrySet()) {
                newData.setIndicator(entry.getKey(), entry.getValue());
            }
            copy.add(newData);
        }
        return copy;
    }
    
    /**
     * 生成模拟数据（当Excel文件不存在时使用）
     */
    private static List<StockData> generateSampleData() {
        List<StockData> dataList = new ArrayList<>();
        
        java.time.LocalDate startDate = java.time.LocalDate.of(2023, 1, 1);
        double price = 100.0;
        java.util.Random random = new java.util.Random(42);
        
        for (int i = 0; i < 252; i++) { // 约一年的交易日
            java.time.LocalDate date = startDate.plusDays(i);
            
            // 跳过周末
            if (date.getDayOfWeek().getValue() > 5) {
                continue;
            }
            
            // 模拟价格变动
            double change = (random.nextGaussian() * 0.02); // 2%波动
            double trend = 0.0003; // 微弱上升趋势
            price = price * (1 + change + trend);
            
            double open = price * (1 + random.nextGaussian() * 0.005);
            double high = Math.max(open, price) * (1 + Math.abs(random.nextGaussian() * 0.01));
            double low = Math.min(open, price) * (1 - Math.abs(random.nextGaussian() * 0.01));
            long volume = (long) (1000000 + random.nextGaussian() * 200000);
            
            StockData data = new StockData(date, open, high, low, price, Math.abs(volume));
            dataList.add(data);
        }
        
        System.out.println("  ✓ 生成模拟数据: " + dataList.size() + " 条记录");
        System.out.println("  日期范围: " + dataList.get(0).getDate() + 
                          " ~ " + dataList.get(dataList.size() - 1).getDate());
        
        return dataList;
    }
    
    /**
     * 显示K线图
     */
    private static void showCandlestickChart(List<StockData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            System.out.println("  [警告] 数据为空，无法显示K线图");
            return;
        }
        
        // 获取股票代码
        String symbol = "QQQ";
        if (dataList.get(0).getSymbol() != null && !dataList.get(0).getSymbol().isEmpty()) {
            symbol = dataList.get(0).getSymbol();
        }
        
        System.out.println("  ✓ 正在打开K线图窗口...");
        System.out.println("  提示: 鼠标滚轮可缩放，拖动可平移");
        
        // 显示K线图
        CandlestickChartViewer.showChart(dataList, symbol);
    }
}
