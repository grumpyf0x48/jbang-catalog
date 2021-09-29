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

public class Catalog {
    public static void main(final String[] args) throws IOException {
        String userDirectory = Paths.get("")
                .toAbsolutePath()
                .toString();
        System.out.println("userDirectory=" + userDirectory);
        System.exit(0);
    }
}
