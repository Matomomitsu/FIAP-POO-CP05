package br.com.fiap.petfiap.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtendimentoAgendamentoConcorrenciaIT extends ApiIntegrationSupport {

    @ParameterizedTest
    @ValueSource(strings = {"BANHO", "TOSA", "CONSULTA"})
    public void deveRetornar201E409EPersistirSomenteUmRegistroEmAgendamentosSimultaneos(String tipo) throws Exception {
        // Arrange: ambas as requisicoes disputam exatamente o mesmo pet e horario.
        String path = agendamento(tipo, "Rex concorrente", "PEQUENO", "Ana", horarioFuturo());

        // Act: dois clientes liberados pela mesma barreira.
        var respostas = simultaneamente(path, path);

        // Assert: um vencedor e uma recusa; nao basta conferir apenas o HTTP.
        assertEquals(List.of(201, 409), respostas.stream().map(r -> r.statusCode()).sorted().toList());
        assertEquals(1L, repository.count());
        var registros = repository.findByPetNome("Rex concorrente");
        assertEquals(1, registros.size());
        assertEquals("AGENDADO", registros.get(0).getStatus());
        assertEquals(tipo, registros.get(0).getTipo());
        var vencedor = respostas.stream().filter(r -> r.statusCode() == 201).findFirst().orElseThrow();
        assertEquals(registros.get(0).getId().longValue(), json(vencedor).get("id").asLong());
        var carregado = chamar("GET", "/api/atendimentos/" + registros.get(0).getId());
        assertEquals(200, carregado.statusCode());
        assertEquals(json(vencedor), json(carregado));
    }
}
