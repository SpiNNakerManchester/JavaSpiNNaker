<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
Copyright (c) 2021-2023 The University of Manchester

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
<footer>
<hr>
<a href="${ machineUri }">Manage Machines</a>
&mdash;
<a href="${ boardsUri }">Manage Boards</a>
&mdash;
<a href="${ usersUri }">Manage Users</a>
&mdash;
<a href="${ groupsUri }">Manage Groups</a>
<br>
<a href="<c:url value='/spalloc/system/' />">Main page</a>
&mdash;
<a href="<c:url value='/spalloc/system/change_password' />">Change Password</a>
&mdash;
<a href="<c:url value='/spalloc/system/perform_logout' />">Log out</a>
</footer>
