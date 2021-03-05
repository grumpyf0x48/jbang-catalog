///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.10.0.202012080955-r
//DEPS info.picocli:picocli:4.5.0
//DEPS commons-io:commons-io:2.8.0
//SOURCES AbstractGit.java

import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

@Command(name = "GitGet", mixinStandardHelpOptions = true, version = "GitGet 0.1", description = "GitGet made with jbang")
class GitGet extends AbstractGit {

    @Parameters(arity = "1", index = "1", description = "The name of a new directory to clone into")
    File directory;

    @Parameters(arity = "1", index = "2..n", description = "The file or directory paths to get from the repository")
    String[] paths;

    File cloneDirectory;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new GitGet()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        super.call();
        Arrays.stream(paths).forEach(path -> {
            final File file = new File(path);
            try {
                final File destFile = new File(directory, file.getPath());
                if (!destFile.getParentFile().exists() && !destFile.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create directory: " + destFile.getParentFile());
                }
                final File srcFile = new File(getCloneDirectory(), file.getPath());
                if (path.endsWith("/")) {
                    FileUtils.copyDirectory(srcFile, destFile);
                } else {
                    FileUtils.copyFile(srcFile, destFile);
                }
            } catch (final IOException ioException) {
                System.err.printf("Failed to copy %s to %s\n", path, directory);
            }
        });
        FileUtils.deleteDirectory(getCloneDirectory());
        return 0;
    }

    @Override
    public File getCloneDirectory() throws IOException {
        if (cloneDirectory == null) {
            cloneDirectory = Files.createTempDirectory("GitGet").toFile();
        }
        return cloneDirectory;
    }
}
