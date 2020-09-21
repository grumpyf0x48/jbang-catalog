import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "start")
public class Start implements Callable<Integer>
{
    @Parameters
    private String serviceName;

    private Start()
    {
        System.out.println("Instantiating class: " + getClass().getName());
    }

    @Override
    public Integer call() throws Exception
    {
        System.out.println("Starting: " + serviceName);
        return null;
    }
}
