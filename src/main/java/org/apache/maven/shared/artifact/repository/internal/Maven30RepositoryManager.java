package org.apache.maven.shared.artifact.repository.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.repository.RepositoryManager;
import org.apache.maven.shared.artifact.repository.RepositoryManagerException;
import org.apache.maven.shared.artifact.repository.internal.Invoker;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositoryCache;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

@Component( role = RepositoryManager.class, hint = "maven3" )
public class Maven30RepositoryManager
    implements RepositoryManager
{

    @Requirement
    private RepositorySystem repositorySystem;

    /**
     * {@inheritDoc}
     */
    public String getPathForLocalArtifact( ProjectBuildingRequest buildingRequest,
                                           org.apache.maven.artifact.Artifact mavenArtifact )
    {
        Artifact aetherArtifact;
        RepositorySystemSession session;
        
        // LRM.getPathForLocalArtifact() won't throw an Exception, so translate reflection error to RuntimeException
        try
        {
            aetherArtifact =
                (Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                           org.apache.maven.artifact.Artifact.class, mavenArtifact );

            session = (RepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession" );
        }
        catch ( RepositoryManagerException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        return session.getLocalRepositoryManager().getPathForLocalArtifact( aetherArtifact );
    }

    /**
     * {@inheritDoc}
     */
    public ProjectBuildingRequest setLocalRepositoryBasedir( ProjectBuildingRequest buildingRequest, File basedir )
    {
        ProjectBuildingRequest newRequest = new DefaultProjectBuildingRequest( buildingRequest );

        RepositorySystemSession session;
        try
        {
            session = (RepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession" );
        }
        catch ( RepositoryManagerException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        // "clone" session and replace localRepository
        DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession( session );

        // Clear cache, since we're using a new local repository
        newSession.setCache( new DefaultRepositoryCache() );

        // keep same repositoryType
        String repositoryType = resolveRepositoryType( session.getLocalRepository() );

        LocalRepositoryManager localRepositoryManager =
            repositorySystem.newLocalRepositoryManager( new LocalRepository( basedir, repositoryType ) );

        newSession.setLocalRepositoryManager( localRepositoryManager );

        try
        {
            Invoker.invoke( newRequest, "setRepositorySession", RepositorySystemSession.class, newSession );
        }
        catch ( RepositoryManagerException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        return newRequest;
    }

    /**
     * {@inheritDoc}
     */
    public File getLocalRepositoryBasedir( ProjectBuildingRequest buildingRequest )
    {
        RepositorySystemSession session;
        try
        {
            session = (RepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession" );
        }
        catch ( RepositoryManagerException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        return session.getLocalRepository().getBasedir();
    }

    protected String resolveRepositoryType( LocalRepository localRepository )
    {
        return localRepository.getContentType();
    }
}
