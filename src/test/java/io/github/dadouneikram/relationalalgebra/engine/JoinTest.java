package io.github.dadouneikram.relationalalgebra.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JoinTest {

  private final Table resultat =
      new Join(() -> Tables.USERS, () -> Tables.ORDERS, "id", "user_id").execute();

  @Test
  void prefixeLesColonnesAmbigues() {
    assertEquals(List.of("users.id", "name", "age", "orders.id", "user_id", "amount"),
        resultat.getColumns());
    assertEquals("users_orders", resultat.getName());
  }

  @Test
  void associeChaqueLigneAuxLignesCorrespondantes() {
    assertEquals(List.of(
        List.of("1", "Alice", "22", "101", "1", "250"),
        List.of("1", "Alice", "22", "103", "1", "40"),
        List.of("2", "Bob", "30", "102", "2", "80"),
        List.of("4", "David", "19", "104", "4", "120")), resultat.getRows());
  }

  @Test
  void ignoreLesLignesSansCorrespondance() {
    assertTrue(resultat.getRows().stream().noneMatch(row -> row.get(1).equals("Charlie")));
  }

  @Test
  void colonnesSansAmbiguiteNonPrefixees() {
    Table villes = new Table("cities", List.of("city_id", "city"), List.of(List.of("1", "Paris")));
    Table personnes = new Table("people", List.of("name", "city_ref"),
        List.of(List.of("Alice", "1")));
    Table jointure = new Join(() -> personnes, () -> villes, "city_ref", "city_id").execute();
    assertEquals(List.of("name", "city_ref", "city_id", "city"), jointure.getColumns());
  }

  @Test
  void tableVide() {
    Table vide = new Table("orders", Tables.ORDERS.getColumns(), List.of());
    assertTrue(new Join(() -> Tables.USERS, () -> vide, "id", "user_id").execute().getRows()
        .isEmpty());
    assertTrue(new Join(() -> vide, () -> Tables.USERS, "user_id", "id").execute().getRows()
        .isEmpty());
  }

  @Test
  void accepteDesOperateursComposes() {
    Operator jeunes = Select.fromPredicate(() -> Tables.USERS, "age < 25");
    Table jointure = new Join(jeunes, () -> Tables.ORDERS, "id", "user_id").execute();
    assertEquals(3, jointure.getRows().size());
  }

  @Test
  void refuseColonneInexistante() {
    Join join = new Join(() -> Tables.USERS, () -> Tables.ORDERS, "email", "user_id");
    assertThrows(IllegalArgumentException.class, join::execute);
  }
}
