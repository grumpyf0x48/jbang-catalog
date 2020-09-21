import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "stop")
public class Stop implements Callable<Integer>
{
    @Parameters
    private String serviceName;

    @Option(names = { "-f", "--force" }, description = "Force stop.")
    private boolean force;

    private Stop()
    {
        System.out.println("Instantiating class: " + getClass().getName());
    }

    @Override
    public Integer call() throws Exception
    {
        System.out.println("Stopping: " + serviceName + " force: " + force);
        return null;
    }
}
