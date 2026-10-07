// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.pages;

import com.kerware.e2e.framework.config.Configuration;
import com.microsoft.playwright.Page;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Instanciation des Page Objects par leur constructeur public {@code (Page, Configuration)}.
 *
 * <p>Utilisée par l'injection JUnit ({@code PlaywrightExtension}) et par le contexte des
 * scénarios Cucumber ({@code ContexteScenario}).</p>
 */
public final class FabriquePages {

    private FabriquePages() {
    }

    /** {@code true} si {@code type} est une page concrète, donc instanciable par {@link #creer}. */
    public static boolean estInstanciable(Class<?> type) {
        return BasePage.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers());
    }

    /**
     * Crée la page {@code type} sur la page Playwright donnée.
     *
     * @throws IllegalArgumentException si la classe ne déclare pas le constructeur attendu
     * @throws IllegalStateException    si le constructeur échoue (cause : l'exception levée)
     */
    public static <T extends BasePage> T creer(Class<T> type, Page page, Configuration config) {
        try {
            return type.getConstructor(Page.class, Configuration.class).newInstance(page, config);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    type.getName() + " doit déclarer un constructeur public (Page, Configuration)", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Erreur dans le constructeur de " + type.getName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Instanciation impossible de " + type.getName(), e);
        }
    }
}
