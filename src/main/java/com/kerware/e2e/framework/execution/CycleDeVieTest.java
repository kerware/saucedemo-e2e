// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.execution;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.navigateur.SessionNavigateur;
import com.kerware.e2e.framework.rapport.RepertoireRapport;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Cycle de vie Playwright d'un test, indépendant du moteur qui l'exécute.
 *
 * <p>Utilisé par l'extension JUnit Jupiter ({@code PlaywrightExtension}) et par les hooks
 * Cucumber ({@code HooksPlaywright}), pour que tests JUnit et scénarios Gherkin aient exactement
 * le même comportement :</p>
 * <ul>
 *     <li>{@link #demarrer} : log par test (clé MDC {@value #MDC_FICHIER}), {@link BrowserContext}
 *         et {@link Page} neufs (isolation), tracing, journal du navigateur ;</li>
 *     <li>{@link #terminer} : en cas d'échec, URL courante et capture d'écran pleine page ;
 *         trace conservée selon {@code traces.mode} ; fermeture du contexte ; publication des
 *         chemins des artefacts pour le rapport HTML.</li>
 * </ul>
 *
 * <p>La publication des artefacts est confiée à l'appelant (fonction {@code (clé, valeur)}) :
 * report entries JUnit pour Jupiter, canal dédié pour Cucumber.</p>
 */
public final class CycleDeVieTest {

    private static final Logger LOG = LoggerFactory.getLogger(CycleDeVieTest.class);
    private static final Logger LOG_NAVIGATEUR = LoggerFactory.getLogger("e2e.navigateur");

    public static final String MDC_FICHIER = "testFichier";
    public static final String MDC_TEST = "test";

    public static final String ENTREE_LOG = "e2e.log";
    public static final String ENTREE_CAPTURE = "e2e.capture";
    public static final String ENTREE_TRACE = "e2e.trace";
    public static final String ENTREE_URL = "e2e.url";

    private final String identifiant;
    private final String nomAffiche;
    private final BrowserContext contexte;
    private final Page page;
    private final boolean traceDemarree;

    private CycleDeVieTest(String identifiant, String nomAffiche, BrowserContext contexte, Page page,
                           boolean traceDemarree) {
        this.identifiant = identifiant;
        this.nomAffiche = nomAffiche;
        this.contexte = contexte;
        this.page = page;
        this.traceDemarree = traceDemarree;
    }

    /** Issue d'un test, vue du cycle de vie. */
    public record Resultat(boolean echec, String message, Throwable cause) {

        public static Resultat succes() {
            return new Resultat(false, null, null);
        }

        /** Échec avec l'exception qui l'a provoqué (cas JUnit). */
        public static Resultat echec(Throwable cause) {
            return new Resultat(true, cause.getMessage(), cause);
        }

        /** Échec connu seulement par un message (cas Cucumber : le hook ne reçoit pas l'exception). */
        public static Resultat echec(String message) {
            return new Resultat(true, message, null);
        }

        public static Resultat depuis(Optional<Throwable> erreur) {
            return erreur.map(Resultat::echec).orElseGet(Resultat::succes);
        }
    }

    // ------------------------------------------------------------------ cycle de vie

    /**
     * Démarre un test : MDC, contexte et page neufs, tracing.
     *
     * @param session     fournit la session partagée (démarrée au premier appel par l'appelant)
     * @param identifiant nom de fichier du test (log, capture, trace), sans extension
     * @param nomAffiche  nom lisible du test (logs, titre de la trace)
     * @param origine     ligne de log décrivant d'où vient le test (classe et méthode, ou fichier .feature)
     */
    public static CycleDeVieTest demarrer(Supplier<SessionNavigateur> session, Configuration config,
                                          String identifiant, String nomAffiche, String origine) {
        MDC.put(MDC_FICHIER, identifiant);
        MDC.put(MDC_TEST, nomAffiche);

        LOG.info("========== DEBUT DU TEST : {} ==========", nomAffiche);
        LOG.info(origine);

        BrowserContext contexte = session.get().nouveauContexte(config);
        boolean traceDemarree = !"jamais".equals(modeTraces(config));
        if (traceDemarree) {
            contexte.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(false)
                    .setTitle(nomAffiche));
        }
        Page page = contexte.newPage();
        brancherJournalNavigateur(page);
        return new CycleDeVieTest(identifiant, nomAffiche, contexte, page, traceDemarree);
    }

    /**
     * Termine le test : artefacts d'échec, trace, fermeture du contexte, nettoyage du MDC.
     *
     * @param publication reçoit chaque artefact sous forme {@code (ENTREE_xxx, chemin relatif ou URL)}
     */
    public void terminer(Resultat resultat, BiConsumer<String, String> publication) {
        String modeTraces = modeTraces(Configuration.instance());
        try {
            if (resultat.echec()) {
                if (resultat.cause() != null) {
                    LOG.error("[ECHEC] {}", resultat.message(), resultat.cause());
                } else {
                    LOG.error("[ECHEC] {}", resultat.message());
                }
                LOG.error("URL au moment de l'échec : {}", urlCourante(page));
                publication.accept(ENTREE_URL, urlCourante(page));
                capturerEcran(publication);
            } else {
                LOG.info("[SUCCES] {}", nomAffiche);
            }
            if (traceDemarree) {
                boolean conserver = "toujours".equals(modeTraces) || resultat.echec();
                arreterTrace(publication, conserver);
            }
        } finally {
            fermerContexte();
            publication.accept(ENTREE_LOG, RepertoireRapport.relatif(RepertoireRapport.LOGS, identifiant + ".log"));
            LOG.info("========== FIN DU TEST : {} ==========", nomAffiche);
            MDC.remove(MDC_TEST);
            MDC.remove(MDC_FICHIER);
        }
    }

    public Page page() {
        return page;
    }

    public String identifiant() {
        return identifiant;
    }

    public String nomAffiche() {
        return nomAffiche;
    }

    // ------------------------------------------------------------------ artefacts d'échec

    private void capturerEcran(BiConsumer<String, String> publication) {
        String nom = identifiant + ".png";
        Path cible = RepertoireRapport.fichier(RepertoireRapport.CAPTURES, nom);
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(cible).setFullPage(true));
            publication.accept(ENTREE_CAPTURE, RepertoireRapport.relatif(RepertoireRapport.CAPTURES, nom));
            LOG.error("Capture d'écran enregistrée : {}", cible.toAbsolutePath());
        } catch (RuntimeException e) {
            LOG.warn("Capture d'écran impossible : {}", e.getMessage());
        }
    }

    private void arreterTrace(BiConsumer<String, String> publication, boolean conserver) {
        try {
            if (conserver) {
                String nom = identifiant + ".zip";
                Path cible = RepertoireRapport.fichier(RepertoireRapport.TRACES, nom);
                contexte.tracing().stop(new Tracing.StopOptions().setPath(cible));
                publication.accept(ENTREE_TRACE, RepertoireRapport.relatif(RepertoireRapport.TRACES, nom));
                LOG.info("Trace Playwright enregistrée : {}", cible.toAbsolutePath());
            } else {
                contexte.tracing().stop();
            }
        } catch (RuntimeException e) {
            LOG.warn("Arrêt de la trace impossible : {}", e.getMessage());
        }
    }

    private void fermerContexte() {
        try {
            contexte.close();
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

    /** Remplace les caractères non sûrs pour un nom de fichier par « _ ». */
    public static String nomDeFichierSur(String texte) {
        return texte.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
