/*
 * Copyright 2014 the original author or authors.
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


package org.gradle.nativeplatform.test.googletest
import org.gradle.integtests.fixtures.Sample
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.ToolChainRequirement
import org.gradle.test.fixtures.file.TestDirectoryProvider
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Assume
import org.junit.Rule
import static org.junit.Assume.*

@Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
class GoogleTestSamplesIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    @Rule public final Sample googleTest = sample(temporaryFolder, 'google-test')

    private static Sample sample(TestDirectoryProvider testDirectoryProvider, String name) {
        return new Sample(testDirectoryProvider, "native-binaries/${name}", name)
    }

    def "googleTest"() {
        // Ignoring Visual Studio toolchain tests gradle/gradle#893
        Assume.assumeFalse(toolChain.isVisualCpp())

        given:
        // On windows, GoogleTest sample only works out of the box with VS2013 and VS2015
        assumeTrue(!OperatingSystem.current().windows || isVisualCpp2013() || isVisualCpp2015())

        sample googleTest

        when:
        succeeds "runOperatorsTestPassingGoogleTestExe"

        then:
        executedAndNotSkipped ":compileOperatorsTestPassingGoogleTestExeOperatorsTestCpp",
                ":linkOperatorsTestPassingGoogleTestExe", ":operatorsTestPassingGoogleTestExe",
                ":installOperatorsTestPassingGoogleTestExe", ":runOperatorsTestPassingGoogleTestExe"

        and:
        def passingResults = new GoogleTestTestResults(googleTest.dir.file("build/test-results/operatorsTest/passing/test_detail.xml"))
        passingResults.suiteNames == ['OperatorTests']
        passingResults.suites['OperatorTests'].passingTests == ['test_minus', 'test_plus']
        passingResults.suites['OperatorTests'].failingTests == []
        passingResults.checkTestCases(2, 2, 0)

        when:
        sample googleTest
        fails "runOperatorsTestFailingGoogleTestExe"

        then:
        executedAndNotSkipped ":compileOperatorsTestFailingGoogleTestExeOperatorsTestCpp",
                ":linkOperatorsTestFailingGoogleTestExe", ":operatorsTestFailingGoogleTestExe",
                ":installOperatorsTestFailingGoogleTestExe", ":runOperatorsTestFailingGoogleTestExe"

        and:
        def failingResults = new GoogleTestTestResults(googleTest.dir.file("build/test-results/operatorsTest/failing/test_detail.xml"))
        failingResults.suiteNames == ['OperatorTests']
        failingResults.suites['OperatorTests'].passingTests == ['test_minus']
        failingResults.suites['OperatorTests'].failingTests == ['test_plus']
        failingResults.checkTestCases(2, 1, 1)
    }

    private static boolean isVisualCpp2013() {
        return toolChain.meets(ToolChainRequirement.VISUALCPP_2013)
    }

    private static boolean isVisualCpp2015() {
        return toolChain.meets(ToolChainRequirement.VISUALCPP_2015)
    }
  }
