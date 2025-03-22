package tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {
    @Test
    public void should_set_boolean_option_to_true_if_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class, "-l");
        assertTrue(option.logging());
    }

    @Test
    public void should_set_boolean_option_to_false_if_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class);
        assertFalse(option.logging());
    }

    static record BooleanOption(@Option("l") boolean logging) {
    }

    @Test
    public void should_parse_int_as_option_value() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        assertEquals(option.port(), 8080);
    }

    static record IntOption(@Option("p") int port) {
    }

    @Test
    public void should_parse_string_as_option_value() {
        StringOption option = Args.parse(StringOption.class, "-d", "/user/logs");
        assertEquals(option.directory(), "/user/logs");
    }
    static record StringOption(@Option("d") String directory) {}

    // TODO: multi options: -l -p 8080 -d /user/logs
    // sad path
    // TODO: -bool -l t
    // TODO: -int -p / -p 8080 8080
    // TODO: -string -d / -d /user/logs/user/vars
    // default value
    // TODO: -bool : false
    // TODO: -int: 0
    // TODO: -sting ""
    @Test
    @Disabled
    public void should_example_1() {
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/user/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/user/logs", options.directory());
    }

    @Test
    @Disabled
    public void should_example_2() {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertEquals(new int[]{1, 2, -3, 5}, options.decimals());
    }

    static record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }

    static record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {
    }
}
