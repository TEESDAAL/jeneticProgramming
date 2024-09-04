public interface Expression<T extends Expression<T>> {
    double evaluate(double x);
    T mutate();
    T copy();
    T crossOver(T other);
}
