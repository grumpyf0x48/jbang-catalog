///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.10.0.202012080955-r
//DEPS info.picocli:picocli:4.5.0
//SOURCES AbstractGit.java

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "GitClone", mixinStandardHelpOptions = true, version = "GitClone 0.1", description = "GitClone made with jbang")
class GitClone extends AbstractGit {

    @Parameters(arity = "0..1", index = "1", description = "The name of a new directory to clone into")
    File directory;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new GitClone()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public File getCloneDirectory() throws Exception {
        return directory;
    }
}
