package br.com.datum.statuspublisher.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "active", " Active "})
    void active_normalizaParaMaiuscula(String value) {
        assertThat(StatusValidator.normalize(value)).isEqualTo("ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "inactive", " Inactive "})
    void inactive_normalizaParaMaiuscula(String value) {
        assertThat(StatusValidator.normalize(value)).isEqualTo("INACTIVE");
    }

    @Test
    void valorInvalido_lancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusValidator.normalize("FOO"))
                .withMessageContaining("FOO");
    }

    @Test
    void valorNulo_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> StatusValidator.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
