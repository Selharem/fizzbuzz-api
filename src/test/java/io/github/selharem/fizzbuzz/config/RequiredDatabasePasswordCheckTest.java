package io.github.selharem.fizzbuzz.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RequiredDatabasePasswordCheckTest {

    @Test
    void acceptsAConfiguredPassword() {
        var environment = new MockEnvironment()
                .withProperty(RequiredDatabasePasswordCheck.PROPERTY, "${DB_PASSWORD}")
                .withProperty("DB_PASSWORD", "s3cret");

        assertThatCode(() -> new RequiredDatabasePasswordCheck(environment).postProcessBeanFactory(null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnresolvedPlaceholder() {
        var environment = new MockEnvironment()
                .withProperty(RequiredDatabasePasswordCheck.PROPERTY, "${DB_PASSWORD}");

        assertThatIllegalStateException()
                .isThrownBy(() -> new RequiredDatabasePasswordCheck(environment).postProcessBeanFactory(null))
                .withMessage(RequiredDatabasePasswordCheck.MESSAGE);
    }

    @Test
    void rejectsABlankPassword() {
        var environment = new MockEnvironment()
                .withProperty(RequiredDatabasePasswordCheck.PROPERTY, " ");

        assertThatIllegalStateException()
                .isThrownBy(() -> new RequiredDatabasePasswordCheck(environment).postProcessBeanFactory(null))
                .withMessage(RequiredDatabasePasswordCheck.MESSAGE);
    }

    @Test
    void rejectsAMissingPassword() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new RequiredDatabasePasswordCheck(new MockEnvironment()).postProcessBeanFactory(null))
                .withMessage(RequiredDatabasePasswordCheck.MESSAGE);
    }
}
