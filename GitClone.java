///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS info.picocli:picocli:4.5.0

import org.eclipse.jgit.api.Git;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "GitClone", mixinStandardHelpOptions = true, version = "GitClone 0.1", description = "GitClone made with jbang")
class GitClone implements Callable<Integer> {

    @Option(names = {"-b", "--branch"}, description = "Branch name")
    private String branch;

    @Option(names={"--bare"}, description = "Make a bare Git repository")
    private boolean bare;

    @Option(names={"-n", "--no-checkout"}, description = "No checkout of HEAD is performed after the clone is complete")
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

        Git.cloneRepository()
                .setURI(repository)
                .setDirectory(directory)
                .setBranch(branch)
                .setBare(bare)
                .setNoCheckout(noCheckout)
                .call();

        return 0;
    }
}
