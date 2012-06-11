/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.build.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;

/**
 * Compares two ZIP files, generating a {@link PatternSet}
 */
public class ZipDiffTask extends Task {

    public static String newline = System.getProperty("line.separator");

    protected File file1;

    protected File file2;

    protected File includesfile;

    protected File excludesfile;

    private FileWriter fileWriter;

    private PatternSet patternSet;

    public void setFile1(File file1) {
        this.file1 = file1;
    }

    public void setFile2(File file2) {
        this.file2 = file2;
    }

    public void setIncludesfile(File includesfile) {
        this.includesfile = includesfile;
    }

    public void setExcludesfile(File excludesfile) {
        this.excludesfile = excludesfile;
    }

    public void setPatternsetid(String id) {
        patternSet = new PatternSet();
        getProject().addReference(id, patternSet);
    }

    @Override
    public void execute() throws BuildException {
        ZipFile zipfile1;
        ZipFile zipfile2;

        try {
            zipfile1 = new ZipFile(file1);
            zipfile2 = new ZipFile(file2);
        } catch (IOException e) {
            throw new BuildException("Error opening " + file1 + " or " + file2,
                    e);
        }

        Set<String> set1 = new LinkedHashSet<String>();
        for (Enumeration<? extends ZipEntry> zipEntries = zipfile1.entries(); zipEntries.hasMoreElements();) {
            set1.add((zipEntries.nextElement()).getName());
        }
        Set<String> set2 = new LinkedHashSet<String>();
        for (Enumeration<? extends ZipEntry> zipEntries = zipfile2.entries(); zipEntries.hasMoreElements();) {
            set2.add((zipEntries.nextElement()).getName());
        }

        try {
            if (includesfile != null) {
                includesfile.createNewFile();
                fileWriter = new FileWriter(includesfile);
            }

            // includes (files from file1 not present or differ in file2)
            for (Iterator<String> i = set1.iterator(); i.hasNext();) {
                String filename = i.next();
                if (!set2.contains(filename)) {
                    log("Only in " + file1.getName() + ": " + filename,
                            Project.MSG_INFO);
                    include(filename, fileWriter);
                    continue;
                }
                set2.remove(filename);
                try {
                    if (!IOUtils.contentEquals(
                            zipfile1.getInputStream(zipfile1.getEntry(filename)),
                            zipfile2.getInputStream(zipfile2.getEntry(filename)))) {
                        log("Content differs: " + filename, Project.MSG_INFO);
                        include(filename, fileWriter);
                        continue;
                    }
                } catch (IOException e) {
                    log(e, Project.MSG_WARN);
                    continue;
                }
            }
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            IOUtils.closeQuietly(fileWriter);
        }

        // excludes (files from file2 not present in file1)
        try {
            if (excludesfile != null) {
                excludesfile.createNewFile();
                fileWriter = new FileWriter(excludesfile);
            }
            for (Iterator<String> i = set2.iterator(); i.hasNext();) {
                String filename = i.next();
                log("Only in " + file2.getName() + ": " + filename,
                        Project.MSG_INFO);
                exclude(filename, fileWriter);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            IOUtils.closeQuietly(fileWriter);
        }

    }

    protected void exclude(String filename, FileWriter writer)
            throws IOException {
        if (patternSet != null) {
            NameEntry exclude = patternSet.createExclude();
            exclude.setName(filename);
        }
        write(filename, writer);
    }

    protected void include(String filename, FileWriter writer)
            throws IOException {
        if (patternSet != null) {
            NameEntry include = patternSet.createInclude();
            include.setName(filename);
        }
        write(filename, writer);
    }

    private void write(String filename, FileWriter writer) throws IOException {
        if (writer != null) {
            writer.write(filename + newline);
        }
    }
}