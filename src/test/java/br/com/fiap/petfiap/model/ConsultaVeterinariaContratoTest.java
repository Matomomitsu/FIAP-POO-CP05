package br.com.fiap.petfiap.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsultaVeterinariaContratoTest {

    @Test
    public void deveCobrar150ReaisQuandoConsultaTiverQualquerPorte() {
        // Arrange
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        ConsultaVeterinaria pequena = new ConsultaVeterinaria(1, "Rex", "PEQUENO", "Ana", horario);
        ConsultaVeterinaria media = new ConsultaVeterinaria(2, "Mimi", "MEDIO", "Bruno", horario);
        ConsultaVeterinaria grande = new ConsultaVeterinaria(3, "Thor", "GRANDE", "Clara", horario);

        // Act
        double precoPequeno = pequena.calcularPreco();
        double precoMedio = media.calcularPreco();
        double precoGrande = grande.calcularPreco();

        // Assert
        assertAll(
                () -> assertEquals(150.0, precoPequeno, 0.001),
                () -> assertEquals(150.0, precoMedio, 0.001),
                () -> assertEquals(150.0, precoGrande, 0.001));
    }
}
