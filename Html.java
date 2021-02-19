///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.3.1
//DEPS commons-io:commons-io:2.8.0
//DEPS info.picocli:picocli:4.5.0

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;

import static java.time.LocalDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Command(name = "Html", mixinStandardHelpOptions = true, version = "Html 0.1", description = "Html made with Gson, Jbang and Picocli")
class Html implements Callable<Integer> {

    @Option(names = {"-i"}, required = true, description = "The input file (.json)")
    private File inputFile;

    @Option(names = {"-o"}, required = true, description = "The output file (.html)")
    private String outputFile;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new Html()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException {
        final Gson gson = new Gson();

        final Reader reader = Files.newBufferedReader(Paths.get(inputFile.getAbsolutePath()));

        final PostInfo[] posts = gson.fromJson(reader, PostInfo[].class);

        final Set<Integer> yearDateHeaders = new HashSet<>();

        final StringBuilder builder = new StringBuilder();

        builder.append("<h1>Archives de mon ancien blog</h1>")
                .append('\n');

        for (final PostInfo post : posts) {
            final Integer yearDate = post.getLocalDateTime().getYear();

            if (!yearDateHeaders.contains(yearDate)) {
                builder.append('\n')
                        .append(String.format("<h2>Année %d</h2>", yearDate))
                        .append('\n')
                        .append('\n');
                yearDateHeaders.add(yearDate);
            }

            builder.append(String.format("<p><a href=\"%s\" target=\"_blank\" title=\"%s\">%s</a></p>", post.url, post.title, post.title))
                    .append('\n');
        }

        FileUtils.writeStringToFile(new File(outputFile), builder.toString());

        return 0;
    }

    private static class PostInfo implements Comparable<PostInfo> {
        final String url;
        final String title;
        final String date;

        public PostInfo(final String url, final String title, final String date) {
            this.url = url;
            this.title = title;
            this.date = date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PostInfo postInfo = (PostInfo) o;
            return Objects.equals(url, postInfo.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url);
        }

        @Override
        public int compareTo(final PostInfo postInfo) {
            return getLocalDateTime(date).compareTo(getLocalDateTime(postInfo.date));
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

        public LocalDateTime getLocalDateTime() {
            return getLocalDateTime(date);
        }

        private static LocalDateTime getLocalDateTime(final String stringDate) {
            return parse(stringDate.substring(0, 19), ISO_LOCAL_DATE_TIME);
        }
    }
}
