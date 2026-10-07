// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.etapes;

import com.kerware.e2e.saucedemo.pages.Tri;

/**
 * Tri désigné dans un scénario par son libellé affiché (« Price (low to high) »).
 *
 * <p>Porte à la fois le libellé (texte attendu à l'écran) et le {@link Tri} correspondant
 * (valeur d'option et comparateur de vérification).</p>
 */
public record TriChoisi(String libelle, Tri tri) {

    @Override
    public String toString() {
        return "« " + libelle + " » (" + tri.codeOption() + ")";
    }
}
