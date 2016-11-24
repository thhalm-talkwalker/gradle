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

package org.gradle.play.plugins;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;

import java.io.File;
import java.util.Set;

/**
 * Conventional locations and names for play plugins.
 */
@Incubating
public class PlayPluginConfigurations {
    public static final String PLATFORM_CONFIGURATION = "playPlatform";
    public static final String COMPILE_CONFIGURATION = "play";
    public static final String RUN_CONFIGURATION = "playRun";
    public static final String TEST_COMPILE_CONFIGURATION = "playTest";

    private final ConfigurationContainer configurations;
    private final DependencyHandler dependencyHandler;

    public PlayPluginConfigurations(ConfigurationContainer configurations, DependencyHandler dependencyHandler) {
        this.configurations = configurations;
        this.dependencyHandler = dependencyHandler;
        Configuration playPlatform = configurations.create(PLATFORM_CONFIGURATION);

        Configuration playCompile = configurations.create(COMPILE_CONFIGURATION);
        playCompile.extendsFrom(playPlatform);

        Configuration playRun = configurations.create(RUN_CONFIGURATION);
        playRun.extendsFrom(playCompile);

        Configuration playTestCompile = configurations.create(TEST_COMPILE_CONFIGURATION);
        playTestCompile.extendsFrom(playCompile);

        configurations.maybeCreate(Dependency.DEFAULT_CONFIGURATION).extendsFrom(playCompile);
    }

    public PlayConfiguration getPlayPlatform() {
        return new PlayConfiguration(PLATFORM_CONFIGURATION);
    }

    public PlayConfiguration getPlay() {
        return new PlayConfiguration(COMPILE_CONFIGURATION);
    }

    public PlayConfiguration getPlayRun() {
        return new PlayConfiguration(RUN_CONFIGURATION);
    }

    public PlayConfiguration getPlayTest() {
        return new PlayConfiguration(TEST_COMPILE_CONFIGURATION);
    }

    /**
     * Wrapper around a Configuration instance used by the PlayApplicationPlugin.
     */
    class PlayConfiguration {
        private final String name;

        PlayConfiguration(String name) {
            this.name = name;
        }

        Configuration getConfiguration() {
            return configurations.getByName(name);
        }

        FileCollection getAllArtifacts() {
            return getConfiguration();
        }

        FileCollection getChangingArtifacts(Set<Project> nonChangingProjects) {
            return new FilterByProjectComponentTypeFileCollection(getConfiguration(), nonChangingProjects, true);
        }

        FileCollection getNonChangingArtifacts(Set<Project> nonChangingProjects) {
           return new FilterByProjectComponentTypeFileCollection(getConfiguration(), nonChangingProjects, false);
        }

        void addDependency(Object notation) {
            dependencyHandler.add(name, notation);
        }

        void addArtifact(PublishArtifact artifact) {
            configurations.getByName(name).getArtifacts().add(artifact);
        }
    }

    private static class FilterByProjectComponentTypeFileCollection extends LazilyInitializedFileCollection {
        private final Configuration configuration;
        private final boolean matchProjectComponents;
        private final Set<Project> nonChangingProjects;

        private FilterByProjectComponentTypeFileCollection(Configuration configuration, Set<Project> nonChangingProjects, boolean matchProjectComponents) {
            this.configuration = configuration;
            this.nonChangingProjects = nonChangingProjects;
            this.matchProjectComponents = matchProjectComponents;
        }

        @Override
        public String getDisplayName() {
            return configuration.toString();
        }

        @Override
        public FileCollectionInternal createDelegate() {
            ImmutableSet.Builder<File> files = ImmutableSet.builder();
            for (ResolvedArtifact artifact : configuration.getResolvedConfiguration().getResolvedArtifacts()) {
                if (artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
                    ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier) artifact.getId().getComponentIdentifier();
                    boolean add = matchProjectComponents;
                    //handle projects marked as "nonChanging"
                    for (Project project : nonChangingProjects) {
                        if (projectComponentIdentifier.getProjectPath().equals(project.getPath())) {
                            add = !matchProjectComponents;
                            break;
                        }
                    }
                    if (add) {
                        files.add(artifact.getFile());
                    }
                } else if (!matchProjectComponents) {
                    files.add(artifact.getFile());
                }
            }
            return new SimpleFileCollection(files.build());
        }

        @Override
        public void visitDependencies(TaskDependencyResolveContext context) {
            context.add(configuration);
        }
    }
}
