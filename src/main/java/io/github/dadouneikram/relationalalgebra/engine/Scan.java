package io.github.dadouneikram.relationalalgebra.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Opérateur SCAN : lit un fichier CSV et le transforme en table.
 *
 * <p>La première ligne non vide est l'en-tête. Les valeurs sont séparées par des virgules et les
 * espaces autour de chaque valeur sont supprimés. Les lignes vides sont ignorées.
 */
public final class Scan implements Operator {

  private final Path file;
  private final String tableName;

  /**
   * Construit l'opérateur. Le fichier n'est lu qu'à l'appel de {@link #execute()}.
   *
   * @param file chemin du fichier CSV
   * @param tableName nom donné à la table produite
   */
  public Scan(Path file, String tableName) {
    this.file = file;
    this.tableName = tableName;
  }

  @Override
  public Table execute() {
    List<List<String>> lines = readLines().stream()
        .filter(line -> !line.isBlank())
        .map(Scan::splitLine)
        .collect(Collectors.toList());
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("Fichier CSV vide : " + file);
    }
    try {
      return new Table(tableName, lines.get(0), lines.subList(1, lines.size()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Fichier CSV invalide " + file + " : " + e.getMessage(), e);
    }
  }

  private List<String> readLines() {
    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Impossible de lire le fichier CSV " + file, e);
    }
  }

  /** Découpe une ligne CSV. Le -1 conserve les valeurs vides en fin de ligne ("a,b," donne 3). */
  static List<String> splitLine(String line) {
    return Arrays.stream(line.split(",", -1)).map(String::trim).collect(Collectors.toList());
  }
}
