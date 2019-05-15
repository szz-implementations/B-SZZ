package br.com.ufal.easy.start;

/**
 * Federal University of Alagoas - 2018
 *
 */

public class Main {

    //Local path to the repository
    public static String repositoryPath = "/Users/project/";

    //Local path to the folder containing the .json input files
    public static String bugPath = "/Users/bugs/";

    //Local path to the .json output file
    public static String output = "/Users/output.json";

    public static void main(String[] args) {

        App.run(repositoryPath, bugPath, output);

    }

}