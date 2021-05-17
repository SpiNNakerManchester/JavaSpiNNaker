/*
 * Copyright (c) 2014-2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.alloc.web.SpallocAPI;

/**
 * Builds the Spring beans in the application.
 * <h3>Critical service properties:</h3>
 * <dl>
 * <dt>num.threads
 * <dd>The number of threads to use in the master thread pool. Defaults to
 * {@code 16}
 * <dt>cxf.rest.path
 * <dd>The location of the CXF service resource in HTTP space in the servlet.
 * Defaults to {@code /}
 * </dl>
 */
@Configuration
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
@ComponentScan
@PropertySource("classpath:service.properties")
@EnableScheduling
public class ServiceConfig {
	private static final Logger log = getLogger(ServiceConfig.class);

	private static final String REST_PATH = "/";

	private static final int POOL_SIZE = 16;

	/**
	 * The thread pool.
	 *
	 * @param numThreads
	 *            The size of the pool. From {@code num.threads} property;
	 *            defaults to {@code 16}.
	 * @return The set up thread pool bean.
	 */
	@Bean(destroyMethod = "shutdown")
	public ScheduledExecutorService scheduledThreadPoolExecutor(
			@Value("${num.threads:" + POOL_SIZE + "}") int numThreads) {
		return newScheduledThreadPool(numThreads);
	}

	/**
	 * The JAX-RS interface. Note that this is only used when not in test mode.
	 *
	 * @param restPath
	 *            Where to deploy services in resource-space
	 * @param service
	 *            The service implementation
	 * @param executor
	 *            The thread pool
	 * @param bus
	 *            The CXF core
	 * @return The REST service core, configured.
	 */
	@Bean
	@Profile("!unittest")
	@DependsOn("bus")
	public Server jaxRsServer(
			@Value("${cxf.rest.path:" + REST_PATH + "}") String restPath,
			SpallocAPI service, Executor executor, SpringBus bus) {
		final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setAddress(restPath);
		factory.setBus(bus);
		factory.setServiceBeans(asList(service));
		factory.setProviders(asList(new JacksonJsonProvider()));
		Server s = factory.create();
		s.getEndpoint().setExecutor(executor);
		return s;
	}

	@Autowired
	private ApplicationContext ctx;

	/**
	 * Log what beans are actually there, ignoring the bits and pieces of
	 * framework. Useful for debugging!
	 */
	@PostConstruct
	private void logBeans() {
		if (log.isInfoEnabled()) {
			log.info("beans defined: {}", stream(ctx.getBeanDefinitionNames())
					// Remove Spring internal beans
					.filter(name -> !name.contains("springframework")
							&& !name.contains("apache"))
					.collect(toList()));
		}
	}
}
