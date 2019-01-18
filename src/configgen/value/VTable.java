package configgen.value;

import configgen.Node;
import configgen.data.DTable;
import configgen.define.Table;
import configgen.type.TTable;
import configgen.type.Type;
import configgen.util.CSVParser;

import java.util.*;

public class VTable extends Node {
    public final TTable tableType;
    private List<VBean> vBeanList;
    final Set<Value> primaryKeyValueSet = new LinkedHashSet<>();
    final Map<String, Set<Value>> uniqueKeyValueSetMap = new LinkedHashMap<>();

    public final Set<String> enumNames = new LinkedHashSet<>();
    public final Map<String, Integer> enumName2IntegerValueMap = new LinkedHashMap<>();

    public List<VBean> getVBeanList() {
        return vBeanList;
    }


    public VTable(VDb parent, TTable ttable, DTable dtable) {
        super(parent, ttable.name);
        tableType = ttable;
        parent.getI18n().enterTable(name);
        List<Integer> columnIndexes = new ArrayList<>();
        ttable.tbean.columns.forEach((fn, type) -> columnIndexes.addAll(dtable.dcolumns.get(fn).indexes));
        require(columnIndexes.size() > 0);

        vBeanList = new ArrayList<>(dtable.recordList.size());
        int row = 1;
        for (List<String> rowData : dtable.recordList) {
            row++; // 从2开始
            if (CSVParser.isEmptyRecord(rowData)) {
                continue;
            }
            List<Cell> cells = new ArrayList<>(columnIndexes.size());
            for (Integer columnIndex : columnIndexes) {
                Cell c = new Cell(row, columnIndex, rowData.get(columnIndex));
                cells.add(c);
            }

            VBean vbean = new VBean(ttable.tbean, cells, false);
            vBeanList.add(vbean);
        }

        // 收集主键和唯一键
        extractKeyValues(tableType.primaryKey.values(), primaryKeyValueSet);
        for (Map<String, Type> uniqueKey : tableType.uniqueKeys) {
            Set<Value> res = new HashSet<>();
            extractKeyValues(uniqueKey.values(), res);
            uniqueKeyValueSetMap.put(String.join(",", uniqueKey.keySet()), res);
        }

        // 收集枚举
        if (tableType.tableDefine.isEnum()) {
            Set<String> names = new HashSet<>();
            for (VBean vbean : vBeanList) {
                Value v = vbean.getColumnValue(tableType.getEnumColumnType());
                VString vStr = (VString) v;
                if (vStr == null) {
                    error("枚举必须是字符串");
                    break;
                }
                String e = vStr.value;
                require(!e.contains(" "), "枚举值字符串不应该包含空格");

                if (e.isEmpty()) {
                    require(tableType.tableDefine.enumType == Table.EnumType.EnumPart, "全枚举不能有空格");
                } else {
                    require(names.add(e.toUpperCase()), "枚举数据重复", e);
                    enumNames.add(e);

                    if (!tableType.tableDefine.isEnumAsPrimaryKey()) { //必须是int，这里是java生成需要
                        Type primaryKeyCol = tableType.primaryKey.values().iterator().next();
                        Value primaryV = vbean.getColumnValue(primaryKeyCol);
                        Integer iv = ((VInt) primaryV).value;
                        enumName2IntegerValueMap.put(e, iv);
                    }
                }
            }
        }
    }

    private void extractKeyValues(Collection<Type> keys, Set<Value> keyValueSet) {
        for (VBean vbean : vBeanList) {
            ArrayList<Value> vs = new ArrayList<>(keys.size());
            for (Type k : keys) {
                Value v = vbean.getColumnValue(k);
                require(v != null);
                vs.add(v);
            }
            Value keyValue;
            if (vs.size() == 1) {
                keyValue = vs.get(0);
            } else {
                keyValue = new VList(vs);
            }
            require(keyValueSet.add(keyValue), "主键或唯一键重复", keyValue);
        }

    }

    void verifyConstraint() {
        for (VBean vBean : vBeanList) {
            vBean.verifyConstraint();
        }
        if (tableType.tableDefine.isPrimaryKeySeq) {
            int seq = 1;
            for (Value value : primaryKeyValueSet) {
                VInt v = (VInt) value;
                if (v == null) {
                    error("设置了isPrimaryKeySeq 则主键必须是int");
                } else {
                    require(v.value == seq, "设置了isPrimaryKeySeq 则主键必须是1,2,3...");
                }
                seq++;
            }
        }
    }


}
