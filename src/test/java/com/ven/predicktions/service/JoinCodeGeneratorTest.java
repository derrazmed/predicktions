package com.ven.predicktions.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodeGeneratorTest {

    private final JoinCodeGenerator joinCodeGenerator = new JoinCodeGenerator();

    @Test
    void generate_returnsFixedLengthCodeFromAlphabet() {
        String joinCode = joinCodeGenerator.generate();

        assertThat(joinCode).hasSize(JoinCodeGenerator.LENGTH);
        assertThat(joinCode).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]+");
    }

    @Test
    void generate_producesDifferentCodes() {
        String first = joinCodeGenerator.generate();
        String second = joinCodeGenerator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}
