// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.cucumber;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.execution.CycleDeVieTest;
import com.kerware.e2e.framework.navigateur.SessionNavigateur;
import com.kerware.e2e.framework.rapport.CanalArtefacts;
import com.kerware.e2e.framework.rapport.RepertoireRapport;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hooks Cucumber du framework : l'adaptateur Cucumber de {@link CycleDeVieTest}.
 *
 * <p>Pour chaque scénario, le même comportement que pour un test JUnit annoté {@code @TestE2E} :
 * contexte navigateur neuf, log par scénario, capture et trace en cas d'échec, artefacts
 * dans le rapport HTML. En plus, la capture d'un scénario en échec est jointe au rapport
 * Cucumber.</p>
 *
 * <p>À déclarer dans la glue : {@code cucumber.glue=com.kerware.e2e.framework.cucumber,...}.</p>
 *
 * <p>La session navigateur est démarrée au premier scénario et fermée après le dernier
 * ({@link AfterAll}). Elle est distincte de celle des tests JUnit, gérée par l'extension.</p>
 */
public class HooksPlaywright {

    private static final Logger LOG = LoggerFactory.getLogger(HooksPlaywright.class);

    /** Ordre des hooks : démarrage avant ceux de l'application, arrêt après eux (les @After d'ordre bas passent en dernier). */
    private static final int ORDRE_SOCLE = 0;
    private static final int LONGUEUR_MAX_NOM = 80;

    private static SessionNavigateur session;

    private final ContexteScenario contexte;

    public HooksPlaywright(ContexteScenario contexte) {
        this.contexte = contexte;
    }

    @Before(order = ORDRE_SOCLE)
    public void demarrerScenario(Scenario scenario) {
        Configuration config = Configuration.instance();
        contexte.demarrer(CycleDeVieTest.demarrer(
                HooksPlaywright::session,
                config,
                identifiantFichier(scenario),
                scenario.getName(),
                "Scénario : " + scenario.getUri() + ":" + scenario.getLine()
                        + " | Tags : " + scenario.getSourceTagNames()));
    }

    @After(order = ORDRE_SOCLE)
    public void terminerScenario(Scenario scenario) {
        CycleDeVieTest test = contexte.test();
        if (test == null) {
            return;
        }
        CycleDeVieTest.Resultat resultat = scenario.isFailed()
                ? CycleDeVieTest.Resultat.echec("Scénario en échec (statut " + scenario.getStatus()
                        + ") : voir l'étape en erreur ci-dessus")
                : CycleDeVieTest.Resultat.succes();
        test.terminer(resultat, (cle, valeur) -> {
            CanalArtefacts.publier(cle, valeur);
            if (CycleDeVieTest.ENTREE_CAPTURE.equals(cle)) {
                joindreCapture(scenario, valeur);
            }
        });
    }

    @AfterAll(order = ORDRE_SOCLE)
    public static void fermerSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    // ------------------------------------------------------------------ interne

    private static synchronized SessionNavigateur session() {
        if (session == null) {
            session = SessionNavigateur.demarrer(Configuration.instance());
        }
        return session;
    }

    /** Joint la capture d'écran au rapport Cucumber (en plus du rapport HTML du framework). */
    private static void joindreCapture(Scenario scenario, String cheminRelatif) {
        Path fichier = RepertoireRapport.racine().resolve(cheminRelatif);
        try {
            scenario.attach(Files.readAllBytes(fichier), "image/png", "Capture au moment de l'échec");
        } catch (IOException | RuntimeException e) {
            LOG.warn("Capture non jointe au rapport Cucumber : {}", e.getMessage());
        }
    }

    /**
     * Ex. : {@code tri_L19_Trier_par___Name__A_to_Z____TRI-01_}. Le fichier .feature et la ligne
     * (celle de la ligne d'exemple pour un plan de scénario) rendent l'identifiant unique.
     */
    static String identifiantFichier(Scenario scenario) {
        // toString() et non getPath() : « classpath:features/x.feature » est une URI opaque (getPath() == null)
        String chemin = scenario.getUri().toString();
        String fichier = chemin.substring(chemin.lastIndexOf('/') + 1).replaceFirst("\\.feature$", "");
        String nom = fichier + "_L" + scenario.getLine() + "_" + scenario.getName();
        String sur = CycleDeVieTest.nomDeFichierSur(nom);
        return sur.length() > LONGUEUR_MAX_NOM ? sur.substring(0, LONGUEUR_MAX_NOM) : sur;
    }
}
