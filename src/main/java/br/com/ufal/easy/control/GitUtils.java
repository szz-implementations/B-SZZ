package br.com.ufal.easy.control;

import br.com.ufal.easy.model.ModifiedFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
* Federal University of Alagoas - 2018
*
*/

public class GitUtils {

    //Singleton
    private static GitUtils instance = null;

    private GitUtils() {
    }

    public static GitUtils getInstance() {
        if (instance == null) {
            instance = new GitUtils();
        }
        return instance;
    }

    /**
     * Returns a Calendar object containing date and time from a commit
     * @param hash
     * @param repositoryPath
     * @return Calendar
     */
    public Calendar getDateTime(String hash, String repositoryPath) {
        String command = "git show " + hash + " -s --date=iso --format=\"%cd\"";
        String output = null;

        try {
            Process process = Runtime.getRuntime().exec(command, null, new File(repositoryPath));
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            output = input.readLine();
            String tokens[] = output.replace("\"", "").split(" ");

            output = tokens[0] + " " + tokens[1];
            input.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            cal.setTime(sdf.parse(output));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cal;
    }


    /**
     * Converts date
     * @param annotateDate
     * @return Calendar
     */
    public Calendar getDateTime(String annotateDate) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

        try {
            cal.setTime(sdf.parse(annotateDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cal;
    }

    /**
     * Returns a List<ModifiedFile> containing all modified files in a commit
     * @param hash
     * @param repository
     * @return List<ModifiedFile>
     */
    public List<ModifiedFile> getChangedFiles(String hash, String repository) {
        ArrayList<ModifiedFile> results = new ArrayList<>();
        String commitHead = Utils.getInstance().executeCommand("git rev-list HEAD | tail -n 1", repository);

        String command;
        if(hash.equals(commitHead)){
            command = "git diff-tree --no-commit-id --name-only -r " + hash + " HEAD";
        } else {
            command = "git log -m -1 --name-only --pretty=" + "format:"+ " " + hash;
        }

        String output;
        try {
            Process process = Runtime.getRuntime().exec(command, null, new File(repository));
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((output = input.readLine()) != null) {
                //B-SZZ take into account any files changed in a revision, not only .java files
                ModifiedFile file = new ModifiedFile(output, new ArrayList<>());
                results.add(file);
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Returns a String containing in the immediately previous commit hash
     * @param hash
     * @param repositoryPath
     * @return String
     */
    private static String retrievePreviousCommit(final String hash, final String repositoryPath) {
        final String lastCommit = Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "rev-parse", "HEAD");
        Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "checkout", hash);
        final String previousCommit = Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "rev-parse", "HEAD~1");
        Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "checkout", lastCommit);
        return previousCommit;
    }

    /**
     * Returns a String containing a file content in the immediately previous commit
     * @param hash
     * @param filePath
     * @param repositoryPath
     * @return String
     * @throws FileNotFoundException
     */
    private String retrieveFileContentFromCommit(final String hash, final String filePath, final String repositoryPath) throws FileNotFoundException {
        final String lastCommit = Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "rev-parse", "HEAD");
        Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "checkout", hash);
        StringBuilder fileContentBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(repositoryPath + filePath))) {
            String lineContent = null;

            while ((lineContent = br.readLine()) != null) {
                fileContentBuilder.append(lineContent + "\n");
            }

        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.getInstance().executeCommand("--work-tree=" + repositoryPath, "--git-dir=" + repositoryPath + ".git", "checkout", lastCommit);
        return fileContentBuilder.toString();
    }

    /**
     * Returns a Map<String, String> containing a file content in its atual and previous revision
     * @param hash
     * @param file
     * @param repositoryPath
     * @return Map<String, String>
     * @throws FileNotFoundException
     * @throws IllegalArgumentException
     */
    private Map<String, String> retrieveTempFilesToDiff(final String hash, final String file, final String repositoryPath)
            throws FileNotFoundException, IllegalArgumentException {
        String fixFileContent;
        String previousFileContent;

        try {
            fixFileContent = retrieveFileContentFromCommit(hash, file, repositoryPath);
            final String previousCommitHash = retrievePreviousCommit(hash, repositoryPath);
            previousFileContent = retrieveFileContentFromCommit(previousCommitHash, file, repositoryPath);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        }

        Map<String, String> tempFilesPath = new HashMap<>();

        if ("".equals(fixFileContent.trim()) || "".equals(previousFileContent.trim())) {
            return tempFilesPath;
        }

        try {
            File fixTempFile = File.createTempFile(file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf(".")), ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(fixTempFile));
            bw.write(fixFileContent);
            bw.close();
            File previousTempFile = File.createTempFile(file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf(".")), ".txt");
            bw = new BufferedWriter(new FileWriter(previousTempFile));
            bw.write(previousFileContent);
            bw.close();

            tempFilesPath.put(previousTempFile.getAbsolutePath(), fixTempFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            System.err.println("The file name is too short");
            throw new IllegalArgumentException();
        }

        return tempFilesPath;
    }

}
