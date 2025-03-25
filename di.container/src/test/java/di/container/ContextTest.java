package di.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
            assertSame(instance, context.get(Component.class).get());
        }
    }

    @Test
    public void should_return_empty_if_component_not_defined() {
        Optional<Component> component = context.get(Component.class);
        assertTrue(component.isEmpty());
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            context.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = context.get(Component.class).get();
            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            context.bind(Component.class, ComponentWithInjectConstructor.class);
            context.bind(Dependency.class, dependency);
            Component instance = context.get(Component.class).get();
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependencies() {
            context.bind(Component.class, ComponentWithInjectConstructor.class);
            context.bind(Dependency.class, DependencyWithInjectConstructor.class);
            context.bind(String.class, "indirect dependency");
            Component instance = context.get(Component.class).get();
            assertNotNull(instance);
            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);
            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
            });
        }

        @Test
        public void should_throw_exception_if_no_inject_or_default_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
            });
        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
            context.bind(Component.class, ComponentWithInjectConstructor.class);
            assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));
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

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private final String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}