///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS info.picocli:picocli:4.5.0
//DEPS org.openjdk.jol:jol-core:0.10

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "SizeOf", mixinStandardHelpOptions = true, version = "SizeOf 0.1", description = "SizeOf made with jbang and OpenJDK jol")
class SizeOf implements Callable<Integer> {

    private static final String[] TYPES = {"byte", "boolean", "char", "short", "int", "float", "long", "double"};

    @Parameters(index = "0..*", arity = "0..*", description = "The primitive types (default: byte, boolean, char, short, int, float, long, double")
    private final String[] types = TYPES; // Default value declared here instead with @Parameters because it is an Array

    private static final Map<String, Object> JAVA_TYPES = new HashMap<>();

    static {
        JAVA_TYPES.put("byte", new Byte((byte) 0));
        JAVA_TYPES.put("boolean", Boolean.FALSE);
        JAVA_TYPES.put("char", new Character('\u0000'));
        JAVA_TYPES.put("short", new Short((short) 0));
        JAVA_TYPES.put("int", new Integer(0));
        JAVA_TYPES.put("float", new Float(0.0f));
        JAVA_TYPES.put("long", new Long(0L));
        JAVA_TYPES.put("double", new Double(0.0d));
    }

    public static void main(final String... args) {
        int exitCode = new CommandLine(new SizeOf()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        final VirtualMachine virtualMachine = VM.current();

        final boolean defaultTypes = types.equals(TYPES);

        for (int index = 0; index < types.length; index++) {
            final String type = types[index];

            if (!JAVA_TYPES.containsKey(type)) {
                throw new IllegalArgumentException("Unknown primitive type: " + type);
            }

            if (defaultTypes && index > 0 && index % 2 == 0) {
                System.out.println();
            }

            final Object object = JAVA_TYPES.get(type);
            System.out.printf("sizeof(%s)=%d, sizeof(%s)=%d\n",
                    type, virtualMachine.sizeOfField(type),
                    object.getClass().getSimpleName(), virtualMachine.sizeOf(object));
        }

        return 0;
    }
}
