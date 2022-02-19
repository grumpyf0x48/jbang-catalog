///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS info.picocli:picocli:4.6.2

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.nio.file.Files.lines;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Stream.iterate;

@Command(name = "Vote", mixinStandardHelpOptions = true, version = "Vote 0.1", description = "Vote")
public class Vote implements Callable<Integer> {

    @Parameters(arity = "1", index = "0", description = "Options to vote for")
    String voteOptions;

    @Option(names = {"-f", "--filepath"}, description = "Text file with votes")
    String filePath;

    public static void main(final String... args) {
        System.exit(new CommandLine(new Vote()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        getPointsPerOption(getLines(filePath), voteOptions, System.out);
        return 0;
    }

    public Map<Character, Integer> getPointsPerOption(final Stream<String> votes, final String options, final PrintStream console) {
        final VotePrinter votePrinter = VotePrinter.getInstance(console);

        final String effectiveOptions = getDistinctLetters(options.toUpperCase());
        final int optionsCount = effectiveOptions.length();

        final Integer[] weightedPoints = iterate(optionsCount, n -> n - 1)
                .limit(optionsCount)
                .toArray(Integer[]::new);

        final Map<Character, Integer> pointsPerOption = new HashMap<>();

        votes.map(String::strip)
                .filter(vote -> vote.matches("([A-Z] )*[A-Z]"))
                .map(vote -> vote.toUpperCase().replaceAll("\\s+", ""))
                .forEach(vote -> {
                    if (vote.length() > optionsCount) {
                        throw new IllegalArgumentException("Too many options in: " + vote);
                    }
                    if (!vote.equals(getDistinctLetters(vote))) {
                        throw new IllegalArgumentException("Duplicate option in: " + vote);
                    }
                    for (int index = 0; index < optionsCount; index++) {
                        final Character option = vote.charAt(index);
                        if (!effectiveOptions.contains("" + option)) {
                            throw new IllegalArgumentException("Invalid option in: " + vote);
                        }
                        final Integer actualPoints = pointsPerOption.getOrDefault(option, 0);
                        final Integer obtainedPoints = weightedPoints[index];
                        votePrinter.addVoteOption(option, obtainedPoints, index == optionsCount - 1);
                        pointsPerOption.put(option, actualPoints + obtainedPoints);
                    }
                });

        final Map<Character, Integer> sortedPointsPerOption = sortByReverseValue(pointsPerOption);
        votePrinter.addVoteResults(sortedPointsPerOption);
        return sortedPointsPerOption;
    }

    private static Stream<String> getLines(final String filePath) throws IOException {
        if (filePath != null) {
            return lines(Paths.get(filePath));
        }
        return new BufferedReader(new InputStreamReader(System.in)).lines();
    }

    private static String getDistinctLetters(final String line) {
        return line.chars()
                .distinct()
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
    }

    private static Map<Character, Integer> sortByReverseValue(final Map<Character, Integer> unSortedMap) {
        final LinkedHashMap<Character, Integer> sortedMap = new LinkedHashMap<>();

        unSortedMap.entrySet()
                .stream()
                .sorted(comparingByValue(reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        return sortedMap;
    }

    private interface VotePrinter {

        default void addVoteOption(final Character option, final Integer obtainedPoints, final boolean lastOption) {
        }

        default void addVoteResults(final Map<Character, Integer> sortedPoints) {
        }

        static VotePrinter getInstance(final PrintStream console) {
            return console == null ? new NoopVotePrinter() : new ConsoleVotePrinter(console);
        }
    }

    public static class NoopVotePrinter implements VotePrinter {
    }

    public static class ConsoleVotePrinter implements VotePrinter {

        private final PrintStream console;

        public ConsoleVotePrinter(final PrintStream console) {
            this.console = console;
        }

        @Override
        public void addVoteOption(final Character option, final Integer obtainedPoints, final boolean lastOption) {
            console.append(option)
                    .append('=')
                    .append(obtainedPoints.toString())
                    .append(lastOption ? '\n' : ' ');
        }

        @Override
        public void addVoteResults(final Map<Character, Integer> sortedPoints) {
            console.append('\n')
                    .append(sortedPoints.toString().replaceAll("[,{}]", ""))
                    .append('\n')
                    .flush();
        }
    }
}
