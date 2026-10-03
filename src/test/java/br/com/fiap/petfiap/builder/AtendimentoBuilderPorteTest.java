package br.com.fiap.petfiap.builder;

import br.com.fiap.petfiap.model.Atendimento;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AtendimentoBuilderPorteTest {

    @ParameterizedTest
    @ValueSource(strings = {"GIGANTE", "MINUSCULO", "pequeno", " PEQUENO "})
    public void deveRecusarMontagemQuandoPorteNaoForReconhecido(String porte) {
        // Arrange
        AtendimentoBuilder builder = new AtendimentoBuilder()
                .comTipo("BANHO")
                .comPet("Rex", porte)
                .comTutor("Ana")
                .comDataHora(LocalDateTime.now().plusDays(1));

        // Act + Assert
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class,
                () -> builder.construir(1));
        assertEquals("Porte do pet invalido: " + porte, excecao.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PEQUENO", "MEDIO", "GRANDE"})
    public void deveMontarAtendimentoQuandoPorteForReconhecido(String porte) {
        // Arrange
        AtendimentoBuilder builder = new AtendimentoBuilder()
                .comTipo("BANHO")
                .comPet("Rex", porte)
                .comTutor("Ana")
                .comDataHora(LocalDateTime.now().plusDays(1));

        // Act
        Atendimento atendimento = builder.construir(1);

        // Assert
        assertEquals(porte, atendimento.getPetPorte());
        assertEquals("AGENDADO", atendimento.getStatus());
    }
}
