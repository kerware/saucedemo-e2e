// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.etapes;

import com.kerware.e2e.saucedemo.donnees.CasTri;
import io.cucumber.java.ParameterType;

/**
 * Types de paramètres des expressions Cucumber de SauceDemo.
 *
 * <p>{@code {tri}} reconnaît un libellé de tri entre guillemets dans le texte d'une étape
 * ({@code je trie les produits par "Name (A to Z)"}) et le convertit en {@link TriChoisi}.
 * La correspondance libellé ↔ code vient de {@code donnees/inventaire/tris.csv} :
 * un libellé inconnu fait échouer l'étape avec un message explicite.</p>
 */
public class TypesParametres {

    @ParameterType(name = "tri", value = "\"([^\"]*)\"")
    public TriChoisi tri(String libelle) {
        return new TriChoisi(libelle, CasTri.triDuLibelle(libelle));
    }
}
