package tdd.args;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

class OptionParsers<T> {
    public static OptionParser<Boolean> bool() {
        return ((arguments, option) -> values(arguments, option, 0).isPresent());
    }

    public static <T> OptionParser<T> unary(T defaultValue, Function<String, T> valueParser) {
        return ((arguments, option) -> values(arguments, option, 1).map(it -> parseValue(option, it.getFirst(), valueParser)).orElse(defaultValue));
    }

    private static Optional<List<String>> values(List<String> arguments, Option option, int expectedSize) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) return Optional.empty();
        List<String> values = values(arguments, index);
        if (values.size() < expectedSize) throw new InsufficientArgumentsException(option.value());
        if (values.size() > expectedSize) throw new TooManyArgumentsException(option.value());
        return Optional.of(values);
    }

    private static <T> T parseValue(Option option, String value, Function<String, T> valueParser) {
        return valueParser.apply(value);
    }

    public static List<String> values(List<String> arguments, int index) {
        int followingFlag = IntStream.range(index + 1, arguments.size()).filter(it -> arguments.get(it).startsWith("-")).findFirst().orElse(arguments.size());
        return arguments.subList(index + 1, followingFlag);
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
