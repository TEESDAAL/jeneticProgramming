import com.sun.source.tree.Tree;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

abstract public class TreeGP implements Expression<TreeGP> {
    static final double subTreeMutationChance = 0.1;
    Optional<Node> parent;
    final static List<Double> intTerminals = IntStream.range(-5, 5).mapToObj(i -> (double) i).toList();
    final static List<Function<Double, Double>> nonIntTerminals = List.of(x -> x);
    final static List<Functions> functions = Arrays.stream(Functions.values()).toList();


    static TreeGP generateRandTree(int maxDepth, GrowthMethod method, Optional<Node> parent) {
        int numTerminals = intTerminals.size() + nonIntTerminals.size();
        boolean finished_growing = Math.random() < numTerminals / ((double) numTerminals + functions.size());

        if (maxDepth == 0 || (method == GrowthMethod.Grow && finished_growing)) {
            return chooseRandomTerminal(parent);
        }

        return Node.generateRandomTree(maxDepth - 1, method, parent);
    }

    static TerminalExpression chooseRandomTerminal(Optional<Node> parent) {
        if (Math.random() > 0.5) {
            return new TerminalExpression(GeneticProgramming.choose(nonIntTerminals), parent);
        }
        return new Constant(GeneticProgramming.choose(intTerminals), parent);
    }

    @Override
    public TreeGP mutate() {
        if (Math.random() <= subTreeMutationChance) {
            return subtreeMutation();
        } else {
             pointMutation();
             return this;
        }
    }

    TreeGP subtreeMutation() {
        return this.crossOver(
                generateRandTree(5, GrowthMethod.Grow, parent())
        );
    }

    abstract void pointMutation();

    public Optional<Node> parent() {
        return this.parent;
    }

    public void updateParent(Optional<Node> parent) {
        this.parent = parent;
    }

    @Override
    public TreeGP copy() {
        assert this.parent().isEmpty() : "Can only copy from top of tree";
        return copy(Optional.empty());
    }

    /**
     * Copy a specific tree node
     * @param parent
     * @return
     */
    protected abstract TreeGP copy(Optional<Node> parent);

    @Override
    public TreeGP crossOver(TreeGP other) {
        TreeGP mainSwapPoint = this.selectRandomNode();
        TreeGP otherSwapPoint = other.selectRandomNode();


        if (mainSwapPoint.parent().isEmpty()) {
            otherSwapPoint.updateParent(Optional.empty());
            return otherSwapPoint;
        }

        otherSwapPoint.updateParent(mainSwapPoint.parent());

        mainSwapPoint.parent.get().replaceChild(mainSwapPoint, otherSwapPoint);
        return this;
    }

    /**
     * Select a non-uniform random node from the tree, weighted towards non-terminals
     * has an 80% chance to select a non-terminal
     * @return A random node in the tree.
     */
    private TreeGP selectRandomNode() {
        boolean selectNonTerminal = Math.random() < 0.8;
        if (selectNonTerminal && toStream().anyMatch(e -> !(e instanceof TerminalExpression))) {
            return GeneticProgramming.choose(toStream().filter(e -> !(e instanceof TerminalExpression)).toList());
        } else {
            return GeneticProgramming.choose(toStream().filter(e -> e instanceof TerminalExpression).toList());
        }
    }

    /**
     * @return A stream containing all the nodes in the tree.
     */
    protected abstract Stream<TreeGP> toStream();


    @Override
    public String toString() {
        return this.string();
    }

    /**
     * @return the string representation of this node
     */
    abstract String string();
}
