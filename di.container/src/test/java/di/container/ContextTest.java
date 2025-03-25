package di.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {
    Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_type() {
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);
            assertSame(instance, context.get(Component.class));
        }
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            context.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = context.get(Component.class);
            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            context.bind(Component.class, ComponentWithInjectConstructor.class);
            context.bind(Dependency.class, dependency);
            Component instance = context.get(Component.class);
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }
    }
}

interface Component {
}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

interface Dependency {
}

class ComponentWithInjectConstructor implements Component {
    private final Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}
