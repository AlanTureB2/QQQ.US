package com.quant.chart;

import com.quant.model.StockData;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * QQQ K线图查看器
 * 使用 JFreeChart 绘制专业的日K线图
 * 
 * 功能：
 *   - K线图（蜡烛图）展示
 *   - 均线叠加 (MA5, MA10, MA20, MA60)
 *   - 成交量柱状图
 *   - 支持缩放和平移
 */
public class CandlestickChartViewer extends JFrame {

    private static final long serialVersionUID = 1L;
    
    // 颜色配置 - 专业暗色主题
    private static final Color BACKGROUND_COLOR = new Color(28, 28, 30);
    private static final Color PLOT_BACKGROUND = new Color(40, 40, 42);
    private static final Color GRID_COLOR = new Color(60, 60, 62);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color UP_COLOR = new Color(39, 174, 96);       // 绿色上涨
    private static final Color DOWN_COLOR = new Color(235, 87, 87);     // 红色下跌
    private static final Color VOLUME_UP_COLOR = new Color(39, 174, 96, 180);
    private static final Color VOLUME_DOWN_COLOR = new Color(235, 87, 87, 180);
    
    // 均线颜色
    private static final Color MA5_COLOR = new Color(255, 193, 7);      // 黄色
    private static final Color MA10_COLOR = new Color(156, 39, 176);    // 紫色
    private static final Color MA20_COLOR = new Color(33, 150, 243);    // 蓝色
    private static final Color MA60_COLOR = new Color(255, 87, 34);     // 橙色

    private List<StockData> dataList;
    private String title;

    /**
     * 构造函数
     * @param dataList 股票数据列表
     * @param title 图表标题
     */
    public CandlestickChartViewer(List<StockData> dataList, String title) {
        super(title);
        this.dataList = dataList;
        this.title = title;
        
        initUI();
    }

    /**
     * 初始化UI
     */
    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        // 创建组合图表
        JFreeChart chart = createCombinedChart();
        
        // 创建图表面板
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1380, 860));
        chartPanel.setMouseWheelEnabled(true);  // 启用鼠标滚轮缩放
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        chartPanel.setBackground(BACKGROUND_COLOR);
        
        add(chartPanel, BorderLayout.CENTER);
        
        // 添加信息标签
        JLabel infoLabel = createInfoLabel();
        add(infoLabel, BorderLayout.SOUTH);
    }

    /**
     * 创建组合图表（K线图 + 成交量）
     */
    private JFreeChart createCombinedChart() {
        // 创建共享的时间轴
        DateAxis dateAxis = new DateAxis("日期");
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        dateAxis.setLabelPaint(TEXT_COLOR);
        dateAxis.setTickLabelPaint(TEXT_COLOR);
        dateAxis.setAxisLinePaint(GRID_COLOR);
        
        // 创建K线图子图
        XYPlot candlestickPlot = createCandlestickPlot();
        
        // 创建成交量子图
        XYPlot volumePlot = createVolumePlot();
        
        // 创建组合图
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(dateAxis);
        combinedPlot.setGap(10.0);
        combinedPlot.add(candlestickPlot, 3);  // K线图占3/4
        combinedPlot.add(volumePlot, 1);       // 成交量占1/4
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);
        combinedPlot.setBackgroundPaint(PLOT_BACKGROUND);
        
        // 创建图表
        JFreeChart chart = new JFreeChart(
                title + " 日K线图",
                new Font("Microsoft YaHei", Font.BOLD, 18),
                combinedPlot,
                true
        );
        
        // 设置图表样式
        chart.setBackgroundPaint(BACKGROUND_COLOR);
        chart.getTitle().setPaint(TEXT_COLOR);
        chart.getLegend().setBackgroundPaint(BACKGROUND_COLOR);
        chart.getLegend().setItemPaint(TEXT_COLOR);
        
        return chart;
    }

    /**
     * 创建K线图子图
     */
    private XYPlot createCandlestickPlot() {
        // 创建K线数据集
        OHLCDataset ohlcDataset = createOHLCDataset();
        
        // 创建K线渲染器
        CandlestickRenderer candlestickRenderer = new CandlestickRenderer();
        candlestickRenderer.setUpPaint(UP_COLOR);
        candlestickRenderer.setDownPaint(DOWN_COLOR);
        candlestickRenderer.setSeriesPaint(0, TEXT_COLOR);
        candlestickRenderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);
        candlestickRenderer.setAutoWidthGap(0.5);
        candlestickRenderer.setDrawVolume(false);
        
        // 创建Y轴（价格轴）
        NumberAxis priceAxis = new NumberAxis("价格");
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setLabelPaint(TEXT_COLOR);
        priceAxis.setTickLabelPaint(TEXT_COLOR);
        priceAxis.setAxisLinePaint(GRID_COLOR);
        
        // 创建K线图
        XYPlot candlestickPlot = new XYPlot(ohlcDataset, null, priceAxis, candlestickRenderer);
        candlestickPlot.setBackgroundPaint(PLOT_BACKGROUND);
        candlestickPlot.setDomainGridlinePaint(GRID_COLOR);
        candlestickPlot.setRangeGridlinePaint(GRID_COLOR);
        candlestickPlot.setDomainGridlinesVisible(true);
        candlestickPlot.setRangeGridlinesVisible(true);
        
        // 添加均线
        addMovingAverages(candlestickPlot);
        
        return candlestickPlot;
    }

    /**
     * 添加均线到K线图
     */
    private void addMovingAverages(XYPlot plot) {
        // MA5
        TimeSeriesCollection ma5Dataset = createMADataset(5, "MA5");
        if (ma5Dataset != null) {
            int index = plot.getDatasetCount();
            plot.setDataset(index, ma5Dataset);
            XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
            maRenderer.setSeriesPaint(0, MA5_COLOR);
            maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(index, maRenderer);
        }
        
        // MA10
        TimeSeriesCollection ma10Dataset = createMADataset(10, "MA10");
        if (ma10Dataset != null) {
            int index = plot.getDatasetCount();
            plot.setDataset(index, ma10Dataset);
            XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
            maRenderer.setSeriesPaint(0, MA10_COLOR);
            maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(index, maRenderer);
        }
        
        // MA20
        TimeSeriesCollection ma20Dataset = createMADataset(20, "MA20");
        if (ma20Dataset != null) {
            int index = plot.getDatasetCount();
            plot.setDataset(index, ma20Dataset);
            XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
            maRenderer.setSeriesPaint(0, MA20_COLOR);
            maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(index, maRenderer);
        }
        
        // MA60
        TimeSeriesCollection ma60Dataset = createMADataset(60, "MA60");
        if (ma60Dataset != null) {
            int index = plot.getDatasetCount();
            plot.setDataset(index, ma60Dataset);
            XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
            maRenderer.setSeriesPaint(0, MA60_COLOR);
            maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(index, maRenderer);
        }
    }

    /**
     * 创建成交量子图
     */
    private XYPlot createVolumePlot() {
        // 创建成交量数据集
        TimeSeriesCollection volumeDataset = createVolumeDataset();
        
        // 创建成交量渲染器
        XYBarRenderer volumeRenderer = new XYBarRenderer() {
            @Override
            public Paint getItemPaint(int series, int item) {
                if (item > 0 && item < dataList.size()) {
                    StockData current = dataList.get(item);
                    StockData previous = dataList.get(item - 1);
                    if (current.getClose() >= previous.getClose()) {
                        return VOLUME_UP_COLOR;
                    } else {
                        return VOLUME_DOWN_COLOR;
                    }
                }
                return VOLUME_UP_COLOR;
            }
        };
        volumeRenderer.setShadowVisible(false);
        volumeRenderer.setMargin(0.1);
        
        // 创建Y轴（成交量轴）
        NumberAxis volumeAxis = new NumberAxis("成交量");
        volumeAxis.setAutoRangeIncludesZero(true);
        volumeAxis.setLabelPaint(TEXT_COLOR);
        volumeAxis.setTickLabelPaint(TEXT_COLOR);
        volumeAxis.setAxisLinePaint(GRID_COLOR);
        
        // 创建成交量图
        XYPlot volumePlot = new XYPlot(volumeDataset, null, volumeAxis, volumeRenderer);
        volumePlot.setBackgroundPaint(PLOT_BACKGROUND);
        volumePlot.setDomainGridlinePaint(GRID_COLOR);
        volumePlot.setRangeGridlinePaint(GRID_COLOR);
        
        return volumePlot;
    }

    /**
     * 创建OHLC数据集
     */
    private OHLCDataset createOHLCDataset() {
        int size = dataList.size();
        Date[] dates = new Date[size];
        double[] highs = new double[size];
        double[] lows = new double[size];
        double[] opens = new double[size];
        double[] closes = new double[size];
        double[] volumes = new double[size];
        
        for (int i = 0; i < size; i++) {
            StockData data = dataList.get(i);
            dates[i] = Date.from(data.getTradeDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            highs[i] = data.getHigh();
            lows[i] = data.getLow();
            opens[i] = data.getOpen();
            closes[i] = data.getClose();
            volumes[i] = data.getVolume();
        }
        
        String symbol = dataList.get(0).getSymbol();
        if (symbol == null || symbol.isEmpty()) {
            symbol = "QQQ";
        }
        
        return new DefaultHighLowDataset(symbol, dates, highs, lows, opens, closes, volumes);
    }

    /**
     * 创建均线数据集
     */
    private TimeSeriesCollection createMADataset(int period, String name) {
        TimeSeries series = new TimeSeries(name);
        
        for (StockData data : dataList) {
            Double maValue = data.getMA(period);
            if (maValue != null) {
                Day day = new Day(
                        data.getTradeDate().getDayOfMonth(),
                        data.getTradeDate().getMonthValue(),
                        data.getTradeDate().getYear()
                );
                try {
                    series.add(day, maValue);
                } catch (Exception e) {
                    // 忽略重复日期
                }
            }
        }
        
        if (series.isEmpty()) {
            return null;
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    /**
     * 创建成交量数据集
     */
    private TimeSeriesCollection createVolumeDataset() {
        TimeSeries series = new TimeSeries("成交量");
        
        for (StockData data : dataList) {
            Day day = new Day(
                    data.getTradeDate().getDayOfMonth(),
                    data.getTradeDate().getMonthValue(),
                    data.getTradeDate().getYear()
            );
            try {
                series.add(day, data.getVolume());
            } catch (Exception e) {
                // 忽略重复日期
            }
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    /**
     * 创建信息标签
     */
    private JLabel createInfoLabel() {
        if (dataList.isEmpty()) {
            return new JLabel("");
        }
        
        StockData first = dataList.get(0);
        StockData last = dataList.get(dataList.size() - 1);
        
        String info = String.format(
                "  数据范围: %s ~ %s  |  共 %d 条记录  |  " +
                "最新价: %.2f  |  最高: %.2f  |  最低: %.2f  |  " +
                "鼠标滚轮可缩放，拖动可平移",
                first.getTradeDate(), last.getTradeDate(), dataList.size(),
                last.getClose(), 
                dataList.stream().mapToDouble(StockData::getHigh).max().orElse(0),
                dataList.stream().mapToDouble(StockData::getLow).min().orElse(0)
        );
        
        JLabel label = new JLabel(info);
        label.setForeground(TEXT_COLOR);
        label.setBackground(BACKGROUND_COLOR);
        label.setOpaque(true);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        return label;
    }

    /**
     * 静态方法：显示K线图
     */
    public static void showChart(List<StockData> dataList, String title) {
        SwingUtilities.invokeLater(() -> {
            CandlestickChartViewer viewer = new CandlestickChartViewer(dataList, title);
            viewer.setVisible(true);
        });
    }
    
    /**
     * 静态方法：显示K线图（使用默认标题）
     */
    public static void showChart(List<StockData> dataList) {
        String symbol = "QQQ";
        if (!dataList.isEmpty() && dataList.get(0).getSymbol() != null) {
            symbol = dataList.get(0).getSymbol();
        }
        showChart(dataList, symbol);
    }
}

