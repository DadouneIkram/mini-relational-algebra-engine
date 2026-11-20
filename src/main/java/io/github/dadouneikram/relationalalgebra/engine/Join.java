package io.github.dadouneikram.relationalalgebra.engine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Opérateur JOIN : équi-jointure entre deux tables sur une colonne de chaque côté.
 *
 * <p>Le résultat contient toutes les colonnes de gauche puis toutes les colonnes de droite. Une
 * colonne présente des deux côtés est préfixée par le nom de sa table ({@code users.id},
 * {@code orders.id}) pour lever l'ambiguïté.
 *
 * <p>Les lignes de droite sont d'abord regroupées par valeur de la clé de jointure (jointure par
 * hachage) : chaque ligne de gauche retrouve ses correspondances directement, sans parcourir
 * toute la table de droite.
 */
public final class Join implements Operator {

  private final Operator left;
  private final Operator right;
  private final String leftColumn;
  private final String rightColumn;

  /**
   * Construit l'opérateur.
   *
   * @param left opérateur qui fournit la table de gauche
   * @param right opérateur qui fournit la table de droite
   * @param leftColumn colonne de jointure de la table de gauche
   * @param rightColumn colonne de jointure de la table de droite
   */
  public Join(Operator left, Operator right, String leftColumn, String rightColumn) {
    this.left = left;
    this.right = right;
    this.leftColumn = leftColumn;
    this.rightColumn = rightColumn;
  }

  @Override
  public Table execute() {
    Table leftTable = left.execute();
    Table rightTable = right.execute();
    int leftIndex = leftTable.columnIndex(leftColumn);
    int rightIndex = rightTable.columnIndex(rightColumn);

    Map<String, List<List<String>>> rightRowsByKey = rightTable.getRows().stream()
        .collect(Collectors.groupingBy(row -> row.get(rightIndex)));

    List<List<String>> rows = leftTable.getRows().stream()
        .flatMap(leftRow -> rightRowsByKey.getOrDefault(leftRow.get(leftIndex), List.of())
            .stream()
            .map(rightRow -> concat(leftRow, rightRow)))
        .collect(Collectors.toList());

    List<String> columns = concat(
        qualifiedColumns(leftTable, rightTable), qualifiedColumns(rightTable, leftTable));
    String name = leftTable.getName() + "_" + rightTable.getName();
    return new Table(name, columns, rows);
  }

  private static List<String> qualifiedColumns(Table table, Table other) {
    return table.getColumns().stream()
        .map(column -> isAmbiguous(column, other) ? table.getName() + "." + column : column)
        .collect(Collectors.toList());
  }

  private static boolean isAmbiguous(String column, Table other) {
    return other.getColumns().stream().anyMatch(column::equalsIgnoreCase);
  }

  private static List<String> concat(List<String> first, List<String> second) {
    return Stream.concat(first.stream(), second.stream()).collect(Collectors.toList());
  }
}
