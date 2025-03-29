package di.container;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    default Optional get(Type type) {
        return get(Ref.of(type));
    }

    Optional get(Ref ref);

    class Ref {
        static Ref of(Type type) {
            if (type instanceof ParameterizedType container) return new Ref(container);
            else return new Ref((Class<?>) type);
        }

        private Type container;
        private final Class<?> component;

        Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return container != null;
        }
    }
}
