package di.container;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }

        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            T instance = this.injectConstructor.newInstance(toDependencies(context, this.injectConstructor));
            for (Field field : injectFields)
                field.set(instance, toDependency(context, field));
            for (Method method : injectMethods)
                method.invoke(instance, toDependencies(context, method));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
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

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(injectConstructor.getParameters()).map(InjectionProvider::toComponentRef),
                        injectFields.stream().map(Field::getGenericType).map(ComponentRef::of)),
                injectMethods.stream().flatMap(m -> stream(m.getParameters()).map(Parameter::getParameterizedType).map(ComponentRef::of)))
                .toList();
    }

    private static ComponentRef<?> toComponentRef(Parameter p) {
        Annotation qualifier = stream(p.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .findFirst().orElse(null);
        return ComponentRef.of(p.getParameterizedType(), qualifier);
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
        return stream(executable.getParameters()).map(p -> toDependency(context, p.getParameterizedType())).toArray();
    }

    private static Object toDependency(Context context, Type type) {
        return context.get(ComponentRef.of(type)).get();
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, field.getGenericType());
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
