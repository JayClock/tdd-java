package tdd.args;

import java.util.List;

class IntParser implements OptionParser {
    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return Integer.parseInt(arguments.get(index + 1));
    }
}
