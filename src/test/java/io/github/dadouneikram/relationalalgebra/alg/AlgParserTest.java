package io.github.dadouneikram.relationalalgebra.alg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgParserTest {

  private static final List<String> TABLES = List.of("users: users.csv", "orders: orders.csv", "");

  private static JsonObject parse(String... lignes) {
    List<String> fichier = new java.util.ArrayList<>(TABLES);
    fichier.addAll(List.of(lignes));
    return AlgParser.parse(fichier);
  }

  private static JsonObject json(String texte) {
    return JsonParser.parseString(texte).getAsJsonObject();
  }

  @Test
  void declareLesTables() {
    JsonObject requete = parse("R = SCAN users");
    assertEquals(json("{\"users\":\"users.csv\",\"orders\":\"orders.csv\"}"),
        requete.get("tables"));
  }

  @Test
  void selectionPuisProjection() {
    JsonObject requete = parse("R1 = SELECT users age < 25", "R = PROJ R1 name");
    assertEquals(json("{\"type\":\"PROJECT\",\"columns\":[\"name\"],\"input\":"
        + "{\"type\":\"SELECT\",\"predicate\":\"age < 25\",\"input\":"
        + "{\"type\":\"SCAN\",\"table\":\"users\"}}}"), requete.get("plan"));
  }

  @Test
  void jointure() {
    JsonObject requete = parse("R1 = PROJ users id name", "R2 = JOIN R1 orders id user_id",
        "R = PROJ R2 name amount");
    assertEquals(json("{\"type\":\"PROJECT\",\"columns\":[\"name\",\"amount\"],\"input\":"
        + "{\"type\":\"JOIN\","
        + "\"left\":{\"type\":\"PROJECT\",\"columns\":[\"id\",\"name\"],"
        + "\"input\":{\"type\":\"SCAN\",\"table\":\"users\"}},"
        + "\"right\":{\"type\":\"SCAN\",\"table\":\"orders\"},"
        + "\"onLeft\":\"id\",\"onRight\":\"user_id\"}}"), requete.get("plan"));
  }

  @Test
  void conserveLesChainesEntreGuillemets() {
    JsonObject plan = parse("R = SELECT users city = 'New York'").getAsJsonObject("plan");
    assertEquals("city = 'New York'", plan.get("predicate").getAsString());
  }

  @Test
  void laDerniereRelationEstLeResultat() {
    JsonObject plan = parse("R1 = SELECT users age < 25", "R2 = SCAN orders")
        .getAsJsonObject("plan");
    assertEquals("SCAN", plan.get("type").getAsString());
    assertEquals("orders", plan.get("table").getAsString());
  }

  @Test
  void insensibleAuxMajuscules() {
    JsonObject plan = parse("r1 = select USERS age < 25", "R = proj R1 name")
        .getAsJsonObject("plan");
    assertEquals("PROJECT", plan.get("type").getAsString());
    JsonObject scan = plan.getAsJsonObject("input").getAsJsonObject("input");
    assertEquals("users", scan.get("table").getAsString());
  }

  @Test
  void accepteProjectCommeSynonymeDeProj() {
    assertEquals("PROJECT",
        parse("R = PROJECT users name").getAsJsonObject("plan").get("type").getAsString());
  }

  @Test
  void refuseRelationInconnue() {
    IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
        () -> parse("R = PROJ clients name"));
    assertTrue(erreur.getMessage().contains("Ligne 4"));
    assertTrue(erreur.getMessage().contains("clients"));
  }

  @Test
  void refuseOperateurInconnu() {
    assertThrows(IllegalArgumentException.class, () -> parse("R = UNION users orders"));
  }

  @Test
  void refuseNombreArgumentsIncorrect() {
    String[] invalides = {"R = SELECT users age <", "R = SELECT users age < 25 30",
        "R = PROJ users", "R = JOIN users orders id", "R = SCAN"};
    for (String invalide : invalides) {
      assertThrows(IllegalArgumentException.class, () -> parse(invalide), invalide);
    }
  }

  @Test
  void refuseLigneIncomprehensible() {
    assertThrows(IllegalArgumentException.class, () -> parse("ceci n'est pas une requête"));
  }

  @Test
  void refuseFichierSansRelation() {
    assertThrows(IllegalArgumentException.class, () -> AlgParser.parse(TABLES));
    assertThrows(IllegalArgumentException.class, () -> AlgParser.parse(List.of()));
  }
}
