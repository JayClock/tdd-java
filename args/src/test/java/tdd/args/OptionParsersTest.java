package tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static tdd.args.OptionParsersTest.BooleanOptionParser.option;

public class OptionParsersTest {
    @Nested
    class UnaryOptionParser {
        @Test // sad path
        public void should_not_accept_extra_argument_for_single_valued_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parse(asList("-p", "8080", "8081"), option("p"));
            });

            assertEquals("p", e.getOption());
        }

        @ParameterizedTest // sad path
        @ValueSource(strings = {"-p -l", "-p"})
        public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) {
            InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parse(asList(arguments.split(" ")), option("p"));
            });
            assertEquals("p", e.getOption());
        }

        @Test // default value
        public void should_set_default_value_for_single_valued_option() {
            Function<String, Object> whatever = (it) -> null;
            Object defaultValue = new Object();
            assertSame(defaultValue, OptionParsers.unary(defaultValue, whatever).parse(List.of(), option("p")));
        }

        @Test // happy path
        public void should_parse_value_if_flag_present() {
            Object parsed = new Object();
            Function<String, Object> parse = (it) -> parsed;
            Object whatever = new Object();
            assertSame(parsed, OptionParsers.unary(whatever, parse).parse(List.of("-p", "8080"), option("p")));
        }
    }

    @Nested
    class BooleanOptionParser {
        @Test // SadPath
        public void should_not_accept_extra_argument_for_boolean_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.bool().parse(asList("-l", "t"), option("l"));
            });
        }

        @Test // Default Value
        public void should_set_default_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parse(List.of(), option("l")));
        }

        @Test // HappyPath
        public void should_set_default_value_to_true_if_option_present() {
            assertTrue(OptionParsers.bool().parse(List.of("-l"), option("l")));
        }


        static Option option(String value) {
            return new Option() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Option.class;
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }
    }
}
