package br.com.ufal.easy.control;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import br.com.ufal.easy.model.Bug;
import br.com.ufal.easy.model.ModifiedFile;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * Federal University of Alagoas - 2018
 *
 */


public class Utils {
	//Singleton
	private static Utils instance = null;

	private Utils() {
	}

	public static Utils getInstance() {
		if (instance == null) {
			instance = new Utils();
		}
		return instance;
	}


	/**
	 * Returns a list of removed lines from a file in a commit
	 * @param hash
	 * @param filePath
	 * @param repositoryPath
	 * @return List<String>
	 */
	public static List<String> getBugIntroductionCandidates(String hash, String filePath, String repositoryPath) {
		List<String> removedLines = new ArrayList<>();
		String command = "git show " + hash + " -- " + filePath;
		String output = null;

		try {
			Process process = Runtime.getRuntime().exec(command, null, new File(repositoryPath));
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

			while ((output = input.readLine()) != null) {
				if (output.length() > 2) {
					if ((output.charAt(0) == '-') && !(output.charAt(1) == '-')) {
						removedLines.add(output.substring(1));
					}
				}
			}

			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return removedLines;
	}

	/**
	 * Returns a Set<String> containing commits who supposed are insertion bugs commits
	 * @param hashFix
	 * @param hashReport
	 * @param file
	 * @param repositoryPath
	 * @return Set<String>
	 */
	public Set<String> getInsertionCommits(String hashFix, String hashReport, ModifiedFile file, String repositoryPath) {
		Set<String> insertionCommits = new HashSet<>();
		List<String> annotateResult = new ArrayList<>();

		String command = "git annotate -l " + hashFix + "^ -- " + file.getPath();
		String output;

		try {
			Calendar dataReport = GitUtils.getInstance().getDateTime(hashReport, repositoryPath);
			Process process = Runtime.getRuntime().exec(command, null, new File(repositoryPath));
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

			while ((output = input.readLine()) != null) {
				annotateResult.add(output);
			}

			for (String removedLine : file.getLineRemoved()) {
				for (int i = 0; i < annotateResult.size(); i++) {
					if (annotateResult.get(i).contains(removedLine)) {
						Calendar dataCommit = GitUtils.getInstance().getDateTime(annotateResult.get(i).split("\t")[2]);

						if (dataCommit.before(dataReport)) {
							insertionCommits.add(annotateResult.get(i).split("\t")[0]+","+annotateResult.get(i).split("\t")[2]);
						} else {
							//System.out.println("The candidate doesn't came before report: " + annotateResult.get(i).split("\t")[0]);
						}
					}
				}

			}

			input.close();

		} catch (NullPointerException e) {
			return insertionCommits;
		} catch (Exception e){
			e.printStackTrace();
		}

		return insertionCommits;
	}


	/**
	 * Converts the output to json format
	 * @param insertionCommit
	 * @return String
	 */
	public String jsonFromInsertionCommit(Set<String> insertionCommit) {
		String ret = "[";
		for (String commit : insertionCommit) {
			ret += "{ \"hash\": \"" + commit.split(",")[0] + "\", \"commit_date\": \""+ commit.split(",")[1] +"\" },";
		}
		if (ret.equals("[")) {
			return "[]";
		} else {
			return ret.substring(0, ret.length() - 1) + "]";
		}
	}

	/**
	 * Returns List<String> containing all .json files in a folder
	 * @param folder
	 * @return List<String>
	 */
	public List<String> getAllJsonFiles(final File folder){
		List<String> files = new ArrayList<>();

		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				getAllJsonFiles(fileEntry);
			} else {
				if(fileEntry.getAbsolutePath().substring(fileEntry.getAbsolutePath().lastIndexOf("." )+1).equals("json"))
					files.add(fileEntry.getName());
			}
		}
		return files;
	}

	/**
	 * Returns a Bug containing data about a bug
	 * @param file
	 * @return Bug
	 */
	public Bug readJson(String file) {
		try {
			Reader reader = new FileReader(file);
			Gson gson = new Gson();

			return gson.fromJson(reader, Bug.class);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Write a content in a file
	 * @param fileName
	 * @param content
	 */
	public void FileWrite(String fileName, String content) {
		File file = new File(fileName);

		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream(new File(fileName), true));
			writer.println(content);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a String containing the output for a command execution
	 * @param arguments
	 * @return String
	 */
	public String executeCommand(final String... arguments) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		CommandLine commandLine = CommandLine.parse("git");

		if (arguments != null) {
			commandLine.addArguments(arguments);
		}

		DefaultExecutor defaultExecutor = new DefaultExecutor();
		defaultExecutor.setExitValue(0);

		try {
			defaultExecutor.setStreamHandler(streamHandler);
			defaultExecutor.execute(commandLine);
		} catch (ExecuteException e) {
			System.err.println("Execution failed.");
			System.err.println(commandLine);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("permission denied.");
			e.printStackTrace();
		}

		return outputStream.toString();
	}

	/**
	 *  Returns a String containing the output for a command execution in a repository
	 * @param comando
	 * @param repository
	 * @return String
	 */
	public String executeCommand(String comando, String repository) {
		File f = new File(repository);
		Process process = null;
		StringBuilder output = new StringBuilder();

		try {
			process = Runtime.getRuntime().exec(comando, null,f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		InputStream inputStream = process.getInputStream(); {
			int n;

			try {
				while ((n = inputStream.read()) != -1) {
					output.append((char) n);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return output.toString();
	}
}