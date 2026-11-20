package io.github.dadouneikram.relationalalgebra.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Table immuable : un nom, une liste de colonnes et une liste de lignes.
 *
 * <p>Chaque ligne est une liste de valeurs, dans l'ordre des colonnes. Le nombre de colonnes
 * n'est pas fixé à l'avance : il est lu dans l'en-tête du fichier CSV.
 */
public final class Table {

  private final String name;
  private final List<String> columns;
  private final List<List<String>> rows;

  /**
   * Construit une table. Les listes sont copiées : la table ne peut plus être modifiée.
   *
   * @param name nom de la table, utilisé pour préfixer les colonnes ambiguës d'une jointure
   * @param columns noms des colonnes
   * @param rows lignes, chacune ayant autant de valeurs que de colonnes
   * @throws IllegalArgumentException si une ligne n'a pas le bon nombre de valeurs
   */
  public Table(String name, List<String> columns, List<List<String>> rows) {
    this.name = Objects.requireNonNull(name, "name");
    List<String> copiedColumns = List.copyOf(columns);
    this.columns = copiedColumns;
    this.rows = IntStream.range(0, rows.size())
        .mapToObj(i -> checkedRow(rows.get(i), i, copiedColumns.size()))
        .collect(Collectors.toUnmodifiableList());
  }

  private static List<String> checkedRow(List<String> row, int index, int expectedSize) {
    if (row.size() != expectedSize) {
      throw new IllegalArgumentException("La ligne " + (index + 1) + " contient " + row.size()
          + " valeurs au lieu de " + expectedSize);
    }
    return List.copyOf(row);
  }

  public String getName() {
    return name;
  }

  public List<String> getColumns() {
    return columns;
  }

  public List<List<String>> getRows() {
    return rows;
  }

  /**
   * Retourne la position d'une colonne, sans tenir compte des majuscules.
   *
   * @param column nom de la colonne
   * @return position de la colonne, à partir de 0
   * @throws IllegalArgumentException si la colonne n'existe pas
   */
  public int columnIndex(String column) {
    return IntStream.range(0, columns.size())
        .filter(i -> columns.get(i).equalsIgnoreCase(column))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Colonne inexistante : " + column + " (colonnes disponibles : " + columns + ")"));
  }

  /**
   * Convertit la table en lignes CSV : l'en-tête puis une ligne par enregistrement.
   *
   * @return les lignes du fichier CSV, sans espace autour des valeurs
   */
  public List<String> toCsvLines() {
    List<String> lines = new ArrayList<>();
    lines.add(String.join(",", columns));
    rows.stream().map(row -> String.join(",", row)).forEach(lines::add);
    return lines;
  }
}
