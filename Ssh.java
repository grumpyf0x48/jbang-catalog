///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.apache.commons:commons-lang3:3.11
//DEPS org.apache.sshd:sshd-core:2.6.0
//DEPS org.apache.logging.log4j:log4j-api:2.7
//DEPS org.apache.logging.log4j:log4j-core:2.7
//DEPS org.apache.logging.log4j:log4j-slf4j-impl:2.7
//DEPS info.picocli:picocli:4.5.0

import org.apache.commons.lang3.StringUtils;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "Ssh", mixinStandardHelpOptions = true, version = "Ssh 0.1", description = "Ssh client written with Apache Mina, Jbang and Picocli")
class Ssh implements Callable<Integer> {

    @Option(names = {"-i", "--identity"}, description = "Identity file")
    private File identityFile;

    @Option(names = {"--port"}, defaultValue = "22", description = "Port (default: ${DEFAULT-VALUE})")
    private int port;

    @Option(names = {"-a", "--authentication-timeout"}, defaultValue = "30", description = "Authentication timeout in seconds (default: ${DEFAULT-VALUE})")
    private int authenticationTimeout;

    @Option(names = {"-c", "--connection-timeout"}, defaultValue = "30", description = "Connection timeout in seconds (default: ${DEFAULT-VALUE})")
    private int connectionTimeout;

    @Option(names = {"-p"}, description = "Password")
    private String password;

    @Parameters(description = "Destination to reach (format: user@hostname)")
    private String destination;

    @Parameters(arity = "0..*", index = "1..*", description = "Command to execute and its parameters")
    private final List<String> command = new ArrayList<>();

    private String user, hostname;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new Ssh()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException {
        final Integer status = parse();
        if (status != 0) {
            return status;
        }
        return execute();
    }

    private Integer parse() {
        final String[] strings = destination.split("@");
        if (strings.length < 2) {
            System.err.println("Invalid format for destination !");
            return 1;
        }

        user = strings[0];
        hostname = strings[1];

        if (identityFile == null) {
            if (password == null) {
                final Console console = System.console();
                final char[] passwordArray = console.readPassword("Enter the password to connect to %s: ", destination);
                password = new String(passwordArray);
            }
        } else if (!identityFile.exists()) {
            System.err.println("IdentityFile does not exist !");
            return 1;
        }

        return 0;
    }

    private Integer execute() throws IOException {
        try (final SshClient sshClient = SshClient.setUpDefaultClient()) {

            sshClient.start();

            try (final ClientSession clientSession =
                         sshClient.connect(user, hostname, port)
                                 .verify(Duration.ofSeconds(connectionTimeout))
                                 .getSession()) {

                if (identityFile == null) {
                    clientSession.addPasswordIdentity(password);
                    sshClient.setKeyIdentityProvider(null);
                } else {
                    final FileKeyPairProvider keyPairProvider = new FileKeyPairProvider();
                    keyPairProvider.setPaths(Collections.singleton(Paths.get(identityFile.getAbsolutePath())));
                    sshClient.setKeyIdentityProvider(keyPairProvider);
                    final KeyPair keyPair = keyPairProvider.loadKeys(clientSession).iterator().next();
                    sshClient.addPublicKeyIdentity(keyPair);
                }

                final AuthFuture authentication = clientSession.auth();
                final boolean awaited = authentication.await(Duration.ofSeconds(authenticationTimeout));
                if (!awaited) {
                    System.err.println("Failed to get authentication result in time !");
                    return 1;
                }
                if (!authentication.isSuccess()) {
                    System.err.printf("Authenticating to '%s' with user '%s' failed: %s !%n", hostname, user, authentication.getException().getMessage());
                    return 1;
                }

                if (!command.isEmpty()) {
                    executeCommand(clientSession, command);
                } else {
                    final Console console = System.console();
                    String userCommand;
                    while ((userCommand = getCommand(console, clientSession)) != null) {
                        executeCommand(clientSession, Collections.singletonList(userCommand));
                    }
                }
            }
        }

        return 0;
    }

    private static void executeCommand(final ClientSession clientSession, final List<String> command) throws IOException {
        final String remoteCommand = buildRemoteCommand(command);
        final String commandOutput = clientSession.executeRemoteCommand(remoteCommand);
        System.out.println(commandOutput);
    }

    private static String buildRemoteCommand(final List<String> command) {
        return command.stream().map(s -> s + ' ').collect(Collectors.joining()).stripTrailing();
    }

    private String getCommand(final Console console, final ClientSession clientSession) throws IOException {
        return StringUtils.trimToNull(console.readLine(getPrompt(clientSession)));
    }

    private String getPrompt(final ClientSession clientSession) throws IOException {
        final String directory = clientSession.executeRemoteCommand("pwd").trim().replaceAll("\n ", "");
        return String.format("%s@%s:%s$ ", user, hostname, directory);
    }
}
