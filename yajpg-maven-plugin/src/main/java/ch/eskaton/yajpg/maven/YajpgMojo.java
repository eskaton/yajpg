/*
 * $Id: YajpgMojo.java,v 1.1 2009/04/26 16:34:22 moser Exp $
 */
package ch.eskaton.yajpg.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import ch.eskaton.yajpg.ConfigException;
import ch.eskaton.yajpg.Generator;

/**
 * Generates a parser class.
 * 
 * @goal generate
 * @phase generate-sources
 */
public class YajpgMojo extends AbstractMojo {

	/**
	 * Grammar file.
	 * 
	 * @parameter
	 * @required
	 */
	private String grammarFile;

	/**
	 * Reference to maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Path to generated sources
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/yajpg"
	 * @required
	 */
	private String sourcePath;

	public void execute() throws MojoFailureException, MojoExecutionException {
		String grammarPath = project.getBasedir() + File.separator
				+ grammarFile;
		getLog().info("Using grammar file: " + grammarPath);

		try {
			File path = new File(sourcePath);

			if (!path.exists() && !path.mkdirs()) {
				throw new MojoFailureException("Couldn't create directory "
						+ sourcePath);
			}

			Generator gen = new Generator(grammarPath);
			gen.run(sourcePath);

			project.addCompileSourceRoot(sourcePath);

			getLog().info("Parser sucessfully generated");
		} catch (IOException e) {
			getLog().error(e.getMessage());
			getLog().debug(e);
			throw new MojoExecutionException("I/O error", e);
		} catch (ConfigException e) {
			getLog().error(
					"Configuration error in " + grammarFile + ": "
							+ e.getMessage());
			getLog().debug(e);
			throw new MojoFailureException("Configuration error in "
					+ grammarFile + ": " + e.getMessage());
		}
	}

}

