///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES *.java
//FILES jbang-catalog.json
//FILES templates/junit4.java.qute
//FILES templates/junit5.java.qute
//FILES templates/testng.java.qute
//FILES .github/workflows/ci-build.yml
//FILES README.md

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class Catalog {
    public static void main(final String[] args) throws IOException {
        Arrays.stream(Paths.get(".").toFile().list()).forEach(System.out::println);
        System.exit(0);
    }
}
