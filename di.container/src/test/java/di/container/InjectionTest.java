package di.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class InjectionTest {
    ContextConfig config;
    Dependency dependency = new Dependency() {
    };

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
        config.bind(Dependency.class, dependency);
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);
            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependencies() {
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");
            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);
            assertNotNull(instance);
            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);
            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>(ContainerTest.ComponentConstruction.AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>(Component.class));
        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class));
        }

        @Test
        public void should_throw_exception_if_no_inject_or_default_constructors_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class)
            );
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }
    }

    private <T, R extends T> T getComponent(Class<T> type, Class<R> implementation) {
        config.bind(type, implementation);
        return config.getContext().get(type).get();
    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubClassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {
        }

        @Test
        public void should_inject_dependency_via_field() {
            FieldInjection.ComponentWithFieldInjection component = getComponent(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
            assertSame(dependency, component.dependency);
        }

        @Test
        void should_inject_dependency_via_superclass_inject_field() {
            FieldInjection.SubClassWithFieldInjection component = getComponent(FieldInjection.SubClassWithFieldInjection.class, FieldInjection.SubClassWithFieldInjection.class);
            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
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
            MethodInjection.InjectMethodWithNoDependency component = getComponent(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
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
            MethodInjection.InjectMethodWithDependency component = getComponent(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
            assertSame(dependency, component.dependency);
        }

        static class SupperClassWithInjectMethod {
            int supperCalled = 0;

            @Inject
            void install() {
                supperCalled++;
            }
        }

        static class SubClassWithInjectMethod extends MethodInjection.SupperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = supperCalled + 1;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method_from_supper_class() {
            MethodInjection.SubClassWithInjectMethod component = getComponent(MethodInjection.SubClassWithInjectMethod.class, MethodInjection.SubClassWithInjectMethod.class);
            assertEquals(1, component.supperCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubClassOverrideSuperClassWithInjectMethod extends MethodInjection.SubClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() {
            MethodInjection.SubClassOverrideSuperClassWithInjectMethod component =getComponent(MethodInjection.SubClassOverrideSuperClassWithInjectMethod.class,MethodInjection.SubClassOverrideSuperClassWithInjectMethod.class);
            assertEquals(1, component.supperCalled);
        }

        static class SubClassOverrideSuperClassWithNoInjectMethod extends MethodInjection.SubClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_overwrite_with_no_inject() {
            SubClassOverrideSuperClassWithNoInjectMethod component = getComponent(SubClassOverrideSuperClassWithNoInjectMethod.class, SubClassOverrideSuperClassWithNoInjectMethod.class);
            assertEquals(0, component.supperCalled);
        }

        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class InjectMethodWithParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithParameter.class));
        }
    }
}
