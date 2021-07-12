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
	<jsp:param value="An error occurred" name="title"/>
</jsp:include>
<body>

<h1>Error</h1>
<pre>
${ error }
</pre>

<h3>Please enter the correct details</h3>
<table>
    <tr>
        <td><a href="${ baseuri }">Retry</a></td>
    </tr>
</table>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
