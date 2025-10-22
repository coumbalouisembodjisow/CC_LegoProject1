package cc.srv.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig {

    private static String cosmosDbUrl = null;
    private static String cosmosDbKey = null;
    private static String cosmosDbName = null;

    // Méthode pour charger les variables depuis un fichier .env
    public static Map<String, String> load(String filePath) {
        Map<String, String> env = new HashMap<>();
        File file = new File(filePath);
        
        // Debug: afficher le chemin recherché
        System.out.println("Recherche du fichier .env: " + file.getAbsolutePath());
        
        if (!file.exists()) {
            System.err.println("Fichier .env non trouvé: " + file.getAbsolutePath());
            return env;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx != -1) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();

                    // Remove optional surrounding quotes
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    env.put(key, value);
                }
            }
            System.out.println(" Fichier .env chargé avec succès: " + env.size() + " variables");
        } catch (IOException e) {
            System.err.println(" Erreur lecture .env: " + e.getMessage());
        }

        return env;
    }

    // Initialisation des variables statiques
    public static void envInit() {
        Map<String, String> env = EnvConfig.load(".env");
        cosmosDbUrl = env.get("COSMOS_DB_URL");
        cosmosDbKey = env.get("COSMOS_DB_KEY");
        cosmosDbName = env.get("COSMOS_DB_NAME");
        
        // Debug
        System.out.println("=== Configuration initialisée ===");
        System.out.println("COSMOS_DB_URL: " + (cosmosDbUrl != null ? "✓" : "✗"));
        System.out.println("COSMOS_DB_KEY: " + (cosmosDbKey != null ? "✓" : "✗"));
        System.out.println("COSMOS_DB_NAME: " + (cosmosDbName != null ? "✓" : "✗"));
    }

    // Getters pour les variables de configuration
    public static String getCosmosDbUrl() {
        if (cosmosDbUrl == null) {
            envInit(); // Initialisation lazy
        }
        return cosmosDbUrl;
    }

    public static String getCosmosDbKey() {
        if (cosmosDbKey == null) {
            envInit(); // Initialisation lazy
        }
        return cosmosDbKey;
    }

    public static String getCosmosDbName() {
        if (cosmosDbName == null) {
            envInit(); // Initialisation lazy
        }
        return cosmosDbName;
    }

    // Méthode générique pour récupérer n'importe quelle variable
    public static String get(String key) {
        // Pour les variables courantes, utiliser les getters dédiés
        switch (key) {
            case "COSMOS_DB_URL":
                return getCosmosDbUrl();
            case "COSMOS_DB_KEY":
                return getCosmosDbKey();
            case "COSMOS_DB_NAME":
                return getCosmosDbName();
            default:
                // Pour les autres variables, charger dynamiquement
                Map<String, String> env = load(".env");
                return env.get(key);
        }
    }

    // Méthode pour afficher toute la configuration (masque les clés sensibles)
    public static void main(String[] args) {
        System.out.println("=== Configuration Cosmos DB ===");
        System.out.println("URL: " + getCosmosDbUrl());
        System.out.println("KEY: " + (getCosmosDbKey() != null ? 
            "***" + getCosmosDbKey().substring(Math.max(0, getCosmosDbKey().length() - 4)) : "null"));
        System.out.println("DB NAME: " + getCosmosDbName());
    }

   
}