package di.container;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
