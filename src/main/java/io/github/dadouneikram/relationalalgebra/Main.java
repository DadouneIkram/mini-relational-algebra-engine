package io.github.dadouneikram.relationalalgebra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.dadouneikram.relationalalgebra.alg.AlgParser;
import io.github.dadouneikram.relationalalgebra.engine.Table;
import io.github.dadouneikram.relationalalgebra.json.JsonExecutor;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Point d'entrée en ligne de commande.
 *
 * <ul>
 *   <li>{@code requete.alg} : traduit la requête en {@code requete.json}, sans l'exécuter ;</li>
 *   <li>{@code requete.json} : exécute la requête et écrit {@code results/output.csv}.</li>
 * </ul>
 */
public final class Main {

  static final String USAGE = "Usage : java -jar mini-relational-algebra-engine.jar "
      + "<requete.alg | requete.json>";

  /** Gson échappe par défaut les caractères comme {@code <} ; on les garde lisibles. */
  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  private Main() {
  }

  /**
   * Lance le programme. Le code de sortie vaut 0 en cas de succès, 1 en cas d'erreur.
   *
   * @param args un seul argument : le fichier de requête
   */
  public static void main(String[] args) {
    PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
    int code = run(args, out, err);
    if (code != 0) {
      System.exit(code);
    }
  }

  /**
   * Traite les arguments et retourne le code de sortie, sans arrêter la machine virtuelle.
   *
   * @param args arguments de la ligne de commande
   * @param out flux des messages normaux
   * @param err flux des messages d'erreur
   * @return 0 en cas de succès, 1 en cas d'erreur
   */
  static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length != 1) {
      err.println(USAGE);
      return 1;
    }
    Path file = Paths.get(args[0]);
    String name = file.getFileName().toString();
    try {
      if (!Files.isRegularFile(file)) {
        throw new IllegalArgumentException("Fichier introuvable : " + file);
      }
      if (name.endsWith(".alg")) {
        out.println("Traduction en " + translate(file).getFileName());
      } else if (name.endsWith(".json")) {
        execute(file);
        out.println("Résultat écrit dans results/output.csv");
      } else {
        throw new IllegalArgumentException(
            "Extension non reconnue (attendu : .alg ou .json) : " + name);
      }
      return 0;
    } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
      err.println("Erreur : " + e.getMessage());
      return 1;
    }
  }

  /**
   * Traduit un fichier {@code .alg} en fichier {@code .json} placé à côté.
   *
   * @param algFile fichier à traduire
   * @return le chemin du fichier JSON écrit
   * @throws IOException en cas d'erreur de lecture ou d'écriture
   */
  static Path translate(Path algFile) throws IOException {
    JsonObject query = AlgParser.parse(Files.readAllLines(algFile, StandardCharsets.UTF_8));
    String name = algFile.getFileName().toString();
    Path jsonFile = algFile.resolveSibling(name.substring(0, name.length() - ".alg".length())
        + ".json");
    Files.writeString(jsonFile, GSON.toJson(query) + "\n", StandardCharsets.UTF_8);
    return jsonFile;
  }

  /**
   * Exécute une requête JSON et écrit le résultat dans {@code results/output.csv}, à côté de la
   * requête.
   *
   * @param queryFile fichier JSON de la requête
   * @return le chemin du fichier CSV écrit
   * @throws IOException en cas d'erreur de lecture ou d'écriture
   */
  static Path execute(Path queryFile) throws IOException {
    Table result = JsonExecutor.execute(queryFile);
    Path resultsDirectory = queryFile.toAbsolutePath().getParent().resolve("results");
    Files.createDirectories(resultsDirectory);
    Path output = resultsDirectory.resolve("output.csv");
    Files.writeString(output, String.join("\n", result.toCsvLines()) + "\n",
        StandardCharsets.UTF_8);
    return output;
  }
}
