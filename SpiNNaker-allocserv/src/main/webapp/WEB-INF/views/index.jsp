<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>
<jsp:include page="head.jsp">
	<jsp:param value="Spalloc Service" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Service</h1>
<sec:authorize access="hasRole('ADMIN')">
<p>
    <a href="admin/">Service Administration</a>
</p>
</sec:authorize>
<sec:authorize access="hasRole('READER')">
<h2>Machines</h2>
<p>
	<a href="machines/">List</a>
</p>
<h2>Jobs</h2>
<p>
	<a href="jobs/">List</a>
</p>
</sec:authorize>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
