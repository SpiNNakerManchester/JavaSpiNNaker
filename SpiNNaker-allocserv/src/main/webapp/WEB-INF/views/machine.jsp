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
    <title>Machine Management</title>
</head>
<body>
<h1>Machine Import</h1>
<c:if test="${ not empty definedMachines }">
	<c:forEach items="${definedMachines}" var="item">
		Machine called "${ item.name }" (${ item.width }&times;${ item.height }) defined.<br>
	</c:forEach>
</c:if>
<form method="POST" enctype="multipart/form-data">
    <table>
        <tr>
            <td><label path="file">Select a configuration file to upload</label></td>
            <td><input type="file" name="file" /></td>
        </tr>
        <tr>
            <td><input type="submit" value="Submit" /></td>
        </tr>
    </table>
</form>
<jsp:include page="footer.jsp" />
</body>
</html>
