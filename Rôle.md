## 📂 src/main/java/com/exemple/backend/
C’est le code principal (production).
- BackendApplication.java → la classe principale Spring Boot, point d’entrée (main).
- config/ → configurations globales : sécurité (Spring Security, JWT), CORS, beans spécifiques.
- controller/ → contient tes API REST (annotés avec @RestController). Exemple : UserController.java.
- dto/ → les Data Transfer Objects (objets pour transférer les données entre API ↔ frontend, évitent d’exposer les entités directement).
- entity/ → les entités JPA (mapping avec la base de données, ex: User, Quincaillerie, Produit).
- repository/ → interfaces qui étendent JpaRepository ou CrudRepository pour accéder aux données. Exemple : UserRepository.
- service/ → la logique métier (implémentation des règles de gestion). Exemple : UserService.