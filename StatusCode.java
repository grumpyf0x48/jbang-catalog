///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS info.picocli:picocli:4.6.1

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;

@Command(name = "StatusCode", mixinStandardHelpOptions = true, version = "StatusCode 0.1", description = "StatusCode made with JBang and Java 11 HttpClient")
class StatusCode implements Callable<Integer> {

    @Parameters(arity = "1", index = "0", description = "source URL")
    URL url;

    public static void main(final String... args) {
        new CommandLine(new StatusCode()).execute(args);
        System.exit(0);
    }

    @Override
    public Integer call() throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        final HttpRequest request = HttpRequest.newBuilder(url.toURI())
                .GET()
                .build();
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        final int statusCode = response.statusCode();
        System.out.println(statusCode);
        return statusCode;
    }
}
