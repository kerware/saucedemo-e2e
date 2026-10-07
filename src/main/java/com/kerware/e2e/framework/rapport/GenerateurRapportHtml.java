package com.kerware.e2e.framework.rapport;

import com.kerware.e2e.framework.extension.PlaywrightExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Génère un rapport HTML autonome (un seul fichier, sans dépendance externe) :
 * synthèse, environnement, filtres par statut, et pour chaque test en échec :
 * message, capture d'écran, trace Playwright, URL, log détaillé et pile d'appels.
 */
final class GenerateurRapportHtml {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int TAILLE_MAX_LOG = 200_000;

    Path generer(Path racine, Collection<ResultatTest> resultats, Map<String, String> environnement,
                 Instant debut, Duration duree) throws IOException {
        Files.createDirectories(racine);
        Map<ResultatTest.Statut, Integer> compteurs = new EnumMap<>(ResultatTest.Statut.class);
        for (ResultatTest.Statut statut : ResultatTest.Statut.values()) {
            compteurs.put(statut, 0);
        }
        resultats.forEach(r -> compteurs.merge(r.statut, 1, Integer::sum));

        Map<String, List<ResultatTest>> parSuite = new LinkedHashMap<>();
        resultats.forEach(r -> parSuite.computeIfAbsent(r.suite, s -> new java.util.ArrayList<>()).add(r));

        StringBuilder html = new StringBuilder(64_000);
        entete(html);
        synthese(html, compteurs, resultats.size(), debut, duree);
        environnement(html, environnement);
        filtres(html);
        parSuite.forEach((suite, tests) -> suite(html, racine, suite, tests));
        pied(html);

        Path fichier = racine.resolve("index.html");
        Files.writeString(fichier, html, StandardCharsets.UTF_8);
        return fichier;
    }

    // ------------------------------------------------------------------ sections

    private static void entete(StringBuilder html) {
        html.append("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Rapport E2E</title>
                <style>
                :root{--fond:#f6f7f9;--carte:#fff;--texte:#1d2330;--doux:#5b6475;--bord:#e2e5eb;
                --succes:#1f8a4c;--echec:#c62f3a;--interrompu:#b7791f;--ignore:#6b7280;--code:#f1f3f6}
                @media (prefers-color-scheme:dark){:root{--fond:#14171c;--carte:#1c2027;--texte:#e6e9ef;--doux:#9aa3b2;
                --bord:#2c323c;--succes:#3fbf73;--echec:#f0606b;--interrompu:#e0a84a;--ignore:#9aa3b2;--code:#11141a}}
                *{box-sizing:border-box}body{margin:0;background:var(--fond);color:var(--texte);
                font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
                main{max-width:1150px;margin:0 auto;padding:24px 16px 64px}
                h1{font-size:26px;margin:0 0 4px}h2{font-size:18px;margin:0}
                .sous-titre{color:var(--doux);margin:0 0 20px}
                .tuiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-bottom:12px}
                .tuile{background:var(--carte);border:1px solid var(--bord);border-radius:10px;padding:14px}
                .tuile b{display:block;font-size:28px;font-variant-numeric:tabular-nums}.tuile span{color:var(--doux);font-size:13px}
                .t-succes b{color:var(--succes)}.t-echec b{color:var(--echec)}.t-ignore b{color:var(--ignore)}
                .barre{display:flex;height:8px;border-radius:4px;overflow:hidden;background:var(--bord);margin:8px 0 24px}
                .barre i{display:block}.b-succes{background:var(--succes)}.b-echec{background:var(--echec)}
                .b-interrompu{background:var(--interrompu)}.b-ignore{background:var(--ignore)}
                .carte{background:var(--carte);border:1px solid var(--bord);border-radius:10px;padding:16px;margin-bottom:16px}
                dl.env{display:grid;grid-template-columns:max-content 1fr;gap:4px 16px;margin:8px 0 0}
                dl.env dt{color:var(--doux)}dl.env dd{margin:0;word-break:break-all}
                .filtres{display:flex;gap:8px;flex-wrap:wrap;margin:0 0 16px}
                .filtres button{border:1px solid var(--bord);background:var(--carte);color:var(--texte);
                border-radius:999px;padding:6px 14px;cursor:pointer;font:inherit}
                .filtres button.actif{background:var(--texte);color:var(--carte)}
                .suite-entete{display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:8px}
                .suite-entete small{color:var(--doux)}
                details.test{border-top:1px solid var(--bord)}details.test:first-of-type{border-top:0}
                details.test>summary{display:flex;align-items:center;gap:10px;padding:10px 4px;cursor:pointer;list-style:none}
                details.test>summary::-webkit-details-marker{display:none}
                .nom{flex:1;min-width:0;overflow-wrap:anywhere}.duree{color:var(--doux);font-size:13px;font-variant-numeric:tabular-nums}
                .badge{font-size:12px;font-weight:600;padding:2px 8px;border-radius:999px;color:#fff;white-space:nowrap}
                .s-succes{background:var(--succes)}.s-echec{background:var(--echec)}.s-interrompu{background:var(--interrompu)}.s-ignore{background:var(--ignore)}
                .corps{padding:0 4px 16px}.corps h3{font-size:14px;margin:14px 0 6px;color:var(--doux)}
                pre{background:var(--code);border:1px solid var(--bord);border-radius:6px;padding:10px;overflow:auto;
                max-height:420px;font:12.5px/1.45 ui-monospace,Consolas,monospace;white-space:pre-wrap;word-break:break-word;margin:0}
                .message{border-left:3px solid var(--echec)}
                img.capture{max-width:100%;border:1px solid var(--bord);border-radius:6px;display:block}
                code{font:12.5px ui-monospace,Consolas,monospace;background:var(--code);padding:1px 5px;border-radius:4px;overflow-wrap:anywhere}
                a{color:inherit}.masque{display:none}
                </style>
                </head>
                <body><main>
                """);
    }

    private static void synthese(StringBuilder html, Map<ResultatTest.Statut, Integer> compteurs, int total,
                                 Instant debut, Duration duree) {
        int succes = compteurs.get(ResultatTest.Statut.SUCCES);
        int echecs = compteurs.get(ResultatTest.Statut.ECHEC);
        int interrompus = compteurs.get(ResultatTest.Statut.INTERROMPU);
        int ignores = compteurs.get(ResultatTest.Statut.IGNORE);
        int executes = total - ignores;
        String taux = executes == 0 ? "-" : Math.round(100.0 * succes / executes) + " %";

        html.append("<h1>Rapport d'exécution des tests E2E</h1>")
                .append("<p class=\"sous-titre\">Lancé le ").append(FORMAT_DATE.format(debut))
                .append(" · durée ").append(formatDuree(duree)).append("</p>")
                .append("<div class=\"tuiles\">")
                .append(tuile("", total, "Tests"))
                .append(tuile("t-succes", succes, "Succès"))
                .append(tuile("t-echec", echecs + interrompus, "Échecs / interrompus"))
                .append(tuile("t-ignore", ignores, "Ignorés"))
                .append("<div class=\"tuile\"><b>").append(taux).append("</b><span>Taux de réussite</span></div>")
                .append("</div><div class=\"barre\" role=\"img\" aria-label=\"Répartition des résultats\">")
                .append(segment("b-succes", succes, total))
                .append(segment("b-echec", echecs, total))
                .append(segment("b-interrompu", interrompus, total))
                .append(segment("b-ignore", ignores, total))
                .append("</div>");
    }

    private static void environnement(StringBuilder html, Map<String, String> environnement) {
        html.append("<div class=\"carte\"><h2>Environnement</h2><dl class=\"env\">");
        environnement.forEach((cle, valeur) ->
                html.append("<dt>").append(echapper(cle)).append("</dt><dd>").append(echapper(valeur)).append("</dd>"));
        html.append("</dl></div>");
    }

    private static void filtres(StringBuilder html) {
        html.append("""
                <div class="filtres" role="group" aria-label="Filtrer par statut">
                <button class="actif" data-filtre="tous">Tous</button>
                <button data-filtre="echec">Échecs</button>
                <button data-filtre="succes">Succès</button>
                <button data-filtre="ignore">Ignorés</button>
                </div>
                """);
    }

    private static void suite(StringBuilder html, Path racine, String suite, List<ResultatTest> tests) {
        long echecs = tests.stream().filter(t -> t.statut == ResultatTest.Statut.ECHEC
                || t.statut == ResultatTest.Statut.INTERROMPU).count();
        html.append("<section class=\"carte suite\"><div class=\"suite-entete\"><h2>").append(echapper(suite))
                .append("</h2><small>").append(tests.size()).append(" test(s) · ").append(echecs).append(" en échec</small></div>");
        tests.forEach(test -> test(html, racine, test));
        html.append("</section>");
    }

    private static void test(StringBuilder html, Path racine, ResultatTest test) {
        boolean enEchec = test.statut == ResultatTest.Statut.ECHEC || test.statut == ResultatTest.Statut.INTERROMPU;
        String filtre = enEchec ? "echec" : test.statut.code;

        html.append("<details class=\"test\" data-statut=\"").append(filtre).append("\"")
                .append(enEchec ? " open" : "").append("><summary>")
                .append("<span class=\"badge s-").append(test.statut.code).append("\">").append(test.statut.libelle).append("</span>")
                .append("<span class=\"nom\">").append(echapper(test.nom)).append("</span>")
                .append("<span class=\"duree\">").append(formatDuree(test.duree)).append("</span>")
                .append("</summary><div class=\"corps\">");

        if (!test.source.isEmpty()) {
            html.append("<h3>Source</h3><code>").append(echapper(test.source)).append("</code>");
        }
        if (test.raison != null) {
            html.append("<h3>Raison</h3><pre>").append(echapper(test.raison)).append("</pre>");
        }
        if (test.erreur != null) {
            html.append("<h3>Message d'erreur</h3><pre class=\"message\">")
                    .append(echapper(String.valueOf(test.erreur.getMessage()))).append("</pre>");
        }
        String url = test.artefacts.get(PlaywrightExtension.ENTREE_URL);
        if (url != null) {
            html.append("<h3>URL au moment de l'échec</h3><code>").append(echapper(url)).append("</code>");
        }
        String capture = test.artefacts.get(PlaywrightExtension.ENTREE_CAPTURE);
        if (capture != null) {
            html.append("<h3>Capture d'écran</h3><a href=\"").append(echapper(capture)).append("\" target=\"_blank\">")
                    .append("<img class=\"capture\" loading=\"lazy\" src=\"").append(echapper(capture))
                    .append("\" alt=\"Capture d'écran au moment de l'échec\"></a>");
        }
        String trace = test.artefacts.get(PlaywrightExtension.ENTREE_TRACE);
        if (trace != null) {
            html.append("<h3>Trace Playwright</h3><p><a href=\"").append(echapper(trace)).append("\">")
                    .append(echapper(trace)).append("</a> — à ouvrir sur <a href=\"https://trace.playwright.dev\">trace.playwright.dev</a> ou avec <code>mvn exec:java \"-Dexec.args=show-trace ")
                    .append(echapper(racine.resolve(trace).toString().replace('\\', '/'))).append("\"</code></p>");
        }
        String log = test.artefacts.get(PlaywrightExtension.ENTREE_LOG);
        if (log != null) {
            html.append("<details").append(enEchec ? " open" : "").append("><summary><h3 style=\"display:inline\">Log détaillé</h3> (<a href=\"")
                    .append(echapper(log)).append("\">fichier</a>)</summary><pre>")
                    .append(echapper(lireLog(racine.resolve(log)))).append("</pre></details>");
        }
        if (test.erreur != null) {
            html.append("<details><summary><h3 style=\"display:inline\">Pile d'appels</h3></summary><pre>")
                    .append(echapper(pile(test.erreur))).append("</pre></details>");
        }
        html.append("</div></details>");
    }

    private static void pied(StringBuilder html) {
        html.append("""
                <script>
                document.querySelectorAll('.filtres button').forEach(bouton => bouton.addEventListener('click', () => {
                  document.querySelectorAll('.filtres button').forEach(b => b.classList.toggle('actif', b === bouton));
                  const filtre = bouton.dataset.filtre;
                  document.querySelectorAll('details.test').forEach(t =>
                    t.classList.toggle('masque', filtre !== 'tous' && t.dataset.statut !== filtre));
                  document.querySelectorAll('section.suite').forEach(s =>
                    s.classList.toggle('masque', !s.querySelector('details.test:not(.masque)')));
                }));
                </script>
                </main></body></html>
                """);
    }

    // ------------------------------------------------------------------ utilitaires

    private static String tuile(String classe, int valeur, String libelle) {
        return "<div class=\"tuile " + classe + "\"><b>" + valeur + "</b><span>" + libelle + "</span></div>";
    }

    private static String segment(String classe, int valeur, int total) {
        if (valeur == 0 || total == 0) {
            return "";
        }
        return "<i class=\"" + classe + "\" style=\"width:" + (100.0 * valeur / total) + "%\"></i>";
    }

    private static String lireLog(Path fichier) {
        try {
            if (!Files.exists(fichier)) {
                return "(fichier de log introuvable : " + fichier + ")";
            }
            String contenu = Files.readString(fichier, StandardCharsets.UTF_8);
            return contenu.length() > TAILLE_MAX_LOG
                    ? contenu.substring(0, TAILLE_MAX_LOG) + "\n[... tronqué, voir le fichier complet]"
                    : contenu;
        } catch (IOException e) {
            return "(lecture du log impossible : " + e.getMessage() + ")";
        }
    }

    private static String pile(Throwable erreur) {
        StringWriter ecrivain = new StringWriter();
        erreur.printStackTrace(new PrintWriter(ecrivain));
        return ecrivain.toString();
    }

    static String formatDuree(Duration duree) {
        long ms = duree.toMillis();
        if (ms < 1000) {
            return ms + " ms";
        }
        if (ms < 60_000) {
            return String.format(java.util.Locale.FRANCE, "%.1f s", ms / 1000.0);
        }
        return (ms / 60_000) + " min " + ((ms % 60_000) / 1000) + " s";
    }

    static String echapper(String texte) {
        if (texte == null) {
            return "";
        }
        StringBuilder sortie = new StringBuilder(texte.length());
        for (char c : texte.toCharArray()) {
            switch (c) {
                case '<' -> sortie.append("&lt;");
                case '>' -> sortie.append("&gt;");
                case '&' -> sortie.append("&amp;");
                case '"' -> sortie.append("&quot;");
                case '\'' -> sortie.append("&#39;");
                default -> sortie.append(c);
            }
        }
        return sortie.toString();
    }
}
