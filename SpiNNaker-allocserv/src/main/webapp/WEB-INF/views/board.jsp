<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<!--
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
-->
<head>
    <meta http-equiv="Content-Type"
      content="text/html; charset=UTF-8">
    <title>Board Management</title>
</head>
<body>
<h1>Spalloc Board Management</h1>

<form:form method="POST" modelAttribute="board">
    <form:label path="machineName">Machine Name: </form:label>
    <form:input path="machineName" type="text"/>
    <br>
    Triad coordinates:
    <form:label path="x">X: </form:label>
    <form:input path="x" type="number"/>
    <form:label path="y">Y: </form:label>
    <form:input path="y" type="number"/>
    <form:label path="z">Z: </form:label>
    <form:input path="z" type="number"/>
    <br>
    Physical coordinates:
    <form:label path="cabinet">Cabinet: </form:label>
    <form:input path="cabinet" type="number"/>
    <form:label path="frame">Frame: </form:label>
    <form:input path="frame" type="number"/>
    <form:label path="board">Board: </form:label>
    <form:input path="board" type="number"/>
    <br>
    Network coordinates:
    <form:label path="address">IP Address: </form:label>
    <form:input path="address" type="text"/>
    <br>
    <c:if test="${ enabled != null }">
		<form:label path="enabled"></form:label>
		<form:checkbox path="enabled"/>
		<p>
		<input type="submit" value="Change State" />
    </c:if>
    <c:if test="${ enabled == null }">
		<input type="submit" value="Look Up Board" />
    </c:if>
</form:form>
<button onclick="window.location.href='${ baseuri }'">Clear form</button>

</body>
</html>
