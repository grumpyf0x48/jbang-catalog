///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.10.0.202012080955-r
//DEPS info.picocli:picocli:4.5.0

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "GitClone", mixinStandardHelpOptions = true, version = "GitClone 0.1", description = "GitClone made with jbang")
class GitClone implements Callable<Integer> {

    private static final String SSH_ID_RSA_DEFAULT = ".ssh/id_rsa";

    @Option(names = {"-i", "--identity"}, description = "Identity file in PEM format (default: ~/" + SSH_ID_RSA_DEFAULT + ")")
    private File identityFile = new File(System.getProperty("user.home"), SSH_ID_RSA_DEFAULT);

    @Option(names = {"-b", "--branch"}, description = "Branch name")
    private String branch;

    @Option(names = {"--bare"}, description = "Make a bare Git repository")
    private boolean bare;

    @Option(names = {"-n", "--no-checkout"}, description = "No checkout of HEAD is performed after the clone is complete")
    private boolean noCheckout;

    @Parameters(description = "The repository to clone from")
    private String repository;

    @Parameters(arity = "0..1", index = "1", description = "The name of a new directory to clone into")
    private File directory;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new GitClone()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (directory == null) {
            directory = new File(".");
        } else {
            directory.mkdirs();
        }

        final CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repository)
                .setDirectory(directory)
                .setBranch(branch)
                .setBare(bare)
                .setNoCheckout(noCheckout);

        if (!repository.startsWith("https://")) {
            cloneCommand.setTransportConfigCallback(new SshTransportConfigCallback());
        }

        cloneCommand.call();

        return 0;
    }

    private class SshTransportConfigCallback implements TransportConfigCallback {
        private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

            @Override
            protected JSch createDefaultJSch(final FS fs) throws JSchException {
                final JSch jSch = super.createDefaultJSch(fs);
                jSch.addIdentity(identityFile.getAbsolutePath());
                return jSch;
            }

            @Override
            protected void configure(final Host hc, final Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
        };

        @Override
        public void configure(final Transport transport) {
            ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
        }
    }
}
