import java.util.Optional;
import java.util.stream.Stream;

public class Node extends TreeGP {
    TreeGP child1;
    TreeGP child2;
    Functions f;

    Node(TreeGP child1, TreeGP child2, Functions f, Optional<Node> parent) {
        this.child1 = child1;
        this.child2 = child2;
        this.f = f;
        this.parent = parent;
    }


    private Node(int maxDepth, GrowthMethod method, Optional<Node> parentNode) {
        this.child1 = generateRandTree(maxDepth, method, Optional.of(this));
        this.child2 = generateRandTree(maxDepth, method, Optional.of(this));
        this.f = Functions.randFunction();
        this.parent = parentNode;
    }

    static Node generateRandomTree(int maxDepth, GrowthMethod method, Optional<Node> parentNode) {
        return new Node(maxDepth, method, parentNode);
    }

    @Override
    public double evaluate(double x) {
        return f.f.apply(child1.evaluate(x), child2.evaluate(x));
    }

    @Override
    public Node copy(Optional<Node> parent) {
        var n =  new Node(null, null, f, parent);
        n.child1 = child1.copy(Optional.of(n));
        n.child2 = child2.copy(Optional.of(n));
        return n;
    }

    @Override
    protected Stream<TreeGP> toStream() {
        return Stream.of(Stream.of(this), child1.toStream(), child2.toStream()).flatMap(s->s);
    }

    @Override
    public void pointMutation() {
        this.f = Functions.randFunction();
    }

    @Override
    public String string() {
        return f + "(" + child1 + "," + child2 + ")";
    }

    public void replaceChild(TreeGP child, TreeGP newChild) {
        if (child == child1) {
            child1 = newChild;
        } else if (child == child2) {
            child2 = newChild;
        } else {
            throw new IllegalStateException("child provided not actually a child of this node");
        }

    }
}
