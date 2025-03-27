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

        abstract static class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
        }
    }

    @Test
    public void should_return_empty_if_component_not_defined() {
        Optional<Component> component = config.getContext().get(Component.class);
        assertTrue(component.isEmpty());
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            config.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, dependency);
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);
            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
            });
        }

        @Test
        public void should_throw_exception_if_no_inject_or_default_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
            });
        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        @Test
        public void should_throw_exception_if_transitive_dependencies_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(String.class, exception.getDependency());
            assertEquals(Dependency.class, exception.getComponent());
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

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubClassWithFieldInjection extends ComponentWithFieldInjection {
        }

        @Test
        public void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
            ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }

        @Test
        void should_inject_dependency_via_superclass_inject_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(SubClassWithFieldInjection.class, SubClassWithFieldInjection.class);
            SubClassWithFieldInjection component = config.getContext().get(SubClassWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
        }
    }

    @Nested
    public class MethodInjection {
        static class InjectMethodWithNoDependency {
            boolean called = false;

            @Inject
            void install() {
                called = true;
            }
        }

        @Test
        public void should_call_inject_method_even_if_no_dependency_declared() {
            config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
            InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
            assertTrue(component.called);
        }

        static class InjectMethodWithDependency {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
            InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();
            assertSame(dependency, component.dependency);
        }

        static class SupperClassWithInjectMethod {
            int supperCalled = 0;

            @Inject
            void install() {
                supperCalled++;
            }
        }

        static class SubClassWithInjectMethod extends SupperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = supperCalled + 1;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method_from_supper_class() {
            config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);
            SubClassWithInjectMethod component = config.getContext().get(SubClassWithInjectMethod.class).get();
            assertEquals(1, component.supperCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubClassOverrideSuperClassWithInjectMethod extends SubClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() {
            config.bind(SubClassOverrideSuperClassWithInjectMethod.class, SubClassOverrideSuperClassWithInjectMethod.class);
            SubClassOverrideSuperClassWithInjectMethod component = config.getContext().get(SubClassOverrideSuperClassWithInjectMethod.class).get();
            assertEquals(1, component.supperCalled);
        }

        static class SubClassOverrideSuperClassWithNoInjectMethod extends SubClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_overwrite_with_no_inject() {
            config.bind(SubClassOverrideSuperClassWithNoInjectMethod.class, SubClassOverrideSuperClassWithNoInjectMethod.class);
            SubClassOverrideSuperClassWithNoInjectMethod component = config.getContext().get(SubClassOverrideSuperClassWithNoInjectMethod.class).get();
            assertEquals(0, component.supperCalled);
        }

        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class InjectMethodWithParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithParameter.class));
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