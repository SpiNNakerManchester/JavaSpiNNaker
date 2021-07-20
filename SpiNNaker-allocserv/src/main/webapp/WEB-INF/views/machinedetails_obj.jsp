<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
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
<script>
machine = (
<json:object>
	<json:property name="name" value="${machine.name}" />
	<json:property name="width" value="${machine.width}" />
	<json:property name="height" value="${machine.height}" />
	<json:property name="num_in_use" value="${machine.numInUse}" />
	<json:array name="tags" items="${machine.tags}" />
	<json:array name="jobs" items="${machine.jobs}" var="job">
		<json:object>
			<json:property name="id" value="${job.id}" />
			<json:property name="url" value="${job.url}" />
			<c:if test="${job.owner.present}">
				<json:property name="id" value="${job.owner.get()}" />
			</c:if>
			<json:array name="boards" items="${job.boards}" var="board">
				<json:object>
					<json:object name="triad">
						<json:property name="x" value="${ board.x }" />
						<json:property name="y" value="${ board.y }" />
						<json:property name="z" value="${ board.z }" />
					</json:object>
					<json:object name="physical">
						<json:property name="cabinet" value="${ board.cabinet }" />
						<json:property name="frame" value="${ board.frame }" />
						<json:property name="board" value="${ board.board }" />
					</json:object>
					<json:object name="network">
						<json:property name="address" value="${ board.address }" />
					</json:object>
				</json:object>
			</json:array>
		</json:object>
	</json:array>
</json:object>
);
</script>
