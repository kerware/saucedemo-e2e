package com.kerware.e2e.framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuration centralisée de l'exécution.
 *
 * <p>Ordre de priorité (du plus fort au plus faible) :</p>
 * <ol>
 *     <li>propriété système JVM : {@code -Dbase.url=...}</li>
 *     <li>variable d'environnement préfixée {@code E2E_} : {@code E2E_BASE_URL=...}</li>
 *     <li>fichier {@code config/<env>.properties} (environnement choisi par {@code -Denv=...})</li>
 *     <li>fichier {@code config/commun.properties} (valeurs par défaut)</li>
 * </ol>
 *
 * <p>Singleton chargé paresseusement et thread-safe (idiome du « holder »).</p>
 */
public final class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    public static final String CLE_ENVIRONNEMENT = "env";
    private static final String ENVIRONNEMENT_PAR_DEFAUT = "recette";
    private static final String PREFIXE_VARIABLE_ENV = "E2E_";

    private final String environnement;
    private final Properties proprietes;

    private Configuration(String environnement, Properties proprietes) {
        this.environnement = environnement;
        this.proprietes = proprietes;
    }

    public static Configuration instance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Configuration INSTANCE = charger();
    }

    private static Configuration charger() {
        String env = premierRenseigne(
                System.getProperty(CLE_ENVIRONNEMENT),
                System.getenv(nomVariableEnvironnement(CLE_ENVIRONNEMENT)),
                ENVIRONNEMENT_PAR_DEFAUT);

        Properties proprietes = new Properties();
        chargerFichier(proprietes, "config/commun.properties");
        chargerFichier(proprietes, "config/" + env + ".properties");

        LOG.info("Configuration chargée pour l'environnement '{}'", env);
        return new Configuration(env, proprietes);
    }

    private static void chargerFichier(Properties cible, String ressource) {
        ClassLoader chargeur = Thread.currentThread().getContextClassLoader();
        try (InputStream flux = chargeur.getResourceAsStream(ressource)) {
            if (flux == null) {
                throw new IllegalStateException(
                        "Fichier de configuration introuvable dans le classpath : " + ressource);
            }
            cible.load(new InputStreamReader(flux, StandardCharsets.UTF_8));
            LOG.debug("Fichier de configuration lu : {}", ressource);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible de " + ressource, e);
        }
    }

    /** Valeur de la clé si elle est renseignée à l'un des niveaux de priorité. */
    public Optional<String> valeur(String cle) {
        String valeur = System.getProperty(cle);
        if (estRenseigne(valeur)) {
            return Optional.of(valeur.trim());
        }
        valeur = System.getenv(nomVariableEnvironnement(cle));
        if (estRenseigne(valeur)) {
            return Optional.of(valeur.trim());
        }
        valeur = proprietes.getProperty(cle);
        return estRenseigne(valeur) ? Optional.of(valeur.trim()) : Optional.empty();
    }

    /** Valeur obligatoire : échoue explicitement si la clé est absente. */
    public String get(String cle) {
        return valeur(cle).orElseThrow(() -> new IllegalStateException(
                "Clé de configuration obligatoire absente : '" + cle + "' (environnement '" + environnement + "')"));
    }

    public String get(String cle, String defaut) {
        return valeur(cle).orElse(defaut);
    }

    public boolean getBoolean(String cle, boolean defaut) {
        return valeur(cle).map(Boolean::parseBoolean).orElse(defaut);
    }

    public int getInt(String cle, int defaut) {
        return valeur(cle).map(Integer::parseInt).orElse(defaut);
    }

    public double getDouble(String cle, double defaut) {
        return valeur(cle).map(Double::parseDouble).orElse(defaut);
    }

    public String environnement() {
        return environnement;
    }

    /** {@code base.url} devient {@code E2E_BASE_URL}. */
    static String nomVariableEnvironnement(String cle) {
        return PREFIXE_VARIABLE_ENV + cle.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    private static boolean estRenseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private static String premierRenseigne(String... valeurs) {
        for (String valeur : valeurs) {
            if (estRenseigne(valeur)) {
                return valeur.trim();
            }
        }
        throw new IllegalArgumentException("Aucune valeur renseignée");
    }
}
