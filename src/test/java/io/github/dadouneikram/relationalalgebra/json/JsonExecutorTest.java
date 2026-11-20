package io.github.dadouneikram.relationalalgebra.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dadouneikram.relationalalgebra.engine.Operator;
import io.github.dadouneikram.relationalalgebra.engine.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonExecutorTest {

  private static final String TABLES =
      "\"tables\": {\"users\": \"users.csv\", \"orders\": \"orders.csv\"}";

  @TempDir
  Path dossier;

  @BeforeEach
  void creerLesTables() throws IOException {
    Files.writeString(dossier.resolve("users.csv"),
        "id,name,age\n1,Alice,22\n2,Bob,30\n3,Charlie,27\n4,David,19\n");
    Files.writeString(dossier.resolve("orders.csv"),
        "id,user_id,amount\n101,1,250\n102,2,80\n103,1,40\n104,4,120\n");
  }

  private Table executer(String plan) throws IOException {
    Path requete = dossier.resolve("requete.json");
    Files.writeString(requete, "{" + TABLES + ", \"plan\": " + plan + "}");
    return JsonExecutor.execute(requete);
  }

  @Test
  void scan() throws IOException {
    assertEquals(4, executer("{\"type\": \"SCAN\", \"table\": \"users\"}").getRows().size());
  }

  @Test
  void projectionApresSelection() throws IOException {
    Table resultat = executer("{\"type\": \"PROJECT\", \"columns\": [\"name\"], \"input\": "
        + "{\"type\": \"SELECT\", \"predicate\": \"age < 25\", \"input\": "
        + "{\"type\": \"SCAN\", \"table\": \"users\"}}}");
    assertEquals(List.of("name", "Alice", "David"), resultat.toCsvLines());
  }

  @Test
  void jointure() throws IOException {
    Table resultat = executer("{\"type\": \"PROJECT\", \"columns\": [\"name\", \"amount\"], "
        + "\"input\": {\"type\": \"JOIN\", "
        + "\"left\": {\"type\": \"SCAN\", \"table\": \"users\"}, "
        + "\"right\": {\"type\": \"SCAN\", \"table\": \"orders\"}, "
        + "\"onLeft\": \"id\", \"onRight\": \"user_id\"}}");
    assertEquals(List.of("name,amount", "Alice,250", "Alice,40", "Bob,80", "David,120"),
        resultat.toCsvLines());
  }

  @Test
  void accepteProjCommeSynonymeEtTypeEnMinuscules() throws IOException {
    Table resultat = executer("{\"type\": \"proj\", \"columns\": [\"id\"], \"input\": "
        + "{\"type\": \"scan\", \"table\": \"USERS\"}}");
    assertEquals(List.of("id"), resultat.getColumns());
  }

  @Test
  void construireLePlanNeLitAucunFichier() throws IOException {
    Path requete = dossier.resolve("absent.json");
    Files.writeString(requete, "{\"tables\": {\"t\": \"absent.csv\"}, "
        + "\"plan\": {\"type\": \"SCAN\", \"table\": \"t\"}}");
    var query = JsonExecutor.read(requete);
    Operator operateur = JsonExecutor.toOperator(query.getAsJsonObject("plan"),
        JsonExecutor.tableFiles(query, dossier));
    assertThrows(java.io.UncheckedIOException.class, operateur::execute);
  }

  @Test
  void refuseTableNonDeclaree() {
    assertThrows(IllegalArgumentException.class,
        () -> executer("{\"type\": \"SCAN\", \"table\": \"clients\"}"));
  }

  @Test
  void refuseTypeInconnu() {
    assertThrows(IllegalArgumentException.class,
        () -> executer("{\"type\": \"UNION\", \"table\": \"users\"}"));
  }

  @Test
  void refuseChampsManquantsOuMalTypes() {
    String[] invalides = {
      "{\"table\": \"users\"}",
      "{\"type\": \"SELECT\", \"input\": {\"type\": \"SCAN\", \"table\": \"users\"}}",
      "{\"type\": \"PROJECT\", \"columns\": \"name\", "
          + "\"input\": {\"type\": \"SCAN\", \"table\": \"users\"}}",
      "{\"type\": \"PROJECT\", \"columns\": [1], "
          + "\"input\": {\"type\": \"SCAN\", \"table\": \"users\"}}",
      "{\"type\": \"SELECT\", \"predicate\": \"age < 25\", \"input\": \"users\"}",
      "{\"type\": \"JOIN\", \"left\": {\"type\": \"SCAN\", \"table\": \"users\"}}",
      "null"
    };
    for (String invalide : invalides) {
      assertThrows(IllegalArgumentException.class, () -> executer(invalide), invalide);
    }
  }

  @Test
  void refuseJsonInvalide() throws IOException {
    Path requete = dossier.resolve("invalide.json");
    Files.writeString(requete, "{\"tables\": ");
    assertThrows(IllegalArgumentException.class, () -> JsonExecutor.execute(requete));
    Files.writeString(requete, "[1, 2]");
    assertThrows(IllegalArgumentException.class, () -> JsonExecutor.execute(requete));
  }

  @Test
  void refuseRequeteSansTables() throws IOException {
    Path requete = dossier.resolve("sans-tables.json");
    Files.writeString(requete, "{\"plan\": {\"type\": \"SCAN\", \"table\": \"users\"}}");
    assertThrows(IllegalArgumentException.class, () -> JsonExecutor.execute(requete));
  }
}
