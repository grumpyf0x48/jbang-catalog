///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.3.1
//DEPS commons-io:commons-io:2.8.0
//DEPS org.jsoup:jsoup:1.10.2
//DEPS info.picocli:picocli:4.6.1

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Node;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.time.LocalDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Command(name = "Links", mixinStandardHelpOptions = true, version = "Links 0.1", description = "A program to retrieve the posts metadata of my former blog archived on https://web.archive.org")
class Links implements Callable<Integer> {

    @Option(names = {"-r", "--root-url"}, description = "The root URL to parse on https://web.archive.org (default: ${DEFAULT-VALUE})", defaultValue = "https://web.archive.org/web/20200211042437")
    private String rootUrl;

    @Option(names = {"-a", "--archived-site-url"}, description = "The URL of the archived web Site (default: ${DEFAULT-VALUE})", defaultValue = "http://blog.onkeyboardst.net")
    private String archivedSiteUrl;

    @Option(names = {"-f", "--first-page"}, description = "First page to parse (default: ${DEFAULT-VALUE})", defaultValue = "1")
    private int firstPage;

    @Option(names = {"-l", "--last-page"}, description = "Last page to parse (default: ${DEFAULT-VALUE})", defaultValue = "1")
    private int lastPage;

    @Parameters(description = "The output file")
    private File file;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose mode")
    private boolean verbose;

    public static void main(final String... args) {
        int exitCode = new CommandLine(new Links()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException {
        final TreeSet<PostInfo> postInfoSet = new TreeSet<>(Comparator.reverseOrder());
        final String url = buildPostUrl(archivedSiteUrl);
        for (int page = firstPage; page <= lastPage; page++) {
            final String pageUrl = page == 1 ? url : String.format("%s/page/%d", url, page);
            addPosts(pageUrl, postInfoSet);
        }
        if (verbose) {
            System.out.printf("Found %d posts in %d-%d pages %n", postInfoSet.size(), firstPage, lastPage);
        }
        FileUtils.writeStringToFile(file, postInfoSet.toString(), StandardCharsets.UTF_8);
        return 0;
    }

    private String buildPostUrl(final String archivedUrl) {
        return String.format("%s/%s", rootUrl, archivedUrl);
    }

    private void addPosts(final String pageUrl, final Set<PostInfo> postInfoSet) {
        if (verbose) {
            System.out.printf("Parsing %s%n", pageUrl);
        }
        getAttributes(pageUrl, "link")
                .forEach(postAttributes -> {
                    final String archivedUrl = postAttributes.get("href");
                    if (archivedUrl.contains("post")) {
                        final String url = buildPostUrl(archivedUrl);
                        final String title = postAttributes.get("title");
                        final String stringDate = getAttributes(url, "meta")
                                .filter(attribute -> attribute.get("name").equals("date"))
                                .findFirst()
                                .map(attribute -> attribute.get("content"))
                                .orElse(null);
                        if (stringDate != null) {
                            if (verbose) {
                                System.out.printf("Adding Post %s%n", url);
                            }
                            postInfoSet.add(new PostInfo(url, title, stringDate));
                        }
                    }
                });
    }

    private static Stream<Attributes> getAttributes(final String url, final String cssQuery) {
        try {
            return Jsoup.connect(url)
                    .get()
                    .select(cssQuery)
                    .stream()
                    .map(Node::attributes);
        } catch (final Exception exception) {
            System.err.printf("Failed to retrieve page: %s%n", url);
            return Stream.empty();
        }
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

        private static LocalDateTime getLocalDateTime(final String stringDate) {
            return parse(stringDate.substring(0, 19), ISO_LOCAL_DATE_TIME);
        }
    }
}
