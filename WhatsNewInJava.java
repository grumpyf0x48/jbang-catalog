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
import java.util.stream.Stream;
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

    @Parameters(arity = "1..n", index = "1..n", description = "Class names or regexps")
    String[] classNames;

    @Option(names = {"--not-modified-classes", "-a"}, defaultValue = "false", description = "Show all classes even not modified ones (default: ${DEFAULT-VALUE})")
    boolean showNotModifiedClasses;

    @Option(names = {"--only-class-names", "-c"}, defaultValue = "false", description = "Show only names of modified classes, not their methods (default: ${DEFAULT-VALUE})")
    boolean showOnlyClassNames;

    @Option(names = {"--verbose", "-v"}, defaultValue = "false", description = "Activate verbose mode (default: ${DEFAULT-VALUE})")
    boolean verbose;

    String searchPath;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new WhatsNewInJava()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        var searchFile = new File(sourcesPath, module);
        if (!searchFile.exists()) {
            System.err.println("Folder does not exist: " + searchFile);
            return 1;
        }
        searchPath = searchFile.getPath();

        if (verbose) {
            System.out.printf("Listing new methods for classes: %s since releases: %s parsing sources in: %s category: %s %n%n",
                    Arrays.deepToString(classNames), Arrays.deepToString(releases), sourcesPath, module);
        }

        try (final Stream<Path> pathStream = getPaths().stream()) {
            System.out.println(
                    pathStream
                            .map(path -> JavaMethod.toString(getMethodsToDisplay(path), showOnlyClassNames))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.joining(showOnlyClassNames ? "\n" : "\n\n")));
        } catch (final IOException exception) {
            System.err.println("An error occurred: " + exception.getMessage());
        }

        return 0;
    }

    private Collection<Path> getPaths() throws IOException {
        try (final Stream<Path> pathStream = Files.walk(Paths.get(searchPath), 8)) {
            return pathStream
                    .filter(path -> isJavaFile(path) && matchesSearch(path))
                    .collect(Collectors.toList());
        }
    }

    private Collection<JavaMethod> getMethodsToDisplay(final Path path) {
        final Collection<JavaMethod> methods = getMethods(path);
        return (methods.size() == 1 && !showNotModifiedClasses) ? Collections.emptyList() : methods;
    }

    private Collection<JavaMethod> getMethods(final Path path) {
        try {
            try (final Stream<JavaMethod> methodStream = StreamSupport.stream(new JavaSinceIterator(Files.lines(path).spliterator()), false)) {
                return methodStream
                        .filter(method -> method.constructor || method.isNewInReleases(releases))
                        .collect(Collectors.toList());
            }
        } catch (final IOException exception) {
            return Collections.emptyList();
        }
    }

    private boolean isJavaFile(final Path path) {
        var stringPath = path.toString();
        return Files.isRegularFile(path) &&
                stringPath.endsWith(".java") &&
                !stringPath.endsWith("module-info.java") &&
                !stringPath.endsWith("package-info.java");
    }

    private boolean matchesSearch(final Path path) {
        try (final Stream<String> stringStream = Arrays.stream(classNames)) {
            return stringStream
                    .anyMatch(requestedClassName -> {
                        final String className = toClassName(path.toString());
                        return className.endsWith(requestedClassName) || className.matches(requestedClassName);
                    });
        }
    }

    private String toClassName(final String filepath) {
        return filepath
                .replace(searchPath + File.separator, "")
                .replace("/", ".")
                .replaceAll(".java$", "");
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
            String signature = line;
            // constructor declaration uses 2 lines
            if (constructor && signature.contains("public") && !signature.contains("class") && !signature.contains("interface")) {
                if (!lineSpliterator.tryAdvance(currentLine -> this.line = currentLine)) {
                    return false;
                }
                signature += " " + line;
            }

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

        public static String toString(final Collection<JavaMethod> methods) {
            return methods
                    .stream()
                    .map(JavaMethod::toStringIndented)
                    .collect(Collectors.joining("\n"))
                    + "\n}";
        }

        public static Optional<String> toString(final Collection<JavaMethod> methods, final boolean showOnlyClassNames) {
            if (methods.isEmpty()) {
                return Optional.empty();
            }
            if (showOnlyClassNames) {
                final JavaMethod constructor = methods.iterator().next();
                return Optional.of(constructor.toString());
            } else {
                return Optional.of(JavaMethod.toString(methods));
            }
        }

        private boolean isNewInReleases(final String[] releases) {
            return Arrays.asList(releases).contains(release);
        }

        @Override
        public String toString() {
            return signature + (constructor ? "" : ";") + " // since " + release;
        }

        public String toStringIndented() {
            return constructor ? (this + "\n{") : ("\t" + this);
        }
    }
}
