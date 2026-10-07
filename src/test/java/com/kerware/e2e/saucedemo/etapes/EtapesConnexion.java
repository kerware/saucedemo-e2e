// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.etapes;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.cucumber.ContexteScenario;
import com.kerware.e2e.saucedemo.pages.InventairePage;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import io.cucumber.java.fr.Etantdonné;

/** Étapes de précondition liées à la connexion. */
public class EtapesConnexion {

    private final ContexteScenario contexte;

    public EtapesConnexion(ContexteScenario contexte) {
        this.contexte = contexte;
    }

    /** Connexion par l'IHM avec {@code utilisateur.defaut} / {@code motdepasse.defaut} : aucun identifiant en dur. */
    @Etantdonné("un utilisateur connecté sur la page d'inventaire")
    public void unUtilisateurConnecte() {
        Configuration config = contexte.configuration();
        InventairePage inventaire = contexte.page(LoginPage.class)
                .ouvrir()
                .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
        contexte.definirPageCourante(inventaire);
    }
}
