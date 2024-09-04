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

    protected abstract TreeGP copy(Optional<Node> parent);

    @Override
    public TreeGP crossOver(TreeGP other) {
        TreeGP mainParent = this.copy();
        TreeGP otherParent = this.copy();

        TreeGP mainSwapPoint = mainParent.selectRandomNode();
        TreeGP otherSwapPoint = otherParent.selectRandomNode();

        if (mainSwapPoint.parent().isEmpty()) {
            otherSwapPoint.updateParent(Optional.empty());
            return otherSwapPoint;
        }

        otherSwapPoint.updateParent(mainSwapPoint.parent());

        mainSwapPoint.parent.get().replaceChild(mainSwapPoint, otherSwapPoint);
        return mainParent;
    }

    private TreeGP selectRandomNode() {
        return GeneticProgramming.choose(toStream().toList());
    }

    protected abstract Stream<TreeGP> toStream();


    @Override
    public String toString() {
        return this.string();
    }

    abstract String string();
}
