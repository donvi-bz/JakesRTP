package biz.donvi.jakesRTP.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


abstract class ArgsTester {

    // REGION FOR PRINT RELATED METHODS

    /***
     * Prints out the keys of a Map like a YAML file
     * @param mapToPrint The map to print
     */
    static void printTree(Map<String, Object> mapToPrint) {
        printTree(mapToPrint, 0);
    }

    /***
     * This is the worker for <c>printTree()</c> and should not be used directly.
     * @param mapToPrint The map to print
     * @param depth The current depth inside the Map (looking at it like a tree)
     */
    @SuppressWarnings(value = "unchecked")
    private static void printTree(Map<String, Object> mapToPrint, int depth) {
        for (String key : mapToPrint.keySet()) {
            System.out.print(depth + " | " + numSpaces(depth) + key + ":");
            if (mapToPrint.get(key) instanceof Map) {
                System.out.print('\n');
                printTree((Map<String, Object>) mapToPrint.get(key), depth + 1);
            } else {
                System.out.print(' ');
                System.out.println((String) mapToPrint.get(key));
            }
        }
    }


    /***
     * Used to make a string of <c>count</c> number of spaces
     * @param count The number of spaces to make
     * @return A string of <c>count</c> number of spaces
     */
    private static String numSpaces(int count) {
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < count; i++) spaces.append(' ');
        return spaces.toString();
    }

    // REGION END

    // REGION FOR NEXT NEXT COMPLETE IN TREE

    /**
     * Searches for the possible completions of the last string in the <c>searchFor</c> String array.
     * The possible completes are the last keys in the given map where all the keys prior match the strings in
     * the <c>searchFor</c> array.
     *
     * @param searchFor The String array to find a complete for.
     * @param map       The map to find the completes from.
     * @return A list of all potential completes with the given searchFor and map.
     */
    static List<String> nextCompleteInTree(String[] searchFor, Map<String, Object> map) {
        if (searchFor[searchFor.length - 1] == null)
            searchFor[searchFor.length - 1] = "";
        return nextCompleteInTree(searchFor, map, 0);
    }


    /**
     * This is the worker for the main <c>nextCompleteInTree()</c> method. It should not be called directly.
     *
     * @param searchFor The String array to find a complete for.
     * @param map       The map to find the completes from.
     * @param depth     How deep into the search are we.
     * @return A list of all potential completes with the given searchFor and map.
     */
    @SuppressWarnings(value = "unchecked")
    private static List<String> nextCompleteInTree(String[] searchFor, Map<String, Object> map, int depth) {
        ArrayList<String> completes = new ArrayList<>();
        if (searchFor.length > depth + 1) { // IF we are not at the end, find the match and go one deeper
            for (String key : map.keySet())
                if (searchFor[depth].equalsIgnoreCase(key)) {
//                    System.out.println("rec~ " + key); //DEBUG
                    if (map.get(key) != null)
                        return nextCompleteInTree(searchFor, (Map<String, Object>) map.get(key), depth + 1);
                }
        } else { // ELSE we are at the end, and we need to find the possible completes
            String lastInSearch = searchFor[searchFor.length - 1];
            for (String key : map.keySet()) {
                if (key.toLowerCase().contains(lastInSearch.toLowerCase()))
                    completes.add(key);
            }
        }
        return completes;
    }

    // REGION END

}