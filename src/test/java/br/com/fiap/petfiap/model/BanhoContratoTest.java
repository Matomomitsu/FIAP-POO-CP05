package br.com.fiap.petfiap.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BanhoContratoTest {

    @Test
    public void deveCobrarPrecoDoBanhoQuandoPorteForPequenoMedioOuGrande() {
        // Arrange
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        Banho pequeno = new Banho(1, "Rex", "PEQUENO", "Ana", horario);
        Banho medio = new Banho(2, "Mimi", "MEDIO", "Bruno", horario);
        Banho grande = new Banho(3, "Thor", "GRANDE", "Clara", horario);

        // Act
        double precoPequeno = pequeno.calcularPreco();
        double precoMedio = medio.calcularPreco();
        double precoGrande = grande.calcularPreco();

        // Assert
        assertAll(
                () -> assertEquals(60.0, precoPequeno, 0.001),
                () -> assertEquals(80.0, precoMedio, 0.001),
                () -> assertEquals(100.0, precoGrande, 0.001));
    }
}
