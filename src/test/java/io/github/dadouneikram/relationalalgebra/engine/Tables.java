package io.github.dadouneikram.relationalalgebra.engine;

import java.util.List;

/** Tables de test partagées par les tests des opérateurs. */
final class Tables {

  static final Table USERS = new Table("users", List.of("id", "name", "age"), List.of(
      List.of("1", "Alice", "22"),
      List.of("2", "Bob", "30"),
      List.of("3", "Charlie", "27"),
      List.of("4", "David", "19")));

  static final Table ORDERS = new Table("orders", List.of("id", "user_id", "amount"), List.of(
      List.of("101", "1", "250"),
      List.of("102", "2", "80"),
      List.of("103", "1", "40"),
      List.of("104", "4", "120")));

  private Tables() {
  }
}
