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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
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
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

/**
 * Builds the Spring beans in the application that are not auto-detected. There
 * are no public methods in this class that can be called by non-framework code.
 *
 * @see SecurityConfig
 */
@Import({JaxRsConfig.class, SecurityConfig.class})
@PropertySource("classpath:service.properties")
@EnableScheduling
@SpringBootApplication
@ApplicationPath("spalloc")
public class ServiceConfig extends Application {
	private static final Logger log = getLogger(ServiceConfig.class);

	static {
		// Workaround for checkstyle being stupid
		List.class.getClass();
	}

	/**
	 * The thread pool. The rest of the application expects there to be a single
	 * such pool.
	 *
	 * @param numThreads
	 *            The size of the pool. From
	 *            {@code spring.task.scheduling.pool.size} property.
	 * @param threadFactory
	 *            How threads that service the pool are made.
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
	 * @return a configured JSON mapper
	 * @see <a href="https://stackoverflow.com/q/38168507/301832">Stack
	 *      Overflow</a>
	 */
	@Bean("ObjectMapper")
	JsonMapper mapper() {
		return JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(KEBAB_CASE).build();
	}

	/**
	 * How we map between JSON and Java classes.
	 *
	 * @param mapper
	 *            The core mapper.
	 * @return A provider.
	 */
	@Bean("JSONProvider")
	JacksonJsonProvider jsonProvider(ObjectMapper mapper) {
		JacksonJsonProvider provider = new JacksonJsonProvider();
		provider.setMapper(mapper);
		return provider;
	}

	/**
	 * A factory for JAX-RS servers. This is a <em>prototype</em> bean; you get
	 * a new instance each time.
	 * <p>
	 * You should call {@link JAXRSServerFactoryBean#setServiceBeans(List)
	 * setServiceBeans(...)} on it, and then
	 * {@link JAXRSServerFactoryBean#create() create()}. You might also need to
	 * call {@link JAXRSServerFactoryBean#setAddress(String) setAddress(...)}.
	 *
	 * @param bus
	 *            The CXF bus.
	 * @return A factory instance
	 */
	@Bean
	@Scope(SCOPE_PROTOTYPE)
	JAXRSServerFactoryBean rawFactory(SpringBus bus) {
		JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setStaticSubresourceResolution(true);
		factory.setAddress("/");
		factory.setBus(bus);
		factory.setProviders(new ArrayList<>(
				ctx.getBeansWithAnnotation(Provider.class).values()));
		factory.setFeatures(asList(new OpenApiFeature()));
		factory.setInInterceptors(asList(
				new JAXRSBeanValidationInInterceptor()));
		return factory;
	}

	@Provider
	@Component
	static class ValidationExceptionMapper
			implements ExceptionMapper<ValidationException> {
		@Override
		public Response toResponse(ValidationException exception) {
			String message = exception.getMessage().replaceAll(".*:\\s*", "");
			return status(BAD_REQUEST).type(TEXT_PLAIN).entity(message).build();
		}
	}

	/**
	 * The JAX-RS interface. Note that this is only used when not in test mode.
	 *
	 * @param service
	 *            The service implementation
	 * @param adminService
	 *            The admin service
	 * @param executor
	 *            The thread pool
	 * @param factory
	 *            A factory used to make servers.
	 * @return The REST service core, configured.
	 */
	@Bean
	@ConditionalOnWebApplication
	@DependsOn("JSONProvider")
	Server jaxRsServer(SpallocServiceAPI service, AdminAPI adminService,
			Executor executor, JAXRSServerFactoryBean factory) {
		factory.setServiceBeans(asList(service, adminService));
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

	/**
	 * Spring Boot entry point.
	 *
	 * @param args
	 *            Command line arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(ServiceConfig.class, args);
	}
}
