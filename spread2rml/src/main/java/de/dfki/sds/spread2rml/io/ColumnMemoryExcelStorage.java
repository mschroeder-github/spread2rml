
package de.dfki.sds.spread2rml.io;

import de.dfki.sds.datasprout.excel.ExcelColumn;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.hephaistos.storage.InternalStorageMetaData;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import de.dfki.sds.hephaistos.storage.excel.ExcelSheet;
import de.dfki.sds.hephaistos.storage.excel.ExcelStorage;
import de.dfki.sds.hephaistos.storage.excel.ExcelStorageSummary;
import de.dfki.sds.hephaistos.storage.excel.ExcelTextStyle;
import de.dfki.sds.mschroeder.commons.lang.data.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class ColumnMemoryExcelStorage extends ExcelStorage {

    /*
    Load Sheet Regelungen with approx. 20248152 cells
    cells: 10000
    cells: 8300
    textStyles: 929
    Load Sheet BA with approx. 2358864 cells
    cells: 1878
    textStyles: 296
    Load Sheet GBA with approx. 7548 cells
    cells: 1433
    textStyles: 38
    Load Sheet Checkliste with approx. 77846750 cells
    cells: 10000
    cells: 10000
    cells: 10000
    cells: 8318
    textStyles: 609
    sheets: 4
    */
    
    private Map<Integer, ExcelTable> sheetId2table;
    private Map<Pair<Integer>, ExcelColumn> sheetCol2Column;
    private int tblIndex;
    
    public ColumnMemoryExcelStorage(InternalStorageMetaData metaData) {
        super(metaData);
        sheetId2table = new HashMap<>();
        sheetCol2Column = new HashMap<>();
    }

    @Override
    public void addCellBulk(Collection<ExcelCell> items) {
        for(ExcelCell cell : items) {
            ExcelTable table = getTable(cell.getSheetId());
            ExcelColumn col = getColumn(cell.getSheetId(), cell.getColumn(), table);
            col.setIndex(cell.getColumn());
            col.getDataCells().add(cell);
        }
    }
    
    private ExcelTable getTable(int sheetId) {
        return sheetId2table.computeIfAbsent(sheetId, id -> new ExcelTable().setIndex(tblIndex++));
    }
    
    private ExcelColumn getColumn(int sheetId, int column, ExcelTable table) {
        return sheetCol2Column.computeIfAbsent(new Pair<>(sheetId, column), id -> {
            ExcelColumn c = new ExcelColumn();
            c.setTable(table);
            table.getColumns().add(c);
            return c;
        });
    }
    
    @Override
    public void addBulk(Collection<ExcelSheet> items) {
        for(ExcelSheet sheet : items) {
            ExcelTable tbl = getTable(sheet.getId());
            tbl.setSheetName(sheet.getName());
        }
    }
    
    public List<ExcelTable> getTables() {
        return new ArrayList<>(sheetId2table.values());
    }
    
    public List<ExcelColumn> getColumns() {
        return new ArrayList<>(sheetCol2Column.values());
    }

    
    //==================================================================
    
    
    
    @Override
    public void addTextStyleBulk(Collection<ExcelTextStyle> items) {
        //System.out.println("textStyles: " + items.size());
    }
    
    @Override
    public void removeCellBulk(Collection<ExcelCell> items) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void removeTextStyleBulk(Collection<ExcelTextStyle> items) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Iterable<ExcelCell> getCellListIter(ExcelSheet sheet) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Iterable<ExcelTextStyle> getTextStyleListIter(ExcelCell cell) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public ExcelSheet getSheet(int id) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public ExcelCell getCell(int id) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public List<ExcelCell> getRow(ExcelSheet sheet, int equalRow, int greaterEqualColumn) {
        throw new RuntimeException("not implemented yet");
    }

    

    @Override
    public void removeBulk(Collection<ExcelSheet> items) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Iterable<ExcelSheet> getListIter() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void clear() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public ExcelStorageSummary summary() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public long size() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void close() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public List<String> listDeepLinks() {
        throw new RuntimeException("not implemented yet");
    }

}
