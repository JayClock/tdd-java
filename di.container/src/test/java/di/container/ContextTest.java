package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class TypeBinding {
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

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            ParameterizedType type = (ParameterizedType) new TypeLiteral<Provider<Component>>() {
            }.getType();
            Provider<Component> provider = (Provider<Component>) context.get(type).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            ParameterizedType type = (ParameterizedType) new TypeLiteral<List<Component>>() {
            }.getType();
            assertFalse(context.get(type).isPresent());
        }

        static abstract class TypeLiteral<T> {
            public Type getType() {
                return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }
    }

    @Nested
    public class DependencyCheck {
        static class ComponentWithInjectConstructor implements Component {
            private final Dependency dependency;

            @Inject
            public ComponentWithInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency getDependency() {
                return dependency;
            }
        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        static class DependencyDependentOnComponent implements Dependency {
            private final Component component;

            @Inject
            public DependencyDependentOnComponent(Component component) {
                this.component = component;
            }

            public Component getComponent() {
                return component;
            }
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


        static class AnotherDependencyDependentOnComponent implements AnotherDependency {
            private final Component component;

            @Inject
            public AnotherDependencyDependentOnComponent(Component component) {
                this.component = component;
            }

            public Component getComponent() {
                return component;
            }
        }

        static class DependencyDependentOnAnotherDependency implements Dependency {
            private final AnotherDependency anotherDependency;

            @Inject
            public DependencyDependentOnAnotherDependency(AnotherDependency anotherDependency) {
                this.anotherDependency = anotherDependency;
            }

            public AnotherDependency getAnotherDependency() {
                return anotherDependency;
            }
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
