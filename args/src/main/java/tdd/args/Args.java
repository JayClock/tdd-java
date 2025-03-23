package tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Args {
    public static <T> T parse(Class<T> optionsClass, String... args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(it, arguments)).toArray();
            return (T) constructor.newInstance(values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<Class<?>, OptionParser> PARSERS = Map.of(
            boolean.class, new BooleanParser(),
            int.class, new SingleValueOptionParser<>(0, Integer::parseInt),
            String.class, new SingleValueOptionParser<>("", String::valueOf));

    private static Object parseOption(Parameter parameter, List<String> arguments) {
        Option option = parameter.getAnnotation(Option.class);
        Class<?> type = parameter.getType();
        OptionParser parser = PARSERS.get(type);
        return parser.parse(arguments, option);
    }

}
