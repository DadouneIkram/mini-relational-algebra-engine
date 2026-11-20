# Mini moteur d'algèbre relationnelle en Java

> **English summary:** A small relational algebra engine written in Java 17. It runs queries made of
> SCAN, SELECT, PROJECT and JOIN operators over CSV files and writes the result as CSV. Queries are
> described in JSON, or in a compact algebraic notation that the tool translates to JSON. Evaluation
> follows a functional approach: a query is a tree of composable, side-effect-free operators built
> from the JSON plan and evaluated lazily. Built with Maven, Gson for JSON, JUnit 5 (72 tests,
> about 98 % line coverage with JaCoCo) and Checkstyle (Google Java Style).

Ce projet exécute des requêtes d'algèbre relationnelle sur des fichiers CSV, sans base de données.
Il a été réalisé dans le cadre d'un master en informatique, avec trois axes : les collections, la
programmation fonctionnelle et les entrées et sorties.

## Fonctionnalités

- Quatre opérateurs de l'algèbre relationnelle :
  - **SCAN** lit un fichier CSV ;
  - **SELECT** filtre les lignes avec un prédicat simple (`age < 25`, `city = 'Paris'`) ;
  - **PROJECT** garde certaines colonnes ;
  - **JOIN** réalise une équi-jointure entre deux relations.
- Requêtes décrites en **JSON** (`.json`), exécutées vers `results/output.csv`.
- Requêtes écrites en **notation algébrique** (`.alg`), traduites en JSON.
- Messages d'erreur précis (numéro de ligne, colonne inexistante, champ JSON manquant) et code de
  sortie 1 en cas d'erreur.

## Exemple d'utilisation

Le dossier [`examples/`](examples) contient deux tables et deux requêtes.

```console
$ cat examples/users.csv
id,name,age
1,Alice,22
2,Bob,30
3,Charlie,27
4,David,19

$ cat examples/optJoin.alg
users: users.csv
orders: orders.csv

R1 = PROJ users id name
R2 = JOIN R1 orders id user_id
R = PROJ R2 name amount
```

**1. Traduire la notation algébrique en JSON.** La requête n'est pas exécutée à cette étape.

```console
$ java -jar target/mini-relational-algebra-engine.jar examples/optJoin.alg
Traduction en optJoin.json
```

Le fichier [`examples/optJoin.json`](examples/optJoin.json) produit décrit le plan sous forme
d'arbre :

```json
{
  "tables": { "users": "users.csv", "orders": "orders.csv" },
  "plan": {
    "type": "PROJECT",
    "columns": ["name", "amount"],
    "input": {
      "type": "JOIN",
      "left": {
        "type": "PROJECT",
        "columns": ["id", "name"],
        "input": { "type": "SCAN", "table": "users" }
      },
      "right": { "type": "SCAN", "table": "orders" },
      "onLeft": "id",
      "onRight": "user_id"
    }
  }
}
```

**2. Exécuter la requête JSON.** Les fichiers CSV sont cherchés dans le dossier de la requête, et
le résultat est écrit dans `results/output.csv`, dans ce même dossier.

```console
$ java -jar target/mini-relational-algebra-engine.jar examples/optJoin.json
Résultat écrit dans results/output.csv

$ cat examples/results/output.csv
name,amount
Alice,250
Alice,40
Bob,80
David,120
```

### Syntaxe de la notation algébrique

| Ligne | Signification |
|---|---|
| `users: users.csv` | déclare la table `users`, lue dans `users.csv` |
| `R1 = SELECT users age < 25` | sélection ; comparateurs `=`, `<`, `>` ; chaîne entre guillemets simples |
| `R2 = PROJ R1 name age` | projection sur une ou plusieurs colonnes |
| `R3 = JOIN R1 orders id user_id` | jointure de `R1.id` avec `orders.user_id` |

Les éléments sont séparés par des espaces (`age < 25`, jamais `age<25`). Chaque variable peut être
réutilisée dans les lignes suivantes ; la dernière définie est le résultat.

## Compilation et lancement

Prérequis : Java 17 ou plus récent, Maven 3.6 ou plus récent.

```bash
mvn clean verify
java -jar target/mini-relational-algebra-engine.jar examples/reqProjSel.json
```

`mvn clean verify` vérifie le style, compile, lance les tests, contrôle la couverture et produit
un jar exécutable qui contient aussi Gson.

## Architecture

```
src/main/java/io/github/dadouneikram/relationalalgebra/
├── Main.java               # ligne de commande : .alg → traduction, .json → exécution
├── alg/
│   └── AlgParser.java      # notation algébrique → requête JSON (Gson)
├── json/
│   └── JsonExecutor.java   # requête JSON → arbre d'opérateurs → table
└── engine/
    ├── Table.java          # table immuable : nom, colonnes, lignes
    ├── Operator.java       # interface fonctionnelle : () -> Table
    ├── Scan.java
    ├── Select.java
    ├── Project.java
    └── Join.java
```

### Une évaluation fonctionnelle

`Operator` est une interface fonctionnelle dont l'unique méthode `execute()` retourne une `Table`.
`Select`, `Project` et `Join` reçoivent d'**autres opérateurs** en entrée, pas des tables : une
requête est donc une composition de fonctions.

L'exécution d'une requête JSON se fait en deux temps :

1. `JsonExecutor.toOperator` parcourt le plan JSON récursivement et construit l'arbre
   d'opérateurs. **Aucun fichier n'est lu** à cette étape.
2. L'appel à `execute()` sur la racine évalue l'arbre : chaque opérateur évalue ses entrées, puis
   applique sa transformation.

Chaque opérateur est une fonction pure de ses entrées : il produit une nouvelle `Table` sans
jamais modifier celles qu'il reçoit. `Table` est d'ailleurs immuable (copies défensives avec
`List.copyOf`). Les transformations sont écrites avec l'API Stream :

- `Select` : `filter` avec un `Predicate` construit à partir du prédicat textuel ;
- `Project` : `map` de chaque ligne vers les colonnes demandées ;
- `Join` : `groupingBy` des lignes de droite selon la clé de jointure, puis `flatMap` de chaque
  ligne de gauche vers ses correspondances. C'est une jointure par hachage : chaque ligne de gauche
  retrouve directement les lignes de droite qui lui correspondent, au lieu de parcourir toute la
  table de droite.

Ce découpage rend chaque opérateur testable seul : dans les tests, une entrée est simplement une
expression lambda `() -> table`.

### Représentation des tables

Les colonnes sont une `List<String>` lue dans l'en-tête du CSV, et chaque ligne une
`List<String>` : le nombre de colonnes n'est pas fixé à l'avance. Les noms de colonnes et de
tables ne tiennent pas compte des majuscules. Lors d'une jointure, une colonne présente des deux
côtés est préfixée par le nom de sa table (`users.id`, `orders.id`).

### Choix techniques

- **Gson** est la seule bibliothèque JSON : `JsonParser` pour lire les requêtes, `JsonObject`
  pour les construire lors de la traduction, `GsonBuilder` pour les écrire de façon lisible. La
  traduction `.alg` produit directement l'arbre JSON, sans structure intermédiaire.
  L'échappement HTML de Gson est désactivé, sinon `<` serait écrit `<` dans les prédicats.
- **Lecture CSV** faite à la main : le format est simple (virgule comme séparateur, pas de
  guillemets), une bibliothèque comme OpenCSV n'apporterait rien ici.
- **SELECT** compare numériquement deux nombres (`9 < 10`) et alphabétiquement dans les autres
  cas.
- **PROJECT** conserve les lignes en double, comme `SELECT` sans `DISTINCT` en SQL.

### Pourquoi JSON pour un plan d'exécution ?

Les objets imbriqués de JSON représentent naturellement un arbre d'opérateurs, et le format est
lisible et outillé partout. En revanche, JSON n'a pas de schéma imposé : il faut vérifier soi-même
la présence et le type de chaque champ, ce que fait `JsonExecutor` avec des messages explicites.
Le format est aussi verbeux, d'où l'intérêt de la notation `.alg`.

## Tests

```bash
mvn test
```

72 tests JUnit 5 couvrent chaque opérateur (cas nominaux, tables vides, colonnes inexistantes,
comparaisons numériques et chaînes entre guillemets), le parseur `.alg` (syntaxe, erreurs avec
numéro de ligne), l'exécuteur JSON (champs manquants ou mal typés, JSON invalide) et des scénarios
de bout en bout sur des fichiers temporaires.

Le rapport de couverture JaCoCo est généré dans `target/site/jacoco/index.html` ; le build échoue
si moins de 80 % des lignes sont couvertes (environ 98 % actuellement). Le style de code est
vérifié par Checkstyle (règles Google) pendant la phase `validate` : toute violation fait échouer
le build.

## Évolutions possibles

- Prédicats composés (`AND`, `OR`, `NOT`).
- Agrégations (`COUNT`, `SUM`, `GROUP BY`) et tri (`ORDER BY`).
- Optimisation du plan, par exemple appliquer les sélections avant les jointures.
- Valeurs CSV entre guillemets, contenant des virgules.
