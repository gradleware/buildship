/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package eclipsebuild

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project

/**
 * Static helper functions which can be used different places around the Eclipse plugin build.
 */
public class PluginUtils {

    /**
     * Copies the content of the plugin project's MANIFEST.MF file to the plugin artifact generated by the <i>jar</i> task of this project.
     * While copying the MANIFEST.MF file, the value of the <i>Bundle-Version</i> attribute is replaced with the build version of the project.
     *
     * @param project the plugin project whose MANIFEST.MF file of the artifact of the jar task to update
     */
    static void updatePluginManifest(Project project) {
        project.jar {
            manifest {
                attributes 'Bundle-Version' : project.version
                from('META-INF/MANIFEST.MF') {
                    eachEntry { entry ->
                        if (entry.key == 'Bundle-Version') {
                            entry.value = project.version
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures the plugin artifact generated by the <i>jar</i> task of this plugin project to have the same content as declared in the
     * plugin project's build.properties file.
     *
     * @param project the plugin project whose content of the artifact of the jar task to configure
     */
    static void configurePluginJarInput(Project project) {
        def buildProperties = readBuildPropertiesFile(project)
        Set resources = splitBinIncludes(buildProperties.getProperty('bin.includes'))
        addPluginJarInput(resources, project)
    }

    /**
     * Configures the feature artifact generated by the <i>jar</i> task of this feature project to have the same content as declared in the
     * plugin project's build.properties file. While configuring the <i>jar</i> task, the version placeholder in the <i>feature.xml<i/> file
     * is replaced with the build version of the project.
     *
     * @param project the plugin project whose content of the artifact of the jar task to configure
     */
    static void configureFeatureJarInput(Project project) {
        def buildProperties = readBuildPropertiesFile(project)
        Set resources = splitBinIncludes(buildProperties.getProperty('bin.includes'))
        addFeatureJarInput(resources, project)
    }

    private static Properties readBuildPropertiesFile(Project project) {
        def buildProperties = new Properties()
        def fis = new FileInputStream(project.file('build.properties'))
        buildProperties.load(fis)
        fis.close()
        buildProperties
    }

    private static Set splitBinIncludes(String binIncludes) {
        Set result = new LinkedHashSet()
        def virtualResources = ['.']
        binIncludes.split(',').each { relPath ->
            relPath = relPath.trim()
            if(!relPath.isEmpty() && !(relPath in virtualResources)) {
                result.add(relPath)
            }
        }
        result
    }

    private static void addPluginJarInput(Set locations, Project project) {
        for (String location in locations) {
            File resource = project.file(location)
            if (resource.isDirectory()) {
                project.jar {
                    from(location, { into(location) })
                }
            } else {
                project.jar {
                    from location
                }
            }
        }
    }

    private static void addFeatureJarInput(Set locations, Project project) {
        for (String location in locations) {
            File resource = project.file(location)
            if (resource.isDirectory()) {
                project.jar {
                    from(location, { into(location) })
                }
            } else {
                project.jar {
                    from location
                    if (resource.name == 'feature.xml') {
                        filter(ReplaceTokens, tokens:['1.0.0.qualifier' : '"' + project.version + '"','0.0.0' : '"' + project.version + '"'], beginToken: '"', endToken: '"')
                    }
                }
            }
        }
    }

}
