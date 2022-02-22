<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
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
<jsp:include page="../head.jsp">
	<jsp:param value="Create Local Group" name="title"/>
</jsp:include>
<body>

<h1>Create Group</h1>

<form:form method="POST" modelAttribute="group">
	<form:label path="name">Group Name: </form:label>
	<form:input path="name" type="text" />
	<br>
	<form:label path="quota">Group Quota: </form:label>
	<form:input path="quota" type="text" id="quotainput" />
	&nbsp;
	<form:label path="quotad">Is quota enabled? </form:label>
	<form:checkbox path="quotad" onchange="quotachange(this.value);" />
	<script type="text/javascript">
		function quotachange(value) {
			document.getElementById("quotainput").disabled = !value;
		}
	</script>
	<p>
	<input type="submit" value="Create" />
</form:form>

<jsp:include page="footer.jsp" />
</body>
</html>
