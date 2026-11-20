package io.github.dadouneikram.relationalalgebra.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanTest {

  @TempDir
  Path dossier;

  private Table scan(String contenu) throws IOException {
    Path fichier = dossier.resolve("data.csv");
    Files.writeString(fichier, contenu);
    return new Scan(fichier, "data").execute();
  }

  @Test
  void litEnTeteEtLignes() throws IOException {
    Table table = scan("id,name,age\n1,Alice,22\n2,Bob,30\n");
    assertEquals("data", table.getName());
    assertEquals(List.of("id", "name", "age"), table.getColumns());
    assertEquals(List.of(List.of("1", "Alice", "22"), List.of("2", "Bob", "30")),
        table.getRows());
  }

  @Test
  void supprimeLesEspacesAutourDesValeurs() throws IOException {
    Table table = scan(" id , name \n 1 , Alice \n");
    assertEquals(List.of("id", "name"), table.getColumns());
    assertEquals(List.of("1", "Alice"), table.getRows().get(0));
  }

  @Test
  void ignoreLesLignesVides() throws IOException {
    assertEquals(2, scan("id\n\n1\n   \n2\n\n").getRows().size());
  }

  @Test
  void conserveLesValeursVides() throws IOException {
    Table table = scan("id,name,city\n1,,Paris\n2,Bob,\n");
    assertEquals(List.of("1", "", "Paris"), table.getRows().get(0));
    assertEquals(List.of("2", "Bob", ""), table.getRows().get(1));
  }

  @Test
  void accepteLesFinsDeLigneWindows() throws IOException {
    assertEquals(List.of("1", "Alice"), scan("id,name\r\n1,Alice\r\n").getRows().get(0));
  }

  @Test
  void tableSansLigne() throws IOException {
    assertTrue(scan("id,name\n").getRows().isEmpty());
  }

  @Test
  void refuseFichierVide() {
    assertThrows(IllegalArgumentException.class, () -> scan(""));
  }

  @Test
  void refuseLigneDeMauvaiseTaille() {
    IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
        () -> scan("id,name\n1,Alice,22\n"));
    assertTrue(erreur.getMessage().contains("data.csv"));
  }

  @Test
  void fichierInexistant() {
    Scan scan = new Scan(dossier.resolve("absent.csv"), "absent");
    assertThrows(UncheckedIOException.class, scan::execute);
  }
}
