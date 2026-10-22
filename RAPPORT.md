# TP5 – Sécurisation d'une API REST avec Spring Boot

Enzo Carreiras – EFREI Paris

## 1. Organisation du projet

Le projet suit la structure demandée (`model`, `controller`, `service`, `security`), avec trois packages en plus : `repository` pour l'accès à la base, `exception` pour la gestion des erreurs et `config` pour HTTPS, CORS, Swagger et l'initialisation des données.

L'API propose deux types d'accès :
- `/api/**` est protégé par la clé API (exercice 1). C'est l'accès « machine à machine ».
- `/utilisateurs/**` est protégé par JWT avec des rôles (exercice 2).

J'ai séparé les deux car les deux mécanismes utilisent le même en-tête `Authorization`. Sans ça, une requête avec un JWT aurait été bloquée par le filtre de clé API.

L'ordre des filtres dans la chaîne Spring Security est le suivant : rate limiting, puis clé API, puis JWT, puis les règles d'accès définies dans `SecurityConfig`. J'ai mis le rate limiting en premier pour rejeter les requêtes en trop avant de faire des traitements coûteux comme la vérification du JWT ou le BCrypt du login.

Les filtres ne font que de l'authentification : ils identifient le client et mettent son rôle dans le `SecurityContextHolder`. C'est `SecurityConfig` qui décide ensuite qui a le droit d'accéder à quoi. J'ai terminé les règles par `anyRequest().denyAll()` : une route que j'aurais oublié de déclarer est donc fermée par défaut.

Quelques choix que j'ai faits en plus de l'énoncé :
- la clé API et le secret JWT sont dans `application.properties` et peuvent être remplacés par des variables d'environnement, au lieu d'être écrits en dur dans le code ;
- la clé API est comparée avec `MessageDigest.isEqual`, qui compare en temps constant. Avec `equals`, le temps de réponse varie selon le nombre de caractères corrects, ce qui peut aider à deviner la clé ;
- au login, le message d'erreur est le même que le compte existe ou non, et le BCrypt est calculé dans les deux cas. On ne peut donc pas savoir quels usernames existent ;
- toutes les erreurs ont le même format JSON, y compris celles renvoyées par les filtres.

## 2. Exercice 1 – Clé API

**Pourquoi cette méthode est-elle faiblement sécurisée ?**

La clé est la même pour tous les clients et elle ne change jamais. Si elle fuit (dans un log, sur GitHub, en interceptant une requête HTTP), l'attaquant a accès à toute l'API sans limite de temps. On ne sait pas non plus *qui* fait la requête, seulement que la personne connaît la clé : impossible d'avoir des droits différents selon les clients ou de savoir qui a fait quoi. Pour révoquer la clé, il faut la changer pour tout le monde. Enfin, elle est envoyée à chaque requête, donc en HTTP n'importe qui sur le réseau peut la lire. Dans le code fourni, elle est en plus écrite en dur dans le code source.

**Comment la renforcer sans tout changer ?**

On peut garder le principe et :
- passer en HTTPS pour qu'elle ne circule plus en clair (fait à l'exercice 5) ;
- la sortir du code et la mettre dans une variable d'environnement (fait) ;
- donner une clé différente à chaque client, stockée hashée en base avec une date d'expiration, pour pouvoir en révoquer une seule ;
- changer les clés régulièrement ;
- la combiner avec le rate limiting (fait) et éventuellement une liste d'IP autorisées.

## 3. Exercice 2 – JWT et rôles

`POST /auth/login` vérifie le username et le mot de passe (comparé au hash BCrypt en base), puis renvoie un JWT signé en HS256 qui contient le username, le rôle et une expiration d'une heure.

`JwtFilter` récupère le token dans `Authorization: Bearer ...`, vérifie la signature et l'expiration, puis met `ROLE_USER` ou `ROLE_ADMIN` dans le contexte de sécurité. Résultat :
- un user sur `/utilisateurs/admin` reçoit 403 ;
- une requête sans token ou avec un token modifié reçoit 401.

**Différence entre un token signé et un token chiffré ?**

Un token signé (JWS, ce qu'on utilise ici) garantit l'intégrité : si quelqu'un modifie le payload, par exemple pour passer son rôle de `user` à `admin`, la signature ne correspond plus et le token est rejeté. Par contre, le contenu n'est pas caché : il est juste encodé en Base64, n'importe qui peut le décoder (sur jwt.io par exemple).

Un token chiffré (JWE) rend en plus le contenu illisible pour quelqu'un qui n'a pas la clé. La signature protège contre la modification, le chiffrement protège contre la lecture.

**Pourquoi ne jamais mettre de données sensibles dans un JWT ?**

Justement parce qu'il est lisible par toute personne qui l'a entre les mains. Et un token passe par beaucoup d'endroits : en-têtes HTTP, stockage du navigateur, logs des serveurs et des proxys… Il est aussi difficile à révoquer avant son expiration. Il faut donc n'y mettre que le minimum (identifiant, rôle, dates) et aller chercher le reste côté serveur.

## 4. Exercice 3 – XSS et injections

Les données envoyées passent par un DTO, `UtilisateurRequest`, validé avec `@Valid` (`@NotBlank`, `@Email`, `@Size`). Le client ne peut donc pas choisir l'id, par exemple. Le nom et la description sont échappés avec `StringEscapeUtils.escapeHtml4` avant d'être enregistrés : `<script>alert('xss')</script>` est stocké sous la forme `&lt;script&gt;alert('xss')&lt;/script&gt;`, qui s'affiche comme du texte et ne s'exécute pas.

Pour l'injection SQL, les données sont stockées dans une base H2 via JPA. J'ai ajouté une route `GET /utilisateurs/recherche?nom=` qui utilise une requête avec un paramètre nommé (`:nom`). Si on envoie `' OR '1'='1`, on obtient une liste vide : la valeur est cherchée comme un nom, elle n'est pas interprétée comme du SQL. Avec une requête construite par concaténation, cette même valeur aurait renvoyé tous les utilisateurs.

**Différence entre injection SQL et XSS ?**

Une injection SQL vise le serveur : l'entrée de l'utilisateur est intégrée dans une requête SQL et en change le sens. Elle permet de lire ou modifier des données, ou de contourner une authentification. On s'en protège avec des requêtes paramétrées.

Une XSS vise les autres utilisateurs : on injecte du HTML/JavaScript qui sera affiché puis exécuté dans leur navigateur. Elle permet de voler leur session ou leur token, ou d'agir à leur place. On s'en protège en échappant les données à l'affichage, en validant les entrées et avec une CSP.

Dans les deux cas, le problème vient d'une donnée qui finit par être exécutée comme du code.

**Pourquoi la validation côté client ne suffit pas ?**

Parce que le client est contrôlé par l'utilisateur : il peut désactiver le JavaScript, modifier la page, ou appeler l'API directement sans passer par l'interface. C'est d'ailleurs ce qu'on fait dans ce TP avec Postman. La validation côté client sert au confort de l'utilisateur ; la sécurité doit être faite côté serveur.

## 5. Exercice 4 – Rate limiting

J'ai fait la version demandée, `RateLimitFilter` : une `Map<String, Deque<Long>>` qui garde l'heure des requêtes de chaque IP sur la dernière minute. Au-delà de 5, on renvoie 429 avec un en-tête `Retry-After`. J'ai utilisé une `ConcurrentHashMap` et un bloc `synchronized` sur la file, car plusieurs requêtes peuvent arriver en même temps et `ArrayDeque` n'est pas thread-safe. L'IP vient de `getRemoteAddr()` et pas de `X-Forwarded-For`, que le client peut mettre lui-même.

En bonus, j'ai ajouté une deuxième version avec Bucket4j (voir partie 9). On choisit laquelle utiliser avec la propriété `app.security.rate-limit.impl`.

**Pourquoi ça améliore la disponibilité ?**

Un seul client (un script, un bot, ou juste un client mal codé) ne peut plus monopoliser le serveur et le ralentir pour tout le monde. Ça limite aussi le brute force sur `/auth/login` : avec 5 essais par minute, tester une liste de mots de passe devient beaucoup trop long. Par contre, ça ne suffit pas contre une attaque venant de milliers d'IP différentes (DDoS), qui se gère plutôt en amont (pare-feu, CDN…).

**Et avec plusieurs instances de l'API ?**

Chaque instance aurait sa propre `Map` en mémoire, donc un client pourrait faire 5 requêtes par instance, et les compteurs seraient perdus à chaque redémarrage. Il faut partager les compteurs entre les instances, par exemple dans Redis : une clé par IP, incrémentée de façon atomique, avec une expiration automatique. Bucket4j sait d'ailleurs fonctionner avec Redis. Une autre solution est de faire la limitation dans une API Gateway placée devant toutes les instances.

## 6. Exercice 5 – HTTPS et gestion des erreurs

J'ai généré un certificat auto-signé avec `keytool` et configuré le port 8443. J'ai aussi ajouté un port HTTP 8080 qui redirige vers HTTPS (`HttpsConfig`), ainsi que l'en-tête HSTS.

**En quoi HTTPS protège la clé API et le JWT ?**

Ces deux jetons fonctionnent « au porteur » : celui qui les a peut s'en servir. Comme ils sont envoyés dans l'en-tête `Authorization` à chaque requête, en HTTP n'importe qui sur le réseau (Wi-Fi public, proxy…) peut les lire et les réutiliser. La signature du JWT n'y change rien : un token volé reste valide.

Avec HTTPS, toute la requête est chiffrée, en-têtes compris, donc on ne peut plus les lire. Les données ne peuvent pas non plus être modifiées en chemin, et le certificat permet de vérifier qu'on parle bien au vrai serveur. Comme notre certificat est auto-signé, Postman affiche un avertissement : le chiffrement fonctionne, mais aucune autorité ne garantit l'identité du serveur. En production, on utiliserait un vrai certificat (Let's Encrypt par exemple).

**Gestion des erreurs**

J'ai repris le `GlobalExceptionHandler` du TP2 et je l'ai complété : validation (400, avec toutes les erreurs de champ et pas seulement la première), JSON invalide (400), mauvais identifiants (401), ressource introuvable (404), email déjà utilisé (409), et une erreur 500 par défaut qui log l'exception mais ne renvoie jamais la stack trace au client.

Le problème, c'est que les 401 et 429 des filtres ne passent pas par ce handler, car les filtres s'exécutent avant les contrôleurs. J'ai donc créé `ErrorResponseWriter`, utilisé par les filtres et par les handlers 401/403 de Spring Security. Toutes les erreurs ont ainsi le même format :

```json
{
  "timestamp": "2026-10-22T14:05:12.345",
  "status": 403,
  "error": "Forbidden",
  "message": "Acces refuse",
  "path": "/utilisateurs/admin"
}
```

## 7. Exercice 6 – Tests

| Scénario | Attendu | Obtenu |
|---|---|---|
| Sans clé API | 401 | 401 |
| Clé API valide | 200 | 200 |
| JWT user sur /utilisateurs/user | autorisé | 200 |
| JWT user sur /utilisateurs/admin | 403 | 403 |
| JWT admin sur /utilisateurs/admin | autorisé | 200 |
| Plus de 5 requêtes en 1 min | 429 | 429 |
| Description avec `<script>` | neutralisé | stocké en `&lt;script&gt;` |
| Requête en http:// | échec ou redirection | redirection 302 vers https |

*(captures Postman en annexe)*

J'ai aussi écrit des tests JUnit avec MockMvc (`SecuriteApiTests` et `RateLimitMaisonTests`) qui vérifient ces scénarios automatiquement, plus quelques cas en plus : mauvaise clé, token modifié, mauvais mot de passe, email invalide, email en double, injection SQL.

**Authentification vs autorisation ?**

L'authentification, c'est vérifier *qui* est le client (clé API, login puis JWT) ; si elle échoue, on renvoie 401. L'autorisation, c'est vérifier *ce qu'il a le droit de faire* une fois qu'on sait qui il est (ici selon son rôle) ; si elle échoue, on renvoie 403. L'authentification passe toujours en premier. Dans le projet, c'est `JwtFilter` qui authentifie et `SecurityConfig` qui autorise : un user connecté qui va sur la route admin reçoit bien 403 et pas 401.

**Rôle du chiffrement dans HTTPS ?**

HTTPS, c'est HTTP dans TLS. Au début de la connexion, le serveur envoie son certificat, qui contient sa clé publique, et le client et le serveur se mettent d'accord sur une clé de session secrète (chiffrement asymétrique). Ensuite, tous les échanges sont chiffrés avec cette clé de session (chiffrement symétrique, plus rapide, par exemple AES). Ça apporte trois choses : la confidentialité (personne ne peut lire les échanges), l'intégrité (personne ne peut les modifier sans que ça se voie) et l'authentification du serveur.

## 8. Réflexion finale

Chaque mécanisme ne protège que contre un type de problème. L'authentification permet de savoir qui fait la requête, mais pas ce qu'il a le droit de faire : c'est l'autorisation qui empêche un simple utilisateur d'accéder aux routes admin. Ces deux protections ne servent à rien si le token peut être intercepté, d'où HTTPS : sans lui, un JWT, même signé, peut être volé et réutilisé. Un utilisateur authentifié et autorisé peut quand même envoyer des données malveillantes, et seuls la validation et l'échappement des entrées bloquent le XSS et les injections. Enfin, rien de tout ça n'empêche quelqu'un de surcharger l'API ou de tester des milliers de mots de passe : c'est le rôle du rate limiting. En combinant ces couches, si l'une a une faille (une clé qui fuit, une validation oubliée), les autres continuent de protéger l'API. Un attaquant doit alors toutes les contourner, ce qui rend une attaque beaucoup plus difficile. C'est le principe de défense en profondeur.

## 9. Pour aller plus loin

J'ai fait les quatre points facultatifs :

- **CORS** : configuré dans `WebConfig` (`WebMvcConfigurer`), seules les origines listées dans `application.properties` sont autorisées.
- **Base de données et BCrypt** : les comptes sont dans une base H2 (JPA), avec le mot de passe hashé en BCrypt. Ils sont créés au démarrage par `DataInitializer`. BCrypt est volontairement lent et ajoute un sel aléatoire à chaque hash : deux mots de passe identiques donnent deux hash différents, et même si la base fuit, retrouver les mots de passe par force brute prend beaucoup de temps. Avec un simple SHA-256, conçu pour être rapide, ce serait beaucoup plus facile.
- **Bucket4j** : `Bucket4jRateLimitFilter`, actif par défaut. Il utilise l'algorithme du « token bucket » : chaque IP a un seau de 5 jetons qui se recharge de 5 jetons par minute, chaque requête consomme un jeton, et quand le seau est vide on renvoie 429. C'est plus léger que ma version (un compteur au lieu d'une liste d'horodatages), c'est thread-safe sans `synchronized`, et ça peut fonctionner avec Redis pour plusieurs instances.
- **Swagger** : avec springdoc-openapi, la documentation est sur `https://localhost:8443/swagger-ui.html`. Le bouton « Authorize » permet de tester les routes avec un JWT ou la clé API. Swagger a sa propre chaîne de sécurité, car la CSP stricte de l'API empêchait la page de s'afficher. En production, il faudrait le désactiver.

L'utilisation d'un assistant IA pour ce TP a été autorisée par l'enseignant.
