# Installation

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| JDK | 21 ou plus (Temurin recommandé) | `java -version` |
| Maven | 3.9 ou plus | `mvn -version` |
| Accès réseau | Maven Central, CDN Playwright, www.saucedemo.com | — |
| IDE (optionnel) | IntelliJ IDEA, Eclipse ou VS Code + Extension Pack for Java | — |

Le plugin `maven-enforcer` bloque le build si le JDK ou Maven sont trop anciens.

## 2. Installer le JDK 21 et Maven

### Windows (PowerShell)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven

# Rouvrir le terminal, puis vérifier
java -version
mvn -version
```

Sans `winget` : télécharger le JDK sur https://adoptium.net et Maven sur https://maven.apache.org/download.cgi. Ensuite, définir `JAVA_HOME` et ajouter `%JAVA_HOME%\bin` et `<maven>\bin` au `Path`.

### macOS

```bash
brew install --cask temurin@21
brew install maven
```

### Linux (Debian/Ubuntu)

```bash
sudo apt install openjdk-21-jdk maven
```

## 3. Récupérer le projet

Décompresser `saucedemo-e2e.zip`, puis ouvrir un terminal dans le dossier `saucedemo-e2e`.

## 4. Télécharger les dépendances et compiler

```bash
mvn clean compile
```

Au premier lancement, Maven télécharge JUnit 6, Playwright, Logback, etc.

## 5. Installer les navigateurs Playwright

Playwright Java télécharge automatiquement ses navigateurs au premier lancement. Les installer à l'avance évite un premier test très lent :

```bash
# Tous les navigateurs (Chromium, Firefox, WebKit)
mvn exec:java "-Dexec.args=install"

# Chromium seulement
mvn exec:java "-Dexec.args=install chromium"

# Linux / CI : avec les dépendances système
mvn exec:java "-Dexec.args=install --with-deps chromium"
```

Les navigateurs sont stockés dans le cache utilisateur (`%USERPROFILE%\AppData\Local\ms-playwright` sous Windows, `~/.cache/ms-playwright` sous Linux, `~/Library/Caches/ms-playwright` sous macOS).

**Alternative sans téléchargement.** Pour utiliser le Chrome ou l'Edge déjà installé sur le poste :

```bash
mvn clean verify "-Dnavigateur.canal=chrome"
mvn clean verify "-Dnavigateur.canal=msedge"
```

## 6. Vérifier l'installation

```bash
mvn clean verify -Dgroups=smoke
```

Résultat attendu : `Tests run: 2, Failures: 0`, puis le rapport `target/rapport-e2e/index.html`.

## 7. Importer dans l'IDE

- **IntelliJ IDEA** : *File › Open* puis sélectionner `pom.xml` › *Open as Project*. Vérifier *Project Structure › SDK = 21*. Les tests se lancent avec la flèche verte. Le rapport HTML est aussi généré, car le listener est enregistré via `ServiceLoader`.
- **Eclipse** : *File › Import › Existing Maven Projects*.
- **VS Code** : ouvrir le dossier avec l'*Extension Pack for Java* installé.

Pour passer une option depuis l'IDE, ajouter `-Denv=local` dans les *VM options* de la configuration d'exécution.

## 8. Derrière un proxy d'entreprise

- **Maven** : déclarer le proxy dans `~/.m2/settings.xml` (section `<proxies>`).
- **Téléchargement des navigateurs** : définir `HTTPS_PROXY=http://proxy:port` avant `mvn exec:java ...`.
- **Miroir interne** : définir `PLAYWRIGHT_DOWNLOAD_HOST`, ou utiliser `navigateur.canal=chrome|msedge`.

## 9. Dépannage de l'installation

| Symptôme | Cause / solution |
|---|---|
| `Rule 0: RequireJavaVersion failed` | Le JDK actif n'est pas en version 21. Corriger `JAVA_HOME`. |
| `Executable doesn't exist at ...ms-playwright...` | Navigateurs non installés : voir l'étape 5. |
| `Host system is missing dependencies` (Linux) | Lancer `install --with-deps`. |
| Erreurs `-D` non reconnues sous PowerShell | Mettre l'argument entre guillemets : `"-Denv=local"`. |
| `Fichier de configuration introuvable : config/xxx.properties` | La valeur de `-Denv` ne correspond à aucun fichier de `src/test/resources/config/`. |
