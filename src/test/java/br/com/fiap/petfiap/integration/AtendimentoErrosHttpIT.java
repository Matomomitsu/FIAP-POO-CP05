package br.com.fiap.petfiap.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtendimentoErrosHttpIT extends ApiIntegrationSupport {

    static Stream<Arguments> entradasInvalidas() {
        List<Arguments> casos = new ArrayList<>();
        String futuro = horarioFuturo();
        for (String tipo : List.of("BANHO", "TOSA", "CONSULTA")) {
            for (String porte : List.of("GIGANTE", "MINUSCULO", "pequeno", " PEQUENO ")) {
                casos.add(Arguments.of(tipo + " / porte: " + porte,
                        agendamento(tipo, "Rex", porte, "Ana", futuro)));
            }
            for (String tutor : List.of("", "   ", "\t", "\n")) {
                casos.add(Arguments.of(tipo + " / tutor em branco",
                        agendamento(tipo, "Rex", "PEQUENO", tutor, futuro)));
            }
            casos.add(Arguments.of(tipo + " / tutor ausente", "/api/atendimentos?tipo=" + tipo
                    + "&petNome=Rex&porte=PEQUENO&dataHora=" + codificar(futuro)));
        }
        casos.add(Arguments.of("data passada", agendamento("BANHO", "Rex", "PEQUENO", "Ana",
                LocalDateTime.now().minusDays(1).withNano(0).toString())));
        casos.add(Arguments.of("tipo inexistente", agendamento("VACINA", "Rex", "PEQUENO", "Ana", futuro)));
        casos.add(Arguments.of("pet vazio", agendamento("BANHO", "", "PEQUENO", "Ana", futuro)));
        casos.add(Arguments.of("pet em branco", agendamento("BANHO", "   ", "PEQUENO", "Ana", futuro)));
        casos.add(Arguments.of("porte em branco", agendamento("BANHO", "Rex", "   ", "Ana", futuro)));
        casos.add(Arguments.of("data malformada", agendamento("BANHO", "Rex", "PEQUENO", "Ana", "data-invalida")));
        casos.add(Arguments.of("parametros ausentes", "/api/atendimentos?tipo=BANHO"));
        return casos.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entradasInvalidas")
    public void deveRetornar400SemPersistirQuandoEntradaForInvalida(String caso, String path) throws Exception {
        // Arrange: banco temporario vazio e entrada invalida do cenario.
        assertEquals(0L, repository.count());

        // Act
        var resposta = chamar("POST", path);

        // Assert
        assertEquals(400, resposta.statusCode(), caso);
        assertEquals(0L, repository.count(), "Entrada invalida nao pode ser persistida");
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/atendimentos/9223372036854775807, 404",
            "GET, /api/atendimentos/9223372036854775807/resumo, 404",
            "POST, /api/atendimentos/9223372036854775807/conclusao, 404",
            "POST, /api/atendimentos/9223372036854775807/cancelamento, 404",
            "GET, /api/atendimentos/id-invalido, 400"
    })
    public void deveTraduzirErrosDeIdSemPersistir(String method, String path, int statusEsperado) throws Exception {
        // Arrange: ID inexistente ou malformado.
        assertEquals(0L, repository.count());

        // Act
        var resposta = chamar(method, path);

        // Assert
        assertEquals(statusEsperado, resposta.statusCode());
        assertEquals(0L, repository.count());
    }

    @Test
    public void deveRetornar409SemSalvarDuplicataQuandoHorarioJaEstiverOcupado() throws Exception {
        // Arrange
        String path = agendamento("BANHO", "Rex", "PEQUENO", "Ana", horarioFuturo());
        assertEquals(201, chamar("POST", path).statusCode());

        // Act
        var resposta = chamar("POST", path);

        // Assert
        assertEquals(409, resposta.statusCode());
        assertEquals(1L, repository.count());
    }
}
