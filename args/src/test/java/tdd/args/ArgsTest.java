package tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArgsTest {
    // TODO: Bool -l
    // TODO: Integer -p 8080
    // TODO: String -d /user/logs
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
