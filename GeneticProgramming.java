import com.sun.source.tree.Tree;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

enum GrowthMethod {
    Grow,
    Full
}


public class GeneticProgramming {
    private static final int TOURNAMENT_SIZE = 5;
    private static List<Double> populationAverages = new ArrayList<>();
    static class ErrorFunctions {
        static <E extends Expression<E>> Function<Expression<E>, Double> absoluteError(List<Double> xValues, Function<Double, Double> groundTruth) {
            return model -> xValues.stream()
                    .mapToDouble(x -> model.evaluate(x) - groundTruth.apply(x))
                    .map(Math::abs)
                    .sum();
        }
        static <E extends Expression<E>> Function<Expression<E>, Double> meanSquaredError(List<Double> xValues, Function<Double, Double> groundTruth) {
            return model -> xValues.stream()
                    .mapToDouble(x -> model.evaluate(x) - groundTruth.apply(x))
                    .map(error -> Math.pow(error, 2))
                    .sum();
        }
    }


    final static double CROSS_OVER_RATE = 0.8;
    final static double MUTATION_RATE = 0.1;

    private final static int POPULATION_SIZE = 100;

    static <U> U choose(List<U> list) {
        assert !list.isEmpty();
        return list.get((int) (Math.random() * list.size()));
    }

    public static void main(String[] args) {
        List<TreeGP> population = IntStream.range(0, POPULATION_SIZE)
                .mapToObj(i -> TreeGP.generateRandTree(5, GrowthMethod.Full, Optional.empty()))
                .toList();


        Function<Double, Double> function = x -> Math.pow(x, 3) + 2*Math.pow(x, 2) - 7*x + 3;
        List<Double> points = pointsWithinRange(-5, 5, 500);

        for (int i=1; i<=100; i++) {
            System.out.println("\nPopulation statistics for generation: " + i);
            populationStatistics(population, ErrorFunctions.absoluteError(points, function));
            population = tournamentSelection(population, ErrorFunctions.absoluteError(points, function), TOURNAMENT_SIZE, POPULATION_SIZE / 2);
            System.out.println("Selected "+ population.size()+ " programs");
            population = breed(population, POPULATION_SIZE);
            System.out.println("Bred a new generation of "+ population.size()+ " programs");
        }
        System.out.println("Average Progression: " + populationAverages);
    }

    static List<Double> pointsWithinRange(int lowerBound, int upperBound, int numberOfPoints) {
        return IntStream.range(0, numberOfPoints).mapToObj(i -> lowerBound + upperBound/ (double) numberOfPoints).toList();
    }

    private static <E extends Expression<E>> void populationStatistics(List<E> population, Function<Expression<E>, Double> fitnessFunction) {
        if (population.isEmpty()) {
            System.out.println("Population is empty");
            return;
        }

        E bestProgram = population.stream()
                .min(Comparator.comparingDouble(fitnessFunction::apply))
                .get();

        System.out.println("The most accurate model : "+bestProgram+"     ("+fitnessFunction.apply(bestProgram)+")");

        populationAverages.add(
                population.stream()
                .mapToDouble(fitnessFunction::apply)
                .sum() / population.size()
        );

        System.out.println("Average Population fitness: " + populationAverages.getLast());
    }

    public static <E extends Expression<E>> List<E> tournamentSelection(List<E> population, Function<Expression<E>, Double> fitnessFunction, int tournamentSize, int numberToSelect) {
        population = new ArrayList<>(population);
        assert tournamentSize >= 1 : "Must try and select at least one individual.";
        if (population.isEmpty() || numberToSelect == 0) {return List.of();}

        Collections.shuffle(population);
        List<E> selectedPopulation = new ArrayList<>();
        while (selectedPopulation.size() < numberToSelect) {
            // Sample with replacement
            var bestExpression = population.stream()
                    .limit(tournamentSize)
                    .min(Comparator.comparingDouble(fitnessFunction::apply))
                    .orElseThrow(IllegalStateException::new);
            selectedPopulation.add(bestExpression);
        }

        return selectedPopulation;
    }

    static <E extends Expression<E>> List<E> breed(List<E> population, int desiredSize) {
        List<E> newPopulation = new ArrayList<>();
        int i = 0;
        while (newPopulation.size() < desiredSize) {
            i = (i+1) % population.size();
            double p = Math.random();
            var e = population.get(i).copy();

            if (p < CROSS_OVER_RATE) {
                newPopulation.add(e.crossOver(choose(population).copy()));
            } else if (p < CROSS_OVER_RATE + MUTATION_RATE) {
                newPopulation.add(e.mutate());
            } else {
                newPopulation.add(e);
            }
        }
        return newPopulation;
    }

}

