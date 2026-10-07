// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.donnees;

import com.kerware.e2e.framework.donnees.DonneesTest;
import com.kerware.e2e.framework.donnees.FichierCsv;
import com.kerware.e2e.saucedemo.pages.Tri;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

/**
 * Ligne du fichier {@code donnees/inventaire/tris.csv}.
 *
 * <p>Colonnes : id, codeOption, libelle, triPrealable.</p>
 * <ul>
 *     <li>{@code codeOption} : valeur HTML de l'option testée (az, za, lohi, hilo) ;</li>
 *     <li>{@code libelle} : texte attendu dans le libellé du tri actif ;</li>
 *     <li>{@code triPrealable} : tri appliqué <em>avant</em> le tri testé, obligatoirement différent,
 *         pour prouver que le tri testé agit (indispensable pour « az », actif par défaut).</li>
 * </ul>
 */
public record CasTri(String id, Tri tri, String libelle, Tri triPrealable) {

    /** Chemin du fichier dans le classpath (constante utilisable dans les annotations). */
    public static final String FICHIER = "/donnees/inventaire/tris.csv";

    public CasTri {
        if (tri == triPrealable) {
            throw new IllegalArgumentException(FICHIER + " : " + id
                    + " : le tri préalable doit différer du tri testé (" + tri.codeOption() + ")");
        }
    }

    /**
     * Tri dont le libellé affiché est {@code libelle}, d'après la correspondance libellé ↔ code
     * de {@code tris.csv}. Utilisé par les scénarios Gherkin, qui désignent les tris par leur libellé.
     *
     * @throws IllegalArgumentException si le libellé est absent du fichier
     */
    public static Tri triDuLibelle(String libelle) {
        return FichierCsv.lire(FICHIER).stream()
                .filter(ligne -> libelle.equals(DonneesTest.resoudre(ligne.get("libelle"))))
                .map(ligne -> Tri.depuisCode(DonneesTest.resoudre(ligne.get("codeOption"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Libellé de tri inconnu de " + FICHIER + " : '" + libelle + "'"));
    }

    @Override
    public String toString() {
        return id + " - " + libelle + " (" + tri.codeOption() + ", après " + triPrealable.codeOption() + ")";
    }

    /** Transforme une ligne CSV en {@link CasTri}, références {@code ${...}} résolues. */
    public static class Agregateur implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor ligne, ParameterContext contexte) {
            return new CasTri(
                    ligne.getString(0),
                    Tri.depuisCode(DonneesTest.resoudre(ligne.getString(1))),
                    DonneesTest.resoudre(ligne.getString(2)),
                    Tri.depuisCode(DonneesTest.resoudre(ligne.getString(3))));
        }
    }
}
