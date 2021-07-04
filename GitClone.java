///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r
//DEPS org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.12.0.202106070339-r
//DEPS info.picocli:picocli:4.6.1
//SOURCES AbstractGit.java

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "GitClone", mixinStandardHelpOptions = true, version = "GitClone 0.1", description = "A basic `git clone` replacement in Java")
class GitClone extends AbstractGit {

    @Parameters(arity = "0..1", index = "1", description = "The name of a new directory to clone into")
    File directory;

    @Option(names = {"--bare"}, description = "Make a bare Git repository")
    boolean bare;

    @Option(names = {"-n", "--no-checkout"}, description = "No checkout of HEAD is performed after the clone is complete")
    boolean noCheckout;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new GitClone()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public File getCloneDirectory() throws Exception {
        return directory;
    }

    public boolean isBare() {
        return bare;
    }

    public boolean isNoCheckout() {
        return noCheckout;
    }
}
