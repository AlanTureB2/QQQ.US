package com.quant.loader;

import com.quant.model.StockData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel数据加载器
 * 负责从Excel文件读取股票数据
 * 
 * 支持的Excel列格式:
 * id, symbol, symbol_id, kline_type, time, market_cc, trade_date, 
 * open, high, low, close, vwap, volume, amount, count, session_id
 */
public class ExcelDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataLoader.class);
    
    // 列名常量
    private static final String COL_ID = "id";
    private static final String COL_SYMBOL = "symbol";
    private static final String COL_SYMBOL_ID = "symbol_id";
    private static final String COL_KLINE_TYPE = "kline_type";
    private static final String COL_TIME = "time";
    private static final String COL_MARKET_CC = "market_cc";
    private static final String COL_TRADE_DATE = "trade_date";
    private static final String COL_OPEN = "open";
    private static final String COL_HIGH = "high";
    private static final String COL_LOW = "low";
    private static final String COL_CLOSE = "close";
    private static final String COL_VWAP = "vwap";
    private static final String COL_VOLUME = "volume";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_COUNT = "count";
    private static final String COL_SESSION_ID = "session_id";
    
    // 日期格式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("yyyy/M/d")
    };
    
    /**
     * 从Excel文件加载数据
     * 
     * @param filePath Excel文件路径
     * @return 股票数据列表
     * @throws IOException 文件读取异常
     */
    public List<StockData> loadFromExcel(String filePath) throws IOException {
        return loadFromExcel(filePath, 0);
    }
    
    /**
     * 从Excel文件加载数据
     * 
     * @param filePath Excel文件路径
     * @param sheetIndex 工作表索引
     * @return 股票数据列表
     * @throws IOException 文件读取异常
     */
    public List<StockData> loadFromExcel(String filePath, int sheetIndex) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        List<StockData> dataList = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = createWorkbook(fis, filePath)) {
            
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            
            // 读取表头，建立列索引映射
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnIndexMap = parseHeader(headerRow);
            
            // 验证必要的列是否存在
            validateColumns(columnIndexMap);
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    StockData data = parseRow(row, columnIndexMap);
                    if (data != null) {
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    logger.warn("解析第 {} 行数据失败: {}", i + 1, e.getMessage());
                }
            }
        }
        
        // 按日期排序
        dataList.sort(Comparator.comparing(StockData::getTradeDate));
        
        logger.info("✓ 成功加载数据: {} 条记录", dataList.size());
        if (!dataList.isEmpty()) {
            StockData first = dataList.get(0);
            StockData last = dataList.get(dataList.size() - 1);
            logger.info("  股票代码: {}", first.getSymbol());
            logger.info("  日期范围: {} ~ {}", first.getTradeDate(), last.getTradeDate());
        }
        
        return dataList;
    }
    
    /**
     * 根据文件扩展名创建Workbook
     */
    private Workbook createWorkbook(FileInputStream fis, String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("不支持的文件格式，请使用 .xlsx 或 .xls 文件");
        }
    }
    
    /**
     * 解析表头，建立列名到列索引的映射
     */
    private Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String columnName = getCellStringValue(cell).toLowerCase().trim();
                columnIndexMap.put(columnName, i);
            }
        }
        
        logger.debug("检测到的列: {}", columnIndexMap.keySet());
        return columnIndexMap;
    }
    
    /**
     * 验证必要的列是否存在
     */
    private void validateColumns(Map<String, Integer> columnIndexMap) {
        // 必须有交易日期和价格列
        String[] requiredColumns = {COL_TRADE_DATE, COL_OPEN, COL_HIGH, COL_LOW, COL_CLOSE};
        List<String> missingColumns = new ArrayList<>();
        
        for (String col : requiredColumns) {
            if (!columnIndexMap.containsKey(col)) {
                missingColumns.add(col);
            }
        }
        
        if (!missingColumns.isEmpty()) {
            throw new IllegalArgumentException("Excel文件缺少必要的列: " + missingColumns);
        }
    }
    
    /**
     * 解析单行数据
     */
    private StockData parseRow(Row row, Map<String, Integer> columnIndexMap) {
        StockData data = new StockData();
        
        // 解析 id
        if (columnIndexMap.containsKey(COL_ID)) {
            data.setId((long) getNumericValue(row, columnIndexMap.get(COL_ID)));
        }
        
        // 解析 symbol
        if (columnIndexMap.containsKey(COL_SYMBOL)) {
            data.setSymbol(getStringValue(row, columnIndexMap.get(COL_SYMBOL)));
        }
        
        // 解析 symbol_id
        if (columnIndexMap.containsKey(COL_SYMBOL_ID)) {
            data.setSymbolId((long) getNumericValue(row, columnIndexMap.get(COL_SYMBOL_ID)));
        }
        
        // 解析 kline_type
        if (columnIndexMap.containsKey(COL_KLINE_TYPE)) {
            data.setKlineType(getStringValue(row, columnIndexMap.get(COL_KLINE_TYPE)));
        }
        
        // 解析 time (时间戳)
        if (columnIndexMap.containsKey(COL_TIME)) {
            double timeValue = getNumericValue(row, columnIndexMap.get(COL_TIME));
            data.setTime((long) timeValue);
        }
        
        // 解析 market_cc
        if (columnIndexMap.containsKey(COL_MARKET_CC)) {
            data.setMarketCc(getStringValue(row, columnIndexMap.get(COL_MARKET_CC)));
        }
        
        // 解析 trade_date (必需)
        Cell dateCell = row.getCell(columnIndexMap.get(COL_TRADE_DATE));
        LocalDate tradeDate = parseDateCell(dateCell);
        if (tradeDate == null) {
            return null; // 日期无效，跳过此行
        }
        data.setTradeDate(tradeDate);
        
        // 解析价格数据 (必需)
        data.setOpen(getNumericValue(row, columnIndexMap.get(COL_OPEN)));
        data.setHigh(getNumericValue(row, columnIndexMap.get(COL_HIGH)));
        data.setLow(getNumericValue(row, columnIndexMap.get(COL_LOW)));
        data.setClose(getNumericValue(row, columnIndexMap.get(COL_CLOSE)));
        
        // 解析 vwap
        if (columnIndexMap.containsKey(COL_VWAP)) {
            data.setVwap(getNumericValue(row, columnIndexMap.get(COL_VWAP)));
        }
        
        // 解析 volume
        if (columnIndexMap.containsKey(COL_VOLUME)) {
            data.setVolume((long) getNumericValue(row, columnIndexMap.get(COL_VOLUME)));
        }
        
        // 解析 amount
        if (columnIndexMap.containsKey(COL_AMOUNT)) {
            data.setAmount(getNumericValue(row, columnIndexMap.get(COL_AMOUNT)));
        }
        
        // 解析 count
        if (columnIndexMap.containsKey(COL_COUNT)) {
            data.setCount((long) getNumericValue(row, columnIndexMap.get(COL_COUNT)));
        }
        
        // 解析 session_id
        if (columnIndexMap.containsKey(COL_SESSION_ID)) {
            data.setSessionId((int) getNumericValue(row, columnIndexMap.get(COL_SESSION_ID)));
        }
        
        // 验证价格数据有效性
        if (data.getClose() <= 0) {
            logger.debug("跳过无效数据行: close <= 0");
            return null;
        }
        
        return data;
    }
    
    /**
     * 解析日期单元格
     */
    private LocalDate parseDateCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                return parseStringDate(dateStr);
            } else if (cell.getCellType() == CellType.NUMERIC) {
                // 可能是 yyyyMMdd 格式的数字
                int dateNum = (int) cell.getNumericCellValue();
                String dateStr = String.valueOf(dateNum);
                return parseStringDate(dateStr);
            }
        } catch (Exception e) {
            logger.debug("日期解析失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析字符串日期
     */
    private LocalDate parseStringDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }
        
        throw new IllegalArgumentException("无法解析日期: " + dateStr);
    }
    
    /**
     * 获取数值单元格的值
     */
    private double getNumericValue(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return 0;
        }
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return 0;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return 0;
                }
                // 处理科学计数法 (如 1.29E+12)
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            logger.debug("数值解析失败: {}", e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * 获取字符串单元格的值
     */
    private String getStringValue(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        
        Cell cell = row.getCell(columnIndex);
        return getCellStringValue(cell);
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numVal = cell.getNumericCellValue();
                // 如果是整数，去掉小数部分
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    
    /**
     * 按日期范围过滤数据
     * 
     * @param dataList 原始数据列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 过滤后的数据列表
     */
    public List<StockData> filterByDateRange(List<StockData> dataList, 
                                              LocalDate startDate, 
                                              LocalDate endDate) {
        List<StockData> filtered = new ArrayList<>();
        
        for (StockData data : dataList) {
            LocalDate date = data.getTradeDate();
            if ((startDate == null || !date.isBefore(startDate)) &&
                (endDate == null || !date.isAfter(endDate))) {
                filtered.add(data);
            }
        }
        
        return filtered;
    }
    
    /**
     * 按股票代码过滤数据
     * 
     * @param dataList 原始数据列表
     * @param symbol 股票代码
     * @return 过滤后的数据列表
     */
    public List<StockData> filterBySymbol(List<StockData> dataList, String symbol) {
        List<StockData> filtered = new ArrayList<>();
        
        for (StockData data : dataList) {
            if (symbol.equalsIgnoreCase(data.getSymbol())) {
                filtered.add(data);
            }
        }
        
        return filtered;
    }
}
