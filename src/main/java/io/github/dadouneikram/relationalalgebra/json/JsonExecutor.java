package io.github.dadouneikram.relationalalgebra.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.dadouneikram.relationalalgebra.engine.Join;
import io.github.dadouneikram.relationalalgebra.engine.Operator;
import io.github.dadouneikram.relationalalgebra.engine.Project;
import io.github.dadouneikram.relationalalgebra.engine.Scan;
import io.github.dadouneikram.relationalalgebra.engine.Select;
import io.github.dadouneikram.relationalalgebra.engine.Table;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Exécute une requête décrite en JSON.
 *
 * <p>Une requête contient deux parties : {@code tables}, qui associe un nom de table à un fichier
 * CSV, et {@code plan}, un arbre d'opérateurs (SCAN, SELECT, PROJECT, JOIN). L'exécution se fait
 * en deux temps :
 * <ol>
 *   <li>l'arbre JSON est traduit en un arbre d'{@link Operator}, sans lire aucune donnée ;</li>
 *   <li>la racine de cet arbre est évaluée, ce qui évalue récursivement ses entrées.</li>
 * </ol>
 */
public final class JsonExecutor {

  private JsonExecutor() {
  }

  /**
   * Lit et exécute un fichier de requête. Les fichiers CSV sont cherchés dans le dossier de la
   * requête.
   *
   * @param queryFile fichier JSON de la requête
   * @return la table résultat
   * @throws IOException si le fichier de requête ne peut pas être lu
   * @throws IllegalArgumentException si la requête est invalide
   */
  public static Table execute(Path queryFile) throws IOException {
    JsonObject query = read(queryFile);
    Path baseDirectory = queryFile.toAbsolutePath().getParent();
    return toOperator(requireObject(query, "plan"), tableFiles(query, baseDirectory)).execute();
  }

  static JsonObject read(Path queryFile) throws IOException {
    try (Reader reader = Files.newBufferedReader(queryFile, StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) {
        throw new IllegalArgumentException("La requête doit être un objet JSON : " + queryFile);
      }
      return root.getAsJsonObject();
    } catch (JsonParseException e) {
      throw new IllegalArgumentException("JSON invalide dans " + queryFile, e);
    }
  }

  /**
   * Associe chaque nom de table (en minuscules) au chemin de son fichier CSV.
   *
   * @param query requête complète
   * @param baseDirectory dossier par rapport auquel les chemins sont résolus
   * @return la table de correspondance nom vers fichier
   */
  static Map<String, Path> tableFiles(JsonObject query, Path baseDirectory) {
    return requireObject(query, "tables").entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().toLowerCase(Locale.ROOT),
            entry -> baseDirectory.resolve(
                asString(entry.getValue(), "tables." + entry.getKey()))));
  }

  /**
   * Traduit un nœud JSON du plan en opérateur. Aucune donnée n'est lue à cette étape.
   *
   * @param node nœud du plan
   * @param tables fichiers CSV par nom de table (en minuscules)
   * @return l'opérateur correspondant au nœud et à ses descendants
   * @throws IllegalArgumentException si le nœud est invalide
   */
  static Operator toOperator(JsonObject node, Map<String, Path> tables) {
    String type = requireString(node, "type").toUpperCase(Locale.ROOT);
    switch (type) {
      case "SCAN":
        String table = requireString(node, "table");
        Path file = tables.get(table.toLowerCase(Locale.ROOT));
        if (file == null) {
          throw new IllegalArgumentException("Table non déclarée dans \"tables\" : " + table);
        }
        return new Scan(file, table);
      case "SELECT":
        return Select.fromPredicate(
            toOperator(requireObject(node, "input"), tables), requireString(node, "predicate"));
      case "PROJECT":
      case "PROJ":
        return new Project(
            toOperator(requireObject(node, "input"), tables), requireStrings(node, "columns"));
      case "JOIN":
        return new Join(
            toOperator(requireObject(node, "left"), tables),
            toOperator(requireObject(node, "right"), tables),
            requireString(node, "onLeft"),
            requireString(node, "onRight"));
      default:
        throw new IllegalArgumentException("Type d'opérateur inconnu : " + type);
    }
  }

  private static JsonElement require(JsonObject node, String field) {
    JsonElement element = node.get(field);
    if (element == null || element.isJsonNull()) {
      throw new IllegalArgumentException("Champ \"" + field + "\" manquant dans " + node);
    }
    return element;
  }

  private static JsonObject requireObject(JsonObject node, String field) {
    JsonElement element = require(node, field);
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("Le champ \"" + field + "\" doit être un objet JSON");
    }
    return element.getAsJsonObject();
  }

  private static String requireString(JsonObject node, String field) {
    return asString(require(node, field), field);
  }

  private static List<String> requireStrings(JsonObject node, String field) {
    JsonElement element = require(node, field);
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException("Le champ \"" + field + "\" doit être un tableau JSON");
    }
    JsonArray array = element.getAsJsonArray();
    return StreamSupport.stream(array.spliterator(), false)
        .map(item -> asString(item, field))
        .collect(Collectors.toList());
  }

  private static String asString(JsonElement element, String field) {
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException("Le champ \"" + field + "\" doit contenir du texte");
    }
    return element.getAsString();
  }
}
