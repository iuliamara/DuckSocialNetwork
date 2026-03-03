package com.ubb.utils;

import com.ubb.utils.PasswordHasher; // Asigură-te că imporți clasa ta

public class HashGenerator {

    public static void main(String[] args) {

        // 1. DATELE TALE (Username, Parola)
        // Nu avem ID-urile aici, așa că folosim username-ul pentru clauza WHERE
        String[][] userData = {
                {"iandru", "iiiii2005"},
                {"iulia_mara81", "mara2005"},
                {"aqua_man", "dpass4"},
                {"fly_high", "dpass5"},
                {"swim_pro", "dpass6"},
                {"rezistenta", "dpass11"},
                {"fast_swim", "dpass12"},
                {"cosmin_pop", "cpass1"},
                {"sky_king", "dpass15"},
                {"mimi", "mimiparl"},
                {"mom", "mompwd"},
                {"ratusca_cea_urata", "ratusca123"},
                {"DuckLover", "iloveducks"},
                {"radu_a", "rpass35"},
                {"slow_aqua", "dpass36"},
                {"ioana_m", "ipass37"},
                {"fast_bird", "dpass38"},
                {"cristi_s", "cpass39"},
                {"maraton_s", "dpass40"},
                {"alina_cas", "apass41"},
                {"high_volt", "dpass42"},
                {"diana_som", "dpass43"},
                {"vlad_nec", "vpass44"},
                {"sorin_ang", "spass45"},
                {"aero_king", "dpass46"},
                {"oana_x", "opass47"},
                {"sub_master", "dpass48"},
                {"bogdan_som", "bpass49"},
                {"high_speed", "dpass50"},
                {"elena_home", "epass51"},
                {"endurance", "dpass52"},
                {"marius_stud", "mpass53"},
                {"wind_cutter", "dpass54"},
                {"livia_ceo", "lpass55"},
                {"balena", "dpass56"},
                {"ovidiu_prog", "opass57"},
                {"rapid_wing", "dpass58"},
                {"maria_pens", "mpass59"},
                {"lunga_dist", "dpass60"},
                {"vasile_som", "vpass61"},
                {"high_altitude", "dpass62"},
                {"adriana_casa", "apass63"},
                {"medie_vit", "dpass64"},
                {"wind_runner", "dpass32"},
                {"swift_wing", "dpass33"},
                {"glider", "dpass34"},
                {"deep_diver", "dpass28"},
                {"slow_but_st", "dpass29"},
                {"speedster", "dpass30"},
                {"water_master", "dpass31"},
                {"andrei_st", "apass25"},
                {"elena_pop", "epass26"},
                {"cosmin_i", "cpass27"}
        };

        // 2. GENERARE COMENZI SQL
        System.out.println("--- COMANDA SQL PENTRU ACTUALIZAREA PAROLELOR HASHUITE ---");
        System.out.println("--- Rulati aceste comenzi direct in clientul PostgreSQL (pgAdmin/DBeaver) ---");

        for (String[] data : userData) {
            String username = data[0];
            String passwordClear = data[1];

            // Apelează funcția ta de hashing
            String passwordHash = PasswordHasher.hashPassword(passwordClear);

            // Generează comanda SQL
            String sqlCommand = String.format(
                    "UPDATE users SET password = '%s' WHERE username = '%s';",
                    passwordHash,
                    username
            );

            System.out.println(sqlCommand);
        }
        System.out.println("-------------------------------------------------------------------------");
    }
}