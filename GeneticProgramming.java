import com.sun.source.tree.Tree;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
            return model -> xValues.parallelStream()
                    .mapToDouble(x -> model.evaluate(x) - groundTruth.apply(x))
                    .map(Math::abs)
                    .sum();
        }
        static <E extends Expression<E>> Function<Expression<E>, Double> meanSquaredError(List<Double> xValues, Function<Double, Double> groundTruth) {
            return model -> xValues.parallelStream()
                    .mapToDouble(x -> model.evaluate(x) - groundTruth.apply(x))
                    .map(error -> Math.pow(error, 2))
                    .sum();
        }
    }


    final static double CROSS_OVER_RATE = 0.8;
    final static double MUTATION_RATE = 0.1;

    private final static int POPULATION_SIZE = 1000;

    /**
     * Randomly select an element out of a **non-empty** list
     * @param list The list to select from
     * @return a random element of from the list
     * @param <U> The type of the element to Select
     */
    static <U> U choose(List<U> list) {
        assert !list.isEmpty();
        return list.get((int) (Math.random() * list.size()));
    }

    public static void main(String[] args) {
        List<TreeGP> population = IntStream.range(0, POPULATION_SIZE)
                .mapToObj(i -> TreeGP.generateRandTree(5, GrowthMethod.Full, Optional.empty()))
                .toList();


//        Function<Double, Double> function = x -> Math.pow(x, 3) + 2*Math.pow(x, 2) - 7*x + 3;
        Function<Double, Double> function = Math::sin;
        List<Double> points = pointsWithinRange(-10, 10, 1000);
        int epochs = 100;

        for (int i=1; i<=epochs; i++) {
            if (i % 10 == 0) {
                System.out.println("\nPopulation statistics for generation: " + i);
                populationStatistics(population, ErrorFunctions.absoluteError(points, function));
            }
            population = tournamentSelection(population, ErrorFunctions.absoluteError(points, function), TOURNAMENT_SIZE, POPULATION_SIZE / 2);
            System.out.println("Selected "+ population.size()+ " programs");
            population = breed(population, POPULATION_SIZE);
            System.out.println("Bred a new generation of "+ population.size()+ " programs");
        }
        System.out.println("Average Progression: " + populationAverages);

        var bestProgram = population.parallelStream()
                .min(Comparator.comparingDouble(m->ErrorFunctions.<TreeGP>absoluteError(points, function).apply(m)))
                .get();
        System.out.println("x = " + pointsWithinRange(-10, 10, 5000));
        System.out.println("y = "+ pointsWithinRange(-10, 10, 5000).stream().map(bestProgram::evaluate).toList());
    }

    /**
     * Generate uniformly distributed points within [lowerBound, upperBound)
     * @param lowerBound the lower bound (Inclusive) of the numbers
     * @param upperBound the upper bound (Exclusive) of the number
     * @param numberOfPoints the number of points
     * @return A list of points in the within [lowerBound, upperBound)
     */
    public static List<Double> pointsWithinRange(double lowerBound, double upperBound, int numberOfPoints) {
        return IntStream.range(0, numberOfPoints).mapToObj(i -> lowerBound + i*Math.abs(lowerBound-upperBound) /  numberOfPoints).toList();
    }

    /**
     * prints some information about the current population, given a current fitness function
     * Prints:
     *   - The best program and it's score
     *   - The average population score
     * This method also keeps track of the average scores of the population to see how the run progresses.
     * @param population The list of expressions to get information about
     * @param fitnessFunction The fitness function to evaluate against - assumed to be an error function
     * @param <E> The type of Expression to pass in
     */
    private static <E extends Expression<E>> void populationStatistics(List<E> population, Function<Expression<E>, Double> fitnessFunction) {
        if (population.isEmpty()) {
            System.out.println("Population is empty");
            return;
        }

        E bestProgram = population.parallelStream()
                .min(Comparator.comparingDouble(fitnessFunction::apply))
                .get();

        System.out.println("The most accurate model : "+bestProgram+"     ("+fitnessFunction.apply(bestProgram)+")");

        populationAverages.add(
                population.parallelStream()
                .mapToDouble(fitnessFunction::apply)
                .sum() / population.size()
        );

        System.out.println("Average Population fitness: " + populationAverages.getLast());
    }

    /**
     * Tournament Selection to select the best n expressions from the population.
     * @param population The population of expressions
     * @param fitnessFunction The fitness function to select by, assumed to be an error function
     * @param tournamentSize The size of the tournament
     * @param numberToSelect The number to select.
     * @return A list of the best n expressions
     * @param <E> The type of expression to select
     */
    private static <E extends Expression<E>> List<E> tournamentSelection(List<E> population, Function<Expression<E>, Double> fitnessFunction, int tournamentSize, int numberToSelect) {
        population = new ArrayList<>(population);
        assert tournamentSize >= 1 : "Must try and select at least one individual.";
        if (population.isEmpty() || numberToSelect == 0) {return List.of();}

        List<E> finalPopulation = population;

        return IntStream.range(0, numberToSelect)
                .parallel()
                .mapToObj(i -> {
                    // Sample with replacement
                     return ThreadLocalRandom.current()
                            .ints(0, finalPopulation.size())
                            .distinct()
                            .limit(tournamentSize)
                            .mapToObj(finalPopulation::get)
                            .min(Comparator.comparingDouble(fitnessFunction::apply))
                            .get();
                })
                .toList();
    }

    /**
     * Breed a new population from a given non-empty population
     * @param population The parent population
     * @param desiredSize The number of nodes to select
     * @return A newly bred child population
     * @param <E> The type of expression
     */
    static <E extends Expression<E>> List<E> breed(List<E> population, int desiredSize) {
        assert !population.isEmpty() : "Population cannot be empty";

        return IntStream.range(0, desiredSize).parallel()
                .map(i -> i % population.size())
                .mapToObj(i -> {
                            double p = Math.random();
                            var e = population.get(i).copy();

                            if (p < CROSS_OVER_RATE) {
                                return e.crossOver(choose(population).copy());
                            } else if (p < CROSS_OVER_RATE + MUTATION_RATE) {
                                return e.mutate();
                            } else {
                                // Reproduction
                                return e;
                            }
                        }
                )
                .toList();
    }
}

