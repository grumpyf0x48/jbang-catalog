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

    @Option(names = {"--release", "-r"}, paramLabel = "release", description = "JDK release (1.8, 9, 10, 11 ... or ALL) (default: 9, 10, 11)")
    JavaRelease[] releases = new JavaRelease[] {JavaRelease.JAVA_9, JavaRelease.JAVA_10, JavaRelease.JAVA_11};

    @Option(names = {"--source-path", "-s"}, defaultValue = "/usr/lib/jvm/openjdk-11", description = "JDK sources path (default: ${DEFAULT-VALUE})")
    File sourcesPath;

    @Option(names = {"--module", "-m"}, defaultValue = "java.base", description = "Module (java.base, java.desktop, java.logging ...) where to search classes (default: ${DEFAULT-VALUE})")
    String module;

    @Parameters(arity = "1..n", index = "0..n", description = "Class names or regexps")
    String[] classNames;

    @Option(names = {"--not-modified-classes", "-a"}, defaultValue = "false", description = "Show all classes even not modified ones (default: ${DEFAULT-VALUE})")
    boolean showNotModifiedClasses;

    @Option(names = {"--only-class-names", "-c"}, defaultValue = "false", description = "Show only names of modified classes, not their methods (default: ${DEFAULT-VALUE})")
    boolean showOnlyClassNames;

    @Option(names = {"--show-abstract-classes", "-b"}, defaultValue = "false", description = "Show abstract classes (default: ${DEFAULT-VALUE})")
    boolean showAbstractClasses;

    @Option(names = {"--verbose", "-v"}, defaultValue = "false", description = "Activate verbose mode (default: ${DEFAULT-VALUE})")
    boolean verbose;

    String searchPath;

    public static void main(final String... args) {
        final int exitCode = new CommandLine(new WhatsNewInJava()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // since 10
        final var searchFile = new File(sourcesPath, module);
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
        try (final Stream<Path> pathStream = Files.walk(/* since 1.8 */Paths.get(searchPath), 8)) {
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
            try (final Stream<JavaMethod> methodStream = StreamSupport.stream(new JavaMethodIterator(Files.lines(/* since 1.8 */path).spliterator()), false)) {
                return methodStream
                        .filter(method -> method.declaration || method.isNewInReleases(releases))
                        .collect(Collectors.toList());
            }
        } catch (final IOException exception) {
            return Collections.emptyList();
        }
    }

    private boolean isJavaFile(final Path path) {
        // since 10
        final var stringPath = path.toString();
        return Files.isRegularFile(path) &&
                stringPath.endsWith(".java") &&
                !stringPath.endsWith("-info.java");
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

    private static boolean isClassDeclaration(final String signature) {
        return signature.contains("class") || signature.contains("interface") || signature.contains("enum");
    }

    private class JavaMethodIterator implements Spliterator<JavaMethod> {

        private final Spliterator<String> lineSpliterator;
        private String line;
        private boolean declaration = true;

        public JavaMethodIterator(final Spliterator<String> lineSpliterator) {
            this.lineSpliterator = lineSpliterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super JavaMethod> action) {
            /* Searching for @since ... */
            if (innerAdvanceWhile(Predicate.not(/* since 11 */currentLine -> currentLine.contains("@since")))) {
                return false;
            }
            final var since = line;

            /* Searching for class declaration, method declaration, or inner (class, interface, annotation) declaration */
            if (innerAdvanceWhile(currentLine -> currentLine.contains("*") || currentLine.contains("@"))) {
                return false;
            }
            if (line.isEmpty() && innerAdvanceWhile(String::isEmpty)) {
                return false;
            }
            if (!showAbstractClasses && line.contains("abstract")) {
                return false;
            }
            var signature = line;
            // signature uses several lines
            while (!isDeclarationComplete(signature)) {
                if (!lineSpliterator.tryAdvance(currentLine -> this.line = currentLine)) {
                    return false;
                }
                signature += " " + line.stripLeading();
            }

            action.accept(new JavaMethod(signature, since, declaration));
            if (declaration) {
                declaration = false;
            }
            return true;
        }

        private boolean innerAdvanceWhile(final Predicate<String> predicate) {
            boolean advanced;
            while ((advanced = lineSpliterator.tryAdvance(currentLine -> this.line = currentLine)) && predicate.test(line))
                ;
            return !advanced;
        }

        private boolean isDeclarationComplete(final String signature) {
            return declaration ? isClassDeclaration(signature) : signature.contains("{");
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

        private final boolean declaration;
        private final String signature;
        private String release = "";

        private JavaMethod(final String signature, final String since, final boolean declaration) {
            final boolean innerDeclaration = !declaration && isClassDeclaration(signature);
            this.declaration = declaration;
            this.signature = signature
                    .strip() // since 11
                    .replace("{", innerDeclaration ? "{ ... }" : "")
                    .stripTrailing(); // since 11
            final String[] strings = since
                    .strip()
                    .replace("*", "")
                    .stripLeading() // since 11
                    .split(" ");
            if (strings.length > 1) {
                release = strings[1];
            }
        }

        private JavaRelease getRelease() {
            return JavaRelease.from(release);
        }

        public static String toString(final Collection<JavaMethod> methods) {
            return methods
                    .stream()
                    .sorted(Comparator.comparing(JavaMethod::getRelease))
                    .map(JavaMethod::toStringIndented)
                    .collect(Collectors.joining("\n"))
                    + "\n}";
        }

        public static Optional<String> toString(final Collection<JavaMethod> methods, final boolean showOnlyClassNames) {
            if (methods.isEmpty()) {
                return Optional.empty();
            }
            if (showOnlyClassNames) {
                final JavaMethod declaration = methods.iterator().next();
                return Optional.of(declaration.toString());
            } else {
                return Optional.of(JavaMethod.toString(methods));
            }
        }

        private boolean isNewInReleases(final JavaRelease[] releases) {
            return Arrays
                    .stream(releases)
                    .anyMatch(javaRelease -> javaRelease.matches(release));
        }

        @Override
        public String toString() {
            final var stringBuilder = new StringBuilder();
            stringBuilder.append(signature);
            if (!declaration) {
                stringBuilder.append(";");
            }
            if (!release.isBlank()) {
                stringBuilder.append(" // since ").append(release);
            }
            return stringBuilder.toString();
        }

        public String toStringIndented() {
            return declaration ? (this + "\n{") : ("\t" + this);
        }
    }

    private enum JavaRelease {
        JAVA_0,
        JAVA_1,
        JAVA_2,
        JAVA_3,
        JAVA_4,
        JAVA_5,
        JAVA_6,
        JAVA_7,
        JAVA_8,
        JAVA_9,
        JAVA_10,
        JAVA_11,
        JAVA_12,
        JAVA_13,
        JAVA_14,
        JAVA_15,
        JAVA_16,
        JAVA_17,
        JAVA_ALL;

        public static JavaRelease from(final String release) {
            var effectiveRelease = release.contains(".") ? release.split("\\.")[1] : release;
            return JavaRelease.valueOf("JAVA_" + effectiveRelease);
        }

        public boolean matches(final String release) {
            return this == JAVA_ALL || release.equals(this.toString());
        }

        @Override
        public String toString() {
            switch (this) {
                case JAVA_0:
                    return "1.0";
                case JAVA_1:
                case JAVA_2:
                case JAVA_3:
                case JAVA_4:
                case JAVA_5:
                case JAVA_6:
                case JAVA_7:
                case JAVA_8:
                    return "1." + this.name().split("_")[1];
                case JAVA_9:
                case JAVA_10:
                case JAVA_11:
                case JAVA_12:
                case JAVA_13:
                case JAVA_14:
                case JAVA_15:
                case JAVA_16:
                case JAVA_17:
                    return this.name().split("_")[1];
                case JAVA_ALL:
                    return "ALL";
                default:
                    throw new IllegalArgumentException("Unknown Java release: " + this);
            }
        }
    }
}
