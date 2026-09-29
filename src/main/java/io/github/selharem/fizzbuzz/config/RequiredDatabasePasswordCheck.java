package io.github.selharem.fizzbuzz.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

/**
 * Fails startup before any bean is created when no database password is configured,
 * instead of silently connecting with an unresolved placeholder.
 */
public final class RequiredDatabasePasswordCheck implements BeanFactoryPostProcessor {

    static final String PROPERTY = "spring.datasource.password";
    static final String MESSAGE = "Database password is not configured. "
            + "Set the DB_PASSWORD environment variable or mount a secret file named DB_PASSWORD.";

    private final Environment environment;

    public RequiredDatabasePasswordCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String password;
        try {
            password = environment.getProperty(PROPERTY);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            throw new IllegalStateException(MESSAGE, unresolvedPlaceholder);
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(MESSAGE);
        }
    }
}
