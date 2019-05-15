package br.com.ufal.easy.model;

import java.util.List;

/**
 * Federal University of Alagoas - 2018
 *
 */


public class ModifiedFile {

	private String path;
	private List<String> lineRemoved;

	public ModifiedFile(String path, List<String> lineRemoved) {
		super();
		this.path = path;
		this.lineRemoved = lineRemoved;
	}

	public List<String> getLineRemoved() {
		return lineRemoved;
	}

	public void setLineRemoved(List<String> lineRemoved) {
		this.lineRemoved = lineRemoved;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void addLineRemoved(String line) {
		lineRemoved.add(line);
	}

	public void removeLineRemoved (String line) {
		lineRemoved.remove(line);
	}
}
