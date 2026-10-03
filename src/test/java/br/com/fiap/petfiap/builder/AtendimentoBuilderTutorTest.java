package br.com.fiap.petfiap.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AtendimentoBuilderTutorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    public void deveRecusarMontagemQuandoNomeDoTutorEstiverAusenteOuEmBranco(String tutorNome) {
        // Arrange
        AtendimentoBuilder builder = new AtendimentoBuilder()
                .comTipo("BANHO")
                .comPet("Rex", "PEQUENO")
                .comTutor(tutorNome)
                .comDataHora(LocalDateTime.now().plusDays(1));

        // Act + Assert
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class,
                () -> builder.construir(1));
        assertEquals("Nome do tutor obrigatorio", excecao.getMessage());
    }
}
