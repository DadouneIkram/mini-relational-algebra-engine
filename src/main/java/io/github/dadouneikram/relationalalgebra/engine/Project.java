package io.github.dadouneikram.relationalalgebra.engine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Opérateur PROJECT : ne garde que certaines colonnes, dans l'ordre demandé.
 *
 * <p>Comme en SQL sans {@code DISTINCT}, les lignes en double ne sont pas supprimées.
 */
public final class Project implements Operator {

  private final Operator input;
  private final List<String> columns;

  /**
   * Construit l'opérateur.
   *
   * @param input opérateur qui fournit la table d'entrée
   * @param columns colonnes à conserver (au moins une)
   * @throws IllegalArgumentException si la liste de colonnes est vide
   */
  public Project(Operator input, List<String> columns) {
    if (columns.isEmpty()) {
      throw new IllegalArgumentException("Une projection doit garder au moins une colonne");
    }
    this.input = input;
    this.columns = List.copyOf(columns);
  }

  @Override
  public Table execute() {
    Table table = input.execute();
    List<Integer> indexes = columns.stream()
        .map(table::columnIndex)
        .collect(Collectors.toList());
    List<String> names = indexes.stream()
        .map(table.getColumns()::get)
        .collect(Collectors.toList());
    List<List<String>> rows = table.getRows().stream()
        .map(row -> indexes.stream().map(row::get).collect(Collectors.toList()))
        .collect(Collectors.toList());
    return new Table(table.getName(), names, rows);
  }
}
