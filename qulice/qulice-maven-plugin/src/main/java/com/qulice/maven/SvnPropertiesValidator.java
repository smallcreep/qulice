/**
 * Copyright (c) 2011-2012, Qulice.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the Qulice.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qulice.maven;

import com.jcabi.log.Logger;
import com.qulice.spi.ValidationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;

/**
 * Check for required svn properties in all text files.
 *
 * <p>Every text file should have two SVN properties:
 *
 * <pre>
 * svn:keywords=Id
 * svn:eol-style=native
 * </pre>
 *
 * <p>Read SVN documentation about how you can set them.
 *
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @author Yegor Bugayenko (yegor@qulice.com)
 * @version $Id$
 * @see <a href="http://svnbook.red-bean.com/en/1.5/svn.ref.properties.html">Properties in Subversion</a>
 */
public final class SvnPropertiesValidator implements MavenValidator {

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     */
    @Override
    public void validate(final MavenEnvironment env)
        throws ValidationException {
        if (this.isSvn(env.project())) {
            final Collection<File> files = FileUtils.listFiles(
                env.project().getBasedir(),
                new String[] {
                    "java", "txt", "xsl", "xml", "html",
                    "php", "py", "groovy", "ini", "properties",
                },
                true
            );
            for (File file : files) {
                this.check(file);
            }
            Logger.info(
                this,
                "%d text files have necessary SVN properties",
                files.size()
            );
        } else {
            Logger.info(this, "This is not an SVN project");
        }
    }

    /**
     * Check whether this project uses SVN.
     * @param project The Maven project
     * @return TRUE if yes
     */
    private boolean isSvn(final MavenProject project) {
        return project.getScm() != null
            && project.getScm().getConnection() != null
            && project.getScm().getConnection().startsWith("scm:svn");
    }

    /**
     * Check one file.
     * @param file The file to check
     * @throws ValidationException If any errors
     * @checkstyle RedundantThrows (4 lines)
     */
    private void check(final File file) throws ValidationException {
        final String style = this.propget(file, "svn:eol-style");
        if (!"native".equals(style)) {
            throw new ValidationException(
                "File %s doesn't have 'svn:eol-style' set to 'native': %s",
                file,
                style
            );
        }
        final String keywords = this.propget(file, "svn:keywords");
        if (!keywords.contains("Id")) {
            throw new ValidationException(
                "File %s doesn't have 'svn:keywords' with 'Id': %s",
                file,
                keywords
            );
        }
    }

    /**
     * Get SVN property from the file.
     * @param file The file to check
     * @param name Property name
     * @return Property value
     * @throws ValidationException If any errors
     * @checkstyle RedundantThrows (4 lines)
     */
    private String propget(final File file, final String name)
        throws ValidationException {
        final ProcessBuilder builder = new ProcessBuilder(
            "svn",
            "propget",
            name,
            file.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        try {
            final Process process = builder.start();
            process.waitFor();
            return IOUtils.toString(process.getInputStream());
        } catch (java.io.IOException ex) {
            throw new ValidationException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ValidationException(ex);
        }
    }

}
