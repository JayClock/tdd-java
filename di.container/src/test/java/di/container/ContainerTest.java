package di.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }


    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_type() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class DependencyCheck {
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependentOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
                List<Class<?>> classes = asList(exception.getComponents());
                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependentOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependentOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
                List<Class<?>> classes = asList(exception.getComponents());
                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }
        }
    }

}

interface Component {
}

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
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

class DependencyDependentOnComponent implements Dependency {
    private final Component component;

    @Inject
    public DependencyDependentOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class AnotherDependencyDependentOnComponent implements AnotherDependency {
    private final Component component;

    @Inject
    public AnotherDependencyDependentOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class DependencyDependentOnAnotherDependency implements Dependency {
    private final AnotherDependency anotherDependency;

    @Inject
    public DependencyDependentOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    public AnotherDependency getAnotherDependency() {
        return anotherDependency;
    }
}