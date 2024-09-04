import java.util.Optional;
class Constant extends TerminalExpression {
    double value;
    Constant(double value, Optional<Node> parent) {
        super((x) -> value, parent);
        this.value = value;
        this.string = ""+value;
    }


    @Override
    public Constant mutate() {
        this.value *= 100+(2*Math.random() - 1);
        super.e = x -> value;
        return this;
    }
}
