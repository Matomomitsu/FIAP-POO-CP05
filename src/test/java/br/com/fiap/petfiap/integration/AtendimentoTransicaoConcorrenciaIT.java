package br.com.fiap.petfiap.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtendimentoTransicaoConcorrenciaIT extends ApiIntegrationSupport {

    @ParameterizedTest
    @ValueSource(strings = {"BANHO", "TOSA", "CONSULTA"})
    public void deveRetornar200E409EPreservarEstadoVencedorEmTransicoesSimultaneas(String tipo) throws Exception {
        // Arrange
        var criado = chamar("POST", agendamento(tipo, "Rex transicao", "PEQUENO", "Ana", horarioFuturo()));
        assertEquals(201, criado.statusCode());
        long id = json(criado).get("id").asLong();
        String conclusao = "/api/atendimentos/" + id + "/conclusao";
        String cancelamento = "/api/atendimentos/" + id + "/cancelamento";

        // Act
        var respostas = simultaneamente(conclusao, cancelamento);

        // Assert: nao depende de qual cliente ganha a disputa.
        assertEquals(List.of(200, 409), respostas.stream().map(r -> r.statusCode()).sorted().toList());
        boolean conclusaoVenceu = respostas.get(0).statusCode() == 200;
        String statusEsperado = conclusaoVenceu ? "CONCLUIDO" : "CANCELADO";
        var vencedor = conclusaoVenceu ? respostas.get(0) : respostas.get(1);
        assertEquals(statusEsperado, json(vencedor).get("status").asText());
        assertEquals(statusEsperado, repository.findById(id).orElseThrow().getStatus());
        assertEquals(1L, repository.count());
        var carregado = chamar("GET", "/api/atendimentos/" + id);
        assertEquals(200, carregado.statusCode());
        assertEquals(statusEsperado, json(carregado).get("status").asText());

        // Repetir a operacao perdedora e a vencedora tambem deve manter o estado.
        assertEquals(409, chamar("POST", conclusao).statusCode());
        assertEquals(409, chamar("POST", cancelamento).statusCode());
        assertEquals(statusEsperado, repository.findById(id).orElseThrow().getStatus());
    }
}
