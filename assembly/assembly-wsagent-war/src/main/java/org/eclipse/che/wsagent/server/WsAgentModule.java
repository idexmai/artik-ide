/*******************************************************************************
 * Copyright (c) 2016 Samsung Electronics Co., Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - Initial implementation
 *   Samsung Electronics Co., Ltd. - Initial implementation
 *******************************************************************************/
package org.eclipse.che.wsagent.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import org.eclipse.che.ApiEndpointAccessibilityChecker;
import org.eclipse.che.ApiEndpointProvider;
import org.eclipse.che.EventBusURLProvider;
import org.eclipse.che.UriApiEndpointProvider;
import org.eclipse.che.UserTokenProvider;
import org.eclipse.che.api.auth.oauth.OAuthTokenProvider;
import org.eclipse.che.api.core.notification.WSocketEventBusClient;
import org.eclipse.che.api.core.rest.ApiInfoService;
import org.eclipse.che.api.core.rest.CoreRestModule;
import org.eclipse.che.api.core.util.FileCleaner.FileCleanerModule;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitUserResolver;
import org.eclipse.che.api.git.LocalGitUserResolver;
import org.eclipse.che.api.project.server.ProjectApiModule;
import org.eclipse.che.api.ssh.server.HttpSshServiceClient;
import org.eclipse.che.api.ssh.server.SshServiceClient;
import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.everrest.CheAsynchronousJobPool;
import org.eclipse.che.git.impl.jgit.JGitConnectionFactory;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.github.server.inject.GitHubModule;
import org.eclipse.che.plugin.java.server.rest.WsAgentURLProvider;
import org.eclipse.che.plugin.machine.artik.keyworddoc.KeywordDocsService;
import org.eclipse.che.plugin.machine.artik.replication.RsyncService;
import org.eclipse.che.plugin.machine.artik.replication.shell.JsonValueHelperFactory;
import org.eclipse.che.plugin.maven.generator.archetype.ArchetypeGenerator;
import org.eclipse.che.plugin.maven.server.inject.MavenModule;
import org.eclipse.che.security.oauth.RemoteOAuthTokenProvider;
import org.everrest.core.impl.async.AsynchronousJobPool;
import org.everrest.core.impl.async.AsynchronousJobService;
import org.everrest.guice.ServiceBindingHelper;

import javax.inject.Named;
import java.net.URI;

/**
 * @author Evgen Vidolob
 */
@DynaModule
public class WsAgentModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiInfoService.class);
        bind(org.eclipse.che.plugin.machine.artik.replication.PushToDeviceService.class);
        bind(org.eclipse.che.plugin.machine.artik.discovery.DeviceDiscoveryService.class);
        bind(KeywordDocsService.class);


        bind(RsyncService.class).asEagerSingleton();
        install(new FactoryModuleBuilder().build(JsonValueHelperFactory.class));

        bind(PreferenceDao.class).to(org.eclipse.che.RemotePreferenceDao.class);

        bind(OAuthTokenProvider.class).to(RemoteOAuthTokenProvider.class);
        bind(SshServiceClient.class).to(HttpSshServiceClient.class);

        bind(org.eclipse.che.plugin.ssh.key.script.SshKeyProvider.class)
                .to(org.eclipse.che.plugin.ssh.key.script.SshKeyProviderImpl.class);

        install(new CoreRestModule());
        install(new FileCleanerModule());
        install(new ProjectApiModule());
        install(new MavenModule());
        install(new GitHubModule());
        install(new org.eclipse.che.swagger.deploy.DocsModule());
        install(new org.eclipse.che.api.debugger.server.DebuggerModule());

        bind(ArchetypeGenerator.class);

        bind(GitUserResolver.class).to(LocalGitUserResolver.class);
        bind(GitConnectionFactory.class).to(JGitConnectionFactory.class);

        bind(AsynchronousJobPool.class).to(CheAsynchronousJobPool.class);
        bind(ServiceBindingHelper.bindingKey(AsynchronousJobService.class, "/async/{ws-id}")).to(AsynchronousJobService.class);

        bind(String.class).annotatedWith(Names.named("api.endpoint")).toProvider(ApiEndpointProvider.class);
        bind(URI.class).annotatedWith(Names.named("api.endpoint")).toProvider(UriApiEndpointProvider.class);
        bind(String.class).annotatedWith(Names.named("user.token")).toProvider(UserTokenProvider.class);
        bind(WSocketEventBusClient.class).asEagerSingleton();

        bind(String.class).annotatedWith(Names.named("event.bus.url")).toProvider(EventBusURLProvider.class);
        bind(ApiEndpointAccessibilityChecker.class);

        bind(String.class).annotatedWith(Names.named("wsagent.endpoint"))
                          .toProvider(WsAgentURLProvider.class);
    }

    //it's need for WSocketEventBusClient and in the future will be replaced with the property
    @Named("notification.client.event_subscriptions")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] eventSubscriptionsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }

    //it's need for EventOriginClientPropagationPolicy and in the future will be replaced with the property
    @Named("notification.client.propagate_events")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] propagateEventsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }
}
