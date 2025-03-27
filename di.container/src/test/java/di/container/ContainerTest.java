package di.container;

import org.junit.jupiter.api.BeforeEach;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }
}

interface Component {
}

interface Dependency {
}

interface AnotherDependency {
}

