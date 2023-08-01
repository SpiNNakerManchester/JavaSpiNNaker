/*
 * Copyright (c) 2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi;

import static org.slf4j.LoggerFactory.getLogger;


import org.slf4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import uk.ac.manchester.spinnaker.nmpi.web.RemoteSpinnakerBeans;


@SuppressWarnings({
	"checkstyle:ParameterNumber", "checkstyle:VisibilityModifier"
})
public abstract class TestSupport {

	@Configuration
	@TestPropertySource("classpath:nmpi.properties")
	@ComponentScan(basePackageClasses = RemoteSpinnakerBeans.class)
	class Config {

	}

	protected static final Logger log = getLogger(TestSupport.class);

}
