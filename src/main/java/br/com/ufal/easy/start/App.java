package br.com.ufal.easy.start;
import br.com.ufal.easy.control.GitUtils;
import br.com.ufal.easy.control.Utils;
import br.com.ufal.easy.model.*;

import java.io.File;
import java.util.*;

/**
 * Federal University of Alagoas - 2018
 *
 */


public class App {

    public static void run(String repositoryPath, String bugsInfoPath, String outputPath) {
        //Get all json input files
        List<String> jsonFilePath = Utils.getInstance().getAllJsonFiles(new File(bugsInfoPath));
        Utils.getInstance().FileWrite(outputPath,"[");
        for (String json : jsonFilePath) {

            System.out.println("Reading: "+ bugsInfoPath + json);
            //Read a json, corresponding to a bug
            Bug bug = Utils.getInstance().readJson(bugsInfoPath + json);

            //Load the fixes and report commit
            ArrayList<CommitFix> _commitFix = bug.getCommitFix();
            CommitReport commitReport = bug.getCommitReport();

            //For each fix commit, collect the insertion bug commits
            for (CommitFix commitFix : _commitFix) {
                Set<String> insertionCommit = new HashSet<>();

                System.out.println("Bug: " + json);
                System.out.println("Fix: " + commitFix.getHash());
                System.out.println("Report: " + commitReport.getHash());

                //Get the files modified to fix the given bug
                List<ModifiedFile> modifiedFile = GitUtils.getInstance().getChangedFiles(commitFix.getHash(), repositoryPath);

                //For each modified file
                for (ModifiedFile file : modifiedFile) {
                    //Collect the removed lines
                    file.setLineRemoved(Utils.getInstance().getBugIntroductionCandidates(commitFix.getHash(), file.getPath(), repositoryPath));
                    //Collect the insertion bug commits
                    insertionCommit.addAll(Utils.getInstance().getInsertionCommits(commitFix.getHash(), commitReport.getHash(), file, repositoryPath));
                }

                //Print result into output file
                if (modifiedFile.size() != 0) {
                    Utils.getInstance().FileWrite(outputPath,
                            "{ \"issue_id\": "+json.replace(".json", "") +
                                    ", \"commit_fix\": \""+ commitFix.getHash() + "\", "+
                                    "\"commit_report\": \"" + commitReport.getHash() + "\", " +
                                    "\"bug_introducing_changes\": " + Utils.getInstance().jsonFromInsertionCommit(insertionCommit)
                                    + " },");
                } else {
                    Utils.getInstance().FileWrite(outputPath,
                            "{ \"issue_id\": "+json.replace(".json", "") +
                                    ", \"commit_fix\": \""+ commitFix.getHash() + "\", "+
                                    "\"commit_report\": \"" + commitReport.getHash() +
                                    "\", \"bug_introducing_changes\": [] },");
                }
            }
        }

        Utils.getInstance().FileWrite(outputPath,"]");

    }
}
