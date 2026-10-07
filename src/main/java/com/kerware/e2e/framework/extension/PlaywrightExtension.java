package com.kerware.e2e.framework.extension;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.navigateur.SessionNavigateur;
import com.kerware.e2e.framework.pages.BasePage;
import com.kerware.e2e.framework.rapport.RepertoireRapport;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
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
 */
public class PlaywrightExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PlaywrightExtension.class);
    private static final Logger LOG_NAVIGATEUR = LoggerFactory.getLogger("e2e.navigateur");

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PlaywrightExtension.class);
    private static final String CLE_RESSOURCES = "ressources";
    private static final Pattern NUMERO_INVOCATION = Pattern.compile("#(\\d+)]$");

    public static final String MDC_FICHIER = "testFichier";
    public static final String MDC_TEST = "test";

    public static final String ENTREE_LOG = "e2e.log";
    public static final String ENTREE_CAPTURE = "e2e.capture";
    public static final String ENTREE_TRACE = "e2e.trace";
    public static final String ENTREE_URL = "e2e.url";

    /** Ressources propres à un test (volontairement non AutoCloseable : fermeture maîtrisée ici). */
    private record RessourcesTest(String identifiant, BrowserContext contexte, Page page, boolean traceDemarree) {
    }

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String identifiant = identifiantFichier(ctx);
        MDC.put(MDC_FICHIER, identifiant);
        MDC.put(MDC_TEST, ctx.getDisplayName());

        Configuration config = Configuration.instance();
        LOG.info("========== DEBUT DU TEST : {} ==========", ctx.getDisplayName());
        LOG.info("Classe : {} | Méthode : {}", ctx.getRequiredTestClass().getName(), ctx.getRequiredTestMethod().getName());

        SessionNavigateur session = ctx.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                SessionNavigateur.class, cle -> SessionNavigateur.demarrer(config), SessionNavigateur.class);

        BrowserContext contexte = session.nouveauContexte(config);
        boolean traceDemarree = !"jamais".equals(modeTraces(config));
        if (traceDemarree) {
            contexte.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(false)
                    .setTitle(ctx.getDisplayName()));
        }
        Page page = contexte.newPage();
        brancherJournalNavigateur(page);

        ctx.getStore(NAMESPACE).put(CLE_RESSOURCES, new RessourcesTest(identifiant, contexte, page, traceDemarree));
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        RessourcesTest ressources = ctx.getStore(NAMESPACE).remove(CLE_RESSOURCES, RessourcesTest.class);
        if (ressources == null) {
            MDC.clear();
            return;
        }
        Optional<Throwable> erreur = ctx.getExecutionException();
        String modeTraces = modeTraces(Configuration.instance());
        try {
            if (erreur.isPresent()) {
                LOG.error("[ECHEC] {}", erreur.get().getMessage(), erreur.get());
                LOG.error("URL au moment de l'échec : {}", urlCourante(ressources.page()));
                ctx.publishReportEntry(ENTREE_URL, urlCourante(ressources.page()));
                capturerEcran(ctx, ressources);
            } else {
                LOG.info("[SUCCES] {}", ctx.getDisplayName());
            }
            if (ressources.traceDemarree()) {
                boolean conserver = "toujours".equals(modeTraces) || erreur.isPresent();
                arreterTrace(ctx, ressources, conserver);
            }
        } finally {
            fermerContexte(ressources);
            ctx.publishReportEntry(ENTREE_LOG,
                    RepertoireRapport.relatif(RepertoireRapport.LOGS, ressources.identifiant() + ".log"));
            LOG.info("========== FIN DU TEST : {} ==========", ctx.getDisplayName());
            MDC.remove(MDC_TEST);
            MDC.remove(MDC_FICHIER);
        }
    }

    // ------------------------------------------------------------------ injection de paramètres

    @Override
    public boolean supportsParameter(ParameterContext parametre, ExtensionContext ctx) {
        Class<?> type = parametre.getParameter().getType();
        return type == Page.class
                || type == Configuration.class
                || (BasePage.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers()));
    }

    @Override
    public Object resolveParameter(ParameterContext parametre, ExtensionContext ctx) {
        Class<?> type = parametre.getParameter().getType();
        if (type == Configuration.class) {
            return Configuration.instance();
        }
        RessourcesTest ressources = ctx.getStore(NAMESPACE).get(CLE_RESSOURCES, RessourcesTest.class);
        if (ressources == null) {
            throw new ParameterResolutionException(
                    "Aucune page disponible : " + type.getSimpleName()
                            + " ne peut être injecté que dans une méthode de test ou un @BeforeEach");
        }
        if (type == Page.class) {
            return ressources.page();
        }
        return instancierPage(type, ressources.page());
    }

    private static Object instancierPage(Class<?> type, Page page) {
        try {
            return type.getConstructor(Page.class, Configuration.class).newInstance(page, Configuration.instance());
        } catch (NoSuchMethodException e) {
            throw new ParameterResolutionException(
                    type.getName() + " doit déclarer un constructeur public (Page, Configuration)", e);
        } catch (InvocationTargetException e) {
            throw new ParameterResolutionException("Erreur dans le constructeur de " + type.getName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new ParameterResolutionException("Instanciation impossible de " + type.getName(), e);
        }
    }

    // ------------------------------------------------------------------ artefacts d'échec

    private static void capturerEcran(ExtensionContext ctx, RessourcesTest ressources) {
        String nom = ressources.identifiant() + ".png";
        Path cible = RepertoireRapport.fichier(RepertoireRapport.CAPTURES, nom);
        try {
            ressources.page().screenshot(new Page.ScreenshotOptions().setPath(cible).setFullPage(true));
            ctx.publishReportEntry(ENTREE_CAPTURE, RepertoireRapport.relatif(RepertoireRapport.CAPTURES, nom));
            LOG.error("Capture d'écran enregistrée : {}", cible.toAbsolutePath());
        } catch (RuntimeException e) {
            LOG.warn("Capture d'écran impossible : {}", e.getMessage());
        }
    }

    private static void arreterTrace(ExtensionContext ctx, RessourcesTest ressources, boolean conserver) {
        try {
            if (conserver) {
                String nom = ressources.identifiant() + ".zip";
                Path cible = RepertoireRapport.fichier(RepertoireRapport.TRACES, nom);
                ressources.contexte().tracing().stop(new Tracing.StopOptions().setPath(cible));
                ctx.publishReportEntry(ENTREE_TRACE, RepertoireRapport.relatif(RepertoireRapport.TRACES, nom));
                LOG.info("Trace Playwright enregistrée : {}", cible.toAbsolutePath());
            } else {
                ressources.contexte().tracing().stop();
            }
        } catch (RuntimeException e) {
            LOG.warn("Arrêt de la trace impossible : {}", e.getMessage());
        }
    }

    private static void fermerContexte(RessourcesTest ressources) {
        try {
            ressources.contexte().close();
        } catch (RuntimeException e) {
            LOG.warn("Fermeture du contexte navigateur impossible : {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ utilitaires

    /** Journalise ce qui se passe dans le navigateur : navigations, console, erreurs JS, requêtes en échec. */
    private static void brancherJournalNavigateur(Page page) {
        page.onFrameNavigated(frame -> {
            if (frame.parentFrame() == null) {
                LOG_NAVIGATEUR.info("Navigation vers {}", frame.url());
            }
        });
        page.onConsoleMessage(message ->
                LOG_NAVIGATEUR.debug("[console.{}] {}", message.type(), message.text()));
        page.onPageError(erreur -> LOG_NAVIGATEUR.warn("[erreur JavaScript] {}", erreur));
        page.onRequestFailed(requete ->
                LOG_NAVIGATEUR.debug("[requête en échec] {} {} : {}", requete.method(), requete.url(), requete.failure()));
    }

    private static String modeTraces(Configuration config) {
        return config.get("traces.mode", "echec").toLowerCase(Locale.ROOT);
    }

    private static String urlCourante(Page page) {
        try {
            return page.url();
        } catch (RuntimeException e) {
            return "(indisponible)";
        }
    }

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
        return nom.toString().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
