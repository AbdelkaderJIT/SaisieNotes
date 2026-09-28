-- Remise à zéro des données de démonstration (base Aiven ou locale).
--
-- 1. Se connecter à la base avec MySQL Workbench (ou un autre client) avec les informations de
--    connexion d'Aiven (hôte, port, utilisateur avnadmin, mot de passe, SSL activé).
-- 2. Exécuter ce script : il supprime les tables de l'application.
-- 3. Redémarrer le service sur Render (Manual Deploy > Restart service).
--    Au démarrage, Hibernate recrée les tables et DataInitializer réinsère les données de démonstration
--    (il n'insère que si la base est vide).

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS inscription;
DROP TABLE IF EXISTS enseigne;
DROP TABLE IF EXISTS etudiant;
DROP TABLE IF EXISTS matiere;
DROP TABLE IF EXISTS enseignant;
SET FOREIGN_KEY_CHECKS = 1;
