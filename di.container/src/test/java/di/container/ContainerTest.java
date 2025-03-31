package di.container;

import org.junit.jupiter.api.BeforeEach;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }
}

interface TestComponent {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {
}

interface AnotherDependency {
}

