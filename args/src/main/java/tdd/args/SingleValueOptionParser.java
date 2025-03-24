package tdd.args;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValueOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;
    T defaultValue;

    public SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());

        if (index == -1) {
            return defaultValue;
        }

        int followingFlag = IntStream.range(index + 1, arguments.size()).filter(it -> arguments.get(it).startsWith("-")).findFirst().orElse(arguments.size());
        List<String> values = arguments.subList(index + 1, followingFlag);

        if (values.isEmpty()) {
            throw new InsufficientArgumentsException(option.value());
        }

        if (values.size() > 1) {
            throw new TooManyArgumentsException(option.value());
        }

        return valueParser.apply(arguments.get(index + 1));
    }

    private static boolean secondArgumentIsNotAFlag(List<String> arguments, int index) {
        return index + 2 < arguments.size() && !arguments.get(index + 2).startsWith("-");
    }

    private static boolean isFollowByOtherFlag(List<String> arguments, int index) {
        return arguments.get(index + 1).startsWith("-");
    }

    private static boolean isReachEndOfList(List<String> arguments, int index) {
        return index + 1 == arguments.size();
    }
}
