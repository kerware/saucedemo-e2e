// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.cucumber;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.execution.CycleDeVieTest;
import com.kerware.e2e.framework.pages.BasePage;
import com.kerware.e2e.framework.pages.FabriquePages;
import com.microsoft.playwright.Page;

/**
 * État partagé par toutes les étapes d'<strong>un</strong> scénario Gherkin.
 *
 * <p>Instancié par le conteneur d'injection de Cucumber (PicoContainer) au début de chaque
 * scénario, puis injecté par constructeur dans les hooks et dans chaque classe d'étapes :
 * toutes reçoivent la même instance pendant le scénario, et une instance neuve au scénario
 * suivant. C'est l'équivalent, côté Cucumber, de l'injection de paramètres de l'extension JUnit.</p>
 *
 * <ul>
 *     <li>{@link #page(Class)} crée un Page Object sur la page Playwright du scénario ;</li>
 *     <li>{@link #definirPageCourante(BasePage)} / {@link #pageCourante(Class)} mémorisent la page
 *         atteinte par chaînage (ex. {@code seConnecterAvecSucces} renvoie l'inventaire), pour
 *         que les étapes suivantes la retrouvent.</li>
 * </ul>
 */
public class ContexteScenario {

    private CycleDeVieTest test;
    private BasePage pageCourante;

    // ------------------------------------------------------------------ réservé aux hooks

    void demarrer(CycleDeVieTest testDemarre) {
        this.test = testDemarre;
    }

    CycleDeVieTest test() {
        return test;
    }

    // ------------------------------------------------------------------ pour les étapes

    /** Page Playwright du scénario (contexte navigateur neuf, propre au scénario). */
    public Page page() {
        if (test == null) {
            throw new IllegalStateException("Aucun navigateur pour ce scénario : le hook HooksPlaywright "
                    + "n'a pas été exécuté (vérifier que com.kerware.e2e.framework.cucumber est dans la glue)");
        }
        return test.page();
    }

    public Configuration configuration() {
        return Configuration.instance();
    }

    /** Nouveau Page Object de type {@code type}, sur la page du scénario. */
    public <T extends BasePage> T page(Class<T> type) {
        return FabriquePages.creer(type, page(), configuration());
    }

    /** Mémorise la page affichée, obtenue par chaînage, pour les étapes suivantes. */
    public <T extends BasePage> T definirPageCourante(T page) {
        this.pageCourante = page;
        return page;
    }

    /**
     * Page affichée mémorisée par une étape précédente.
     *
     * @throws IllegalStateException si aucune page de ce type n'a été mémorisée
     *                               (étape de précondition manquante dans le scénario)
     */
    public <T extends BasePage> T pageCourante(Class<T> type) {
        if (!type.isInstance(pageCourante)) {
            throw new IllegalStateException("La page courante devrait être " + type.getSimpleName()
                    + " mais est " + (pageCourante == null ? "absente" : pageCourante.getClass().getSimpleName())
                    + " : une étape de précondition manque-t-elle au scénario ?");
        }
        return type.cast(pageCourante);
    }
}
