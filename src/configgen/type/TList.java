package configgen.type;

import configgen.Node;

import java.util.Objects;

public class TList extends Type {
    public final Type value;
    public final int count; // >=0; 0 means list store in one column separated by ;
    public final char compressSeparator;

    TList(Node parent, String name, Constraint cons, String value, int count, char compressSeparator) {
        super(parent, name, cons);
        require(cons.range == null, "list not support range");
        for (SRef sref : cons.references) {
            require(!sref.refNullable, "list not support nullableRef");
            require(null == sref.mapKeyRefTable, "list not support keyRef");
        }
        this.value = resolveType("value", cons, value);
        require(Objects.nonNull(this.value), this.fullName()+" column, type = " + value + " is not exist");
        this.count = count;
        this.compressSeparator = compressSeparator;
    }


    @Override
    public String toString() {
        return "list," + value + (count > 0 ? "," + count : "");
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(TypeVisitorT<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean hasRef() {
        return value.hasRef();
    }

    @Override
    public boolean hasSubBean() {
        return value instanceof TBean;
    }

    @Override
    public boolean hasText() {
        return value.hasText();
    }

    @Override
    public int columnSpan() {
        return count == 0 ? 1 : (value.columnSpan() * count);
    }

}
