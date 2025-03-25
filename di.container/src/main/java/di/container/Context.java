package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Context {
    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?>[] injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toArray(Constructor<?>[]::new);
        if (injectConstructors.length > 1) {
            throw new IllegalComponentException();
        }
        if (injectConstructors.length == 0 && stream(implementation.getConstructors()).noneMatch(c -> c.getParameters().length == 0)) {
            throw new IllegalComponentException();
        }
        providers.put(type, (Provider<Type>) () -> {
            try {
                Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
                Object[] dependencies = stream(injectConstructor.getParameters())
                        .map(Parameter::getType)
                        .map(this::get)
                        .toArray();
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) throws NoSuchMethodException {
        Stream<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class));
        return (Constructor<Type>) injectConstructors.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }
}
