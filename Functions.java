import java.util.Arrays;
import java.util.function.BiFunction;

public enum Functions {
    Add(Double::sum),
    Sub((a, b) -> a - b),
    Div(Functions::safeDivision),
    Mul((a, b) -> a * b),
    //Min(Math::min),
    //Max(Math::max)
    ;

    static Functions randFunction() {
       return GeneticProgramming.choose(Arrays.stream(Functions.values()).toList());
    }
    final BiFunction<Double, Double, Double> f;

    Functions(BiFunction<Double, Double, Double> f) {
        this.f = f;
    }

    static double safeDivision(double a, double b) {
        if (b == 0.0) {
            return 1.0;
        } // Common practice...
        return a / b;
    }
}
