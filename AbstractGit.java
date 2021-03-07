import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

abstract class AbstractGit implements Callable<Integer> {

    private static final String SSH_ID_RSA_DEFAULT = ".ssh/id_rsa";

    @Option(names = {"-i", "--identity"}, description = "Identity file in PEM format (default: ~/" + SSH_ID_RSA_DEFAULT + ")")
    File identityFile = new File(System.getProperty("user.home"), SSH_ID_RSA_DEFAULT);

    @Option(names = {"-b", "--branch"}, description = "Branch name")
    String branch;

    @Parameters(description = "The repository to clone from")
    String repository;

    @Option(names = {"--bare"}, description = "Make a bare Git repository")
    boolean bare;

    @Option(names = {"-n", "--no-checkout"}, description = "No checkout of HEAD is performed after the clone is complete")
    boolean noCheckout;

    @Override
    public Integer call() throws Exception {
        final CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repository)
                .setDirectory(getCloneCommandDirectory())
                .setBranch(branch)
                .setBare(bare)
                .setNoCheckout(noCheckout);
        if (!repository.startsWith("https://")) {
            cloneCommand.setTransportConfigCallback(new SshTransportConfigCallback());
        }
        cloneCommand.call();
        return 0;
    }

    public abstract File getCloneDirectory() throws Exception;

    private File getCloneCommandDirectory() throws Exception {
        File cloneDirectory = getCloneDirectory();
        if (cloneDirectory == null) {
            cloneDirectory = new File(".");
        } else if (!cloneDirectory.exists() && !cloneDirectory.mkdirs()) {
            throw new IOException("Failed to create directory: " + cloneDirectory);
        }
        return cloneDirectory;
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
            protected void configure(final OpenSshConfig.Host hc, final Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
        };

        @Override
        public void configure(final Transport transport) {
            ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
        }
    }
}
