package com.quant;

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
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║        QQQ.US 量化分析系统 v1.0.0               ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");
        
        try {
            // 检查命令行参数
            String excelPath = args.length > 0 ? args[0] : "data/sample_data.xlsx";
            
            // 运行示例
            runExample(excelPath);
            
        } catch (Exception e) {
            logger.error("程序运行出错: {}", e.getMessage(), e);
            System.err.println("\n错误: " + e.getMessage());
            System.err.println("\n使用方法: java -jar qqq-us-quant.jar <excel文件路径>");
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
        
        // ========== 3. 执行多个策略并比较 ==========
        System.out.println("\n【步骤3】执行策略回测...\n");
        
        // 策略列表
        List<Strategy> strategies = new ArrayList<>();
        strategies.add(new MACrossStrategy(5, 20));      // MA5/MA20交叉
        strategies.add(new MACrossStrategy(10, 30));     // MA10/MA30交叉
        strategies.add(new DualMAStrategy(5, 20));       // 双均线趋势
        strategies.add(new RSIStrategy(14, 30, 70));     // RSI策略
        
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
        
        // ========== 4. 详细统计报告 ==========
        System.out.println("\n【步骤4】详细统计报告（使用MA交叉策略 5/20）...");
        
        // 使用第一个策略的结果生成详细报告
        List<StockData> reportData = copyDataList(dataList);
        Strategy reportStrategy = new MACrossStrategy(5, 20);
        reportStrategy.execute(reportData);
        
        PerformanceStatistics stats = new PerformanceStatistics(reportData);
        stats.printSummary();
        
        // 显示最近几条数据
        System.out.println("【最近5条数据】");
        System.out.println("-".repeat(80));
        System.out.printf("%-12s %10s %10s %10s %10s %8s\n", 
                "日期", "收盘价", "MA5", "MA20", "信号", "持仓");
        System.out.println("-".repeat(80));
        
        int startIdx = Math.max(0, reportData.size() - 5);
        for (int i = startIdx; i < reportData.size(); i++) {
            StockData data = reportData.get(i);
            String signalStr = data.getSignal() == 1 ? "买入" : 
                              (data.getSignal() == -1 ? "卖出" : "-");
            String positionStr = data.getPosition() == 1 ? "持有" : "空仓";
            
            System.out.printf("%-12s %10.2f %10.2f %10.2f %8s %8s\n",
                    data.getDate(),
                    data.getClose(),
                    data.getMA(5) != null ? data.getMA(5) : 0,
                    data.getMA(20) != null ? data.getMA(20) : 0,
                    signalStr,
                    positionStr);
        }
        
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
}

