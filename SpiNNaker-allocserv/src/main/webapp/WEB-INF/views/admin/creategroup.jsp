<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
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
