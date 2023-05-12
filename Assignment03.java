/**
 * Assignment03-Friends in a Scandal
 * @author Noah Steaderman
 */

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assignment03 {

    private static final Map<String, Set<String>> mailGraph = new HashMap<>();

    /** function that adds an edge into a directed graph from one email to another
     * @param adjacencyMap given map
     * @param from from address
     * @param to to address
     */
    public static void addEdge(Map<String, Set<String>> adjacencyMap, String from, String to) {
        if (!adjacencyMap.containsKey(from)) {
            adjacencyMap.put(from, new HashSet<>());
        }
        if (!adjacencyMap.containsKey(to)) {
            adjacencyMap.put(to, new HashSet<>());
        }
        adjacencyMap.get(from).add(to);
    }

    /** recursively moves through the given folders to read the mail files within and add them to the graph.
     * @param folder folder to traverse
     */
    public static void recurseFiles(final File folder) {
        String line;
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                recurseFiles(fileEntry);
            }
            else {
                try {
                    BufferedReader bufferedreader = new BufferedReader(new FileReader(fileEntry.getPath()));
                    String fromAdd = null;
                    String toAdd;
                    while ((line = bufferedreader.readLine()) != null) {
                        if (line.startsWith("From: ")) {
                            fromAdd = extractEmail(line);
                        }
                        if (line.startsWith("To: ")) {
                            String[] arr = line.split(" ");
                            for (int i = 1; i < arr.length; i++) {
                                toAdd = extractEmail(arr[i]);
                                if (toAdd != null && fromAdd != null) {
                                    addEdge(mailGraph, fromAdd, toAdd);
                                }
                            }
                        }
                        if (line.startsWith("Cc: ")) {
                            String[] arr = line.split(" ");
                            for (int i = 1; i < arr.length; i++) {
                                toAdd = extractEmail(arr[i]);
                                if (toAdd != null && fromAdd != null) {
                                    addEdge(mailGraph, fromAdd, toAdd);
                                }
                            }
                        }
                        if (line.startsWith("Bcc: ")) {
                            String[] arr = line.split(" ");
                            for (int i = 1; i < arr.length; i++) {
                                toAdd = extractEmail(arr[i]);
                                if (toAdd != null && fromAdd != null) {
                                    addEdge(mailGraph, fromAdd, toAdd);
                                }
                            }
                        }
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /** detects email address in the given string line from the bufferedreader.
     * @param input given line
     */
    public static String extractEmail(String input) {
        Matcher matcher = Pattern.compile("([a-zA-Z0-9.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z0-9._-]+)").matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /** function to give number of sent emails by looking at the neighbors of a given node.
     * @param adjacencyMap given map
     * @param email given email address
     * @return count (# of emails sent)
     */
    public static int sentMail(Map<String, Set<String>> adjacencyMap, String email) {
        int count = 0;
        for (String node : adjacencyMap.keySet()) {
            if (Objects.equals(node, email)) {
                Set<String> neighbors = adjacencyMap.get(node);
                for (String neighbor : neighbors) {
                    count++;
                }
            }
        }
        return count;
    }

    /** function to give number of recieved emails by looking for the given node within every nodes neighbor.
     * @param adjacencyMap given map
     * @param email given email
     * @return count (# of emails received)
     */
    public static int recMail(Map<String, Set<String>> adjacencyMap, String email) {
        int count = 0;
        for (String node : adjacencyMap.keySet()) {
            Set<String> neighbors = adjacencyMap.get(node);
            for (String neighbor : neighbors) {
                if (Objects.equals(neighbor, email)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** function to get the number of team members by using BFS to traverse through the graph
     * @param adjacencyMap given map
     * @param email given email
     * @return team.size() --> (# of people in the team)
     */
    public static int getTeam(Map<String, Set<String>> adjacencyMap, String email) {
        Set<String> team = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(email);
        visited.add(email);
        while (!queue.isEmpty()) {
            String node = queue.poll();
            team.add(node);

            Set<String> neighbors = adjacencyMap.getOrDefault(node, new HashSet<>());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        return team.size();
    }

    /** modified dfs function to find instances of connectors
     * @param adjacencyMap given map
     * @param vertex current node being visited
     * @param parent parent of current node
     * @param dfsnum map to store dfs number for each vertex
     * @param back  map to store back number for each vertex
     * @param connectors uses a set to store found connectors(no duplicates!)
     * @param dfsCount current dfs count
     */
    private static void dfs(Map<String, Set<String>> adjacencyMap, String vertex, String parent, Map<String,
            Integer> dfsnum, Map<String, Integer> back, Set<String> connectors, int dfsCount) {
        dfsnum.put(vertex, dfsCount);
        back.put(vertex, dfsCount);
        dfsCount++;
        int childCount = 0;
        boolean isConnector = false;
        Set<String> neighbors = adjacencyMap.get(vertex);

        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (neighbor.equals(parent)) {
                    continue;
                }
                if (!dfsnum.containsKey(neighbor)) {
                    dfs(adjacencyMap, neighbor, vertex, dfsnum, back, connectors, dfsCount);
                    childCount++;
                    if (dfsnum.get(vertex) <= back.get(neighbor)) {
                        isConnector = true;
                    } else {
                        back.put(vertex, Math.min(back.get(vertex), back.get(neighbor)));
                    }
                } else {
                    back.put(vertex, Math.min(back.get(vertex), dfsnum.get(neighbor)));
                }
            }
        }
        if ((parent != null && isConnector) || (parent == null && childCount > 1)) {
            connectors.add(vertex);
        }
    }

    /**
     * calls dfs function to find connectors in the graph, and also provides code for writing connectors to a given file
     * @param adjacencyMap    given map
     * @param outputFileName  provided file name to write to, but not necessary to input
     */
    public static void findConnectors(Map<String, Set<String>> adjacencyMap, String outputFileName) {
        Set<String> connectors = new HashSet<>();
        Map<String, Integer> dfsnum = new HashMap<>();
        Map<String, Integer> back = new HashMap<>();

        for (String vertex : adjacencyMap.keySet()) {
            if (!dfsnum.containsKey(vertex)) {
                dfs(adjacencyMap, vertex, null, dfsnum, back, connectors, 1);
            }
        }
        System.out.println(connectors.size());
        try {
            FileWriter writer = null;
            if (outputFileName != null) {
                try {
                    writer = new FileWriter(outputFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (writer != null) {
                for (String connector : connectors) {
                    System.out.println(connector);
                    writer.write(connector + "\n");
                }
                writer.close();
            } else {
                for (String connector : connectors) {
                    System.out.println(connector);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * user interface to allow interaction with values in the provided file
     * @return false to break the loop
     */
    public static boolean ui() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Email address of the individual (or EXIT to quit):");
        String input = sc.nextLine();
        while (!input.equals("EXIT")) {
            if(mailGraph.containsKey(input)) {
                System.out.println("* " + input + " has sent messages to " + sentMail(mailGraph, input) + " others");
                System.out.println("* " + input + " has recieved messages from " + recMail(mailGraph, input) + " others");
                System.out.println("* " + input + " is in a team with  " + getTeam(mailGraph, input) + " individuals");
                ui();
            } else {
                System.out.println("Email address (" + input + ") not found in the dataset.");
                ui();
            }
            return false;
        }
        return false;
    }

    public static void main(String[] args) {
        final File folder = new File(args[0]);
        recurseFiles(folder);
        System.out.print("Printing Connectors... ");
        if (args.length == 2) {
            findConnectors(mailGraph, args[1]);
        } else {
            findConnectors(mailGraph, null);
        }
        ui();
    }
}
