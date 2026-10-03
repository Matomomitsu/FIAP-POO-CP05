package br.com.fiap.petfiap.model;

import br.com.fiap.petfiap.exception.StatusInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AtendimentoStatusContratoTest {

    @Test
    public void deveRecusarConclusaoQuandoAtendimentoEstiverCancelado() {
        // Arrange
        Atendimento cancelado = new Banho(1, "Rex", "PEQUENO", "Ana", LocalDateTime.now().plusDays(1));
        cancelado.cancelar();

        // Act + Assert
        assertThrows(StatusInvalidoException.class, cancelado::concluir);
        assertEquals("CANCELADO", cancelado.getStatus());
    }
}
