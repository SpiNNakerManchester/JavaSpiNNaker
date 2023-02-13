<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<%--
Copyright (c) 2021-2023 The University of Manchester

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
<jsp:include page="../head.jsp">
	<jsp:param value="Create Local User" name="title"/>
</jsp:include>
<body>

<h1>Create User</h1>

<form:form method="POST" modelAttribute="user">
	<form:hidden path="internal"/>
	<form:label path="userName">User Name: </form:label>
	<form:input path="userName" type="text"/>
	<form:select path="trustLevel">
		<form:option value="">pick a level</form:option>
		<form:options items="${ trustLevels }"/>
	</form:select>
	<br>
	<form:label path="password">Password: </form:label>
	<form:input path="password" type="password" />
	<form:label path="hasPassword">Has Password: </form:label>
	<form:checkbox path="hasPassword"/>
	<br>
	<form:label path="enabled">Is enabled? </form:label>
	<form:checkbox path="enabled"/>
	<p>
	<input type="submit" value="Create" />
</form:form>

<p>
<em>Note that users created with this form are not added to any group by default.</em>

<jsp:include page="footer.jsp" />
</body>
</html>
