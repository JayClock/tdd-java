package di.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component);
    }

    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    static ComponentRef of(Type type) {
        return new ComponentRef<>(type, null);
    }

    private Type container;
    private Component component;

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    ComponentRef(Class<ComponentType> component) {
        init(component, null);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = new Component((Class<ComponentType>) container.getActualTypeArguments()[0], qualifier);
        } else {
            this.component = new Component((Class<ComponentType>) type, qualifier);
        }
    }

    public Type getContainer() {
        return container;
    }

    public Component component() {
        return component;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public boolean isContainer() {
        return container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
