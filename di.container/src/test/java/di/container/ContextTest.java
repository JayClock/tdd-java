package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(Context.Ref.of(Component.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_a_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Context context = config.getContext();
            Optional<Component> component = context.get(Context.Ref.of(Component.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_a_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructorInjection implements Component {
            private final Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Context context = config.getContext();
            Optional<Component> component = context.get(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            Context.Ref<Provider<Component>> ref = new Context.Ref<>() {
            };
            Provider<Component> provider = context.get(ref).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertFalse(context.get(new Context.Ref<List<Component>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_bind_instance_with_qualifier() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"));
                Context context = config.getContext();
                Component chosenOne = context.get(Context.Ref.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(instance, chosenOne);
            }

            @Test
            public void should_bind_component_with_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        new NamedLiteral("ChosenOne")
                );
                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(Context.Ref.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(dependency, chosenOne.dependency);
            }
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> componentType) {
            config.bind(Component.class, componentType);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)));
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements Component {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements Component {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component, Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            List<Class<?>> classes = asList(exception.getComponents());
            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentField.class),
                    Named.of("Inject Method", CyclicComponentMethod.class)))
                for (Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class)))
                    arguments.add(Arguments.of(component, dependency));
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentField implements Component {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies(Class<? extends Component> component,
                                                                             Class<? extends Dependency> dependency,
                                                                             Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            List<Class<?>> classes = asList(exception.getComponents());
            assertEquals(3, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentField.class),
                    Named.of("Inject Method", CyclicComponentMethod.class)))
                for (Named dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class)))
                    for (Named anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class)))
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
            return arguments.stream();
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> component) {
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
            Context context = config.getContext();
            assertTrue(context.get(Context.Ref.of(Component.class)).isPresent());
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }
}

