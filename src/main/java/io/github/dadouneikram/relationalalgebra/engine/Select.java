package io.github.dadouneikram.relationalalgebra.engine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Opérateur SELECT : garde les lignes qui vérifient un prédicat simple de la forme
 * {@code colonne comparateur valeur}.
 *
 * <p>Les comparateurs sont {@code =}, {@code <} et {@code >}. Si la valeur de la cellule et la
 * constante sont toutes deux des nombres, la comparaison est numérique ; sinon elle est
 * alphabétique. Une constante chaîne s'écrit entre guillemets simples : {@code city = 'Paris'}.
 */
public final class Select implements Operator {

  private static final Pattern PREDICATE = Pattern.compile("(\\S+)\\s+([=<>])\\s+(.+)");
  private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
  private static final Set<String> COMPARATORS = Set.of("=", "<", ">");

  private final Operator input;
  private final String column;
  private final String comparator;
  private final String value;

  /**
   * Construit l'opérateur.
   *
   * @param input opérateur qui fournit la table à filtrer
   * @param column colonne testée
   * @param comparator {@code =}, {@code <} ou {@code >}
   * @param value constante à laquelle la colonne est comparée (sans guillemets)
   * @throws IllegalArgumentException si le comparateur est inconnu
   */
  public Select(Operator input, String column, String comparator, String value) {
    if (!COMPARATORS.contains(comparator)) {
      throw new IllegalArgumentException("Comparateur inconnu : " + comparator);
    }
    this.input = input;
    this.column = column;
    this.comparator = comparator;
    this.value = value;
  }

  /**
   * Construit l'opérateur à partir d'un prédicat textuel, par exemple {@code age < 25}.
   *
   * @param input opérateur qui fournit la table à filtrer
   * @param predicate prédicat de la forme {@code colonne comparateur valeur}
   * @return l'opérateur SELECT correspondant
   * @throws IllegalArgumentException si le prédicat est mal formé
   */
  public static Select fromPredicate(Operator input, String predicate) {
    Matcher matcher = PREDICATE.matcher(predicate.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Prédicat invalide : " + predicate);
    }
    return new Select(input, matcher.group(1), matcher.group(2), unquote(matcher.group(3).trim()));
  }

  private static String unquote(String text) {
    boolean quoted = text.length() >= 2 && text.startsWith("'") && text.endsWith("'");
    return quoted ? text.substring(1, text.length() - 1) : text;
  }

  @Override
  public Table execute() {
    Table table = input.execute();
    int index = table.columnIndex(column);
    Predicate<List<String>> condition = row -> matches(row.get(index));
    List<List<String>> rows = table.getRows().stream()
        .filter(condition)
        .collect(Collectors.toList());
    return new Table(table.getName(), table.getColumns(), rows);
  }

  private boolean matches(String cell) {
    int comparison = compare(cell, value);
    switch (comparator) {
      case "=":
        return comparison == 0;
      case "<":
        return comparison < 0;
      default:
        return comparison > 0;
    }
  }

  private static int compare(String cell, String constant) {
    Optional<Double> cellNumber = asNumber(cell);
    Optional<Double> constantNumber = asNumber(constant);
    if (cellNumber.isPresent() && constantNumber.isPresent()) {
      return Double.compare(cellNumber.get(), constantNumber.get());
    }
    return cell.compareTo(constant);
  }

  private static Optional<Double> asNumber(String text) {
    if (!NUMBER.matcher(text).matches()) {
      return Optional.empty();
    }
    return Optional.of(Double.parseDouble(text));
  }
}
