<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<jsp:include page="../head.jsp">
	<jsp:param value="User Details" name="title" />
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<c:choose>
	<c:when test="${ user.internal }">
		<h1>Spalloc Local User Details</h1>
	</c:when>
	<c:otherwise>
		<h1>Spalloc OpenID User Details</h1>
	</c:otherwise>
</c:choose>

<form:form method="POST" modelAttribute="user">
	<form:label path="userName">User Name: </form:label>
	<form:input path="userName" type="text"/>
	<form:select path="trustLevel">
		<form:option value="">pick a level</form:option>
		<form:options items="${ trustLevels }"/>
	</form:select>
	<c:if test="${ not empty user.openIdSubject }">
		<span class="minordetail">
			(OpenID Subject: <code><c:out value="${ user.openIdSubject }" /></code>)
		</span>
	</c:if>
	<br>
	<c:choose>
		<c:when test="${ user.internal }">
			<form:label path="password">Password: </form:label>
			<form:input path="password" type="password" />
			<form:label path="hasPassword">Has Password: </form:label>
			<form:checkbox path="hasPassword" />
		</c:when>
		<c:otherwise>
			<%-- Apparently, we can't have readonly checkboxes. The reasons why
				appear murky but are approximately "because Google doesn't feel
				like it and is happy doing horrible workarounds" according to
				https://github.com/whatwg/html/issues/2311

				I could scream.
			--%>
			<label for="password">Password: </label>
			<input id="password" type="password" disabled="disabled" />
			<form:input path="password" type="hidden" />
			<label for="hasPassword">Has Password: </label>
			<input id="hasPassword" type="checkbox" value="false" disabled="disabled" />
			<form:input path="hasPassword" type="hidden" />
		</c:otherwise>
	</c:choose>
	<br>
	<form:label path="enabled">Is enabled? </form:label>
	<form:checkbox path="enabled"/>
	<form:label path="locked">Is temporarily locked? </form:label>
	<form:checkbox path="locked"/>
	<br>
	Last successful login: <span id="lastSuccessfulLogin">${ user.lastSuccessfulLogin }</span>
	<br>
	Last failed login: <span id="lastFailedLogin">${ user.lastFailedLogin }</span>
	<script defer="defer">
		prettyTimestamp("lastSuccessfulLogin");
		prettyTimestamp("lastFailedLogin");
	</script>
	<p>
	<input type="submit" value="Update" />
</form:form>
<p>
<form method="POST" action="${ deleteUri }">
	<sec:csrfInput />
	<input type="submit" class="warningbutton" value="Delete this user" />
</form>

<jsp:include page="footer.jsp" />
</body>
</html>
