package com.kerware.e2e.framework.navigateur;

import com.kerware.e2e.framework.config.Configuration;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Session Playwright partagée par toute l'exécution : un seul {@link Playwright}
 * et un seul {@link Browser} (coûteux à démarrer), puis un {@link BrowserContext}
 * neuf et isolé par test (cookies, stockage, cache).
 *
 * <p>Implémente {@link AutoCloseable} : stockée dans le store racine de JUnit,
 * elle est fermée automatiquement en fin d'exécution.</p>
 *
 * <p>Attention : Playwright n'est pas thread-safe. En cas d'exécution parallèle,
 * il faut une session par thread.</p>
 */
public final class SessionNavigateur implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNavigateur.class);

    private final Playwright playwright;
    private final Browser navigateur;

    private SessionNavigateur(Playwright playwright, Browser navigateur) {
        this.playwright = playwright;
        this.navigateur = navigateur;
    }

    public static SessionNavigateur demarrer(Configuration config) {
        String nom = config.get("navigateur", "chromium").toLowerCase(Locale.ROOT);
        boolean headless = config.getBoolean("headless", true);
        LOG.info("Démarrage de Playwright : navigateur={}, headless={}", nom, headless);

        PlaywrightAssertions.setDefaultAssertionTimeout(config.getDouble("timeout.assertion.ms", 15_000));

        Playwright playwright = Playwright.create();
        try {
            playwright.selectors().setTestIdAttribute(config.get("testid.attribut", "data-test"));

            BrowserType type = switch (nom) {
                case "chromium" -> playwright.chromium();
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> throw new IllegalArgumentException(
                        "Navigateur non supporté : '" + nom + "' (attendu : chromium, firefox ou webkit)");
            };

            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(config.getDouble("slowmo", 0));
            // Canal : "chrome" ou "msedge" pour utiliser le navigateur installé sur le poste
            config.valeur("navigateur.canal").ifPresent(options::setChannel);
            config.valeur("navigateur.executable").map(Path::of).ifPresent(options::setExecutablePath);

            Browser navigateur = type.launch(options);
            LOG.info("Navigateur démarré : {} {}", navigateur.browserType().name(), navigateur.version());
            return new SessionNavigateur(playwright, navigateur);
        } catch (RuntimeException e) {
            playwright.close();
            throw e;
        }
    }

    /** Nouveau contexte isolé, préconfiguré (URL de base, viewport, locale, timeouts). */
    public BrowserContext nouveauContexte(Configuration config) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setBaseURL(config.get("base.url"))
                .setViewportSize(config.getInt("viewport.largeur", 1366), config.getInt("viewport.hauteur", 768))
                .setLocale(config.get("locale", "fr-FR"));

        BrowserContext contexte = navigateur.newContext(options);
        contexte.setDefaultTimeout(config.getDouble("timeout.action.ms", 10_000));
        contexte.setDefaultNavigationTimeout(config.getDouble("timeout.navigation.ms", 30_000));
        return contexte;
    }

    @Override
    public void close() {
        LOG.info("Fermeture du navigateur et de Playwright");
        try {
            navigateur.close();
        } finally {
            playwright.close();
        }
    }
}
