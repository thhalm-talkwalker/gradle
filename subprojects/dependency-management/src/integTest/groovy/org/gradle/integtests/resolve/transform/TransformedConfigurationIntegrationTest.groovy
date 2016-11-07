/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.transform

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class TransformedConfigurationIntegrationTest extends AbstractIntegrationSpec {

    def "Can resolve transformed configuration"() {
        when:
        buildFile << """
import org.gradle.api.artifacts.transform.*
            buildscript {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath 'com.google.guava:guava:19.0'
                }
            }

            apply plugin: 'java'
            repositories {
                mavenCentral()
            }
            dependencies {
                compile 'com.google.guava:guava:19.0'
            }
            configurations.compile.resolutionStrategy {
                registerTransform(FileHasher) {
                    outputDirectory = project.file("\${buildDir}/transformed")
                }
            }
            task resolve(type: Copy) {
                from configurations.compile.withType('md5')
                into "\${buildDir}/libs"
            }

            @TransformInput(type = 'jar')
            class FileHasher extends DependencyTransform {
                private File output

                @TransformOutput(type = 'md5')
                File getOutput() {
                    return output
                }

                void transform(File input) {
                    output = new File(outputDirectory, input.name + ".md5")
                    println "Transforming \${input} to \${output}"

                    if (!output.exists()) {
                        def inputHash = com.google.common.io.Files.hash(input, com.google.common.hash.Hashing.md5())
                        output << inputHash
                    }
                }
            }
"""

        succeeds "resolve"

        then:
        file("build/libs").assertContainsDescendants("guava-19.0.jar.md5")
        assert file("build/libs/guava-19.0.jar.md5").text == "43bfc49bdc7324f6daaa60c1ee9f3972"
    }

}