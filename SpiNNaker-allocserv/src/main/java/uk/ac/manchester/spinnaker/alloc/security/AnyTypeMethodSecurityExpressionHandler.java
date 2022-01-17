/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Component;

/**
 * Because Spring otherwise can't apply {@link PostFilter} to {@link Stream}.
 *
 * @author Donal Fellows
 * @see <a href="https://stackoverflow.com/q/66107075/301832">Stack Overflow</a>
 */
@Component
class AnyTypeMethodSecurityExpressionHandler
		extends DefaultMethodSecurityExpressionHandler
		implements MethodSecurityExpressionHandler {
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
		// Java 8 language profile makes this a little messy
		List<T> a = new ArrayList<T>();
		target.ifPresent(a::add);
		return ((Stream<T>) super.filter(a.stream(), expr, ctx)).findFirst();
	}
}
