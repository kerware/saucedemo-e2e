// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Collection typée de composants répétés (lignes de tableau, cartes produit, éléments de liste…).
 *
 * <p>Elle est construite à partir :</p>
 * <ul>
 *     <li>d'un {@link Locator} qui désigne <em>tous</em> les éléments répétés ;</li>
 *     <li>d'une fabrique {@code (nomMetier, locatorElement) -> composant}, typiquement
 *         le constructeur du composant : {@code CarteProduit::new}.</li>
 * </ul>
 *
 * <p>Comme les composants, la liste est paresseuse : rien n'est cherché dans le DOM
 * à la construction, et elle ne contient aucune assertion. Pour vérifier le nombre d'éléments
 * avec attente automatique : {@code assertThat(liste.elements()).hasCount(6)}.</p>
 *
 * <p>Deux façons d'obtenir un élément :</p>
 * <ul>
 *     <li><strong>par position</strong> ({@link #element(int)}, {@link #tous()}) : le locator est
 *         {@code nth(i)}, il désigne « l'élément à la position i » au moment de l'action
 *         (après un tri, ce n'est plus le même élément) ;</li>
 *     <li><strong>par contenu</strong> ({@link #elementContenantTexte(String)},
 *         {@link #elementAyant(String, Locator)}) : le locator est filtré, il suit l'élément même si
 *         la liste est réordonnée. À privilégier dans les scénarios.</li>
 * </ul>
 *
 * @param <T> type de composant des éléments
 */
public class ListeDeComposants<T extends Composant> {

    private final String nom;
    private final Locator elements;
    private final BiFunction<String, Locator, T> fabrique;

    /**
     * @param nom      nom métier d'un élément (ex. « Produit ») : sert à nommer chaque composant créé
     * @param elements locator désignant l'ensemble des éléments répétés
     * @param fabrique crée le composant d'un élément à partir de son nom métier et de son locator
     */
    public ListeDeComposants(String nom, Locator elements, BiFunction<String, Locator, T> fabrique) {
        this.nom = nom;
        this.elements = elements;
        this.fabrique = fabrique;
    }

    /** Locator de l'ensemble des éléments, pour les assertions : {@code assertThat(liste.elements()).hasCount(n)}. */
    public Locator elements() {
        return elements;
    }

    public String nom() {
        return nom;
    }

    /** Nombre d'éléments présents (lecture immédiate, sans attente). */
    public int nombre() {
        return elements.count();
    }

    /** Tous les éléments présents, dans l'ordre du DOM (lecture immédiate du nombre d'éléments). */
    public List<T> tous() {
        int nombre = nombre();
        List<T> composants = new ArrayList<>(nombre);
        for (int i = 0; i < nombre; i++) {
            composants.add(element(i));
        }
        return composants;
    }

    /** Élément à la position {@code index} (à partir de 0), désigné par sa position. */
    public T element(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index négatif : " + index);
        }
        return fabrique.apply(nom + " n°" + (index + 1), elements.nth(index));
    }

    /**
     * Premier élément qui satisfait le prédicat, en parcourant les éléments présents.
     *
     * <p>Lecture immédiate : le prédicat lit généralement le DOM de chaque élément.</p>
     */
    public Optional<T> premierQui(Predicate<T> critere) {
        return tous().stream().filter(critere).findFirst();
    }

    /**
     * Élément qui contient un descendant dont le texte vaut <strong>exactement</strong>
     * {@code texteExact} (casse et ponctuation comprises, espaces normalisés).
     *
     * <p>Le locator est filtré par contenu : il reste valable si la liste est réordonnée.
     * « Bolt T-Shirt » ne correspond pas à « Test.allTheThings() T-Shirt (Red) ».</p>
     */
    public T elementContenantTexte(String texteExact) {
        Locator critere = elements.page().getByText(texteExact, new Page.GetByTextOptions().setExact(true));
        return fabrique.apply(nom + " '" + texteExact + "'",
                elements.filter(new Locator.FilterOptions().setHas(critere)));
    }

    /**
     * Élément qui contient un descendant correspondant au locator {@code critere}
     * (évalué relativement à chaque élément).
     *
     * <p>Permet un critère plus précis que {@link #elementContenantTexte(String)}, par exemple
     * « le sous-élément <em>nom</em> vaut exactement X ».</p>
     *
     * @param description texte utilisé pour nommer le composant dans les logs
     */
    public T elementAyant(String description, Locator critere) {
        return fabrique.apply(nom + " " + description,
                elements.filter(new Locator.FilterOptions().setHas(critere)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + nom + "]";
    }
}
