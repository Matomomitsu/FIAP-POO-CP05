package br.com.fiap.petfiap.service;

import br.com.fiap.petfiap.exception.HorarioOcupadoException;
import br.com.fiap.petfiap.model.Banho;
import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgendaNomeContratoTest {

    @Mock
    private AtendimentoRepository repository;

    @InjectMocks
    private AgendaService service;

    @Test
    public void deveRecusarConflitoQuandoNomesIguaisForemStringsDistintas() {
        // Arrange: nomes com o mesmo valor, mas sem compartilhar a referencia.
        String nomeExistente = new String("Rex");
        String nomeNovo = new String("Rex");
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        Banho existente = new Banho(1, nomeExistente, "PEQUENO", "Ana", horario);
        Banho novo = new Banho(2, nomeNovo, "PEQUENO", "Ana", horario);
        when(repository.findByPetNome(nomeNovo)).thenReturn(List.of(existente));
        assertNotSame(nomeExistente, nomeNovo);

        // Act + Assert
        assertThrows(HorarioOcupadoException.class, () -> service.agendar(novo));
        verify(repository, never()).save(any());
    }
}
