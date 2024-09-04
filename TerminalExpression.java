import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class TerminalExpression extends TreeGP {
    protected Function<Double, Double> e;
    protected String string = "x";

    public TerminalExpression(Function<Double, Double> e, Optional<Node> parent) {
        this.e = e;
        super.parent = parent;
    }

    public TerminalExpression(Function<Double, Double> e, Optional<Node> parent, String string) {
        this.e = e;
        super.parent = parent;
        this.string = string;
    }

    @Override
    public double evaluate(double x) {
        return e.apply(x);
    }

    @Override
    public TerminalExpression copy(Optional<Node> parent) {
        return new TerminalExpression(e, parent, string);
    }

    @Override
    protected Stream<TreeGP> toStream() {
        return Stream.of(this);
    }

    public void pointMutation() {
        TerminalExpression newNode = chooseRandomTerminal(parent);
        this.e = newNode.e;
        this.string = newNode.string();
    }

    @Override
    public String string() {return string;}
}