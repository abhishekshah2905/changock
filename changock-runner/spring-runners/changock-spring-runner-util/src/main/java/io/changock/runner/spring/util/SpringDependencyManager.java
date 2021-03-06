package io.changock.runner.spring.util;

import io.changock.driver.api.common.Validable;
import io.changock.driver.api.driver.ChangeSetDependency;
import io.changock.migration.api.exception.ChangockException;
import io.changock.runner.core.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

/**
 * DependencyManager with support for ApplicationContext from Spring
 */
public class SpringDependencyManager extends DependencyManager implements Validable {

  private static final Logger logger = LoggerFactory.getLogger(SpringDependencyManager.class);

  private final ApplicationContext springContext;

  public SpringDependencyManager(ApplicationContext springContext) {
    this.springContext = springContext;
  }


  @Override
  public Optional<Object> getDependency(Class type, boolean lockGuarded) {
    return getDependency(type, null, lockGuarded);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<Object> getDependency(Class type, String name, boolean lockGuarded) {
    Optional<Object> dependencyFromParent = super.getDependency(type, name, lockGuarded);
    if (dependencyFromParent.isPresent()) {
      return dependencyFromParent;
    } else if (springContext != null) {
      try {
        boolean byName = name != null && !name.isEmpty() && !ChangeSetDependency.DEFAULT_NAME.equals(name);
        Optional<Object> dependencyFromContext = Optional.of(
            byName ? springContext.getBean(name) : springContext.getBean(type))  ;
        if (lockGuarded) {
          if (!type.isInterface()) {
            throw new ChangockException(String.format("Parameter of type [%s] must be an interface", type.getSimpleName()));
          }
          return dependencyFromContext.map(instance -> lockGuardProxyFactory.getRawProxy(instance, type));
        } else {
          return dependencyFromContext;
        }
      } catch (BeansException ex) {
        logger.warn("Dependency not found: {}", ex.getMessage());
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void runValidation() throws ChangockException {
    if (springContext == null) {
      throw new ChangockException("SpringContext not injected to SpringDependencyManager");
    }
  }
}
