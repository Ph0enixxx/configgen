package configgen.value;

import configgen.CSV;
import configgen.Node;
import configgen.type.Cfg;
import configgen.type.KeysRef;
import configgen.type.TBean;
import configgen.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VBean extends Value {
    public final TBean tbean;
    public final Map<String, Value> map = new LinkedHashMap<>();

    public VBean(Node parent, String link, TBean type, List<Cell> data) {
        super(parent, link, type, data);
        this.tbean = type;

        List<Cell> sdata;
        if (type.define.compress) {
            Assert(data.size() == 1);
            Cell dat = data.get(0);
            sdata = CSV.parseList(dat.data).stream().map(s -> new Cell(dat.row, dat.col, s)).collect(Collectors.toList());
        } else {
            Assert(data.size() == type.columnSpan());
            sdata = data;
        }

        int s = 0;
        for (Map.Entry<String, Type> e : type.fields.entrySet()) {
            String name = e.getKey();
            Type t = e.getValue();
            int span = t.columnSpan();
            map.put(name, Value.create(this, name, t, sdata.subList(s, s + span)));
            s += span;
        }
    }

    @Override
    public void accept(ValueVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void verifyConstraint() {
        verifyRefs();

        map.values().forEach(Value::verifyConstraint);

        for (KeysRef kr : tbean.keysRefs) {
            List<Value> vs = new ArrayList<>();
            for (String k : kr.define.keys) {
                vs.add(map.get(k));
            }
            VList key = new VList(this, "__ref_" + kr.define.name, vs);
            if (null != kr.ref){
                Assert(!key.isNull(), key.toString(), "null not support ref", kr.ref.location());
                Assert(kr.ref.value.vkeys.contains(key), key.toString(), "not found in ref", kr.ref.location());
            } else if (null != kr.nullableRef && !key.isNull()){
                Assert(kr.nullableRef.value.vkeys.contains(key), key.toString(), "not found in ref", kr.nullableRef.location());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof VBean && type == ((VBean) o).type && map.equals(((VBean) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
