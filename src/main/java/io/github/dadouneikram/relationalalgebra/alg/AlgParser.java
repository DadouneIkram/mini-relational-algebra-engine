package io.github.dadouneikram.relationalalgebra.alg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduit une requête écrite en notation algébrique ({@code .alg}) en requête JSON.
 *
 * <p>Exemple de fichier {@code .alg} :
 * <pre>
 * users: users.csv
 * orders: orders.csv
 *
 * R1 = SELECT users age &lt; 25
 * R = PROJ R1 name
 * </pre>
 *
 * <p>Les lignes {@code nom: fichier.csv} déclarent les tables. Chaque ligne
 * {@code VARIABLE = OPERATEUR arguments} définit une relation intermédiaire, réutilisable dans
 * les lignes suivantes. La dernière relation définie est le résultat de la requête. Les noms de
 * tables et de variables ne tiennent pas compte des majuscules.
 */
public final class AlgParser {

  private static final Pattern TOKEN = Pattern.compile("'[^']*'|\\S+");
  private static final Pattern TABLE_DECLARATION = Pattern.compile("(\\w+)\\s*:\\s*(\\S+)");
  private static final Pattern ASSIGNMENT = Pattern.compile("(\\w+)\\s*=\\s*(.+)");

  private final JsonObject tables = new JsonObject();
  private final Map<String, String> tableNames = new HashMap<>();
  private final Map<String, JsonObject> relations = new HashMap<>();

  private AlgParser() {
  }

  /**
   * Traduit les lignes d'un fichier {@code .alg}.
   *
   * @param lines contenu du fichier, ligne par ligne
   * @return la requête JSON, avec ses parties {@code tables} et {@code plan}
   * @throws IllegalArgumentException si une ligne est invalide ou si aucune relation n'est définie
   */
  public static JsonObject parse(List<String> lines) {
    return new AlgParser().parseLines(lines);
  }

  private JsonObject parseLines(List<String> lines) {
    JsonObject plan = null;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) {
        continue;
      }
      try {
        Matcher assignment = ASSIGNMENT.matcher(line);
        Matcher declaration = TABLE_DECLARATION.matcher(line);
        if (assignment.matches()) {
          plan = parseExpression(tokenize(assignment.group(2)));
          relations.put(key(assignment.group(1)), plan);
        } else if (declaration.matches()) {
          tables.addProperty(declaration.group(1), declaration.group(2));
          tableNames.put(key(declaration.group(1)), declaration.group(1));
        } else {
          throw new IllegalArgumentException("syntaxe non reconnue : " + line);
        }
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Ligne " + (i + 1) + " : " + e.getMessage(), e);
      }
    }
    if (plan == null) {
      throw new IllegalArgumentException("Aucune relation définie dans le fichier");
    }
    JsonObject query = new JsonObject();
    query.add("tables", tables);
    query.add("plan", plan);
    return query;
  }

  private static List<String> tokenize(String expression) {
    List<String> tokens = new ArrayList<>();
    Matcher matcher = TOKEN.matcher(expression);
    while (matcher.find()) {
      tokens.add(matcher.group());
    }
    return tokens;
  }

  private JsonObject parseExpression(List<String> tokens) {
    String operator = tokens.get(0).toUpperCase(Locale.ROOT);
    JsonObject node = new JsonObject();
    switch (operator) {
      case "SCAN":
        requireArguments(tokens, tokens.size() == 2, "SCAN table");
        return resolve(tokens.get(1));
      case "SELECT":
        requireArguments(tokens, tokens.size() == 5, "SELECT relation colonne comparateur valeur");
        node.addProperty("type", "SELECT");
        node.addProperty("predicate", String.join(" ", tokens.subList(2, 5)));
        node.add("input", resolve(tokens.get(1)));
        return node;
      case "PROJ":
      case "PROJECT":
        requireArguments(tokens, tokens.size() >= 3, "PROJ relation colonne [colonne ...]");
        JsonArray columns = new JsonArray();
        tokens.subList(2, tokens.size()).forEach(columns::add);
        node.addProperty("type", "PROJECT");
        node.add("columns", columns);
        node.add("input", resolve(tokens.get(1)));
        return node;
      case "JOIN":
        requireArguments(tokens, tokens.size() == 5,
            "JOIN relationGauche relationDroite colonneGauche colonneDroite");
        node.addProperty("type", "JOIN");
        node.add("left", resolve(tokens.get(1)));
        node.add("right", resolve(tokens.get(2)));
        node.addProperty("onLeft", tokens.get(3));
        node.addProperty("onRight", tokens.get(4));
        return node;
      default:
        throw new IllegalArgumentException("opérateur inconnu : " + tokens.get(0));
    }
  }

  private static void requireArguments(List<String> tokens, boolean valid, String usage) {
    if (!valid) {
      throw new IllegalArgumentException(
          "arguments invalides pour " + tokens.get(0) + " (attendu : " + usage + ")");
    }
  }

  /** Une relation est soit une variable déjà définie, soit une table déclarée (lue par SCAN). */
  private JsonObject resolve(String name) {
    JsonObject relation = relations.get(key(name));
    if (relation != null) {
      return relation.deepCopy();
    }
    String table = tableNames.get(key(name));
    if (table == null) {
      throw new IllegalArgumentException("relation inconnue : " + name);
    }
    JsonObject scan = new JsonObject();
    scan.addProperty("type", "SCAN");
    scan.addProperty("table", table);
    return scan;
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
