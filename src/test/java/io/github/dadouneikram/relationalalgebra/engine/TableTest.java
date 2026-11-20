package io.github.dadouneikram.relationalalgebra.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TableTest {

  @Test
  void accesseurs() {
    assertEquals("users", Tables.USERS.getName());
    assertEquals(List.of("id", "name", "age"), Tables.USERS.getColumns());
    assertEquals(4, Tables.USERS.getRows().size());
  }

  @Test
  void positionDeColonneSansTenirCompteDesMajuscules() {
    assertEquals(1, Tables.USERS.columnIndex("name"));
    assertEquals(1, Tables.USERS.columnIndex("NAME"));
  }

  @Test
  void colonneInexistante() {
    IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
        () -> Tables.USERS.columnIndex("email"));
    assertTrue(erreur.getMessage().contains("email"));
  }

  @Test
  void refuseLigneDeMauvaiseTaille() {
    List<List<String>> lignes = List.of(List.of("1", "Alice"));
    assertThrows(IllegalArgumentException.class,
        () -> new Table("t", List.of("id", "name", "age"), lignes));
  }

  @Test
  void estImmuable() {
    List<String> colonnes = new ArrayList<>(List.of("id"));
    Table table = new Table("t", colonnes, List.of(List.of("1")));
    colonnes.add("name");
    assertEquals(List.of("id"), table.getColumns());
    assertThrows(UnsupportedOperationException.class, () -> table.getRows().add(List.of("2")));
  }

  @Test
  void conversionEnCsv() {
    Table table = new Table("t", List.of("id", "city"), List.of(List.of("1", "Paris"),
        List.of("2", "Lyon")));
    assertEquals(List.of("id,city", "1,Paris", "2,Lyon"), table.toCsvLines());
  }

  @Test
  void conversionEnCsvTableSansLigne() {
    assertEquals(List.of("id"), new Table("t", List.of("id"), List.of()).toCsvLines());
  }
}
