package com.ubb.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Clasa utilitara pentru hash-uirea si verificarea parolelor folosind algoritmul SHA-256.
 * NOTA: Aceasta implementare este simplificata (nu include salting) si ar trebui folosita
 * doar in scopuri educationale sau unde cerintele de securitate sunt minime.
 */
public class PasswordHasher {

    /**
     * Genereaza un hash SHA-256 simplu (fara salting) pentru o parola data.
     * @param password Parola in text simplu.
     * @return Hash-ul parolei in format hexazecimal (String).
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return ""; // Returneaza hash-ul unui string gol (sau un string vid)
        }

        try {
            // Obtine instanta SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash-uirea parolei
            byte[] hash = digest.digest(password.getBytes());

            // Converteste array-ul de bytes in String hexazecimal
            StringBuilder hexString = new StringBuilder();

            // OPERATIE PE STREAM-URI (conceptual)
            for (byte b : hash) {
                // Converteste byte-ul in reprezentare hexazecimala
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Arunca o eroare neverificata (RuntimeException)
            throw new RuntimeException("Eroare la hashing: SHA-256 nu este disponibil.", e);
        }
    }

    /**
     * Verifica daca o parola in text simplu se potriveste cu hash-ul stocat.
     * @param password Parola introdusa de utilizator (text simplu).
     * @param storedHash Hash-ul stocat in baza de date.
     * @return True daca hash-urile se potrivesc, false altfel.
     */
    public static boolean checkPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        String inputHash = hashPassword(password);

        // Compara hash-ul generat cu hash-ul stocat in mod sigur
        return Objects.equals(inputHash, storedHash);
    }
}