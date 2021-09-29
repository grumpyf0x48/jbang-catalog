///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES *.java
//FILES jbang-catalog.json
//FILES templates/junit4.java.qute
//FILES templates/junit5.java.qute
//FILES templates/testng.java.qute
//FILES .github/workflows/ci-build.yml
//FILES README.md

import java.io.IOException;

public class Catalog {
    public static void main(final String[] args) throws IOException {
        Runtime.getRuntime().exec("ls");
        System.exit(0);
    }
}
