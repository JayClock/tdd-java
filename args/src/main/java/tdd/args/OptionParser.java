package tdd.args;

import java.util.List;

interface OptionParser {
    Object parse(List<String> arguments, Option option);
}
