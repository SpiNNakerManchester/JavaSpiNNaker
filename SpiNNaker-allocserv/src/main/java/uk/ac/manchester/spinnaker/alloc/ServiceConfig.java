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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

/**
 * Builds the Spring beans in the application that are not auto-detected. There
 * are no public methods in this class that can be called by non-framework code.
 * <h3>Critical service properties:</h3>
 * <dl>
 * <dt>num.threads
 * <dd>The number of threads to use in the master thread pool. Defaults to
 * {@code 16} ({@link #POOL_SIZE})
 * <dt>cxf.rest.path
 * <dd>The location of the CXF service resource in HTTP space in the servlet.
 * Defaults to {@code /} ({@link #REST_PATH})
 * </dl>
 */
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
@PropertySource("classpath:service.properties")
@EnableScheduling
@SpringBootApplication
@ApplicationPath("spalloc")
public class ServiceConfig extends Application {
	private static final Logger log = getLogger(ServiceConfig.class);

	/**
	 * The thread pool. The rest of the application expects there to be a single
	 * such pool.
	 *
	 * @param numThreads
	 *            The size of the pool. From
	 *            {@code spring.task.scheduling.pool.size} property.
	 * @return The set up thread pool bean.
	 */
	@Bean(destroyMethod = "shutdown")
	@DependsOn("databaseEngine")
	ScheduledExecutorService scheduledThreadPoolExecutor(
			@Value("${spring.task.scheduling.pool.size}") int numThreads,
			TerminationNotifyingThreadFactory threadFactory) {
		return newScheduledThreadPool(numThreads, threadFactory);
	}

	/**
	 * Set up mapping of java.util.Instant to/from JSON. Critical!
	 *
	 * @see <a href="https://stackoverflow.com/q/38168507/301832">Stack
	 *      Overflow</a>
	 */
	@Bean("ObjectMapper")
	JsonMapper mapper() {
		return JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(KEBAB_CASE).build();
	}

	@Bean("JSONProvider")
	JacksonJsonProvider jsonProvider(ObjectMapper mapper) {
		JacksonJsonProvider provider = new JacksonJsonProvider();
		provider.setMapper(mapper);
		return provider;
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
	 * @param jsonProvider
	 *            The JSON object serializer/deserializer
	 * @param bus
	 *            The CXF core
	 * @return The REST service core, configured.
	 */
	@Bean
	@ConditionalOnWebApplication
	@DependsOn("JSONProvider")
	Server jaxRsServer(SpallocServiceAPI service, Executor executor, Bus bus) {
		JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setAddress("/");
		factory.setBus(bus);
		factory.setServiceBeans(asList(service));
		factory.setProviders(new ArrayList<>(
				ctx.getBeansWithAnnotation(Provider.class).values()));
		Server s = factory.create();
		s.getEndpoint().setExecutor(executor);
		return s;
	}

	// TODO application security model
	// TODO administration interface (loading DB description, managing boards)

	@Autowired
	private ApplicationContext ctx;

	/**
	 * Log what beans are actually there, ignoring the bits and pieces of
	 * framework. Useful for debugging!
	 */
	private void logBeans() {
		if (log.isInfoEnabled()) {
			log.info("beans defined: {}", asList(ctx.getBeanDefinitionNames()));
		}
	}

	/**
	 * Sets up everything before we enter service.
	 */
	@PostConstruct
	private void readyForService() {
		logBeans();
	}

	public static void main(String[] args) {
		SpringApplication.run(ServiceConfig.class, args);
	}
}
