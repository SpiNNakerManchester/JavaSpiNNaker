<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
	<jsp:param value="Spalloc Machine" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Machine</h1>
<table class="lineTitle">
<tr>
	<th>Name:</th>
	<td>${machine.name }</td>
</tr>
<tr>
	<th>Tags:</th>
	<td><c:forEach items="${machine.tags }" var="tag" varStatus="loop">
	${tag }<c:if test="${!loop.last }">,</c:if>
	</c:forEach></td>
</tr>
<tr>
	<th>Dimensions:</th>
	<td>${machine.width }&times;${machine.height }</td>
</tr>
<tr>
	<th>In-use:</th>
	<td>${machine.numInUse }</td>
</tr>
<tr>
	<th>Jobs:</th>
	<td>${machine.jobs.size() }</td>
</tr>
</table
<tr>
<p>
<em>TODO: live machine layout</em>
</p>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
