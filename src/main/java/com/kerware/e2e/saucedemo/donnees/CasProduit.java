// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.donnees;

import com.kerware.e2e.framework.donnees.DonneesTest;
import com.kerware.e2e.framework.donnees.FichierCsv;
import com.kerware.e2e.saucedemo.pages.ProduitAffiche;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Ligne du fichier {@code donnees/inventaire/produits.csv} : le catalogue attendu.
 *
 * <p>Colonnes : id, nom, prix. Le prix est écrit sans symbole monétaire, avec un point décimal
 * et deux décimales, comme à l'affichage (« 29.99 »).</p>
 *
 * <p>Deux usages :</p>
 * <ul>
 *     <li>une ligne par invocation : {@code @CsvFileSource(resources = CasProduit.FICHIER)}
 *         + {@link Agregateur} ;</li>
 *     <li>le catalogue complet : {@link #catalogue()}.</li>
 * </ul>
 */
public record CasProduit(String id, String nom, BigDecimal prix) {

    /** Chemin du fichier dans le classpath (constante utilisable dans les annotations). */
    public static final String FICHIER = "/donnees/inventaire/produits.csv";

    /** Tous les produits du fichier, dans l'ordre du fichier. */
    public static List<CasProduit> catalogue() {
        return FichierCsv.lire(FICHIER).stream()
                .map(ligne -> creer(ligne.get("id"), ligne.get("nom"), ligne.get("prix")))
                .toList();
    }

    /** Produit tel qu'il doit apparaître dans la liste de l'inventaire. */
    public ProduitAffiche enProduitAffiche() {
        return new ProduitAffiche(nom, prix);
    }

    @Override
    public String toString() {
        return id + " - " + nom + " ($" + prix.toPlainString() + ")";
    }

    private static CasProduit creer(String id, String nom, String prix) {
        String texte = DonneesTest.resoudre(prix);
        try {
            // Échelle 2 imposée : « 7.9 » dans le fichier serait comparé à « 7.90 » à l'affichage
            return new CasProduit(id, DonneesTest.resoudre(nom), new BigDecimal(texte).setScale(2, RoundingMode.UNNECESSARY));
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException(FICHIER + " : prix invalide pour " + id + " : '" + texte
                    + "' (attendu : nombre à 2 décimales max, point décimal, sans symbole)", e);
        }
    }

    /** Transforme une ligne CSV en {@link CasProduit}, références {@code ${...}} résolues. */
    public static class Agregateur implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor ligne, ParameterContext contexte) {
            return creer(ligne.getString(0), ligne.getString(1), ligne.getString(2));
        }
    }
}
