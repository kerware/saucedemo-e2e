package com.kerware.e2e.framework.pages;

import com.kerware.e2e.framework.config.Configuration;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base des Page Objects.
 *
 * <p>Une page <em>assemble</em> des composants du référentiel et expose des actions métier.
 * Toute sous-classe concrète doit déclarer un constructeur public {@code (Page, Configuration)} :
 * c'est ce qui permet à l'extension de l'injecter directement en paramètre de test.</p>
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Page page;
    protected final Configuration config;

    protected BasePage(Page page, Configuration config) {
        this.page = page;
        this.config = config;
    }

    /** Page Playwright sous-jacente, pour les assertions : {@code assertThat(loginPage.page()).hasURL(...)}. */
    public Page page() {
        return page;
    }

    public String urlCourante() {
        return page.url();
    }

    public String titre() {
        return page.title();
    }

    /** Navigation relative à {@code base.url} (configurée sur le contexte navigateur). */
    protected void naviguer(String cheminRelatif) {
        log.info("Ouverture de la page {}{}", config.get("base.url"), cheminRelatif);
        page.navigate(cheminRelatif);
    }
}
