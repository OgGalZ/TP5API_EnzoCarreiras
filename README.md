# TP5 — Sécurisation d'une API REST avec Spring Boot

## Lancer le projet (Eclipse)

1. `File > Import > Maven > Existing Maven Projects` → sélectionner le dossier `tp-securite-api`.
2. Attendre le téléchargement des dépendances (clic droit projet > `Maven > Update Project` si besoin).
3. Clic droit sur `TpSecuriteApiApplication.java` > `Run As > Java Application`.
4. L'API écoute sur **https://localhost:8443** (et http://localhost:8080 redirige vers HTTPS).

Lancer les tests automatisés (tout le tableau de l'exercice 6) : clic droit sur `SecuriteApiTests.java` > `Run As > JUnit Test`, ou `mvn test`.

## Postman

1. `Import` → `TP5-securite.postman_collection.json`.
2. `Settings > General` → désactiver **SSL certificate verification** (certificat auto-signé, normal pour le TP).
3. Lancer d'abord `0a` et `0b` : les tokens user/admin sont enregistrés automatiquement dans les variables de la collection.
4. Lancer les requêtes 1 à 8 et faire une capture pour chacune.

⚠️ Le rate limit (5 requêtes/minute par IP) s'applique à **toutes** les routes, login compris. Pendant les captures, attends ~1 minute entre deux groupes de requêtes, sinon tu prendras des 429 au mauvais moment. Pour la capture du 429, lance la requête 6 six fois d'affilée.

## Comptes et clés

| Élément | Valeur |
|---|---|
| Clé API | `Authorization: ApiKey 123456abcdef` |
| Compte admin | `admin` / `admin123` |
| Compte user | `user` / `user123` |
| Mot de passe du keystore | `tpsecurite` |

Tous les secrets sont surchargeables par variables d'environnement : `API_KEY`, `JWT_SECRET`, `KEYSTORE_PASSWORD`.

## Endpoints

| Méthode | Route | Protection |
|---|---|---|
| POST | `/auth/login` | Public — renvoie un JWT |
| GET | `/api/utilisateurs` | Clé API |
| GET | `/utilisateurs/user` | JWT (user ou admin) |
| GET | `/utilisateurs/admin` | JWT rôle admin |
| GET | `/utilisateurs`, `/utilisateurs/{id}` | JWT (user ou admin) |
| POST | `/utilisateurs` | JWT rôle admin — validation + anti-XSS |
| GET | `/utilisateurs/recherche?nom=` | JWT — requête paramétrée (anti-injection SQL) |
| GET | `/swagger-ui.html`, `/v3/api-docs` | Public — documentation |

## Bonus réalisés

- **Base H2 + JPA** : comptes (`comptes`) et utilisateurs (`utilisateurs`) en base, mots de passe en BCrypt, créés au démarrage par `DataInitializer`. Base en mémoire : elle repart de zéro à chaque redémarrage.
- **Bucket4j** : implémentation par défaut du rate limiting. Pour revenir à la version maison de l'exo 4 : `app.security.rate-limit.impl=maison`.
- **Swagger UI** : https://localhost:8443/swagger-ui.html → bouton **Authorize** :
  - `bearerAuth` : coller le token seul (sans « Bearer ») ;
  - `apiKey` : saisir `ApiKey 123456abcdef`.
- **CORS** : `config/WebConfig.java`.

## Régénérer le certificat (facultatif)

```bash
keytool -genkeypair -alias tpapi -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 365 \
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
```
Puis mettre le mot de passe choisi dans `server.ssl.key-store-password`.
