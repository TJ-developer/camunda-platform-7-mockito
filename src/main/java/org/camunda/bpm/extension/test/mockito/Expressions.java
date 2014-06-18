package org.camunda.bpm.extension.test.mockito;

import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.test.mockito.function.NameForType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.camunda.bpm.extension.test.mockito.function.CreateInstance.mockInstance;
import static org.camunda.bpm.extension.test.mockito.function.CreateInstance.newInstanceByDefaultConstructor;
import static org.camunda.bpm.extension.test.mockito.function.NameForType.juelNameFor;

/**
 * Util class that delegates to {@link org.camunda.bpm.engine.test.mock.Mocks#register(String, Object)} and
 * {@link org.camunda.bpm.engine.test.mock.Mocks#get(Object)} in a type-safe way.
 * <p/>
 * When mocking JavaDelegate, ExecutionListener or TaskListener, use {@link DelegateExpressions} instead.
 */
public final class Expressions {

  private static final Logger LOG = LoggerFactory.getLogger(Expressions.class);

  /**
   * Util class, do not instantiate!
   */
  private Expressions() {
    // hide constructor
  }

  /**
   * Registers mock instances for every public static nested class found in
   * parentClass.
   *
   * @param parentClass the parentClass to scan for nested public static types
   */
  public static void registerMockInstancesForNestedTypes(final Class<?> parentClass) {
    final Collection<Class<?>> nestedClasses = findNestedClasses(parentClass);
    for (final Class<?> type : nestedClasses) {
      registerMockInstance(type);
    }
  }

  private static Collection<Class<?>> findNestedClasses(final Class<?> parentClass) {
    final List<Class<?>> result = newArrayList();

    for (final Class<?> nestedClass : parentClass.getDeclaredClasses()) {
      final int modifiers = nestedClass.getModifiers();
      if (isPublic(modifiers) && isStatic(modifiers)) {
        result.add(nestedClass);
      }
    }
    return result;
  }

  /**
   * Registers mocks via {@link org.camunda.bpm.engine.test.mock.Mocks#register(String, Object)} for all
   * attributes with Named-types.
   *
   * @param instance instance who's fields are registered (maybe Junit test or jbehave steps).
   */
  public static void registerInstancesForFields(final Object instance) {
    checkArgument(instance != null, "instance must not be null!");
    for (final Field field : instance.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        final Object value = field.get(instance);
        if (value != null) {
          registerInstance(juelNameFor(field.getType()), value);
        }
      } catch (final Exception e) {
        // fallThrough
      }
    }
  }

  /**
   * Registers new instances for every public static nested class found in
   * parentClass.
   *
   * @param parentClass the parentClass to scan for nested public static types
   */
  public static void registerNewInstancesForNestedTypes(final Class<?> parentClass) {
    for (final Class<?> type : findNestedClasses(parentClass)) {
      registerNewInstance(type);
    }
  }

  /**
   * Creates and registers mock instance for every given type.
   *
   * @param types collection of types to mock and register
   * @see #registerMockInstances(java.util.Collection)
   */
  public static void registerMockInstances(final Class<?>... types) {
    checkArgument(types != null, "types must not be null!");
    registerMockInstances(newArrayList(types));
  }

  /**
   * Creates and registers mock instance for every given type.
   *
   * @param types collection of types to mock and register
   */
  public static void registerMockInstances(final Collection<Class<?>> types) {
    for (final Class<?> type : types) {
      registerMockInstance(type);
    }
  }

  /**
   * Creates a mock for the given type and registers it.
   *
   * @param name the juel name under which the mock is registered
   * @param type the type of the mock to create
   * @return the registered mock instance
   */
  public static <T> T registerMockInstance(final String name, final Class<T> type) {
    return registerInstance(name, mockInstance(type));
  }

  /**
   * Creates a mock for the given type and registers it.
   *
   * @param type the type of the mock to create
   * @return the registered mock instance
   */
  public static <T> T registerMockInstance(final Class<T> type) {
    return registerMockInstance(juelNameFor(type), type);
  }

  /**
   * Creates a new instance for the given type and registers it under the given
   * name.
   *
   * @param name the name for the registered instance
   * @param type the type of the instance to create
   * @return the registered instance
   */
  public static <T> T registerNewInstance(final String name, final Class<T> type) {
    return registerInstance(name, newInstanceByDefaultConstructor(type));
  }

  /**
   * Creates a new instance for the given type using the default constructor and
   * registers it.
   *
   * @param type the type of the instance to create
   * @return the registered instance
   * @see #registerNewInstance(String, Class)
   */
  public static <T> T registerNewInstance(final Class<T> type) {
    return registerNewInstance(juelNameFor(type), type);
  }

  /**
   * If you already have the instance, register it directly. Name is guessed via
   * {@link NameForType}.
   *
   * @param instance the instance or mock to register
   * @return the registered instance
   */
  public static <T> T registerInstance(final T instance) {
    return registerInstance(juelNameFor(instance), instance);
  }

  /**
   * Delegates to {@link org.camunda.bpm.engine.test.mock.Mocks#register(String, Object)}
   *
   * @param name     the juel name for the registered instance
   * @param instance the instance to register
   * @return the registered instance
   */
  public static <T> T registerInstance(final String name, final T instance) {
    checkArgument(isNotBlank(name), "name must not be blank!");
    checkArgument(instance != null, "instance must not be null!");
    LOG.debug(format("registered instance: name=%s, value=%s", name, instance));

    Mocks.register(name, instance);

    return instance;
  }

  /**
   * @param name juel name of the registered instance or mock
   * @return registered instance or mock of type
   */
  @SuppressWarnings("unchecked")
  public static <T> T getRegistered(final String name) {
    final T mock = (T) Mocks.get(name);
    checkState(mock != null, "no instance registered for name=" + name);

    return mock;
  }

  /**
   * @param type the type of the registered instance or mock
   * @return registered instance or mock for type
   */
  public static <T> T getRegistered(final Class<?> type) {
    return getRegistered(juelNameFor(type));
  }

  /**
   * @see org.camunda.bpm.engine.test.mock.Mocks#reset()
   */
  public static void reset() {
    Mocks.reset();
  }

}
