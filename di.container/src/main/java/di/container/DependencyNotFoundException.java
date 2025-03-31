package di.container;

public class DependencyNotFoundException extends RuntimeException {

    private final Component component;
    private final Component dependency;

    public DependencyNotFoundException(Component component, Component dependency) {
        this.dependency = dependency;
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

    public Component getDependency() {
        return dependency;
    }
}
