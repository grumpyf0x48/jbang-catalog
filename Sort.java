///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS info.picocli:picocli:4.5.0

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;

@Command(name = "Sort", mixinStandardHelpOptions = true, version = "Sort 0.1", description = "Sort made with jbang")
class Sort implements Callable<Integer> {

    @Spec
    private CommandSpec commandSpec;

    @Option(names = {"-i", "--ignore-case"}, description = "Enable ignore case")
    private boolean ignoreCase;

    @Option(names = {"-m", "--human-numeric-sort"}, description = "Enable human numeric sort")
    private boolean humanNumericSort;

    @Option(names = {"-n", "--numeric-sort"}, description = "Enable numeric sort")
    private boolean numericSort;

    @Option(names = {"-r", "--reverse"}, description = "Enable reverse sort")
    private boolean reverse;

    @Parameters(description = "Elements to sort")
    private String[] elements;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new Sort()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (elements == null) {
            commandSpec.commandLine().usage(System.out);
            return 1;
        }
        final Comparator comparator = reverse ? Comparator.reverseOrder() : Comparator.naturalOrder();
        Arrays.stream(elements)
                .map(element -> ignoreCase ? element.toUpperCase() : element)
                .map(this::mapElement)
                .sorted(comparator)
                .forEach(element -> System.out.printf("%s ", element));
        System.out.println();
        return 0;
    }

    private Object mapElement(final String element) {
        if (numericSort) {
            return Double.parseDouble(element);
        }
        if (humanNumericSort) {
            return HumanNumeric.parse(element);
        }
        return String.valueOf(element);
    }

    private enum Unit {
        U(1),
        k(1000 * U.size),
        K(1000 * U.size),
        M(1000 * K.size),
        G(1000 * M.size),
        T(1000 * G.size);

        private final double size;

        Unit(final double size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return this == U ? "" : name();
        }
    }

    private static class HumanNumeric implements Comparable<HumanNumeric> {

        private final double size;
        private final Unit unit;

        HumanNumeric(final double size, final Unit unit) {
            this.size = size;
            this.unit = unit;
        }

        public double getHumanSize() {
            return size * unit.size;
        }

        public static HumanNumeric parse(final String element) {
            final char lastChar = element.charAt(element.length() - 1);
            if (Character.isDigit(lastChar)) {
                return new HumanNumeric(Double.parseDouble(element), Unit.U);
            }
            for (final Unit unit : Unit.values()) {
                if (element.endsWith(unit.name())) {
                    final String[] strings = element.split(unit.name());
                    return new HumanNumeric(Double.parseDouble(strings[0]), unit);
                }
            }
            throw new IllegalArgumentException("Unknown unit: " + lastChar);
        }

        @Override
        public int compareTo(final HumanNumeric human) {
            return Double.compare(getHumanSize(), human.getHumanSize());
        }

        @Override
        public String toString() {
            return size + unit.toString();
        }
    }
}
