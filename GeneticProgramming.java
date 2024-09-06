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


    static List<Double> pointsWithinRange(int lowerBound, int upperBound, int numberOfPoints) {
        return IntStream.range(0, numberOfPoints).mapToObj(i -> lowerBound + i*Math.abs(lowerBound-upperBound)/ (double) numberOfPoints).toList();
    }

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

    static <E extends Expression<E>> List<E> breed(List<E> population, int desiredSize) {

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

