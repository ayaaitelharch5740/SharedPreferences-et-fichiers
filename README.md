# Lab 2 — SecureStorageLabJava (Persistance locale sécurisée Android)

## Objectif

Mettre en place une **persistance locale complète et sécurisée** dans une application Android Java :
- préférences non sensibles via `SharedPreferences`,
- secrets chiffrés via `EncryptedSharedPreferences`,
- fichiers internes (texte UTF-8, JSON),
- cache temporaire (`cacheDir`),
- stockage externe app-specific.

L'application fonctionne **entièrement hors connexion**.

---

## Prérequis

| Élément | Détail |
|---|---|
| IDE | Android Studio |
| Langage | Java |
| SDK minimum | API 24 (recommandé) |
| Dépendance | `androidx.security:security-crypto:1.1.0-alpha06` |
| Connexion Internet | Non requise à l'exécution |

---

## Architecture du projet

```
SecureStorageLabJava/
└── app/src/main/java/com/example/securestoragejava/
    ├── ui/
    │   └── MainActivity.java          ← Écran unique, tous les boutons
    ├── prefs/
    │   ├── AppPrefs.java              ← SharedPreferences (nom, langue, thème)
    │   └── SecurePrefs.java           ← EncryptedSharedPreferences (token)
    ├── files/
    │   ├── InternalTextStore.java     ← Lecture/écriture texte UTF-8 interne
    │   └── StudentsJsonStore.java     ← Sauvegarde/chargement JSON interne
    ├── cache/
    │   └── CacheStore.java            ← Écriture/lecture/purge dans cacheDir
    ├── external/
    │   └── ExternalAppFilesStore.java ← Export vers stockage externe app-specific
    └── model/
        └── Student.java               ← Modèle de données simple (id, name, age)
```

---

## Étapes de réalisation

### Tâche 1 — Création du projet et dépendances

- Créer un projet **Empty Views Activity** (Java, min SDK 24).
- Ajouter la dépendance `security-crypto` dans `build.gradle` (Module : app).
- Vérifier : Sync Gradle réussi, build OK, lancement sans erreur.

### Tâche 2 — SharedPreferences : préférences non sensibles

Classe `prefs/AppPrefs.java` :
- Stocker `name`, `lang`, `theme` avec `MODE_PRIVATE`.
- `apply()` pour les sauvegardes UI (asynchrone, sans retour).
- `commit()` disponible si une confirmation synchrone est nécessaire.
- Méthode `clear()` pour le nettoyage complet.

### Tâche 3 — EncryptedSharedPreferences : token sécurisé

Classe `prefs/SecurePrefs.java` :
- Utiliser `MasterKey` (schéma `AES256_GCM`) appuyé sur le Keystore Android.
- Chiffrer clés et valeurs avec `EncryptedSharedPreferences` (schémas `AES256_SIV` / `AES256_GCM`).
- **Ne jamais logger le token** : afficher uniquement sa longueur (`tokenLength`).
- Gérer les exceptions au niveau UI sans fuiter d'informations sensibles.

### Tâche 4 — Fichiers internes : texte UTF-8 et JSON

Classes `files/InternalTextStore.java` et `files/StudentsJsonStore.java` :
- Écrire et lire des fichiers via `context.openFileOutput` / `openFileInput` avec `MODE_PRIVATE`.
- Encodage **UTF-8** imposé pour tous les fichiers texte.
- Sérialisation/désérialisation JSON avec `org.json` (`JSONArray`, `JSONObject`).
- En cas de fichier absent ou corrompu : retourner une liste vide (pas d'exception propagée).
- Vérification via **Device File Explorer** : `/data/data/<package>/files/`.

### Tâche 5 — Cache temporaire (`cacheDir`)

Classe `cache/CacheStore.java` :
- Écrire dans `context.getCacheDir()` pour les données temporaires régénérables.
- Méthode `purge()` pour supprimer manuellement tous les fichiers du cache.
- Vérification via Device File Explorer : `/data/data/<package>/cache/`.

### Tâche 6 — Stockage externe app-specific

Classe `external/ExternalAppFilesStore.java` :
- Utiliser `context.getExternalFilesDir(null)` : aucune permission requise (API 19+).
- Retourner le chemin absolu du fichier créé pour vérification.
- Réservé à l'export contrôlé : **pas de stockage public** pour les données sensibles.

---

## Interface utilisateur (écran unique)

| Élément | Rôle |
|---|---|
| `EditText` nom | Saisie du nom utilisateur |
| `Spinner` langue | Sélection fr / en / ar |
| `Switch` thème | Activation du thème sombre |
| `EditText` token | Saisie du secret (masqué, `textPassword`) |
| Bouton **Sauvegarder prefs** | Sauvegarde nom/langue/thème + token chiffré |
| Bouton **Charger prefs** | Restaure les valeurs depuis les préférences |
| Bouton **Sauvegarder fichier JSON** | Écrit `students.json` et `note.txt` |
| Bouton **Charger fichier JSON** | Lit et affiche la liste des étudiants |
| Bouton **Effacer** | Nettoyage complet (prefs + secure + fichiers + cache) |
| `TextView` résultat | Affichage des retours (sans données sensibles) |

---

## Règles de sécurité appliquées

1. Aucun token / mot de passe n'apparaît dans Logcat.
2. `EncryptedSharedPreferences` utilisé pour tous les secrets.
3. `MODE_PRIVATE` systématique pour fichiers internes et préférences claires.
4. Token masqué à l'écran (`inputType="textPassword"`).
5. Nettoyage complet disponible (prefs + secure prefs + fichiers + cache).
6. Cache réservé aux données temporaires et régénérables.
7. Export externe limité au répertoire app-specific (pas de stockage public).
8. Exceptions gérées sans fuite d'informations sensibles.
9. Encodage UTF-8 imposé pour tous les fichiers texte.
10. Token non affiché en clair : longueur uniquement (`tokenLength`).
11. Vérification systématique via Device File Explorer.
12. Concept d'expiration de token : stocker une date de création, invalider après délai, forcer le renouvellement.

---

## Récapitulatif des mécanismes de stockage

| Mécanisme | Classe | Usage | Permission |
|---|---|---|---|
| `SharedPreferences` | `AppPrefs` | Préférences UI non sensibles | Aucune |
| `EncryptedSharedPreferences` | `SecurePrefs` | Secrets (token, clé API) | Aucune |
| Fichiers internes | `InternalTextStore`, `StudentsJsonStore` | Texte UTF-8, JSON | Aucune |
| Cache | `CacheStore` | Données temporaires régénérables | Aucune |
| Externe app-specific | `ExternalAppFilesStore` | Export contrôlé | Aucune (API 19+) |

---

## Différence `apply()` vs `commit()`

| | `apply()` | `commit()` |
|---|---|---|
| Exécution | Asynchrone | Synchrone (bloquant) |
| Retour | Aucun | `boolean` (succès/échec) |
| Usage recommandé | Préférences UI | Quand la confirmation immédiate est critique |

---

## Notes importantes

- La dépendance `security-crypto` peut nécessiter que les artefacts Gradle soient déjà présents sur la machine de développement (pas de téléchargement réseau requis à l'exécution).
- `ANDROID_ID` peut changer après une réinitialisation d'usine ou dans certains contextes multi-utilisateurs : ne pas l'utiliser comme identifiant permanent critique.
- Le `cacheDir` peut être vidé par le système Android en cas de manque d'espace : ne jamais y stocker de données essentielles.
