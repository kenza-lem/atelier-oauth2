# Atelier OAuth 2.0 et OpenID Connect - React + Spring Boot + Keycloak

Application de gestion des demandes : Keycloak authentifie l'utilisateur
(OpenID Connect, Authorization Code avec PKCE), React envoie l'Access Token a
l'API Spring Boot, qui verifie le JWT et applique les roles `USER` et `ADMIN`.

| Composant | Technologie | Port |
|---|---|---|
| Serveur d'identite | Keycloak 26.8.1 (Docker) | 8080 |
| API `demandes-api` | Spring Boot 3.5.16, OAuth2 Resource Server | 8081 |
| Front `demandes-front` | React, Vite, keycloak-js | 5173 |

- **USER** : creer et consulter ses demandes.
- **ADMIN** : consulter toutes les demandes, les valider ou les refuser.
- **Profil** : `GET /api/profil` renvoie l'identifiant et le nom d'utilisateur extraits du JWT.

## Lancement

**1. Keycloak**

```powershell
docker compose up -d
```

Console : `http://localhost:8080` (`admin` / `admin`, usage local uniquement).
Configuration a creer une fois :

- realm `enset-realm` ;
- client public `react-client` : Standard flow uniquement, PKCE `S256`,
  redirect URI `http://localhost:5173/*`, web origin `http://localhost:5173` ;
- realm roles `USER` et `ADMIN` ;
- utilisateurs `alice` / `alice123` (USER) et `bob` / `bob123` (USER, ADMIN).

**2. API**

```powershell
cd demandes-api
.\mvnw.cmd spring-boot:run
```

**3. Front**

```powershell
cd demandes-front
npm install
npm run dev
```

Ouvrir `http://localhost:5173` : la connexion se fait sur la page Keycloak.

## Tests

Tests automatises (sans Keycloak, `JwtDecoder` simule) :

```powershell
cd demandes-api
.\mvnw.cmd test
```

Tests manuels avec Postman :

| Requete | Token | Resultat |
|---|---|---|
| `GET /api/demandes` | aucun | 401 |
| `GET /api/admin/demandes` | bob (ADMIN) | 200 |
| `GET /api/admin/demandes` | token modifie | 401 |
| `GET /api/admin/demandes` | alice (USER) | 403 |

## Remarques

- start.spring.io ne proposant plus la branche 3.5, le projet a ete ramene a Spring Boot 3.5.16 comme demande.
- Les jetons restent en memoire (keycloak-js) et ne sont pas stockes dans le `localStorage`.
- Les demandes sont en memoire et disparaissent au redemarrage de l'API.
