package di.container;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class CyclicDependenciesFoundException extends RuntimeException {
    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
