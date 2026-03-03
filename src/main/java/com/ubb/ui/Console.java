package com.ubb.ui;

import com.ubb.domain.*;
import com.ubb.domain.exceptions.DomainException;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.NetworkServiceInterface;
import com.ubb.service.CardServiceInterface;
import com.ubb.service.EventServiceInterface;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Console {

    private final UserServiceInterface userService;
    private final FriendshipServiceInterface friendshipService;
    private final NetworkServiceInterface networkService;
    private final CardServiceInterface cardService;
    private final EventServiceInterface eventService;
    private final Scanner scanner;

    public Console(UserServiceInterface userService,
                   FriendshipServiceInterface friendshipService,
                   NetworkServiceInterface networkService,
                   CardServiceInterface cardService,
                   EventServiceInterface eventService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.networkService = networkService;
        this.cardService = cardService;
        this.eventService = eventService;
        this.scanner = new Scanner(System.in);
    }

    private void printMenu() {
        System.out.println("\n--- MENIU SOCIAL NETWORK ---");
        System.out.println("1. Adauga Utilizator (Persoana/Rata)");
        System.out.println("2. Sterge Utilizator");
        System.out.println("3. Afiseaza Toti Utilizatorii");
        System.out.println("4. Adauga Prietenie");
        System.out.println("5. Sterge Prietenie");
        System.out.println("6. Afiseaza Toate Prieteniile");
        System.out.println("7. Numar Comunitati");
        System.out.println("8. Cea Mai Sociabila Comunitate");
        System.out.println("9.Adauga Card");
        System.out.println("10.Adauga Membru Card");
        System.out.println("11.Afiseaza Toate Cardurile");
        System.out.println("12.Calculeaza Performanta Medie Card");
        System.out.println("13. Afiseaza Membrii Cardului");
        System.out.println("14. Creeaza Cursa (RaceEvent)");
        System.out.println("15. Aboneaza Utilizator la Event");
        System.out.println("16. Finalizeaza Eveniment & Notifica");
        System.out.println("0. Iesire");
        System.out.print("Alege o optiune: ");
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            try {
                int option = scanner.nextInt();
                scanner.nextLine();

                switch (option) {
                    case 1: uiAddUser(); break;
                    case 2: uiDeleteUser(); break;
                    case 3: uiViewAllUsers(); break;
                    case 4: uiAddFriendship(); break;
                    case 5: uiDeleteFriendship(); break;
                    case 6: uiViewAllFriendships(); break;
                    case 7: uiGetNumberOfCommunities(); break;
                    case 8: uiGetMostSociableCommunity(); break;
                    case 9: uiAddCard(); break;
                    case 10: uiAddMembruCard(); break;
                    case 11: uiViewAllCards(); break;
                    case 12: uiGetPerformantaMedie(); break;
                    case 13: uiViewCardMembers(); break;
                    case 14: uiCreateRaceEvent(); break;
                    case 15: uiSubscribeUser(); break;
                    case 16: uiFinishEvent(); break;
                    case 0: running = false; break;
                    default: System.out.println("Optiune invalida.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Eroare: Va rugam introduceti un numar valid pentru optiune.");
                scanner.nextLine();
            } catch (DomainException e) {
                // Prinde ValidationException, RepositoryException, ServiceException etc.
                System.out.println("EROARE APLICATIE: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("EROARE NECUNOSCUTA: " + e.getMessage());
            }
        }
        System.out.println("Aplicatia Social Network a fost inchisa.");
    }

    //METODE UI PENTRU UTILIZATOR

    private void uiAddUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Tip utilizator (1=Persoana, 2=Duck): ");
        int type = scanner.nextInt();
        scanner.nextLine();

        User newUser ;
        if (type == 1) {
            // LOGICA PERSOANA
            System.out.print("Nume: "); String nume = scanner.nextLine();
            System.out.print("Prenume: "); String prenume = scanner.nextLine();
            System.out.print("Data Nasterii (YYYY-MM-DD): "); String dataNasteriiStr = scanner.nextLine();
            System.out.print("Ocupatie: "); String ocupatie = scanner.nextLine();
            System.out.print("Nivel Empatie (0-10): "); int empatie = Integer.parseInt(scanner.nextLine());

            LocalDate dataNasterii = LocalDate.parse(dataNasteriiStr);
            newUser = new Persoana(username, email, password, nume, prenume, dataNasterii, ocupatie, empatie);
        }

        else if (type == 2) {
            // LOGICA DUCK (NOUA IERARHIE)
            System.out.print("Subtip Rata (1=SwimmingDuck, 2=FlyingDuck, 3=FlyingAndSwimmingDuck): ");
            int duckType = Integer.parseInt(scanner.nextLine());

            System.out.print("Viteza (double): "); double viteza = Double.parseDouble(scanner.nextLine());
            System.out.print("Rezistenta (double): "); double rezistenta = Double.parseDouble(scanner.nextLine());

            if (duckType == 1) {
                newUser = new SwimmingDuck(username, email, password, viteza, rezistenta);
            } else if (duckType == 2) {
                newUser = new FlyingDuck(username, email, password, viteza, rezistenta);
            }else if (duckType == 3) {
                newUser=new FlyingAndSwimmingDuck(username, email, password, viteza, rezistenta);
            }
            else {
                System.out.println("Subtip Rata invalid. Adaugare anulata.");
                return;
            }
        } else {
            System.out.println("Tip utilizator invalid. Adaugare anulata.");
            return;
        }

            userService.addUser(newUser);
            System.out.println("Utilizator adaugat cu succes!");
    }

    private void uiDeleteUser() {
        System.out.print("ID-ul utilizatorului de sters: ");
        Long id = scanner.nextLong(); scanner.nextLine();
        userService.deleteUser(id);
        System.out.println("Utilizator sters cu succes!");
    }

    private void uiViewAllUsers() {
        System.out.println("\n--- LISTA UTILIZATORI ---");
        userService.findAllUsers().forEach(user -> System.out.println(user));
    }

    //METODE UI PENTRU PRIETENII

    private void uiAddFriendship() {
        System.out.print("ID User 1: "); Long id1 = scanner.nextLong(); scanner.nextLine();
        System.out.print("ID User 2: "); Long id2 = scanner.nextLong(); scanner.nextLine();
        friendshipService.addFriendship(id1, id2);
        System.out.println("Prietenie adaugata cu succes!");
    }

    private void uiDeleteFriendship() {
        System.out.print("ID User 1: "); Long id1 = scanner.nextLong(); scanner.nextLine();
        System.out.print("ID User 2: "); Long id2 = scanner.nextLong(); scanner.nextLine();
        friendshipService.deleteFriendship(id1, id2);
        System.out.println("Prietenie stearsa cu succes!");
    }

    private void uiViewAllFriendships() {
        System.out.println("\n--- LISTA PRIETENII ---");
        friendshipService.findAllFriendships().forEach(friendship -> System.out.println(friendship));
    }

    //METODE UI PENTRU RETEA

    private void uiGetNumberOfCommunities() {
        int count = networkService.getNumberOfCommunities();
        System.out.println("Numarul total de comunitati din retea: " + count);
    }

    private void uiGetMostSociableCommunity() {
        System.out.println("\n--- CEA MAI SOCIABILA COMUNITATE (Cel mai lung drum) ---");
        networkService.getMostSociableCommunity().forEach(user ->
                System.out.println("ID: " + user.getId() + ", Username: " + user.getUsername()));
    }

    //METODE UI PENTRU CARD

    private void uiAddCard() {
        System.out.print("Nume Card: ");
        String numeCard = scanner.nextLine();

        Card<? extends Duck> newCard = null;
        Long firstDuckId = null; // ID-ul primei rate necesare

        try {
            System.out.print("Tip Card (1=SwimmingCard, 2=FlyingCard): ");
            int type = Integer.parseInt(scanner.nextLine());

            System.out.print("ID-ul primei Rate (Membru Fondator): "); // NOU: Cerem ID-ul ratei
            firstDuckId = Long.parseLong(scanner.nextLine());

            // 1. Instantierea Cardului
            if (type == 1) {
                newCard = new SwimmingCard(numeCard);
            } else if (type == 2) {
                newCard = new FlyingCard(numeCard);
            } else {
                System.out.println("Tip Card invalid. Adaugare anulata.");
                return;
            }

            // 2. Adaugam Cardul in Repository (fara membri inca)
            // Service-ul genereaza ID-ul Cardului
            cardService.addCard(newCard);

            // 3. Adaugam prima Rata (Membru Fondator) - Logica de business critica
            // Aceasta va verifica existenta si compatibilitatea tipului
            cardService.addMembru(newCard.getId(), firstDuckId);

            System.out.println("Card adaugat cu succes! ID: " + newCard.getId());
            System.out.println("Rata " + firstDuckId + " a fost adaugata ca membru fondator.");

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti numere valide pentru tip/ID-ul Ratei.");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("EROARE NECUNOSCUTA: Nu s-a putut adauga Cardul. " + e.getMessage());
        }
    }

    private void uiAddMembruCard() {
        try {
            System.out.print("ID Card: ");
            // Folosim nextLine() pentru citire consistenta si parsare explicita
            Long cardId = Long.parseLong(scanner.nextLine());
            System.out.print("ID Rata (de adaugat): ");
            Long duckId = Long.parseLong(scanner.nextLine());

            // Service-ul face verificarea: Rata exista? Este tipul corect pentru Card?
            cardService.addMembru(cardId, duckId);
            System.out.println("Rata " + duckId + " a fost adaugata in Cardul " + cardId + ".");

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti ID-uri valide (numere).");
        } catch (DomainException e) {
            // Prinde ValidationException, EntityNotFoundException, ServiceException
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }
    private void uiViewAllCards() {
        System.out.println("\n--- LISTA CaRDURI ---");
        // Service-ul reconstruieste membrii (Duck) inainte de a returna lista
        try {
            cardService.findAllCards().forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("EROARE la afisare Carduri: " + e.getMessage());
        }
    }
    private void uiGetPerformantaMedie() {
        try {
            System.out.print("ID Card: ");
            Long cardId = Long.parseLong(scanner.nextLine());

            // Service-ul gaseste Cardul, il reconstruieste si returneaza calculul
            double performanta = cardService.getPerformantaMedie(cardId);

            System.out.printf("Performanta medie a Cardului %d este: %.2f\n", cardId, performanta);

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti un ID valid (numar).");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }

    // com/ubb/ui/Console.java (Fragment)

    private void uiViewCardMembers() {
        try {
            System.out.print("ID Card pentru vizualizare membri: ");
            Long cardId = Long.parseLong(scanner.nextLine());

            List<? extends Duck> members = cardService.getCardMembers(cardId);

            System.out.printf("\n--- Membri Card %d (%s) ---\n", cardId, members.get(0).getClass().getSimpleName());

            if (members.isEmpty()) {
                System.out.println("Cardul nu contine membri.");
            } else {
                members.forEach(duck ->
                        System.out.println("- ID: " + duck.getId() + ", Username: " + duck.getUsername() + ", Viteza: " + duck.getViteza())
                );
            }

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti un ID valid (numar).");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }

    // com/ubb/ui/Console.java (Metode Noi)

    private void uiCreateRaceEvent() {
        try {
            System.out.print("Nume Cursa: ");
            String name = scanner.nextLine();
            System.out.print("Numar Participanti doriti (M): ");
            int m = Integer.parseInt(scanner.nextLine());

            // Citeste distantele balizelor (d1, d2, d3,...)
            System.out.print("Distante Balize (separate prin virgula, ex: 3,6,10): ");
            String distancesStr = scanner.nextLine();

            List<Double> beaconDistances = Arrays.stream(distancesStr.split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            // Apel Service
            eventService.createRaceEvent(name, m, beaconDistances);
            System.out.println("RaceEvent '" + name + "' creat cu succes!");

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti numere valide pentru M sau distante.");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }

    private void uiSubscribeUser() {
        try {
            System.out.print("ID Eveniment (pentru abonare): ");
            Long eventId = Long.parseLong(scanner.nextLine());
            System.out.print("ID Utilizator (care se aboneaza): ");
            Long userId = Long.parseLong(scanner.nextLine());

            eventService.subscribe(eventId, userId);
            System.out.printf("Utilizatorul %d s-a abonat la Evenimentul %d.\n", userId, eventId);

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti ID-uri valide.");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }

    private void uiFinishEvent() {
        try {
            System.out.print("ID Eveniment de finalizat: ");
            Long eventId = Long.parseLong(scanner.nextLine());

            // Service-ul ruleaza logica cursei si notifica abonatii
            String results = eventService.finishEvent(eventId);

            System.out.println("--- FINALIZARE CURSA ---");
            System.out.println("Rezultatul Final: " + results);
            System.out.println("Abonatii au fost notificati!");

        } catch (NumberFormatException e) {
            System.out.println("EROARE: Va rugam introduceti ID-ul valid al Evenimentului.");
        } catch (DomainException e) {
            System.out.println("EROARE APLICATIE: " + e.getMessage());
        }
    }

}