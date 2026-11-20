package io.github.dadouneikram.relationalalgebra.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SelectTest {

  private static List<String> noms(Table table) {
    int index = table.columnIndex("name");
    return table.getRows().stream().map(row -> row.get(index)).collect(Collectors.toList());
  }

  private static List<String> selection(String predicat) {
    return noms(Select.fromPredicate(() -> Tables.USERS, predicat).execute());
  }

  @Test
  void inferieurNumerique() {
    assertEquals(List.of("Alice", "David"), selection("age < 25"));
  }

  @Test
  void superieurNumerique() {
    assertEquals(List.of("Bob", "Charlie"), selection("age > 25"));
  }

  @Test
  void egaliteNumerique() {
    assertEquals(List.of("Bob"), selection("age = 30"));
    assertEquals(List.of("Bob"), selection("age = 30.0"));
  }

  @Test
  void comparaisonNumeriqueEtNonAlphabetique() {
    Table table = new Table("t", List.of("name", "n"), List.of(List.of("a", "9"),
        List.of("b", "10")));
    Table resultat = Select.fromPredicate(() -> table, "n < 10").execute();
    assertEquals(List.of("a"), noms(resultat));
  }

  @Test
  void egaliteDeChaineEntreGuillemets() {
    assertEquals(List.of("Charlie"), selection("name = 'Charlie'"));
  }

  @Test
  void chaineAvecEspaces() {
    Table table = new Table("t", List.of("name", "city"), List.of(
        List.of("Alice", "New York"), List.of("Bob", "Paris")));
    Table resultat = Select.fromPredicate(() -> table, "city = 'New York'").execute();
    assertEquals(List.of("Alice"), noms(resultat));
  }

  @Test
  void colonneSansTenirCompteDesMajuscules() {
    assertEquals(List.of("Bob"), selection("AGE = 30"));
  }

  @Test
  void aucuneLigneNeCorrespond() {
    Table resultat = Select.fromPredicate(() -> Tables.USERS, "age > 100").execute();
    assertTrue(resultat.getRows().isEmpty());
    assertEquals(Tables.USERS.getColumns(), resultat.getColumns());
  }

  @Test
  void neModifiePasLaTableEntree() {
    Select.fromPredicate(() -> Tables.USERS, "age < 25").execute();
    assertEquals(4, Tables.USERS.getRows().size());
  }

  @Test
  void refusePredicatMalForme() {
    assertThrows(IllegalArgumentException.class,
        () -> Select.fromPredicate(() -> Tables.USERS, "age<25"));
    assertThrows(IllegalArgumentException.class,
        () -> Select.fromPredicate(() -> Tables.USERS, "age <"));
  }

  @Test
  void refuseComparateurInconnu() {
    assertThrows(IllegalArgumentException.class,
        () -> new Select(() -> Tables.USERS, "age", "!=", "25"));
  }

  @Test
  void refuseColonneInexistante() {
    Select select = Select.fromPredicate(() -> Tables.USERS, "email = 'x'");
    assertThrows(IllegalArgumentException.class, select::execute);
  }
}
