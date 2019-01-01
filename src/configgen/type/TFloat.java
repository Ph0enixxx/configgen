package configgen.type;

import configgen.Node;

public class TFloat extends TPrimitive {

    TFloat(Node parent, String name, Constraint cons) {
        super(parent, name, cons);
        require(cons.references.isEmpty(), "浮点数不支持外键");
    }

    @Override
    public boolean hasText() {
        return false;
    }

    @Override
    public String toString() {
        return "float";
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(TypeVisitorT<T> visitor) {
        return visitor.visit(this);
    }

}