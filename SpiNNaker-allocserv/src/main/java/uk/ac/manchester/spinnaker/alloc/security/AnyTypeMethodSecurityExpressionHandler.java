/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.util.Objects.isNull;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.annotation.Role;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Because Spring otherwise can't apply {@link PostFilter} to {@link Optional}.
 *
 * @author Donal Fellows
 * @see <a href="https://stackoverflow.com/q/66107075/301832">Stack Overflow</a>
 */
@Component
@Role(ROLE_INFRASTRUCTURE)
@UsedInJavadocOnly(PostFilter.class)
class AnyTypeMethodSecurityExpressionHandler
		extends DefaultMethodSecurityExpressionHandler {
	@Override
	public Object filter(Object target, Expression expr,
			EvaluationContext ctx) {
		if (isNull(target)) {
			// We can handle this case here!
			return null;
		}
		if (target instanceof Collection || target.getClass().isArray()
				|| target instanceof Map || target instanceof Stream) {
			return super.filter(target, expr, ctx);
		}
		if (target instanceof Optional) {
			return filterOptional((Optional<?>) target, expr, ctx);
		} else {
			return filterOptional(Optional.of(target), expr, ctx).orElse(null);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Optional<T> filterOptional(Optional<T> target, Expression expr,
			EvaluationContext ctx) {
		return ((Stream<T>) super.filter(target.stream(), expr, ctx))
				.findFirst();
	}
}
