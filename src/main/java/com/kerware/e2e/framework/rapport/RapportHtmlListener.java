package com.kerware.e2e.framework.rapport;

import com.kerware.e2e.framework.config.Configuration;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Listener JUnit Platform qui produit le rapport HTML {@code target/rapport-e2e/index.html}.
 *
 * <p>Enregistré automatiquement par {@code ServiceLoader}
 * ({@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}) :
 * il fonctionne aussi bien sous Maven que depuis l'IDE.</p>
 *
 * <p>Il récupère les artefacts publiés par {@code PlaywrightExtension} (capture, trace, log, URL)
 * via les « report entries » JUnit, sans couplage direct avec l'extension.</p>
 */
public class RapportHtmlListener implements TestExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(RapportHtmlListener.class);
    private static final String PREFIXE_ARTEFACT = "e2e.";

    private TestPlan plan;
    private Instant debut;
    private final Map<String, ResultatTest> resultats = new LinkedHashMap<>();
    private final Map<String, Long> departs = new HashMap<>();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.plan = testPlan;
        this.debut = Instant.now();
        resultats.clear();
        departs.clear();
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        departs.put(id.getUniqueId(), System.nanoTime());
        if (id.isTest()) {
            resultat(id);
        }
    }

    @Override
    public void reportingEntryPublished(TestIdentifier id, ReportEntry entree) {
        entree.getKeyValuePairs().forEach((cle, valeur) -> {
            if (cle.startsWith(PREFIXE_ARTEFACT)) {
                resultat(id).artefacts.put(cle, valeur);
            }
        });
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult resultatExecution) {
        boolean conteneurEnErreur = id.isContainer()
                && resultatExecution.getStatus() != TestExecutionResult.Status.SUCCESSFUL;
        if (!id.isTest() && !conteneurEnErreur) {
            return;
        }
        ResultatTest resultat = resultat(id);
        resultat.statut = switch (resultatExecution.getStatus()) {
            case SUCCESSFUL -> ResultatTest.Statut.SUCCES;
            case ABORTED -> ResultatTest.Statut.INTERROMPU;
            case FAILED -> ResultatTest.Statut.ECHEC;
        };
        resultat.erreur = resultatExecution.getThrowable().orElse(null);
        Long depart = departs.remove(id.getUniqueId());
        if (depart != null) {
            resultat.duree = Duration.ofNanos(System.nanoTime() - depart);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String raison) {
        if (id.isTest()) {
            marquerIgnore(id, raison);
            return;
        }
        var descendants = plan.getDescendants(id).stream().filter(TestIdentifier::isTest).toList();
        if (descendants.isEmpty()) {
            marquerIgnore(id, raison);
        } else {
            descendants.forEach(test -> marquerIgnore(test, raison));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (resultats.isEmpty()) {
            return;
        }
        Path racine = RepertoireRapport.racine();
        try {
            Path rapport = new GenerateurRapportHtml().generer(
                    racine, resultats.values(), environnement(), debut, Duration.between(debut, Instant.now()));
            LOG.info("Rapport HTML généré : {}", rapport.toAbsolutePath().toUri());
        } catch (Exception e) {
            LOG.error("Génération du rapport HTML impossible", e);
        }
    }

    // ------------------------------------------------------------------ interne

    private void marquerIgnore(TestIdentifier id, String raison) {
        ResultatTest resultat = resultat(id);
        resultat.statut = ResultatTest.Statut.IGNORE;
        resultat.raison = raison;
    }

    private ResultatTest resultat(TestIdentifier id) {
        return resultats.computeIfAbsent(id.getUniqueId(), cle -> creerResultat(id));
    }

    /** Suite = classe de test (la plus externe) ; nom = méthode › invocation. */
    private ResultatTest creerResultat(TestIdentifier id) {
        Deque<String> chemin = new ArrayDeque<>();
        Optional<TestIdentifier> courant = Optional.of(id);
        while (courant.isPresent() && plan.getParent(courant.get()).isPresent()) {
            chemin.addFirst(courant.get().getDisplayName());
            courant = plan.getParent(courant.get());
        }
        String suite = chemin.isEmpty() ? id.getDisplayName() : chemin.removeFirst();
        String nom = chemin.isEmpty() ? "(initialisation de la classe)" : String.join(" › ", chemin);
        return new ResultatTest(id.getUniqueId(), suite, nom, source(id));
    }

    private static String source(TestIdentifier id) {
        return id.getSource().map(source -> {
            if (source instanceof MethodSource methode) {
                return methode.getClassName() + "#" + methode.getMethodName();
            }
            if (source instanceof ClassSource classe) {
                return classe.getClassName();
            }
            return source.toString();
        }).orElse("");
    }

    private static Map<String, String> environnement() {
        Map<String, String> env = new LinkedHashMap<>();
        try {
            Configuration config = Configuration.instance();
            env.put("Environnement", config.environnement());
            env.put("URL de base", config.get("base.url", "?"));
            env.put("Navigateur", config.get("navigateur", "chromium")
                    + config.valeur("navigateur.canal").map(c -> " (" + c + ")").orElse(""));
            env.put("Headless", config.get("headless", "true"));
            env.put("Traces", config.get("traces.mode", "echec"));
        } catch (RuntimeException e) {
            env.put("Configuration", "non chargée : " + e.getMessage());
        }
        env.put("Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        env.put("Système", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        env.put("Utilisateur", System.getProperty("user.name"));
        return env;
    }
}
