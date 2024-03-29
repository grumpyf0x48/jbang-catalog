///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS info.picocli:picocli:4.6.2

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
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

    @Option(names = {"--not-modified-classes", "-a"}, defaultValue = "false", description = "Show all classes even not modified ones (default: ${DEFAULT-VALUE})")
    boolean showNotModifiedClasses;

    @Option(names = {"--show-abstract-classes", "-b"}, defaultValue = "false", description = "Show abstract classes (default: ${DEFAULT-VALUE})")
    boolean showAbstractClasses;

    @Option(names = {"--only-class-names", "-c"}, defaultValue = "false", description = "Show only names of modified classes, not their methods (default: ${DEFAULT-VALUE})")
    boolean showOnlyClassNames;

    @Option(names = {"--deprecation", "-d"}, defaultValue = "false", description = "Show deprecated methods instead of added or updated ones (default: ${DEFAULT-VALUE})")
    boolean showDeprecatedMethods;

    @Option(names = {"--all-deprecations"}, defaultValue = "false", description = "Show deprecated methods without 'since' mention (regardless of '--release') (default: ${DEFAULT-VALUE})")
    boolean allDeprecations;

    @Option(names = {"--verbose", "-v"}, defaultValue = "false", description = "Activate verbose mode (default: ${DEFAULT-VALUE})")
    boolean verbose;

    @Parameters(arity = "1..n", index = "0..n", description = "Class names or regexps")
    String[] classNames;

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
            System.out.printf("Listing new methods for classes: %s since releases: %s parsing sources in: %s module: %s %n%n",
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
            final SearchType searchType = showDeprecatedMethods ? SearchType.DEPRECATED : SearchType.SINCE;
            try (final Stream<JavaMethod> methodStream = StreamSupport.stream(new JavaMethodIterator(Files.lines(/* since 1.8 */path).spliterator(), searchType), false)) {
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
            return stringStream.anyMatch(classNameSearch -> classMatchesSearch(toClassName(path), classNameSearch));
        }
    }

    private boolean classMatchesSearch(final String className, final String classNameSearch) {
        return className.endsWith(classNameSearch) || className.matches(classNameSearch);
    }

    private String toClassName(final Path path) {
        return path
                .toString()
                .replace(searchPath + File.separator, "")
                .replace("/", ".")
                .replaceAll(".java$", "");
    }

    private static boolean isClassDeclaration(final String signature) {
        return signature.contains("class") || signature.contains("interface") || signature.contains("enum");
    }

    private enum SearchType {
        SINCE,      // @Since 11
        DEPRECATED; // @Deprecated(since="9")

        public String getSearchedToken() {
            switch (this) {
                case SINCE:
                    return "@since";
                case DEPRECATED:
                    return "@Deprecated";
                default:
                    throw new IllegalArgumentException("Unexpected SearchType: " + this);
            }
        }
    }

    private class JavaMethodIterator implements Spliterator<JavaMethod> {

        private final Spliterator<String> lineSpliterator;
        private final SearchType searchType;
        private String line;
        private boolean declaration = true;

        public JavaMethodIterator(final Spliterator<String> lineSpliterator, final SearchType searchType) {
            this.lineSpliterator = lineSpliterator;
            this.searchType = searchType;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super JavaMethod> action) {
            /* Searching for @since or @Deprecated...
               When searching the class declaration, @since is always searched even if -d flag is used.
             */
            final SearchType effectiveSearchType = declaration ? SearchType.SINCE : searchType;
            if (innerAdvanceWhile(Predicate.not(/* since 11 */currentLine -> currentLine.contains(effectiveSearchType.getSearchedToken())))) {
                return false;
            }
            final var searchedLine = line;

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
            final var signatureBuilder = new StringBuilder(line);
            while (isDeclarationIncomplete(line)) {
                if (!lineSpliterator.tryAdvance(currentLine -> line = currentLine)) {
                    return false;
                }
                signatureBuilder
                    .append(" ")
                    .append(line.stripLeading());
            }

            action.accept(new JavaMethod(signatureBuilder.toString(), declaration, effectiveSearchType, searchedLine, allDeprecations ));
            if (declaration) {
                declaration = false;
            }
            return true;
        }

        private boolean innerAdvanceWhile(final Predicate<String> predicate) {
            boolean advanced;
            while ((advanced = lineSpliterator.tryAdvance(currentLine -> line = currentLine)) && predicate.test(line))
                ;
            return !advanced;
        }

        private boolean isDeclarationIncomplete(final String signature) {
            return declaration ? !isClassDeclaration(signature) : !signature.contains("{");
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
        private final boolean declaration;
        private final SearchType searchType;
        private final String searchedLine;
        private final String release;
        private final boolean allDeprecations;

        private JavaMethod(final String signature, final boolean declaration, final SearchType searchType, final String searchedLine, boolean allDeprecations) {
            this.signature = signature
                    .strip() // since 11
                    .replace("}", "")
                    .replace("{", isInnerDeclaration(signature, declaration) ? "{ ... }" : "")
                    .stripTrailing(); // since 11
            this.declaration = declaration;
            this.searchType = searchType;
            this.searchedLine = searchedLine.strip();
            this.release = JavaRelease.fromSearchedLine(searchedLine, searchType);
            this.allDeprecations = allDeprecations;
        }

        private JavaRelease getRelease() {
            return JavaRelease.from(release);
        }

        private boolean isNewInReleases(final JavaRelease[] releases) {
            return Arrays
                    .stream(releases)
                    .anyMatch(javaRelease -> (allDeprecations && release.equals(JavaRelease.JAVA_NOT_SET.toString())) || javaRelease.matches(release));
        }

        private static boolean isInnerDeclaration(final String signature, final boolean declaration) {
            return !declaration && isClassDeclaration(signature);
        }

        private static String toString(final Collection<JavaMethod> methods) {
            final var previousMethod = new JavaMethod[]{ null };
            return methods
                    .stream()
                    .sorted(Comparator.comparing(JavaMethod::getRelease))
                    .map(javaMethod -> {
                        try {
                            final var builder = new StringBuilder();
                            if (previousMethod[0] != null && !previousMethod[0].declaration && previousMethod[0].getRelease() != javaMethod.getRelease())
                            {
                                builder.append("\n");
                            }
                            builder.append(javaMethod.toStringIndented());
                            return builder.toString();
                        }
                        finally {
                            previousMethod[0] = javaMethod;
                        }
                    })
                    .collect(Collectors.joining("\n"))
                    + "\n}";
        }

        private static Optional<String> toString(final Collection<JavaMethod> methods, final boolean showOnlyClassNames) {
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

        @Override
        public String toString() {
            final var stringBuilder = new StringBuilder();
            stringBuilder.append(signature);
            if (!declaration) {
                stringBuilder.append(";");
            }
            if (!release.isBlank()) {
                stringBuilder.append(" // ");
                if (searchType == SearchType.SINCE) {
                    stringBuilder.append("since ")
                            .append(release);
                }
                else {
                    stringBuilder.append(searchedLine);
                }
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
        JAVA_ALL,
        JAVA_NOT_SET;

        public static JavaRelease from(final String release) {
            var effectiveRelease = release.contains(".") ? release.split("\\.")[1] : release;
            return JavaRelease.valueOf("JAVA_" + effectiveRelease);
        }

        public static String fromSearchedLine(final String searchedLine, final SearchType searchType) {
            final Optional<String> optionalRelease = searchType == SearchType.SINCE ? fromSinceLine(searchedLine) : fromDeprecatedLine(searchedLine);
            return optionalRelease.orElse(JavaRelease.JAVA_NOT_SET.toString());
        }

        public boolean matches(final String release) {
            return this == JAVA_ALL || release.equals(this.toString());
        }

        private static Optional<String> fromSinceLine(final String sinceLine) {
            final String[] strings = sinceLine
                    .strip()
                    .replace("*", "")
                    .stripLeading() // since 11
                    .split("\\s+");
            if (strings.length > 1) {
                return Optional.of(strings[1]);
            }
            return Optional.empty();
        }

        private static Optional<String> fromDeprecatedLine(final String deprecatedLine) {
            final var sincePattern = Pattern.compile(".*since\\s*=\\s*\"(.+)\".*");
            final var matcher = sincePattern.matcher(deprecatedLine);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
            return Optional.empty();
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
                case JAVA_ALL:
                case JAVA_NOT_SET:
                    return this.name().split("_")[1];
                default:
                    throw new IllegalArgumentException("Unknown Java release: " + this);
            }
        }
    }
}
