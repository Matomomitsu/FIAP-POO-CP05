package br.com.fiap.petfiap.integration;

import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:petfiap-integration;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false"
})
abstract class ApiIntegrationSupport {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).version(HttpClient.Version.HTTP_1_1).build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected AtendimentoRepository repository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void limparSomenteBancoTemporario() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"),
                    "Testes de integracao devem usar exclusivamente H2 em memoria");
        }
        repository.deleteAll();
    }

    protected HttpResponse<String> chamar(String method, String path) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(8));
        if ("POST".equals(method)) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.GET();
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected static String agendamento(String tipo, String pet, String porte, String tutor, String data) {
        return "/api/atendimentos?tipo=" + codificar(tipo) + "&petNome=" + codificar(pet)
                + "&porte=" + codificar(porte) + "&tutorNome=" + codificar(tutor)
                + "&dataHora=" + codificar(data);
    }

    protected static String codificar(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    protected static String horarioFuturo() {
        return LocalDateTime.now().plusDays(2).withNano(0).toString();
    }

    protected static JsonNode json(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    protected List<HttpResponse<String>> simultaneamente(String primeiroPath, String segundoPath) throws Exception {
        CountDownLatch prontos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<HttpResponse<String>> primeiro = executor.submit(() -> {
                prontos.countDown();
                assertTrue(iniciar.await(3, TimeUnit.SECONDS));
                return chamar("POST", primeiroPath);
            });
            Future<HttpResponse<String>> segundo = executor.submit(() -> {
                prontos.countDown();
                assertTrue(iniciar.await(3, TimeUnit.SECONDS));
                return chamar("POST", segundoPath);
            });
            assertTrue(prontos.await(3, TimeUnit.SECONDS));
            iniciar.countDown();
            return List.of(primeiro.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
