package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {
    Dependency dependency = mock(Dependency.class);
    Provider<Dependency> dependencyProvider = mock(Provider.class);
    Context context = mock(Context.class);
    ParameterizedType dependencyProviderType;

    @BeforeEach
    public void setUp() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {
        @Nested
        class Injection {

            static class ComponentWithDefaultConstructor implements TestComponent {
                public ComponentWithDefaultConstructor() {
                }
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                TestComponent instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class InjectConstructor {
                Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);
                assertSame(dependency, instance.dependency);
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }
        }

        @Nested
        class IllegalInjectionConstructors {
            abstract static class AbstractComponent implements TestComponent {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(TestComponent.class));
            }

            static class ComponentWithMultiInjectConstructors implements TestComponent {
                @Inject
                public ComponentWithMultiInjectConstructors(String name, Double value) {
                }

                @Inject
                public ComponentWithMultiInjectConstructors(String name) {
                }
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(ComponentWithMultiInjectConstructors.class));
            }

            static class ComponentWithNoInjectConstructorNorDefaultConstructor implements TestComponent {
                public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
                }
            }

            @Test
            public void should_throw_exception_if_no_inject_or_default_constructors_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class)
                );
            }
        }
    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_inject_dependency_via_superclass_inject_field() {
                SubClassWithFieldInjection component = new InjectionProvider<>(SubClassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                    assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_inject_provider_via_inject_field() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }
        }

        @Nested
        class IllegalInjectFields {
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjection {
        @Nested
        class Injection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
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
                SubClassWithInjectMethod component = new InjectionProvider<>(SubClassWithInjectMethod.class).get(context);
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
                SubClassOverrideSuperClassWithInjectMethod component = new InjectionProvider<>(SubClassOverrideSuperClassWithInjectMethod.class).get(context);
                assertEquals(1, component.supperCalled);
            }

            static class SubClassOverrideSuperClassWithNoInjectMethod extends SubClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_overwrite_with_no_inject() {
                SubClassOverrideSuperClassWithNoInjectMethod component = new InjectionProvider<>(SubClassOverrideSuperClassWithNoInjectMethod.class).get(context);
                assertEquals(0, component.supperCalled);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_inject_provider_via_inject_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }
        }

        @Nested
        class IllegalInjectionMethods {
            static class InjectMethodWithParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithParameter.class));
            }
        }
    }
}
