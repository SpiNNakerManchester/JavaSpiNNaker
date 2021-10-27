<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
<footer>
<hr>
<a href="${ machineUri }">Machines</a>
&mdash;
<a href="${ boardsUri }">Boards</a>
&mdash;
<a href="${ usersUri }">Users</a>
<br>
<a href='<c:url value="/system/"/>'>Main page</a>
&mdash;
<a href='<c:url value="/system/change_password"/>'>Change Password</a>
&mdash;
<a href='<c:url value="/system/perform_logout"/>'>Log out</a>
</footer>
