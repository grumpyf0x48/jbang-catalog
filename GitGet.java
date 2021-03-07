///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.10.0.202012080955-r
//DEPS info.picocli:picocli:4.5.0
//DEPS commons-io:commons-io:2.8.0
//SOURCES AbstractGit.java

import org.apache.commons.io.FileUtils;

import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Command(name = "GitGet", mixinStandardHelpOptions = true, version = "GitGet 0.1", description = "GitGet made with jbang")
class GitGet extends AbstractGit {

    @Parameters(arity = "1", index = "1", description = "The name of a new directory where to store files")
    File directory;

    @Parameters(arity = "1", index = "2..n", description = "The file or directory paths to get from the repository")
    String[] paths;

    @Option(names = {"--fresh"}, description = "Make a fresh clone of the repository")
    boolean fresh;

    File cloneDirectory;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new GitGet()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        final Path clonePath = getCloneDirectory().toPath();
        if (fresh) {
            Files.deleteIfExists(clonePath);
        }
        Files.createDirectories(clonePath);
        if (!RepositoryCache.FileKey.isGitRepository(new File(getCloneDirectory(), ".git"), FS.DETECTED)) {
            super.call();
        }
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
            } catch (final Exception exception) {
                System.err.printf("Failed to copy %s to %s\n", path, directory);
            }
        });
        return 0;
    }

    @Override
    public File getCloneDirectory() throws Exception {
        if (cloneDirectory == null) {
            cloneDirectory = new File(System.getProperty("java.io.tmpdir"), new URIish(repository).getPath());
        }
        return cloneDirectory;
    }
}
