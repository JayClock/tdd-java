package di.container;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Class<?>, Object> components = new HashMap<>();
    private final Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        components.put(type, instance);
    }

    public <ComponentType, ComponentImplementation extends ComponentType> void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        componentImplementations.put(type, implementation);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        if (components.containsKey(type)) {
            return (ComponentType) components.get(type);
        }
        Class<?> implementation = componentImplementations.get(type);
        try {
            return (ComponentType) implementation.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
