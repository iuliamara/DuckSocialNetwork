package com.ubb.service;

import com.ubb.domain.Friendship;
import com.ubb.domain.User;
import com.ubb.repository.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Serviciu care implementeaza algoritmi de Graf (DFS, BFS) pentru analiza structurii retelei sociale.
 * Trateaza utilizatorii ca noduri si prieteniile ca muchii intr-un graf neorientat.
 */
public class NetworkService implements NetworkServiceInterface {

    private final Repository<Long, User> userRepository;
    // Tip generic pentru ID-ul Friendship, care este Tuple<Long, Long>
    private final Repository<?, Friendship> friendshipRepository;

    // Graf intern, cheia e ID-ul User, valoarea e lista vecinilor (prieteni)
    private Map<Long, List<Long>> networkGraph;

    /**
     * Constructor cu Dependency Injection.
     */
    public NetworkService(Repository<Long, User> userRepository, Repository<?, Friendship> friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    // --- METODE AUXILIARE PENTRU GRAF (cu Stream-uri) ---

    /**
     * Metoda privata pentru a construi structura de graf (lista de adiacenta) din datele Repositoriilor.
     */
    private void buildNetworkGraph() {
        // OPERATIE PE STREAM-URI: Initializarea grafului cu toate nodurile (User.id)
        networkGraph = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toMap(User::getId, user -> new ArrayList<>()));

        // Adauga muchiile (prieteniile)
        for (Friendship friendship : friendshipRepository.findAll()) {
            Long id1 = friendship.getIdUser1();
            Long id2 = friendship.getIdUser2();

            // Graful e neorientat, adaugam muchia in ambele directii (verificam existenta)
            if (networkGraph.containsKey(id1) && networkGraph.containsKey(id2)) {
                networkGraph.get(id1).add(id2);
                networkGraph.get(id2).add(id1);
            }
        }
    }

    // DFS Helper pentru a extrage toate nodurile dintr-o comunitate
    private void dfsCommunity(Long currentId, Set<Long> visited, List<Long> communityNodes) {
        visited.add(currentId);
        communityNodes.add(currentId);

        List<Long> neighbors = networkGraph.get(currentId);
        if (neighbors != null) {
            for (Long neighborId : neighbors) {
                if (!visited.contains(neighborId)) {
                    dfsCommunity(neighborId, visited, communityNodes);
                }
            }
        }
    }

    // BFS Helper pentru a calcula distantele si parintii
    private void bfs(Long startNode, Map<Long, Integer> distances, Map<Long, Long> parents) {
        Queue<Long> queue = new LinkedList<>();
        distances.put(startNode, 0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Long current = queue.poll();

            for (Long neighbor : networkGraph.getOrDefault(current, Collections.emptyList())) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, distances.get(current) + 1);
                    parents.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
    }

    // Reconstruieste drumul de la nodul start la end, folosind harta parintilor
    private List<Long> reconstructPath(Long start, Long end, Map<Long, Long> parents) {
        List<Long> path = new LinkedList<>();
        Long current = end;
        while (current != null) {
            path.add(0, current); // Adauga la inceput
            if (current.equals(start)) break;
            current = parents.get(current);
        }
        return path;
    }

    // --- IMPLEMENTAREA CONTRACTULUI SERVICE ---

    /**
     * Calculeaza numarul de comunitati (Componente Conexe).
     * Utilizeaza algoritmul DFS.
     */
    @Override
    public int getNumberOfCommunities() {
        buildNetworkGraph(); // Reconstruieste graful la fiecare operatie
        Set<Long> visited = new HashSet<>();
        int communityCount = 0;

        // Nu se folosesc stream-uri aici pentru a mentine logica DFS secventiala
        for (Long userId : networkGraph.keySet()) {
            if (!visited.contains(userId)) {
                // Daca gasim un nod nevizitat, am gasit o noua componenta conexa
                // Folosim dfsCommunity pentru a vizita toate nodurile si a marca componenta
                dfsCommunity(userId, visited, new ArrayList<>());
                communityCount++;
            }
        }
        return communityCount;
    }

    /**
     * Identifica si returneaza membrii celei mai sociabile comunitati (diametru maxim).
     * Utilizeaza algoritmul Twice BFS.
     */
    @Override
    public List<User> getMostSociableCommunity() {
        buildNetworkGraph(); // Reconstruieste graful

        Set<Long> visited = new HashSet<>();
        List<Long> longestPathNodes = new ArrayList<>();
        int maxPathLength = -1;

        // Itereaza prin toate nodurile pentru a gasi toate comunitatile
        for (Long userId : networkGraph.keySet()) {
            if (!visited.contains(userId)) {

                // 1. Identifica componenta conexa curenta
                List<Long> currentCommunityNodes = new ArrayList<>();
                // Extrage toate nodurile si le marcheaza in 'visited'
                dfsCommunity(userId, visited, currentCommunityNodes);

                // Ignora nodurile izolate sau cu un singur prieten (nu au lant lung)
                if (currentCommunityNodes.size() <= 1) continue;

                // 2. Twice BFS pentru a gasi diametrul comunitatii (cel mai lung drum)

                // A. Primul BFS: Pornim de la un nod arbitrar (A)
                Long arbitraryStart = currentCommunityNodes.get(0);
                Map<Long, Integer> distA = new HashMap<>();
                Map<Long, Long> parentsA = new HashMap<>();
                bfs(arbitraryStart, distA, parentsA);

                // Gaseste nodul B, cel mai indepartat de nodul A, in cadrul comunitatii
                Long nodeB = arbitraryStart;
                int maxDist1 = 0;
                for (Long nodeId : currentCommunityNodes) {
                    if (distA.containsKey(nodeId) && distA.get(nodeId) > maxDist1) {
                        maxDist1 = distA.get(nodeId);
                        nodeB = nodeId;
                    }
                }

                // B. Al doilea BFS: Pornim de la nodul B pentru a gasi C (diametrul)
                Map<Long, Integer> distB = new HashMap<>();
                Map<Long, Long> parentsB = new HashMap<>();
                bfs(nodeB, distB, parentsB);

                // Gaseste nodul C, cel mai indepartat de nodul B (lungimea diametrului)
                Long nodeC = nodeB;
                int currentDiameter = 0;
                for (Long nodeId : currentCommunityNodes) {
                    if (distB.containsKey(nodeId) && distB.get(nodeId) > currentDiameter) {
                        currentDiameter = distB.get(nodeId);
                        nodeC = nodeId;
                    }
                }

                // 3. Compara si actualizeaza cel mai bun rezultat
                if (currentDiameter > maxPathLength) {
                    maxPathLength = currentDiameter;
                    // Reconstruim calea de la nodul B la nodul C (drumul cel mai lung)
                    longestPathNodes = reconstructPath(nodeB, nodeC, parentsB);
                }
            }
        }

        // 4. Converteste ID-urile nodurilor de pe cel mai lung drum in obiecte User
        // OPERATIE PE STREAM-URI: Mapare ID -> User
        List<User> resultUsers = longestPathNodes.stream()
                .map(userRepository::findOne)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return resultUsers;
    }
}