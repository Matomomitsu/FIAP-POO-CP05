package br.com.fiap.petfiap.integration;

import br.com.fiap.petfiap.model.Atendimento;
import br.com.fiap.petfiap.model.Banho;
import br.com.fiap.petfiap.model.ConsultaVeterinaria;
import br.com.fiap.petfiap.model.Tosa;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AtendimentoPersistenciaIT extends ApiIntegrationSupport {

    @ParameterizedTest
    @CsvSource({
            "BANHO, PEQUENO, 60, 20, 45", "BANHO, MEDIO, 80, 20, 45", "BANHO, GRANDE, 100, 20, 45",
            "TOSA, PEQUENO, 70, 30, 60", "TOSA, MEDIO, 90, 30, 60", "TOSA, GRANDE, 120, 30, 60",
            "CONSULTA, PEQUENO, 150, 50, 30", "CONSULTA, MEDIO, 150, 50, 30", "CONSULTA, GRANDE, 150, 50, 30"
    })
    public void devePersistirPeloHttpComIdGeradoEDadosEResumoCorretos(
            String tipo, String porte, double preco, int pontos, int duracao) throws Exception {
        // Arrange
        String pet = "Rex " + tipo + " " + porte;
        String tutor = "Ana Tomomitsu";
        String horario = horarioFuturo();
        String path = agendamento(tipo, pet, porte, tutor, horario);

        // Act: controller, service e repository reais.
        var criado = chamar("POST", path);

        // Assert: identidade gerada pelo JPA e dados recuperados do banco.
        assertEquals(201, criado.statusCode());
        JsonNode dados = json(criado);
        long id = dados.get("id").asLong();
        assertTrue(id > 0);
        assertTrue(dados.get("protocolo").asInt() > 0);
        Atendimento persistido = repository.findById(id).orElseThrow();
        assertEquals(pet, persistido.getPetNome());
        assertEquals(porte, persistido.getPetPorte());
        assertEquals(tutor, persistido.getTutorNome());
        assertEquals(LocalDateTime.parse(horario), persistido.getDataHora());
        assertEquals("AGENDADO", persistido.getStatus());
        assertEquals(dados.get("protocolo").asInt(), persistido.getProtocolo());
        Class<? extends Atendimento> classeEsperada = switch (tipo) {
            case "BANHO" -> Banho.class;
            case "TOSA" -> Tosa.class;
            default -> ConsultaVeterinaria.class;
        };
        assertInstanceOf(classeEsperada, persistido);

        var carregado = chamar("GET", "/api/atendimentos/" + id);
        assertEquals(200, carregado.statusCode());
        assertEquals(dados, json(carregado));
        var porPet = chamar("GET", "/api/atendimentos/pet/" + codificar(pet));
        assertEquals(200, porPet.statusCode());
        assertEquals(1, json(porPet).size());
        assertEquals(dados, json(porPet).get(0));
        var resumo = chamar("GET", "/api/atendimentos/" + id + "/resumo");
        assertEquals(200, resumo.statusCode());
        assertEquals(tipo, json(resumo).get("tipo").asText());
        assertEquals(preco, json(resumo).get("preco").asDouble());
        assertEquals(pontos, json(resumo).get("pontosFidelidade").asInt());
        assertEquals(duracao, json(resumo).get("duracaoMinutos").asInt());
        assertEquals(1L, repository.count());
    }
}
