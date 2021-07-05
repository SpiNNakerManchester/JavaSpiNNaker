<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>User List</title>
</head>
<body>

<h1>Index page</h1>
<table>
<c:if test="${not empty userlist}">
    <c:forEach items="${userlist}" var="item">
    <tr>
    <td>${item.key }
    <td><a href="${item.value }">Details</a>
    </c:forEach>
</table>
</c:if>
