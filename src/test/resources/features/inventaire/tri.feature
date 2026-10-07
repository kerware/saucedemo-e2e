# Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
# language: fr
@e2e @gherkin @inventaire @tri
Fonctionnalité: Tri des produits de l'inventaire
  En tant qu'utilisateur connecté
  Je veux trier les produits du catalogue
  Afin de trouver plus facilement ce que je cherche

  Contexte:
    Étant donné un utilisateur connecté sur la page d'inventaire

  # Le tri préalable est différent du tri testé : « Name (A to Z) » est actif au chargement,
  # le sélectionner directement ne prouverait pas que le tri agit.
  # Les tris par prix tolèrent les ex-aequo ($15.99) : seul l'ordre des prix est vérifié.
  Plan du scénario: Trier par « <tri> » (<id>)
    Étant donné les produits triés par "<préalable>"
    Quand je trie les produits par "<tri>"
    Alors le tri actif affiché est "<tri>"
    Et les produits sont dans l'ordre "<tri>"
    Et tous les produits du catalogue sont présents

    Exemples:
      | id     | tri                 | préalable           |
      | TRI-01 | Name (A to Z)       | Name (Z to A)       |
      | TRI-02 | Name (Z to A)       | Price (low to high) |
      | TRI-03 | Price (low to high) | Price (high to low) |
      | TRI-04 | Price (high to low) | Price (low to high) |
