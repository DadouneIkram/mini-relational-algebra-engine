package io.github.dadouneikram.relationalalgebra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests de bout en bout : fichiers sur disque, ligne de commande, fichiers produits. */
class MainTest {

  @TempDir
  Path dossier;

  private final ByteArrayOutputStream sortie = new ByteArrayOutputStream();
  private final ByteArrayOutputStream erreurs = new ByteArrayOutputStream();

  @BeforeEach
  void creerLesTables() throws IOException {
    Files.writeString(dossier.resolve("users.csv"),
        "id,name,age\n1,Alice,22\n2,Bob,30\n3,Charlie,27\n4,David,19\n");
    Files.writeString(dossier.resolve("orders.csv"),
        "id,user_id,amount\n101,1,250\n102,2,80\n103,1,40\n104,4,120\n");
  }

  private int lancer(String... arguments) {
    return Main.run(arguments, new PrintStream(sortie, true, StandardCharsets.UTF_8),
        new PrintStream(erreurs, true, StandardCharsets.UTF_8));
  }

  private Path ecrire(String nom, String contenu) throws IOException {
    Path fichier = dossier.resolve(nom);
    Files.writeString(fichier, contenu);
    return fichier;
  }

  private String resultat() throws IOException {
    return Files.readString(dossier.resolve("results").resolve("output.csv"));
  }

  @Test
  void traduitPuisExecuteUneSelection() throws IOException {
    Path alg = ecrire("reqProjSel.alg",
        "users: users.csv\norders: orders.csv\n\nR1 = SELECT users age < 25\nR = PROJ R1 name\n");

    assertEquals(0, lancer(alg.toString()));
    assertTrue(sortie.toString(StandardCharsets.UTF_8).contains("Traduction en reqProjSel.json"));
    String json = Files.readString(dossier.resolve("reqProjSel.json"));
    assertTrue(json.contains("\"predicate\": \"age < 25\""), "pas d'échappement de <");

    assertEquals(0, lancer(dossier.resolve("reqProjSel.json").toString()));
    assertTrue(sortie.toString(StandardCharsets.UTF_8)
        .contains("Résultat écrit dans results/output.csv"));
    assertEquals("name\nAlice\nDavid\n", resultat());
  }

  @Test
  void traduitPuisExecuteUneJointure() throws IOException {
    Path alg = ecrire("optJoin.alg", "users: users.csv\norders: orders.csv\n\n"
        + "R1 = PROJ users id name\nR2 = JOIN R1 orders id user_id\nR = PROJ R2 name amount\n");

    assertEquals(0, lancer(alg.toString()));
    assertEquals(0, lancer(dossier.resolve("optJoin.json").toString()));
    assertEquals("name,amount\nAlice,250\nAlice,40\nBob,80\nDavid,120\n", resultat());
  }

  @Test
  void traductionSansExecution() throws IOException {
    Path alg = ecrire("q.alg", "users: users.csv\nR = PROJ users name\n");
    assertEquals(0, lancer(alg.toString()));
    assertFalse(Files.exists(dossier.resolve("results")));
  }

  @Test
  void sansArgument() {
    assertEquals(1, lancer());
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("Usage"));
  }

  @Test
  void fichierIntrouvable() {
    assertEquals(1, lancer(dossier.resolve("absent.json").toString()));
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("Fichier introuvable"));
  }

  @Test
  void extensionNonReconnue() throws IOException {
    assertEquals(1, lancer(ecrire("requete.txt", "").toString()));
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("Extension non reconnue"));
  }

  @Test
  void erreurDeTraduction() throws IOException {
    assertEquals(1, lancer(ecrire("vide.alg", "users: users.csv\n").toString()));
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("Aucune relation"));
    assertFalse(Files.exists(dossier.resolve("vide.json")));
  }

  @Test
  void erreurExecutionSansMessageDeSucces() throws IOException {
    Path json = ecrire("erreur.json", "{\"tables\": {\"users\": \"users.csv\"}, "
        + "\"plan\": {\"type\": \"PROJECT\", \"columns\": [\"email\"], "
        + "\"input\": {\"type\": \"SCAN\", \"table\": \"users\"}}}");
    assertEquals(1, lancer(json.toString()));
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("Colonne inexistante : email"));
    assertFalse(sortie.toString(StandardCharsets.UTF_8).contains("Résultat écrit"));
  }

  @Test
  void erreurDeLectureCsv() throws IOException {
    Path json = ecrire("absent.json", "{\"tables\": {\"t\": \"absent.csv\"}, "
        + "\"plan\": {\"type\": \"SCAN\", \"table\": \"t\"}}");
    assertEquals(1, lancer(json.toString()));
    assertTrue(erreurs.toString(StandardCharsets.UTF_8).contains("absent.csv"));
  }
}
