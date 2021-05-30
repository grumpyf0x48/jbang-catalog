///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS info.picocli:picocli:4.5.0

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "WhatsNewInJava", mixinStandardHelpOptions = true, version = "WhatsNewInJava 0.1", description = "Display methods added to a Java class in a given JDK release")
class WhatsNewInJava implements Callable<Integer> {

    @Option(names = {"--since", "-s"}, paramLabel = "release", description = "JDK release (1.8, 9, 10, 11 ...) (default: 9, 10, 11)")
    String[] releases = new String[]{"9", "10", "11"};

    @Parameters(index = "0", description = "JDK sources path")
    String sourcesPath;

    @Option(names = {"--module", "-m"}, defaultValue = "java.base", description = "Module (java.base, java.desktop, java.logging ...) where to search classes (default: ${DEFAULT-VALUE})")
    String module;

    @Parameters(arity = "1..n", index = "1..n", description = "Class names")
    String[] classNames;

    @Option(names = {"--not-modified"}, defaultValue = "false", description = "Show not modified classes (default: ${DEFAULT-VALUE})")
    boolean showNotModified;

    @Option(names = {"--verbose", "-v"}, defaultValue = "false", description = "Activate verbose mode (default: ${DEFAULT-VALUE})")
    boolean verbose;

    String searchPath;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new WhatsNewInJava()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        final File searchFile = new File(sourcesPath, module);
        if (!searchFile.exists()) {
            System.err.println("Folder does not exist: " + searchFile);
            return 1;
        }
        searchPath = searchFile.getPath();
        if (verbose) {
            System.out.printf("Listing new methods for classes: %s since releases: %s parsing sources in: %s category: %s %n%n",
                    Arrays.deepToString(classNames), Arrays.deepToString(releases), sourcesPath, module);
        }
        for (int index = 0, classNamesLength = classNames.length; index < classNamesLength; index++) {
            String className = classNames[index];
            try {
                final Collection<JavaMethod> methods = getJavaMethods(className);
                if (methods.size() == 1 && !showNotModified) {
                    continue;
                }
                if (!methods.isEmpty()) {
                    System.out.println(JavaMethod.toString(methods));
                    if (index < classNamesLength - 1) {
                        System.out.println();
                    }
                }
            } catch (final IOException exception) {
                System.err.println("An error occurred: " + exception.getMessage());
            }
        }
        return 0;
    }

    private Collection<JavaMethod> getJavaMethods(final String className) throws IOException {
        return Files.walk(Paths.get(searchPath), 8)
                .filter(path -> Files.isRegularFile(path) && toClassName(path.toString()).endsWith(className))
                .findFirst()
                .map(this::getJavaMethods)
                .orElse(Collections.emptyList());
    }

    private String toClassName(final String filepath) {
        return filepath.replace(searchPath + File.separator, "")
                .replace("/", ".")
                .replaceAll(".java$", "");
    }

    private Collection<JavaMethod> getJavaMethods(final Path path) {
        try {
            return StreamSupport.stream(new JavaSinceIterator(Files.lines(path).spliterator()), false)
                    .filter(method -> method.constructor || method.isAddedInReleases(releases))
                    .collect(Collectors.toList());
        } catch (final IOException exception) {
            return Collections.emptyList();
        }
    }

    private static class JavaSinceIterator implements Spliterator<JavaMethod> {

        private final Spliterator<String> lineSpliterator;
        private String line;
        private boolean constructor = true;

        public JavaSinceIterator(final Spliterator<String> lineSpliterator) {
            this.lineSpliterator = lineSpliterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super JavaMethod> action) {
            if (innerAdvanceWhile(Predicate.not(currentLine -> currentLine.contains("@since")))) {
                return false;
            }
            final String since = line;

            if (innerAdvanceWhile(currentLine -> currentLine.contains("*") || currentLine.contains("@"))) {
                return false;
            }
            if (line.isEmpty() && innerAdvanceWhile(String::isEmpty)) {
                return false;
            }
            final String signature = line;

            action.accept(new JavaMethod(signature, since, constructor));
            if (constructor) {
                constructor = false;
            }
            return true;
        }

        private boolean innerAdvanceWhile(final Predicate<String> predicate) {
            boolean advanced;
            while ((advanced = lineSpliterator.tryAdvance(currentLine -> this.line = currentLine)) && predicate.test(line))
                ;
            return !advanced;
        }

        @Override
        public Spliterator<JavaMethod> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return 0;
        }

        @Override
        public int characteristics() {
            return lineSpliterator.characteristics();
        }
    }

    private static class JavaMethod {

        private final String signature;
        private final boolean constructor;
        private String release;

        private JavaMethod(final String signature, final String since, final boolean constructor) {
            this.signature = signature.strip().replace("{", "").stripTrailing();
            this.constructor = constructor;
            final String[] strings = since.strip().replace("*", "").stripLeading().split(" ");
            if (strings.length > 1) {
                release = strings[1];
            }
        }

        private boolean isAddedInReleases(final String[] releases) {
            return Arrays.asList(releases).contains(release);
        }

        @Override
        public String toString() {
            return signature + (constructor ? "" : ";") + " // since " + release;
        }

        public String toStringIndented() {
            return constructor ? (this + "\n{") : ("\t" + this);
        }

        public static String toString(final Collection<JavaMethod> methods) {
            return methods.stream()
                    .map(JavaMethod::toStringIndented)
                    .collect(Collectors.joining("\n"))
                    + "\n}";
        }
    }
}
