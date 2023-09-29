<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
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

<%--
This is a USER form; all users may access this to alter their own password.
--%>

<jsp:include page="head.jsp">
	<jsp:param value="Change Password" name="title"/>
</jsp:include>
<body>

<h1>Change Password</h1>

<form:form method="POST" modelAttribute="user">
	User Name: <c:out value="${ user.username }" escapeXml="true" />
	<br>
	<form:label path="oldPassword">Old Password: </form:label>
	<form:input path="oldPassword" type="password" />
	<p>
	<form:label path="newPassword">New Password: </form:label>
	<form:input path="newPassword" type="password" />
	<br>
	<form:label path="newPassword2">New Password (again): </form:label>
	<form:input path="newPassword2" type="password" />
	<p>
	<input type="submit" value="Change Password" />
</form:form>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
