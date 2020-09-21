//DEPS info.picocli:picocli:4.5.0
//SOURCES Start.java Stop.java
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "service", subcommands = { Start.class, Stop.class })
public class Service implements Callable<Integer>
{
    @Override
    public Integer call() throws Exception
    {
        return null;
    }

    public static void main(String... args)
    {
        CommandLine commandLine = new CommandLine(new Service());
        commandLine.execute(args);
    }
}
