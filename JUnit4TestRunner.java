///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS junit:junit-dep:4.11
//SOURCES *.java

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.List;

import static java.lang.Class.forName;
import static org.junit.runner.Request.aClass;
import static org.junit.runner.Request.method;

public class JUnit4TestRunner {

    // Usage: ./JUnit4TestRunner <scriptOrFile>
    public static void main(final String... testNames) throws ClassNotFoundException {
        for (final String testName : testNames) {
            final Result result = runTest(testName);
            if (!result.wasSuccessful()) {
                display(result.getFailures());
                System.exit(1);
            }
        }
        System.exit(0);
    }

    public static Result runTest(final String testName) throws ClassNotFoundException {
        final String[] classAndMethod = testName.split("#");
        final Request request = classAndMethod.length == 1 ? aClass(forName(classAndMethod[0])) : method(forName(classAndMethod[0]), classAndMethod[1]);
        return new JUnitCore().run(request);
    }

    public static Result runTest(final Class<?> clazz) throws ClassNotFoundException {
        return runTest(clazz.getSimpleName());
    }

    private static void display(final List<Failure> failures) {
        failures.forEach(failure -> failure.getException().printStackTrace(System.err));
    }
}
