///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//SOURCES ../Vote.java
//DEPS org.junit.jupiter:junit-jupiter-api:5.8.2
//DEPS org.junit.jupiter:junit-jupiter-engine:5.8.2
//DEPS org.junit.platform:junit-platform-launcher:1.8.2

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import static java.lang.System.exit;
import static java.lang.System.out;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class VoteTest {

    private static Vote vote;
    private OutputStream consoleOutput;
    private PrintStream console;

    @BeforeAll
    static void setUpAll() {
        vote = new Vote();
    }

    @BeforeEach
    void setUp() {
        consoleOutput = new ByteArrayOutputStream();
        console = new PrintStream(consoleOutput);
    }

    @AfterEach
    void tearDown() {
        out.print(consoleOutput.toString());
    }

    @Test
    public void acceptance3OptionsTest() {
        final String votes = """
                Hugues 17:23:45
                H T B

                Thomas 17:23:45
                T H B

                Baptiste 17:23:48
                B T H
                """;

        final Map<Character, Integer> pointsPerOption = vote.getPointsPerOption(votes.lines(), "HTB", console);

        assertEquals(of('T', 7, 'H', 6, 'B', 5), pointsPerOption, "Incorrect points");
        final String results = """
                H=3 T=2 B=1
                T=3 H=2 B=1
                B=3 T=2 H=1
                                        
                T=7 H=6 B=5
                """;
        assertEquals(results, consoleOutput.toString(), "Incorrect output");
    }

    @Test
    public void acceptance4OptionsTest() {
        final String votes = """
                Hugues 17:23:45
                H N T B
                                
                Thomas 17:23:45
                T N H B
                                
                Nicolas 17:23:47
                N B T H
                                
                Baptiste 17:23:48
                B T N H
                """;

        final Map<Character, Integer> pointsPerOption = vote.getPointsPerOption(votes.lines(), "HNTB", console);

        assertEquals(of('N', 12, 'T', 11, 'B', 9, 'H', 8), pointsPerOption, "Incorrect points");
        final String results = """
                H=4 N=3 T=2 B=1
                T=4 N=3 H=2 B=1
                N=4 B=3 T=2 H=1
                B=4 T=3 N=2 H=1
                                        
                N=12 T=11 B=9 H=8
                """;
        assertEquals(results, consoleOutput.toString(), "Incorrect output");
    }

    @Test
    public void tooManyOptionsInVoteTest() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            final String votes = """
                    H N T B C
                    T N H B
                    N B T H
                    B T N H
                        """;
            vote.getPointsPerOption(votes.lines(), "HNTB", null);
        }, "Exception was not raised");
        assertEquals("Too many options in: HNTBC", exception.getMessage(), "Exception raised is not the expected one");
    }

    @Test
    public void duplicateOptionInVoteTest() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            final String votes = """
                    H N T B
                    T N B B
                    N B T H
                    """;
            vote.getPointsPerOption(votes.lines(), "HNTB", null);
        }, "Exception was not raised");
        assertEquals("Duplicate option in: TNBB", exception.getMessage(), "Exception raised is not the expected one");
    }

    @Test
    public void invalidOptionInVoteTest() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            final String votes = """
                    H N T B
                    T N H C
                    N B T H
                    """;
            vote.getPointsPerOption(votes.lines(), "HNTB", null);
        }, "Exception was not raised");
        assertEquals("Invalid option in: TNHC", exception.getMessage(), "Exception raised is not the expected one");
    }

    public static void main(final String... args) {
        final LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(selectClass(VoteTest.class))
                        .build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener generatingListener = new SummaryGeneratingListener();
        launcher.execute(request, generatingListener);
        out.println(summary(generatingListener.getSummary()));
        final int exitCode = generatingListener.getSummary().getTestsFailedCount() > 0 ? 1 : 0;
        exit(exitCode);
    }

    private static String summary(final TestExecutionSummary summary) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Results:")
                .append('\n')
                .append('\n');
        if (summary.getTestsFailedCount() > 0) {
            builder.append("Failed tests:")
                    .append('\n');
            for (final Failure failure : summary.getFailures()) {
                builder.append('\t')
                        .append(failure.getTestIdentifier())
                        .append(' ')
                        .append(failure.getException().getMessage());
            }
            builder.append('\n')
                    .append('\n');
        }

        final String message = String.format(
                "Tests run: %d, Failure: %d, Skipped: %d",
                summary.getTestsStartedCount(),
                summary.getTestsFailedCount(),
                summary.getTestsSkippedCount());
        builder.append(message);
        return builder.toString();
    }
}
