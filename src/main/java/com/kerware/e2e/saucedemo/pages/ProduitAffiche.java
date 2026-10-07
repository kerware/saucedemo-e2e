// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.pages;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Instantané d'un produit tel qu'il est affiché dans la liste de l'inventaire : nom et prix.
 *
 * <p>Valeur immuable, comparable par {@code equals} (le prix est comparé avec son échelle :
 * les prix SauceDemo ont toujours deux décimales, comme les données de test).</p>
 */
public record ProduitAffiche(String nom, BigDecimal prix) {

    public ProduitAffiche {
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(prix, "prix");
    }

    @Override
    public String toString() {
        return nom + " ($" + prix.toPlainString() + ")";
    }
}
