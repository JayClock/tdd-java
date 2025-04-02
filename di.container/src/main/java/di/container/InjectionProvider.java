package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        Constructor<T> constructor = getInjectConstructor(component);
        ComponentRef<?>[] required = stream(constructor.getParameters()).map(InjectionProvider::toComponentRef).toArray(ComponentRef<?>[]::new);
        this.injectConstructor = new Injectable<>(constructor, required);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }

        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }

        getDependencies();
    }

    @Override
    public T get(Context context) {
        try {
            T instance = this.injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Field field : injectFields)
                field.set(instance, toDependency(context, field));
            for (Method method : injectMethods)
                method.invoke(instance, toDependencies(context, method));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(injectConstructor.required()),
                        injectFields.stream().map(InjectionProvider::toComponentRef)),
                injectMethods.stream().flatMap(m -> stream(m.getParameters()).map(InjectionProvider::toComponentRef)))
                .toList();
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (current, fields) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(methods.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(m, current))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
        reverse(injectMethods);
        return injectMethods;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();

        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static boolean isOverrideByInjectMethod(Method m, List<Method> injectMethods) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> isOverride(m, o));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters()).map(p -> toDependency(context, toComponentRef(p))).toArray();
    }

    private static Object toDependency(Context context, ComponentRef of) {
        return context.get(of).get();
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, toComponentRef(field));
    }

    private static ComponentRef toComponentRef(Field field) {
        return ComponentRef.of(field.getGenericType(), getQualifier(field));
    }

    private static ComponentRef<?> toComponentRef(Parameter parameter) {
        return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
    }

    private static Annotation getQualifier(AnnotatedElement parameter) {
        List<Annotation> qualifiers = stream(parameter.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        if (qualifiers.size() > 1) throw new IllegalComponentException();
        return qualifiers.stream().findFirst().orElse(null);
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<Class<?>, List<T>, List<T>> finder) {
        List<T> member = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            member.addAll(finder.apply(current, member));
            current = current.getSuperclass();
        }
        return member;
    }
}
