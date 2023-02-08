/*
 * Copyright (c) 2014-2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.System.setProperty;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;
import static org.apache.cxf.phase.Phase.RECEIVE;
import static org.apache.cxf.transport.http.AbstractHTTPDestination.HTTP_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;
import javax.validation.ValidationException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AuthProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.HistoricalDataProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.KeepaliveProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.QuotaProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI;
import uk.ac.manchester.spinnaker.alloc.db.TerminationNotifyingThreadFactory;
import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.alloc.security.SecurityConfig;
import uk.ac.manchester.spinnaker.alloc.web.MvcConfig;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI;

/**
 * Builds the Spring beans in the application that are not auto-detected. There
 * are no public methods in this class that can be called by non-framework code.
 *
 * @see SecurityConfig
 * @author Donal Fellows
 */
@Import({JaxRsConfig.class, MvcConfig.class, SecurityConfig.class})
@PropertySource("classpath:service.properties")
@EnableScheduling
@EnableAsync
@SpringBootApplication
@ApplicationPath("spalloc")
@Role(ROLE_APPLICATION)
@EnableConfigurationProperties(SpallocProperties.class)
public class ServiceConfig extends Application {
	static {
		// DISABLE IPv6 SUPPORT; SpiNNaker can't use it and it's a pain
		setProperty("java.net.preferIPv4Stack", "false");
	}

	private static final Logger log = getLogger(ServiceConfig.class);

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
	@Role(ROLE_INFRASTRUCTURE)
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
	@Role(ROLE_INFRASTRUCTURE)
	JsonMapper mapper() {
		return JsonMapper.builder().findAndAddModules()
				.addModule(new JavaTimeModule())
				.addModule(new Jdk8Module())
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
	@Role(ROLE_INFRASTRUCTURE)
	JacksonJsonProvider jsonProvider(ObjectMapper mapper) {
		var provider = new JacksonJsonProvider();
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
	 * @param protocolCorrector
	 *            How to correct the protocol
	 * @return A factory instance
	 */
	@Bean
	@Prototype
	@Role(ROLE_INFRASTRUCTURE)
	JAXRSServerFactoryBean rawFactory(SpringBus bus,
			ProtocolUpgraderInterceptor protocolCorrector) {
		var factory = new JAXRSServerFactoryBean();
		factory.setStaticSubresourceResolution(true);
		factory.setAddress("/");
		factory.setBus(bus);
		factory.setProviders(List.of(
				ctx.getBeansWithAnnotation(Provider.class).values()));
		factory.setFeatures(List.of(new OpenApiFeature()));
		factory.setInInterceptors(List
				.of(new JAXRSBeanValidationInInterceptor(), protocolCorrector));
		return factory;
	}

	/**
	 * Handles the upgrade of the CXF endpoint protocol to HTTPS when the
	 * service is behind a reverse proxy like nginx which might be handling
	 * SSL/TLS for us. In theory, CXF should do this itself; this is the kind of
	 * obscure rubbish that frameworks are supposed to handle for us. In
	 * practice, it doesn't. Yuck.
	 *
	 * @author Donal Fellows
	 */
	@Component
	@Role(ROLE_SUPPORT)
	static class ProtocolUpgraderInterceptor
			extends AbstractPhaseInterceptor<Message> {
		ProtocolUpgraderInterceptor() {
			super(RECEIVE);
		}

		private static final String FORWARDED_PROTOCOL = "x-forwarded-proto";

		private static final String ENDPOINT_ADDRESS =
				"org.apache.cxf.transport.endpoint.address";

		@SuppressWarnings("unchecked")
		private Map<String, List<String>> getHeaders(Message message) {
			// If we've got one of these in the message, it's of this type
			return (Map<String, List<String>>) message
					.getOrDefault(PROTOCOL_HEADERS, Map.of());
		}

		@Override
		public void handleMessage(Message message) throws Fault {
			var headers = getHeaders(message);
			if (headers.getOrDefault(FORWARDED_PROTOCOL, List.of())
					.contains("https")) {
				upgradeEndpointProtocol(
						(ServletRequest) message.get(HTTP_REQUEST));
			}
		}

		/**
		 * Upgrade the endpoint address if necessary; it's dumb that it might
		 * need it, but we're being careful.
		 */
		private void upgradeEndpointProtocol(ServletRequest request) {
			var addr = (String) request.getAttribute(ENDPOINT_ADDRESS);
			if (addr != null && addr.startsWith("http:")) {
				request.setAttribute(ENDPOINT_ADDRESS,
						addr.replace("http:", "https:"));
			}
		}
	}

	@Provider
	@Component
	@Role(ROLE_INFRASTRUCTURE)
	static class ValidationExceptionMapper
			implements ExceptionMapper<ValidationException> {
		@Override
		public Response toResponse(ValidationException exception) {
			var message = exception.getMessage().replaceAll(".*:\\s*", "");
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
	@Bean(destroyMethod = "destroy")
	@ConditionalOnWebApplication
	@DependsOn("JSONProvider")
	Server jaxRsServer(SpallocServiceAPI service, AdminAPI adminService,
			Executor executor, JAXRSServerFactoryBean factory) {
		factory.setServiceBeans(List.of(service, adminService));
		var s = factory.create();
		s.getEndpoint().setExecutor(executor);
		return s;
	}

	/**
	 * Used for making paths to things in the service in contexts where we can't
	 * ask for the current request session to help. An example of such a context
	 * is in configuring the access control rules on the paths, which has to be
	 * done prior to any message session existing.
	 *
	 * @author Donal Fellows
	 */
	@Component
	@Role(ROLE_SUPPORT)
	public static final class URLPathMaker {
		@Value("${spring.mvc.servlet.path}")
		private String mvcServletPath;

		@Value("${cxf.path}")
		private String cxfPath;

		/**
		 * Create a full local URL for the system components, bearing in mind
		 * the deployment configuration.
		 *
		 * @param suffix
		 *            The URL suffix; <em>should not</em> start with {@code /}
		 * @return The full local URL (absolute path, without protocol or host)
		 */
		public String systemUrl(String suffix) {
			var prefix = mvcServletPath;
			if (!prefix.endsWith("/")) {
				prefix += "/";
			}
			prefix += "system/";
			return prefix + suffix;
		}

		/**
		 * Create a full local URL for web service components, bearing in mind
		 * the deployment configuration.
		 *
		 * @param suffix
		 *            The URL suffix; <em>should not</em> start with {@code /}
		 * @return The full local URL (absolute path, without protocol or host)
		 */
		public String serviceUrl(String suffix) {
			var prefix = cxfPath;
			if (!prefix.endsWith("/")) {
				prefix += "/";
			}
			return prefix + suffix;
		}
	}

	@Autowired
	private ApplicationContext ctx;

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	AllocatorProperties allocatorProperties(SpallocProperties properties) {
		return properties.getAllocator();
	}

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	KeepaliveProperties keepaliveProperties(SpallocProperties properties) {
		return properties.getKeepalive();
	}

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	HistoricalDataProperties historyProperties(SpallocProperties properties) {
		return properties.getHistoricalData();
	}

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	QuotaProperties quotaProperties(SpallocProperties properties) {
		return properties.getQuota();
	}

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	TxrxProperties txrxProperties(SpallocProperties properties) {
		return properties.getTransceiver();
	}

	// Exported so we can use a short name in SpEL in @Scheduled annotations
	@Bean
	@Role(ROLE_SUPPORT)
	AuthProperties authProperties(SpallocProperties properties) {
		return properties.getAuth();
	}

	@Bean
	@Role(ROLE_SUPPORT)
	ViewResolver jspViewResolver() {
		var bean = new InternalResourceViewResolver() {
			@Override
			protected AbstractUrlBasedView buildView(String viewName)
					throws Exception {
				var v = super.buildView(viewName);
				var path = v.getUrl();
				if (path.startsWith("/WEB-INF/views/system")) {
					var path2 = path.replaceFirst("/system", "");
					log.debug("rewrote [{}] to [{}]", path, path2);
					v.setUrl(path2);
				}
				return v;
			}
		};
		bean.setPrefix("/WEB-INF/views/");
		bean.setSuffix(".jsp");
		return bean;
	}

	/**
	 * Log what beans are actually there, ignoring the bits and pieces of
	 * framework. Useful for debugging!
	 */
	private void logBeans() {
		if (log.isDebugEnabled()) {
			log.debug("beans defined: {}",
					List.of(ctx.getBeanDefinitionNames()));
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
