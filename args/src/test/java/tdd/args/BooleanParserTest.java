package tdd.args;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class BooleanParserTest {
    @Test // SadPath
    public void should_not_accept_extra_argument_for_boolean_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
            new BooleanParser().parse(asList("-l", "t"), option("l"));
        });
    }

    @Test // Default Value
    public void should_set_default_value_to_false_if_option_not_present() {
        assertFalse(new BooleanParser().parse(List.of(), option("l")));
    }

    @Test // HappyPath
    public void should_set_default_value_to_true_if_option_present() {
        assertTrue(new BooleanParser().parse(List.of("-l"), option("l")));
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
