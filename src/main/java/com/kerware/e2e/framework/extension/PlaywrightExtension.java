// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.extension;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.execution.CycleDeVieTest;
import com.kerware.e2e.framework.navigateur.SessionNavigateur;
import com.kerware.e2e.framework.pages.BasePage;
import com.kerware.e2e.framework.pages.FabriquePages;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.MDC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension JUnit au cœur du framework.
 *
 * <ul>
 *     <li>démarre une fois la {@link SessionNavigateur} partagée (fermée en fin d'exécution) ;</li>
 *     <li>crée un {@link BrowserContext} et une {@link Page} neufs par test (isolation) ;</li>
 *     <li>injecte en paramètre de test : {@link Page}, {@link Configuration} et toute page
 *         {@link BasePage} (ex. {@code LoginPage}) ;</li>
 *     <li>journalise chaque test dans son propre fichier (clé MDC {@value #MDC_FICHIER}) ;</li>
 *     <li>en cas d'échec : capture d'écran pleine page + trace Playwright ;</li>
 *     <li>publie les chemins des artefacts (report entries) pour le rapport HTML.</li>
 * </ul>
 *
 * <p>Le cycle de vie lui-même est porté par {@link CycleDeVieTest}, partagé avec les hooks
 * Cucumber : cette extension n'en est que l'adaptateur JUnit Jupiter.</p>
 */
public class PlaywrightExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PlaywrightExtension.class);
    private static final String CLE_RESSOURCES = "ressources";
    private static final Pattern NUMERO_INVOCATION = Pattern.compile("#(\\d+)]$");

    public static final String MDC_FICHIER = CycleDeVieTest.MDC_FICHIER;
    public static final String MDC_TEST = CycleDeVieTest.MDC_TEST;

    public static final String ENTREE_LOG = CycleDeVieTest.ENTREE_LOG;
    public static final String ENTREE_CAPTURE = CycleDeVieTest.ENTREE_CAPTURE;
    public static final String ENTREE_TRACE = CycleDeVieTest.ENTREE_TRACE;
    public static final String ENTREE_URL = CycleDeVieTest.ENTREE_URL;

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void beforeEach(ExtensionContext ctx) {
        Configuration config = Configuration.instance();
        CycleDeVieTest test = CycleDeVieTest.demarrer(
                () -> ctx.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                        SessionNavigateur.class, cle -> SessionNavigateur.demarrer(config), SessionNavigateur.class),
                config,
                identifiantFichier(ctx),
                ctx.getDisplayName(),
                "Classe : " + ctx.getRequiredTestClass().getName()
                        + " | Méthode : " + ctx.getRequiredTestMethod().getName());

        ctx.getStore(NAMESPACE).put(CLE_RESSOURCES, test);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        CycleDeVieTest test = ctx.getStore(NAMESPACE).remove(CLE_RESSOURCES, CycleDeVieTest.class);
        if (test == null) {
            MDC.clear();
            return;
        }
        test.terminer(CycleDeVieTest.Resultat.depuis(ctx.getExecutionException()), ctx::publishReportEntry);
    }

    // ------------------------------------------------------------------ injection de paramètres

    @Override
    public boolean supportsParameter(ParameterContext parametre, ExtensionContext ctx) {
        Class<?> type = parametre.getParameter().getType();
        return type == Page.class
                || type == Configuration.class
                || FabriquePages.estInstanciable(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parametre, ExtensionContext ctx) {
        Class<?> type = parametre.getParameter().getType();
        if (type == Configuration.class) {
            return Configuration.instance();
        }
        CycleDeVieTest test = ctx.getStore(NAMESPACE).get(CLE_RESSOURCES, CycleDeVieTest.class);
        if (test == null) {
            throw new ParameterResolutionException(
                    "Aucune page disponible : " + type.getSimpleName()
                            + " ne peut être injecté que dans une méthode de test ou un @BeforeEach");
        }
        if (type == Page.class) {
            return test.page();
        }
        try {
            return FabriquePages.creer(type.asSubclass(BasePage.class), test.page(), Configuration.instance());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ParameterResolutionException(e.getMessage(), e.getCause());
        }
    }

    // ------------------------------------------------------------------ utilitaires

    /** Ex. : LoginNonPassantTest_connexionRefusee_03 (numéro d'invocation des tests paramétrés). */
    static String identifiantFichier(ExtensionContext ctx) {
        StringBuilder nom = new StringBuilder()
                .append(ctx.getRequiredTestClass().getSimpleName())
                .append('_')
                .append(ctx.getRequiredTestMethod().getName());
        Matcher invocation = NUMERO_INVOCATION.matcher(ctx.getUniqueId());
        if (invocation.find()) {
            nom.append('_').append(String.format("%02d", Integer.parseInt(invocation.group(1))));
        }
        return CycleDeVieTest.nomDeFichierSur(nom.toString());
    }
}
