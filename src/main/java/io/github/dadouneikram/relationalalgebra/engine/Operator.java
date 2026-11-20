package io.github.dadouneikram.relationalalgebra.engine;

/**
 * Opérateur de l'algèbre relationnelle.
 *
 * <p>Un opérateur est une fonction sans argument qui produit une {@link Table}. Les opérateurs
 * se composent : {@code Select}, {@code Project} et {@code Join} reçoivent d'autres opérateurs en
 * entrée. Un plan de requête est donc un arbre de fonctions, qui n'est évalué qu'au moment où
 * l'on appelle {@link #execute()} sur la racine.
 */
@FunctionalInterface
public interface Operator {

  /**
   * Évalue l'opérateur.
   *
   * @return une nouvelle table ; les tables d'entrée ne sont jamais modifiées
   */
  Table execute();
}
