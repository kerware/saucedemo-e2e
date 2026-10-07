// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.pages;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Tris proposés par la liste déroulante de l'inventaire.
 *
 * <p>Chaque valeur porte :</p>
 * <ul>
 *     <li>la valeur HTML de l'option ({@code <option value="az">}) ;</li>
 *     <li>le comparateur qui définit l'ordre attendu des produits affichés.</li>
 * </ul>
 *
 * <p>Les comparateurs de prix ne départagent pas les ex-aequo (deux produits à $15.99) :
 * une liste est « bien triée » si elle est égale à sa copie triée de façon <em>stable</em>
 * ({@link java.util.stream.Stream#sorted(Comparator)} sur une liste), ce qui revient à vérifier
 * que la suite des prix est monotone sans imposer l'ordre des produits de même prix.</p>
 *
 * <p>Les libellés affichés (« Name (A to Z) »…) ne sont pas portés ici : ce sont des
 * données de test, externalisées dans {@code donnees/inventaire/tris.csv}.</p>
 */
public enum Tri {

    NOM_A_Z("az", Comparator.comparing(ProduitAffiche::nom)),
    NOM_Z_A("za", Comparator.comparing(ProduitAffiche::nom).reversed()),
    PRIX_CROISSANT("lohi", Comparator.comparing(ProduitAffiche::prix)),
    PRIX_DECROISSANT("hilo", Comparator.comparing(ProduitAffiche::prix).reversed());

    private final String codeOption;
    private final Comparator<ProduitAffiche> comparateur;

    Tri(String codeOption, Comparator<ProduitAffiche> comparateur) {
        this.codeOption = codeOption;
        this.comparateur = comparateur;
    }

    /** Valeur de l'attribut {@code value} de l'option correspondante. */
    public String codeOption() {
        return codeOption;
    }

    /** Ordre attendu des produits affichés (sans départage des ex-aequo). */
    public Comparator<ProduitAffiche> comparateur() {
        return comparateur;
    }

    /** Tri correspondant à une valeur d'option HTML (« az », « za », « lohi », « hilo »). */
    public static Tri depuisCode(String codeOption) {
        return Arrays.stream(values())
                .filter(tri -> tri.codeOption.equals(codeOption))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Code de tri inconnu : '" + codeOption
                        + "' (attendu : az, za, lohi ou hilo)"));
    }
}
