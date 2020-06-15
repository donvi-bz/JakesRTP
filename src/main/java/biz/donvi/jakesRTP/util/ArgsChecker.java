package biz.donvi.jakesRTP.util;


import java.util.ArrayList;
import java.util.Arrays;

/***
 * Class created to easily check if a users command arguments match different sets of pre-set arguments.
 * To use, create the object with the arguments given by the USER (the ones that may be incorrect).
 * Then make an if statement for each possible sub command, with the value to be tested being
 *   <c>this.matches(exactLength,x)</c>
 *   where <c>this</c> is the name of the object in context,
 *   and <c>x</c> is an array of arguments that match a particular command (null matching anything).
 */
public class ArgsChecker {

    private String[] inputArgs;
    private boolean matchFound;
    private String[] remainingArgs;

    /**
     * Constructor;
     * Sets the object's input args to the given args,
     *   and sets <c>matchFound</c> to <c>false</c>.
     * @param inputArgs The original arguments the user gave.
     */
    public ArgsChecker(String[] inputArgs) {
        this.inputArgs = inputArgs;
        matchFound = false;
    }

    /**
     * Tests if the arguments given upon creation match the ones given in this method.
     * This method will only ever return true ONCE per instance of the object.
     * @param exactLength Do the number of arguments have to match?
     * @param argsToMatch The array of strings, that if the users match, will cause this method to return true.
     * @return True: The users args DO match the given args, and this is the FIRST match.
     *         False: The users args and given args DO NOT match, or a match was already found.
     */
    public boolean matches(boolean exactLength, String... argsToMatch) {
        //If a match has already been found, we want to return false.
        //If exact length is set to true, return false if the lengths do not match.
        //If less args are given then required, it is impossible for everything to match, so return false.
        if (matchFound
            || (exactLength && inputArgs.length != argsToMatch.length)
            || (inputArgs.length < argsToMatch.length))
            return false;

        //All the args given by the user must match the args provided to the checker.
        //By this point, it is assumed that if more args are given than required,
        //  the extra args do not need to be checked.
        for (int i = 0; i < argsToMatch.length; i++)
//            if (argsToMatch[i] == null || argsToMatch[i].equals("")) continue; //Most likely unnecessary
            if (argsToMatch[i] != null && !inputArgs[i].equalsIgnoreCase(argsToMatch[i]))
                return false;

        //By this point, it is assumed that a match HAS been found.
        //Now we set up the remaining args property.
        ArrayList<String> remainingArgs = new ArrayList<>();
        //This for loop adds every arg given by the user that matches to a null in the checker.
        for (int i = 0; i < argsToMatch.length; i++)
            if (argsToMatch[i] == null)
                remainingArgs.add(inputArgs[i]);
        //Add any more args given by the user to the remainingArgs list if necessary.
        if (!exactLength) remainingArgs.addAll(Arrays.asList(inputArgs).subList(argsToMatch.length, inputArgs.length));
        //Set remainingArgs array to the values of the remainingArgs list.

        this.remainingArgs = remainingArgs.toArray(new String[0]);


        //The match has been found, and the remainingArgs array has been set,
        //  so we set matchFound to true, and return true at the same time.
        return matchFound = true;
    }

    /***
     * Returns any string from the user that did not need to directly match a string to be valid.
     * This will ONLY work when a match has been found
     * @return An array of all the arguments that did not directly match.
     */
    public String[] getRemainingArgs() {
        if (matchFound) return remainingArgs;
        else throw new Error("Can not find remaining args when no match has been found.");
    }
}
