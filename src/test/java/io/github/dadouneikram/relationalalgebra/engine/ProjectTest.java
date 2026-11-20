package io.github.dadouneikram.relationalalgebra.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTest {

  @Test
  void garderUneColonne() {
    Table resultat = new Project(() -> Tables.USERS, List.of("name")).execute();
    assertEquals(List.of("name"), resultat.getColumns());
    assertEquals(List.of(List.of("Alice"), List.of("Bob"), List.of("Charlie"), List.of("David")),
        resultat.getRows());
  }

  @Test
  void respecteOrdreDemande() {
    Table resultat = new Project(() -> Tables.USERS, List.of("age", "id")).execute();
    assertEquals(List.of("age", "id"), resultat.getColumns());
    assertEquals(List.of("22", "1"), resultat.getRows().get(0));
  }

  @Test
  void conserveLeNomOriginalDesColonnes() {
    Table resultat = new Project(() -> Tables.USERS, List.of("NAME")).execute();
    assertEquals(List.of("name"), resultat.getColumns());
  }

  @Test
  void conserveLesDoublons() {
    Table table = new Table("t", List.of("city"), List.of(List.of("Paris"), List.of("Paris")));
    assertEquals(2, new Project(() -> table, List.of("city")).execute().getRows().size());
  }

  @Test
  void refuseListeVide() {
    assertThrows(IllegalArgumentException.class, () -> new Project(() -> Tables.USERS, List.of()));
  }

  @Test
  void refuseColonneInexistante() {
    Project project = new Project(() -> Tables.USERS, List.of("email"));
    assertThrows(IllegalArgumentException.class, project::execute);
  }
}
