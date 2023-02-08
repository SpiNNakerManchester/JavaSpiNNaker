<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<jsp:include page="head.jsp">
	<jsp:param value="Log in" name="title"/>
</jsp:include>
<body>

<h1>Login</h1>
<c:if test="${ error != null }">
	<p>Please try again...</p>
</c:if>
<form name="f" method="POST"
		action="<c:url value='/spalloc/system/perform_login' />">
	<sec:csrfInput />
	<table>
		<tr>
			<td>User:</td>
			<td><input type="text" name="username" value=""></td>
		</tr>
		<tr>
			<td>Password:</td>
			<td><input type="password" name="password" /></td>
		</tr>
		<tr>
			<td colspan="2">
				<input name="submit" type="submit" value="Log In" />
			</td>
		</tr>
	</table>
</form>
<p>
<form method="GET"
		action="<c:url value='/spalloc/system/perform_oidc/auth/hbp-ebrains' />">
	Alternatively, <input type="submit" value="log in with HBP/EBRAINS">
</form>

</body>
</html>
