public interface Expression<T extends Expression<T>> {
    /**
     * Get the value of the expression at x
     * @param x The x value provided to the function
     * @return a double representing f(x)
     */
    double evaluate(double x);

    /**
     * get a mutated version of this expression - may or may not mutate the node in place
     * @return A mutated version of this expression, could be the original.
     */
    T mutate();

    /**
     * Copies the expression
     * @return a copy of the expression
     */
    T copy();

    /**
     * Cross this expression with another
     * @param other the other expression to crossover with
     * @return one of the two results from cross over
     */
    T crossOver(T other);
}
