/*
 *  Copyright (c) 2009, Adrian Moser
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author nor the
 *  names of its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL AUTHOR BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
     * Generate a lexer?
     * 
     * @parameter default="false"
     */
    private boolean generateLexer;

    /**
     * Write debugging output?
     * 
     * @parameter default="false"
     */
    private boolean debugging;

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
        getLog().info("Using grammar file: " + grammarFile);

        try {
            File path = new File(sourcePath);

            if (!path.exists() && !path.mkdirs()) {
                throw new MojoFailureException("Couldn't create directory "
                        + sourcePath);
            }

            Generator gen = new Generator(grammarFile);
            gen.run(sourcePath, generateLexer);

            if (debugging) {
                gen.printStates();
            }

            project.addCompileSourceRoot(sourcePath);

            getLog().info("Parser successfully generated");
        } catch (IOException e) {
            getLog().error(e.getMessage());
            getLog().debug(e);
            throw new MojoExecutionException("i/o error", e);
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
