package com.example.minitomcat.container;

import com.example.minitomcat.exception.container.ContainerException;
import com.example.minitomcat.servlet.ClassScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {
    private Container container;

    @BeforeEach
    void setUp() {
        container = new Container(new ClassScanner());
    }

    @Test
    void getBean_throwsContainerException_whenConstructorThrows() {
        // given

        // when

        // then
        assertThrows(ContainerException.class, () -> container.getBean(ConstructorThrows.class));
    }

    @Test
    void getBean_throwsException_whenNoPublicConstructor() {
        //given

        // when

        // then
        assertThrows(ContainerException.class, () -> container.getBean(NoPublicConstructor.class));
    }

    @Test
    void getBean_throwsContainerException_whenCircularDependencyExists() {
        // given

        // when

        // then
        assertThrows(ContainerException.class, () -> container.getBean(FirstCircularDependency.class));
    }

    @Test
    void getBean_returnsInstance_whenNoDependency() {
        // given

        // when
        NoDependency result = container.getBean(NoDependency.class);

        // then
        assertNotNull(result);
    }

    @Test
    void getBean_injectsConstructorDependency_whenDependencyExists() {
        // given

        // when
        DependencyExistence result = container.getBean(DependencyExistence.class);

        // then
        assertNotNull(result);
        assertNotNull(result.field);
    }

    @Test
    void getBean_returnsSameInstance_whenCalledTwice() {
        // given

        // when
        Object first = container.getBean(DependencyExistence.class);
        Object second = container.getBean(DependencyExistence.class);

        // then
        assertSame(first, second);
    }

    public static class ConstructorThrows {
        public ConstructorThrows() {
            throw new IllegalStateException("Intentional failure");
        }
    }

    static class NoPublicConstructor { }

    public static class FirstCircularDependency{
        public final SecondCircularDependency secondCircularDependency;
        public FirstCircularDependency(SecondCircularDependency secondCircularDependency) {
            this.secondCircularDependency = secondCircularDependency;
        }
    }

    public static class SecondCircularDependency {
        public final FirstCircularDependency firstCircularDependency;
        public SecondCircularDependency(FirstCircularDependency firstCircularDependency) {
            this.firstCircularDependency = firstCircularDependency;
        }
    }

    public static class DependencyExistence {
        public NoDependency field;

        public DependencyExistence(NoDependency arg) {
            this.field = arg;
        }
    }

    public static class NoDependency { }
}